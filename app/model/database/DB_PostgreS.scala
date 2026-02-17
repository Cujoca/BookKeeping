package model.database


import model.domain.objects.Transaction
import play.api.Logger

import java.sql.{Connection, ResultSet, Statement}

class DB_PostgreS  (override val url: String) extends DB_Interface {

  private val logger = Logger(this.getClass)

  /**
   * Connection for querying and updating DB through
   * TODO: need to actually use this instead of implicitly creating every time
   */
  override val conn: java.sql.Connection = createConn()

  /**
   * Starts up the DB with all necessary tables
   *
   * @param state: Statement, for interacting with DB
   * @return: Option[ResultSet] is just None since there's no return for creating
   */
  override def initDb (state: Statement): Option[ResultSet] = {

    var SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS Acc (
         |  Account_ID    integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
         |  Account_Name  VARCHAR(250)  NOT NULL UNIQUE,
         |  Account_Type  VARCHAR(250)  NOT NULL
         |)
         |""".stripMargin
    conn.prepareStatement(SQL).executeUpdate()

    SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS Txn (
         |  Transaction_ID    integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
         |  Account_ID        integer REFERENCES Acc(Account_ID),
         |  From_ACC          VARCHAR(250)  NOT NULL,
         |  To_ACC            VARCHAR(250)  NOT NULL,
         |  Date              DATE,
         |  Type              VARCHAR(250) NOT NULL,
         |  ID                VARCHAR(250),
         |  Name              VARCHAR(250),
         |  Description       VARCHAR(250),
         |  Amount            decimal(15,2),
         |  Tax               decimal(15,2)
         |  )""".stripMargin
    conn.prepareStatement(SQL).executeUpdate()

    SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS Match (
         |  Expense_ID integer REFERENCES Txn(Transaction_ID),
         |  Matched_ID integer REFERENCES Txn(Transaction_ID)
         |)""".stripMargin
    conn.prepareStatement(SQL).executeUpdate()
    None
  }

  /**
   * Adds a new transaction into the DB
   *
   * @param state     :Statement, for interacting with DB
   * @param trade     :Transaction, transaction to be added
   * @param accountID :String, account that owns this transaction
   * @return          :Option[ResultSet],
   */
  override def addTxn (state: Statement, trade: Transaction): Option[ResultSet] = {
    // Use a parameterized statement to safely handle quotes and avoid SQL injection
    val SQL =
      """
        |INSERT INTO Txn (
        |  From_ACC,
        |  To_ACC,
        |  Date,
        |  Type,
        |  ID,
        |  Name,
        |  Description,
        |  Amount
        |) VALUES (?,?,?,?,?,?,?,?)
        |""".stripMargin

    val ps = conn.prepareStatement(SQL)

    ps.setString(1, trade.getFromAcc)
    ps.setString(2, trade.getToAcc)
    ps.setDate(3, java.sql.Date.valueOf(trade.getDate))
    ps.setString(4, trade.getTxnType)
    ps.setString(5, trade.getID)
    ps.setString(6, trade.getName)
    ps.setString(7, trade.getDesc)
    // Store amount with correct precision
    ps.setBigDecimal(8, new java.math.BigDecimal(trade.getAmount).setScale(2, java.math.RoundingMode.HALF_UP))
    ps.executeUpdate()

    None
  }

  def addTxnBatch (state: Statement, trades: List[Transaction], accs: Map[String, Int]): Option[ResultSet] = {

    if (trades.isEmpty) return None

    val sql =
      """
        |INSERT INTO Txn (
        |  Account_ID,
        |  From_ACC,
        |  To_ACC,
        |  Date,
        |  Type,
        |  ID,
        |  Name,
        |  Description,
        |  Amount,
        |  Tax
        |) VALUES (?,?,?,?,?,?,?,?,?,?)
        |""".stripMargin

    val prevAutoCommit = conn.getAutoCommit
    conn.setAutoCommit(false)
    val ps = conn.prepareStatement(sql)
    try {
      trades.foreach { trade =>
        ps.setInt(1, accs(trade.getFromAcc))
        ps.setString(2, trade.getFromAcc)
        ps.setString(3, trade.getToAcc)
        ps.setDate(4, java.sql.Date.valueOf(trade.getDate))
        ps.setString(5, trade.getTxnType)
        ps.setString(6, trade.getID)
        ps.setString(7, trade.getName)
        ps.setString(8, trade.getDesc)
        // Store amount with correct precision
        ps.setBigDecimal(9, new java.math.BigDecimal(trade.getAmount).setScale(2, java.math.RoundingMode.HALF_UP))
        ps.setBigDecimal(10, new java.math.BigDecimal(trade.getTax).setScale(2, java.math.RoundingMode.HALF_UP))
        ps.addBatch()
      }
      ps.executeBatch()
      conn.commit()
      None
    } catch {
      case e: Exception =>
        conn.rollback()
        throw e
    } finally {
      try ps.close() catch {
        case _: Throwable =>
      }
      conn.setAutoCommit(prevAutoCommit)
    }
  }

  override def getTxns (statement: Statement, account: String = ""): Option[ResultSet] = {
    logger.info("getting transactions")
    var SQL = ""
    if (account.isEmpty) {
      SQL =
        s"""
           |select * from Txn order by Transaction_ID
           |""".stripMargin
    } else {
      SQL =
        s"""
           |select * from Txn
           |  where From_ACC='$account'
           |  order by Transaction_ID
           |""".stripMargin
    }

    if (SQL.isEmpty){
      throw new Error("something went wrong with the getTxns function")
      None
    }
    Option(statement.executeQuery(SQL))
  }
  
  override def getSpecificTxn (statement: Statement, ID: Int): Option[ResultSet] = {
    val SQL =
      s"""
         |select * from Txn where Transaction_ID=$ID
         |""".stripMargin
      
    Some(statement.executeQuery(SQL))
  }

  override def getTxnsByID (statement: Statement, ID: String): Option[ResultSet] = {
    var SQL =
      s"""
         |select * from Acc
         |where Account_ID=$ID""".stripMargin

    val res = statement.executeQuery(SQL)
    res.next()
    val name = res.getString("Account_Name")

    getTxns(statement, name)
  }

  override def getReport (statement: Statement): Option[ResultSet] = {

    val SQL =
      s"""
         |select from_acc, sum(amount) as total from txn
         |group by from_acc;
         |""".stripMargin
    Some(statement.executeQuery(SQL))
  }

  override def getAccountsFromTxn (statement: Statement): Option[ResultSet] = {
    val SQL =
      s"""
         |select distinct from_acc from Txn
         |""".stripMargin

    Some(statement.executeQuery(SQL))
  }

  override def getAccounts (statement: Statement): Option[ResultSet] = {
    val SQL =
      s"""
         |select * from Acc;
         |""".stripMargin

    Some(statement.executeQuery(SQL))
  }

  override def getAccountByID(state: Statement, ID: String): Option[ResultSet] = {
    val sql = s"select * from Acc where Account_ID = $ID"
    Option(state.executeQuery(sql))
  }

  override def updateAccountType(state: Statement, ID: String, newType: String): Option[ResultSet] = {
    val sql = s"update Acc set Account_Type = '$newType' where Account_ID = ${ID.toInt}"
    state.executeUpdate(sql)
    None
  }

  override def addAccounts (statement: Statement, accounts: Set[String]): Option[ResultSet] = {

    logger.info("adding accounts")

    val sql =
      """
        |INSERT INTO Acc (
        |  Account_Name,
        |  Account_Type
        |) VALUES (?, ?)
        | ON CONFLICT (Account_Name) DO NOTHING;
        |""".stripMargin

    val prevAutoCommit = conn.getAutoCommit
    conn.setAutoCommit(false)
    val ps = conn.prepareStatement(sql)
    try {
      accounts.foreach { acc =>
        ps.setString(1, acc)
        ps.setString(2, "?")
        // Store amount with correct precision
        ps.addBatch()
      }
      ps.executeBatch()
      conn.commit()
      logger.info("added accounts")
      None
    } catch {
      case e: Exception =>
        conn.rollback()
        throw e
    } finally {
      try ps.close() catch {
        case _: Throwable =>
      }
      conn.setAutoCommit(prevAutoCommit)
    }
  }

  override def getTxnsExpenseMatch (statement: Statement, id: Int): Option[ResultSet] = {
    val SQL =
      s"""
         |select * from Match where Expense_ID = $id;
         |""".stripMargin
         
    Some(statement.executeQuery(SQL))
  }
  
  def getBankAccounts (statement: Statement) = {
    
  }

  override def matchTxns(state: Statement, txnID: Int, matchID: Int): Option[ResultSet] = {
    val SQL =
      s"""
         |INSERT INTO Match (
         |  Expense_ID,
         |  Matched_ID
         |) VALUES (?,?)
         |""".stripMargin

    val ps = conn.prepareStatement(SQL)
    ps.setInt(1, matchID)
    ps.setInt(2, txnID)

    None
  }
  
  override def getPossibleMatches (statement: Statement, amount: Double): Option[ResultSet] = {
    val SQL = s"select * from Txn where amount = $amount"
    Option(statement.executeQuery(SQL))
  }
}