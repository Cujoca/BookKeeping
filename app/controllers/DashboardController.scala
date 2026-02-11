package controllers

import model.domain.objects.Transaction

import javax.inject.*
import play.api.mvc.*

@Singleton
class DashboardController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  def dashboard() = Action { implicit request: Request[AnyContent] =>
    val report = Transaction.getReportFromDB()
    Ok(views.html.dashboard(report.toMap))
  }
}
