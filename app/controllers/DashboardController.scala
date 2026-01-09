package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class DashboardController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  def dashboard() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.dashboard())
  }
}
