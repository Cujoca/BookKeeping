package model.database

import model.domain.objects.Transaction
import play.api.libs.Files.logger

import java.sql.{DriverManager, ResultSet, SQLException, Statement}

trait DB_Interface {

  def url: String

  def conn: java.sql.Connection

  /**
   * Initiates a connection to the database
   *
   * @return java.sql.Connection - the connection object
   */
  def createConn (): java.sql.Connection = {
    try {
      val conn = DriverManager.getConnection (url)
      conn
    } catch {
      case e: SQLException => throw new RuntimeException(e)
    }
  }

  def close(): Unit = conn.close()

  def openState(): Statement = conn.createStatement ()

  def dropDB(state: Statement): Option[ResultSet] = {
    logger.info("in dropdb")
    try {
      conn.prepareStatement("drop table if exists Txn").executeUpdate()
      logger.info("deleted txn")
      conn.prepareStatement("drop table if exists Acc").executeUpdate()
      logger.info("deleted acc")
    } catch {
      case e:Exception => println(e)
    }
    logger.info("Database has been cleared")
    None
  }

  def initDb (state: Statement): Option[ResultSet]

  def addTxn (state: Statement, trade: Transaction): Option[ResultSet] = {

    val SQL = {
      s"""
         |INSERT INTO TXN VALUES (
         |  DEFAULT,
         |  '${trade.getFromAcc}',
         |  '${trade.getToAcc}',
         |  '${trade.getDate.toString}',
         |  '${trade.getTxnType}',
         |  ${trade.getID},
         |  '${trade.getName}',
         |  '${trade.getDesc}',
         |  ${trade.getAmount},
         |);""".stripMargin
    }

    Option(state.executeQuery(SQL))
  }

  def getTxns         (state: Statement, account: String = ""): Option[ResultSet]
  
  def getTxnsByID     (state: Statement, ID: String): Option[ResultSet]
  
  def addTxnBatch     (state: Statement, trades: List[Transaction], accRes: Map[String, Int] = Map.empty): Option[ResultSet]

  def getReport       (state: Statement): Option[ResultSet]

  def getAccounts     (state: Statement): Option[ResultSet]
  
  def getAccountByID  (state: Statement, ID: String): Option[ResultSet]
  
  def updateAccountType (state: Statement, ID: String, newType: String): Option[ResultSet]
  
  def getAccountsFromTxn (state: Statement): Option[ResultSet]
  
  def getTxnsExpenseMatch (state: Statement, id: Int): Option[ResultSet]
  
  def getSpecificTxn (state: Statement, ID: Int): Option[ResultSet]
  
  def getPossibleMatches (state: Statement, amount: Double): Option[ResultSet]
  
  def matchTxns (state: Statement, txnID: Int, matchID: Int): Option[ResultSet]

  def addAccounts   (state: Statement, account: Set[String]): Option[ResultSet]

}
