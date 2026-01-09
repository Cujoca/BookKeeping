package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class AccountsController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  def list() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.accounts.list())
  }

  def add() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.accounts.add())
  }
}
