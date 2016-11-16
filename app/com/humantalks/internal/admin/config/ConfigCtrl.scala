package com.humantalks.internal.admin.config

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.persons.Person
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{ Result, RequestHeader, Controller }

import scala.concurrent.Future

case class ConfigCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    configDbService: ConfigDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val configForm = Form(Config.fields)

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    for {
      configList <- configDbService.find()
    } yield Ok(views.html.list(configList))
  }

  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    formView(Ok, configForm, None)
  }

  def doCreate() = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    configForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      configData => configDbService.create(configData, req.identity.id).map {
        case (_, id) =>
          Redirect(routes.ConfigCtrl.get(id))
      }
    )
  }

  def get(id: Config.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(configDbService)(id) { config =>
      Future.successful(Ok(views.html.detail(config)))
    }
  }

  def update(id: Config.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(configDbService)(id) { config =>
      formView(Ok, configForm.fill(config.data), Some(config))
    }
  }

  def doUpdate(id: Config.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    configForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(configDbService)(id) { config => formView(BadRequest, formWithErrors, Some(config)) },
      configData => CtrlHelper.withItem(configDbService)(id) { config =>
        configDbService.update(config, configData, req.identity.id).map {
          case _ => Redirect(routes.ConfigCtrl.get(id))
        }
      }
    )
  }

  def doUpdateValue(id: Config.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    CtrlHelper.getFieldValue(req.body, "value").map { value =>
      configDbService.setValue(id, value, req.identity.id).map {
        case _ =>
          Redirect(routes.ConfigCtrl.find()).flashing("success" -> "Configuration mise à jour")
      }
    }.getOrElse {
      Future.successful(Redirect(routes.ConfigCtrl.find()).flashing("error" -> "Bad request: you should set 'value' in form parameters !"))
    }
  }

  def doDelete(id: Config.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    configDbService.delete(id).map {
      case Left(_) => Redirect(routes.ConfigCtrl.get(id)).flashing("error" -> s"Unable to delete config")
      case Right(res) => Redirect(routes.ConfigCtrl.find()).flashing("success" -> "Config deleted")
    }
  }

  private def formView(status: Status, configForm: Form[Config.Data], configOpt: Option[Config])(implicit request: RequestHeader, userOpt: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.form(configForm, configOpt)))
  }
}
