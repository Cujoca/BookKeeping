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

    // create account table
    var SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS Acc (
         |  Account_ID    integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
         |  Account_Name  VARCHAR(250)  NOT NULL UNIQUE,
         |  Account_Type  VARCHAR(250)  NOT NULL
         |)
         |""".stripMargin
    conn.prepareStatement(SQL).executeUpdate()

    // create transaction table
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

    /* create matched table
     * this table is a synthetic comprised of the DBIDs of two transactions, indicating
     * that one is directly correlated to another. One transaction may be matched to
     * multiple, though each individual match is its own entry in the match table.
     */
    SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS Match (
         |  Match_ID integer REFERENCES Txn(Transaction_ID),
         |  Expense_ID integer REFERENCES Txn(Transaction_ID)
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

  /**
   * Adds all transactions from a list in a batch statement. Meant to be used when uploading
   * many transactions at once, such as from an uploaded csv file.
   *
   * @param state : Statement, jdbc connection statement
   * @param trades: List[Transaction], list of txns to upload
   * @param accs  : Map[String, Int],  Accounts were uploaded before so their ID can be used as a foreign key
   * @return      : Option[ResultSet], result of DB operation, is None since DB returns nothing, error is thrown if
   *                                   unsuccessful
   */
  def addTxnBatch (state: Statement, trades: List[Transaction], accs: Map[String, Int]): Option[ResultSet] = {
    logger.info(s"processing ${trades.size} transactions into SQL and sending to DB")
    val startTime = System.currentTimeMillis()

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
      logger.info(s"Finished uploading, took ${System.currentTimeMillis()-startTime}ms")
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

  /**
   * Gets all transactions, assumes all if no account specified, otherwise will grab
   * only those associated with a specific account
   *
   * @param statement : Statement,  jdbc connection statement
   * @param account   : String, account to grab txns from, will grab all if empty (default)
   * @return          : Option[ResultSet], result of DB operation, will be None if operation was unsuccessful. Though
   *                                       it doesn't matter since error will be thrown before the None is returned.
   *                                       Maybe change that for error handling in the future.
   */
  override def getTxns (statement: Statement, account: String = ""): Option[ResultSet] = {
    logger.info("getting transactions")
    val startTime = System.currentTimeMillis()

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
      logger.info(s"grabbing txns ended in failure, took ${System.currentTimeMillis()-startTime}ms")
      throw new Error("something went wrong with the getTxns function")
      None
    }
    logger.info(s"grabbing txns ended in success, took ${System.currentTimeMillis()-startTime}ms")
    Option(statement.executeQuery(SQL))
  }

  /**
   * Grabs a specifc transaction by ID from the DB
   * @param statement: Statement, jdbc connection statement
   * @param ID       : Int, internal DBID of transaction
   * @return         : Option[ResultSet], result of DB operation
   */
  override def getSpecificTxn (statement: Statement, ID: Int): Option[ResultSet] = {
    logger.info(s"Getting txn $ID from DB")
    val startTime = System.currentTimeMillis()
    val SQL =
      s"""
         |select * from Txn where Transaction_ID=$ID
         |""".stripMargin

    logger.info(s"Obtained txn $ID, took ${System.currentTimeMillis()-startTime}ms")
    Some(statement.executeQuery(SQL))
  }

  /**
   * So far just used to get a specific transaction by ID
   * TODO: perhaps can use to grab a set of transactions so I don't need to
   *       create a new connection instance every fucking time?
   *
   * @param ID : Int, internal DBID of transaction
   * @return
   */
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

  /**
   * Useless for now, ignore
   *
   * @param statement
   * @return
   */
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
         |select * from Match where Match_ID != $id;
         |""".stripMargin
         
    Some(statement.executeQuery(SQL))
  }
  
  def getBankAccounts (statement: Statement) = {
    
  }

  override def matchTxns(state: Statement, txnID: Int, matchID: Int): Option[ResultSet] = {

    val SQL =
      s"""
         |INSERT INTO Match (
         |  Match_ID,
         |  Expense_ID
         |) VALUES (?,?)
         |""".stripMargin

    val ps = conn.prepareStatement(SQL)
    ps.setInt(2, matchID)
    ps.setInt(1, txnID)
    println(ps.toString)
    ps.executeUpdate()

    None
  }
  
  override def getPossibleMatches (statement: Statement, amount: Double): Option[ResultSet] = {
    val SQL = s"select * from Txn where amount = $amount"
    Option(statement.executeQuery(SQL))
  }
}