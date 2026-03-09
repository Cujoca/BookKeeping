package controllers

import model.database.DB_Factory
import model.domain.objects.Transaction

import javax.inject.*
import play.api.mvc.*

/**
 * Controller which directs most user needs surrounding Transactions
 *
 * @param controllerComponents: dw about it
 */
@Singleton
class TransactionsController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  private val logger = play.api.Logger(this.getClass)

  /**
   * Lists all transactions
   */
  def list() = Action { implicit request: Request[AnyContent] =>
    logger.info("Listing all transactions")
    val txns = Transaction.getTxnsFromDB()
    Ok(views.html.transactions.list(txns))
  }

  /**
   * adds a transaction, currently unused
   *
   * TODO: Actually implement
   */
  def add() = Action { implicit request: Request[AnyContent] =>
    logger.info("Attempting to add a transaction")
    Ok(views.html.transactions.add())
  }

  /**
   * Views details about a specific transaction
   *
   * @param txnID: Int, internal DB id of the transaction to view
   */
  def view(txnID: Int) = Action { implicit request: Request[AnyContent] =>
    logger.info(s"Getting details about txn $txnID")

    val txns = Transaction.getTxnAndMatched(txnID)
    Ok(views.html.transactions.view(txns._1, txns._2.toSet))
  }

  /**
   * Called by the transactions/view page to get possible matches for a given transaction.
   *
   * @param txnID: ID of transaction to match
   * @return : Stringified JSON set of possible matches
   */
  def getMatches(txnID: Int) = Action { implicit request: Request[AnyContent] =>
    logger.info(s"finding possible matches for: ${txnID.toString}")

    val result = Transaction.stringifyJsonSet(Transaction.possibleMatches(txnID))
    Ok(result)
  }

  /**
   * After user looks at possible matches, calls this to officially match
   * the two transactions to eachother.
   *
   * TODO:  make it so that a non-expense txn can only be
   *        matched to an expense txn, and vice versa
   *        Might be best done when presenting transactions
   * @param id      : Int, internal DB id of the txn that we're matching to
   * @param matchID : Int, internal DB id of the txn that the user has chosen
   * @return
   */
  def foundMatch(id: Int, matchID: Int) = Action { implicit request: Request[AnyContent] =>
    logger.info(s"Attempting to match $id with $matchID")

    val result = Transaction.matchTxns(id, matchID)
    val txns = Transaction.getTxnAndMatched(id)

    if (result) Ok(views.html.transactions.view(txns._1, txns._2.toSet))
    else throw Exception("something went wrong matching transactions")
  }
}