package com.humantalks.common.services.meetup

import com.humantalks.common.Conf
import com.humantalks.common.services.meetup.models.{ EventCreate, VenueCreate }
import com.humantalks.internal.meetups.{ MeetupDbService, Meetup }
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.{ Venue, VenueDbService }
import global.Contexts

import scala.concurrent.Future

case class MeetupSrv(conf: Conf, ctx: Contexts, meetupApi: MeetupApi, venueDbService: VenueDbService, meetupDbService: MeetupDbService) {
  import Contexts.wsToEC
  import ctx._

  def create(meetup: Meetup, venueOpt: Option[Venue], talkList: List[Talk], personList: List[Person], by: Person.Id): Future[Either[List[String], Meetup.MeetupRef]] = {
    val group = conf.Meetup.group
    meetup.meetupRef.map { ref =>
      Future.successful(Left(List(s"Meetup reference found for ${meetup.data.title} (already created)")))
    }.getOrElse {
      withMeetupRef(group, venueOpt, by).flatMap { venueWithMeetupRefOpt =>
        val eventCreate = EventCreate.from(meetup, venueWithMeetupRefOpt, talkList, personList)
        meetupApi.createEvent(group, eventCreate).flatMap {
          case Right(event) => meetupDbService.setMeetupRef(meetup.id, Meetup.MeetupRef(group, event.id.toLong, announced = false), by).map { _ =>
            Right(Meetup.MeetupRef(group, event.id.toLong, announced = false))
          }
          case Left(errs) => Future.successful(Left(errs))
        }
      }
    }
  }

  def update(meetup: Meetup, venueOpt: Option[Venue], talkList: List[Talk], personList: List[Person], by: Person.Id): Future[Either[List[String], Meetup.MeetupRef]] = {
    meetup.meetupRef.map { ref =>
      withMeetupRef(ref.group, venueOpt, by).flatMap { venueWithMeetupRefOpt =>
        val eventCreate = EventCreate.from(meetup, venueWithMeetupRefOpt, talkList, personList)
        meetupApi.updateEvent(ref.group, ref.id, eventCreate).map {
          case Right(event) => Right(ref)
          case Left(errs) => Left(errs)
        }
      }
    }.getOrElse {
      Future.successful(Left(List(s"No meetup reference found for ${meetup.data.title}")))
    }
  }

  def announce(meetup: Meetup, by: Person.Id): Future[Either[List[String], Meetup.MeetupRef]] = {
    meetup.meetupRef.map { ref =>
      meetupApi.announceEvent(ref.group, ref.id).flatMap {
        case Right(event) => meetupDbService.setMeetupRef(meetup.id, ref.copy(announced = true), by).map { _ =>
          Right(ref.copy(announced = true))
        }
        case Left(errs) => Future.successful(Left(errs))
      }
    }.getOrElse {
      Future.successful(Left(List(s"No meetup reference found for ${meetup.data.title}")))
    }
  }

  def delete(meetup: Meetup, by: Person.Id): Future[Either[List[String], Unit]] = {
    meetup.meetupRef.map { ref =>
      meetupApi.deleteEvent(ref.group, ref.id).flatMap { res =>
        meetupDbService.unsetMeetupRef(meetup.id, by).map { _ =>
          res
        }
      }
    }.getOrElse {
      Future.successful(Left(List(s"No meetup reference found for ${meetup.data.title}")))
    }
  }

  private def withMeetupRef(group: String, venueOpt: Option[Venue], by: Person.Id): Future[Option[Venue]] =
    venueOpt.map { venue =>
      getVenueMeetupId(group, venue, by).map { res =>
        Some(venue.copy(meetupRef = res.map(_._2)))
      }
    }.getOrElse(Future.successful(None))
  private def getVenueMeetupId(group: String, venue: Venue, by: Person.Id): Future[Option[(Venue.Id, Venue.MeetupRef)]] =
    venue.meetupRef.map(ref => Future.successful(Some((venue.id, ref)))).getOrElse {
      createVenue(group, venue, by).map(_.map(id => (venue.id, id)))
    }
  private def createVenue(group: String, venue: Venue, by: Person.Id): Future[Option[Venue.MeetupRef]] =
    VenueCreate.from(venue).map { venueCreate =>
      meetupApi.createVenue(group, venueCreate).flatMap {
        case Right(res) => venueDbService.setMeetupRef(venue.id, Venue.MeetupRef(group, res.id), by).map { _ =>
          Some(Venue.MeetupRef(group, res.id))
        }
        case Left(err) => Future.successful(None)
      }
    }.getOrElse(Future.successful(None))
}
