package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class TransactionsController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  def list() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.transactions.list())
  }

  def add() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.transactions.add())
  }
}
