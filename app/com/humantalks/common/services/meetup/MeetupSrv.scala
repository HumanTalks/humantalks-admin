package com.humantalks.common.services.meetup

import com.humantalks.common.Conf
import com.humantalks.common.services.meetup.models.{ MeetupEventCreate$, MeetupVenueCreate$ }
import com.humantalks.internal.admin.config.ConfigDbService
import com.humantalks.internal.events.{ EventDbService, Event }
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.partners.{ Partner, PartnerDbService }
import global.Contexts

import scala.concurrent.Future
import scala.util.{ Failure, Success }

case class MeetupSrv(
    conf: Conf,
    ctx: Contexts,
    meetupApi: MeetupApi,
    partnerDbService: PartnerDbService,
    eventDbService: EventDbService,
    configDbService: ConfigDbService
) {
  import Contexts.wsToEC
  import ctx._

  def create(event: Event, partnerOpt: Option[Partner], talkList: List[Talk], personList: List[Person], by: Person.Id): Future[Either[List[String], Event.MeetupRef]] = {
    val group = conf.Meetup.group
    event.meetupRef.map { ref =>
      Future.successful(Left(List(s"Meetup reference found for ${event.data.title} (already created)")))
    }.getOrElse {
      withMeetupRef(group, partnerOpt, by).flatMap { partnerWithMeetupRefOpt =>
        configDbService.buildMeetupEventDescription(Some(event), partnerWithMeetupRefOpt, talkList, personList).flatMap {
          case (Success(description), _) =>
            val eventCreate = MeetupEventCreate.from(event, description, partnerWithMeetupRefOpt, talkList, personList)
            meetupApi.createEvent(group, eventCreate).flatMap {
              case Right(meetupEvent) => eventDbService.setMeetupRef(event.id, Event.MeetupRef(group, meetupEvent.id.toLong, announced = false), by).map { _ =>
                Right(Event.MeetupRef(group, meetupEvent.id.toLong, announced = false))
              }
              case Left(errs) => Future.successful(Left(errs))
            }
          case (Failure(e), _) => Future.successful(Left(List(e.getMessage)))
        }
      }
    }
  }

  def update(event: Event, partnerOpt: Option[Partner], talkList: List[Talk], personList: List[Person], by: Person.Id): Future[Either[List[String], Event.MeetupRef]] = {
    event.meetupRef.map { ref =>
      withMeetupRef(ref.group, partnerOpt, by).flatMap { partnerWithMeetupRefOpt =>
        configDbService.buildMeetupEventDescription(Some(event), partnerWithMeetupRefOpt, talkList, personList).flatMap {
          case (Success(description), _) =>
            val eventCreate = MeetupEventCreate.from(event, description, partnerWithMeetupRefOpt, talkList, personList)
            meetupApi.updateEvent(ref.group, ref.id, eventCreate).map {
              case Right(_) => Right(ref)
              case Left(errs) => Left(errs)
            }
          case (Failure(e), _) => Future.successful(Left(List(e.getMessage)))
        }
      }
    }.getOrElse {
      Future.successful(Left(List(s"No meetup reference found for ${event.data.title}")))
    }
  }

  def announce(event: Event, by: Person.Id): Future[Either[List[String], Event.MeetupRef]] = {
    event.meetupRef.map { ref =>
      meetupApi.announceEvent(ref.group, ref.id).flatMap {
        case Right(meetupEvent) => eventDbService.setMeetupRef(event.id, ref.copy(announced = true), by).map { _ =>
          Right(ref.copy(announced = true))
        }
        case Left(errs) => Future.successful(Left(errs))
      }
    }.getOrElse {
      Future.successful(Left(List(s"No meetup reference found for ${event.data.title}")))
    }
  }

  def delete(event: Event, by: Person.Id): Future[Either[List[String], Unit]] = {
    event.meetupRef.map { ref =>
      meetupApi.deleteEvent(ref.group, ref.id).flatMap { res =>
        eventDbService.unsetMeetupRef(event.id, by).map { _ =>
          res
        }
      }
    }.getOrElse {
      Future.successful(Left(List(s"No meetup reference found for ${event.data.title}")))
    }
  }

  private def withMeetupRef(group: String, partnerOpt: Option[Partner], by: Person.Id): Future[Option[Partner]] =
    partnerOpt.map { partner =>
      getVenueMeetupId(group, partner, by).map { res =>
        Some(partner.copy(meetupRef = res.map(_._2)))
      }
    }.getOrElse(Future.successful(None))
  private def getVenueMeetupId(group: String, partner: Partner, by: Person.Id): Future[Option[(Partner.Id, Partner.MeetupRef)]] =
    partner.meetupRef.map(ref => Future.successful(Some((partner.id, ref)))).getOrElse {
      createVenue(group, partner, by).map(_.map(id => (partner.id, id)))
    }
  private def createVenue(group: String, partner: Partner, by: Person.Id): Future[Option[Partner.MeetupRef]] =
    MeetupVenueCreate.from(partner).map { venueCreate =>
      meetupApi.createVenue(group, venueCreate).flatMap {
        case Right(res) => partnerDbService.setMeetupRef(partner.id, Partner.MeetupRef(group, res.id), by).map { _ =>
          Some(Partner.MeetupRef(group, res.id))
        }
        case Left(err) => Future.successful(None)
      }
    }.getOrElse(Future.successful(None))
}
