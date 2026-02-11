package controllers

import model.database.DB_Factory
import play.api.libs.json.Json
import play.filters.csrf.{CSRF, CSRFAddToken}

import javax.inject.*
import play.api.mvc.*

@Singleton
class SettingsController @Inject() (
    val controllerComponents: ControllerComponents,
    addToken: CSRFAddToken
)
    extends BaseController {

  def index() = addToken(Action { implicit request: Request[AnyContent] =>
    val token = CSRF.getToken(using request).map(_.value).getOrElse("")
    Ok(views.html.settings.index(token))
  })
  
  def clearDB() = Action { implicit request: Request[AnyContent] =>
    try {
      DB_Factory.withDB { (db, state) => db.dropDB(state) }
      Ok(Json.obj("status" -> "success", "message" -> "Database cleared"))
    } catch {
      case e: Exception => InternalServerError(Json.obj("status" -> "error", "message" -> e.getMessage))
    }
  }
}
