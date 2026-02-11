package controllers

import model.domain.objects.{Account, Transaction}
import model.domain.types.AccountType
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{mapping, text}

import javax.inject.*
import play.api.mvc.*

case class AccountFormData(ID: String, accType: String)
object AccountFormData {
  def unapply(formData: AccountFormData): Option[(String, String)] = Some((formData.ID, formData.accType))
}

@Singleton
class AccountsController @Inject() (val cc: MessagesControllerComponents)
    extends MessagesAbstractController(cc)
    with play.api.i18n.I18nSupport {

  private val logger = Logger(this.getClass)
  
  val AccountForm: Form[AccountFormData] = Form(
    mapping(
      "ID" -> text,
      "accType" -> text
    )(AccountFormData.apply)(AccountFormData.unapply)
  )

  def list() = Action { implicit request: Request[AnyContent] =>
    logger.info("in accounts list")
    val accs = Account.getAccountsFromDB
    Ok(views.html.accounts.list(accs))
  }

  def add() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.accounts.add())
  }

  def view(account: String) = Action { implicit request: MessagesRequest[AnyContent] =>
    val txns = Transaction.getTxnsFromDB(account)
    val acc = Account.getSpecificAccount(account)
    AccountForm.bind(Map(
      "ID" -> acc.getID,
      "accType" -> acc.getType.toString
    ))
    Ok(views.html.accounts.view(txns.toSet, acc, AccountForm))
  }

  def changeType() = Action { implicit request: MessagesRequest[AnyContent] =>
    AccountForm.bindFromRequest().fold(
      formWithErrors => {
        val accID = formWithErrors.data("ID")
        val accType = formWithErrors.data("accType")
        val acc = Account.getSpecificAccount(accID)
        val txns = Transaction.getTxnsFromDB(accID)
        Redirect(routes.AccountsController.view(accID))
          .flashing("error" -> s"Invalid account type: $accType")
      },
      formData => {
        val accountID = formData.ID
        val accType = AccountType.parse(formData.accType)
        Account.updateAccountType(accountID, accType)
        Redirect(routes.AccountsController.view(accountID))
          .flashing("success" -> s"Account type changed to $accType")
      }
    )
  }

  /*
  def bankTransactions() = Action { implicit request: Request[AnyContent] =>
    val txns = Transaction.getTxnsFromDB()
    Ok(views.html.accounts.bank(txns))
  }
  */
}
