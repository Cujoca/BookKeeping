package controllers

import model.database.DB_Factory
import model.domain.objects.Transaction

import javax.inject.*
import play.api.mvc.*

@Singleton
class TransactionsController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  private val logger = play.api.Logger(this.getClass)

  def list() = Action { implicit request: Request[AnyContent] =>
    val txns = Transaction.getTxnsFromDB()
    Ok(views.html.transactions.list(txns))
  }

  def add() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.transactions.add())
  }

  def view(txnID: Int) = Action { implicit request: Request[AnyContent] =>
    
    val txns = Transaction.getTxnAndMatched(txnID)
    
    Ok(views.html.transactions.view(txns._1, txns._2.toSet))
  }

  /**
   * Called by the transactions/view page to get possible matches for a given transaction.
   * @param txnID: ID of transaction to match
   * @return : Stringified JSON set of possible matches
   */
  def getMatches(txnID: Int) = Action { implicit request: Request[AnyContent] =>
    println("in match request")
    logger.info(txnID.toString)

    val result = Transaction.stringifyJsonSet(Transaction.possibleMatches(txnID))

    logger.info(result)

    Ok(result)
  }
}
