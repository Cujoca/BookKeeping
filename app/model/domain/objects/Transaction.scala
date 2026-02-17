package model.domain.objects

import model.database.DB_Factory
import org.apache.commons.csv.CSVRecord
import play.api.Logger

import java.sql.ResultSet
import java.time.LocalDate
import scala.collection.mutable

/**
 * Core object containing data for transactions. Basis for entire program
 * @param fromAcc : String, account that the transaction is from
 * @param toAcc   : String, account that the transaction is to
 * @param date    : LocalDate, date of transaction
 * @param txnType : String, type or method of transaction,
 *                  EG: cheque, cash, expense, bill payment, etc
 * @param id      : String, unique identifier for transaction,
 *                  EG: cheque number, invoice number, etc
 * @param name    : String, name of transaction, EG: supplier name, customer name, etc
 * @param desc    : String, description of transaction, EG: memo, invoice description, etc
 * @param amount  : Double, amount of transaction, EG: amount of cheque, amount of invoice, etc
 * @param tax     : Double, tax amount of transaction if applicable, is 0 otherwise,
 *                  EG: tax amount of invoice, etc
 */
class Transaction ( fromAcc: String, 
                    toAcc: String, 
                    date: LocalDate, 
                    txnType: String, 
                    id: String,
                    name: String, 
                    desc: String,
                    amount: Double,
                    tax: Double = 0.0,
                    dbID: Int = -1) {

  // could probably remove these, but keeping for now for ease of debugging
  def getFromAcc: String = fromAcc
  def getToAcc: String = toAcc
  def getDate: LocalDate = date
  def getTxnType: String = txnType
  def getID: String = id
  def getName: String = name
  def getDesc: String = desc
  def getAmount: Double = amount
  def getTax: Double = tax
  def getDBID: Int = dbID

  // shit is weird, probably should make a better version
  def changeTax(tax: Double): Transaction = new Transaction(fromAcc, toAcc, date, txnType, id, name, desc, amount, tax)

  /**
   * Self-explanatory
   * @return : String, formatted string representation of transaction, EG:
   */
  def mkString =
    s"""
       |Transaction: $fromAcc -> $toAcc on $date with $txnType of $amount
       |""".stripMargin


  def stringifyJson: String = {
    s"""
       |{"FromAcc":"$fromAcc",
       |"ToAcc":"$toAcc",
       |"Date":"$date",
       |"Type":"$txnType",
       |"ID":"$id",
       |"Name":"$name",
       |"Description":"$desc",
       |"Amount":$amount,
       |"Tax":$tax,
       |"DBID":$dbID}
       |""".stripMargin
  }
}

/**
 * Companion object for Transaction,
 * contains factory methods for creating Transaction objects from CSV files.
 */
object Transaction {

  private val logger = Logger(this.getClass)
  
  /**
   * Factory method for creating a Transaction object from a CSV file.
   *
   * @param record : CSVRecord from parsed file, containing entries in order:
   *             1 -> from Account
   *             7 -> to Account
   *             2 -> Date
   *             3 -> Type
   *             4 -> ID
   *             5 -> Name
   *             6 -> Description
   *             8 -> Amount
   * @return : Transaction
   */
  def TxnFromCSV(record: CSVRecord): Transaction = {
    new Transaction(
      record.get("Distribution account"),                       // from account
      record.get("Split account"),                              // to account
      Date.fromString(record.get("Transaction date")),          // date
      record.get("Transaction type"),                           // type
      if (record.get("#").isEmpty) "N/A" else record.get("#"),  // ID
      record.get("name").replace("\'", "\\\'"),                 // name
      record.get("Memo/Description"),                           // description
      record.get("Amount").replaceAll(",", "").toDouble)        // amount
  }
  
  def TxnFromPostgres (result: ResultSet): Transaction = {
    
    val tax = if (result.getDouble("Tax") == 0) 0.0 else result.getDouble("Tax")
    new Transaction(
      result.getString("From_ACC"),           // from account
      result.getString("To_ACC"),             // to account
      Date.fromString(result.getString("Date")), // date
      result.getString("Type"),               // type
      result.getString("ID"),                 // invoice/check ID (if applicable)
      result.getString("Name"),               // name (if applicable)
      result.getString("Description"),        // description (if applicable)
      result.getDouble("Amount"),             // amount
      tax,                                    // tax (if applicable)
      result.getInt("Transaction_ID")         // database ID
    )
  }

  /**
   * Factory method for creating a Transaction object from a CSV file.
   *
   * @param record: CSVRecord from parsed file, containing entries in order:
   *                Bill No,
   *                Bill Date,
   *                Supplier,
   *                Invoice Number,
   *                Line Description,
   *                Method,
   *                Line Amount (grand total),
   *                Line Tax Amount,
   *                Delivery (unused FOR NOW),
   *                Fuel Surcharge (unused FOR NOW),
   *                Freight Total (unused FOR NOW),
   *                Purchase Only (subtotal, no tax),
   *                Account,
   *                Line Tax Code (type, assume HST ON),
   *                Due Date (unused),
   *                Month (unused),
   *                Total (unused),
   *                HST (unused),
   *                HST Total (unused)
   * @return
   */
  def TxnFromCSV_Invoice (record: CSVRecord): Transaction = {

    // in case tax not applicable
    val tax = if (record.get("Line Tax Amount").isEmpty) 0.0
              else record.get("Line Tax Amount")
                .trim
                .slice(2, record.get("Line Tax Amount").length-1)
                .replaceAll(",", "")
                .toDouble

    // this and tax above are done here rather than in the transaction def
    // because it clutters the code
    val amount = record.get("Line Amount")
      .trim
      .slice(2, record.get("Line Amount").length-1)
      .replaceAll(",", "")
      .toDouble

    new Transaction(
      "Expenses",                                 // from account
      record.get("Account"),                      // to account
      Date.fromString(record.get("Bill Date")),   // date
      "Expense",                                  // type
      record.get("Bill No"),                      // ID
      record.get("Supplier"),                     // name
      record.get("Line Description"),             // description
      amount,                                     // amount
      tax                                         // tax
    )
  }

  /**
   * Gets all transactions from the database, optionally filtering by account.
   * @param account: String, optional account to filter by, if empty, returns all transactions
   * @return : mutable.HashSet[Transaction], set of transactions from database
   */
  def getTxnsFromDB (account: String = ""): mutable.HashSet[Transaction] = {
    val out = new mutable.HashSet[Transaction]()
    println("getting transactions from db")
    logger.info("getting transactions")
    val res = DB_Factory.withDB { (db, state) => 
      if (account.nonEmpty) db.getTxnsByID(state, account)
      else db.getTxns(state, account) 
    }
    if (res.isEmpty) {throw new Error("something went wrong with the getTxnsFromDB function")}
    while (res.get.next()) {
      val tax = if (res.get.getDouble("tax") == 0) 0.0 else res.get.getDouble("tax")
      out.add(
        new Transaction(
          res.get.getString("From_ACC"),              // from account
          res.get.getString("To_ACC"),                // to account
          Date.fromString(res.get.getString("Date")), // date
          res.get.getString("Type"),                  // type
          res.get.getString("ID"),                    // invoice/check ID (if applicable)
          res.get.getString("Name"),                  // name (if applicable)
          res.get.getString("Description"),           // description (if applicable)
          res.get.getDouble("Amount"),                // amount
          tax,                                        // tax (if applicable)
          res.get.getInt("Transaction_ID")            // database ID
        )
      )
    }
    out
  }
  
  def getTxnAndMatched (id: Int): (Transaction, mutable.HashSet[Transaction]) = {

    val resMain = DB_Factory.withDB((db, state) => db.getSpecificTxn(state, id))
    val resMatched = DB_Factory.withDB((db, state) => db.getTxnsExpenseMatch(state, id))

    resMain.get.next()
    val txn = new Transaction(
      resMain.get.getString("From_ACC"),              // from account
      resMain.get.getString("To_ACC"),                // to account
      Date.fromString(resMain.get.getString("Date")), // date
      resMain.get.getString("Type"),                  // type
      resMain.get.getString("ID"),                    // invoice/check ID (if applicable)
      resMain.get.getString("Name"),                  // name (if applicable)
      resMain.get.getString("Description"),           // description (if applicable)
      resMain.get.getDouble("Amount"),                // amount
      resMain.get.getDouble("Tax"),                   // tax (if applicable)
      resMain.get.getInt("Transaction_ID")            // database ID
    )

    val matched = new mutable.HashSet[Transaction]()

    if (resMatched.isDefined) {
      while (resMatched.get.next()) {
        matched.add(
          new Transaction(
            resMatched.get.getString("From_ACC"), // from account
            resMatched.get.getString("To_ACC"), // to account
            Date.fromString(resMatched.get.getString("Date")), // date
            resMatched.get.getString("Type"), // type
            resMatched.get.getString("ID"), // invoice/check ID (if applicable)
            resMatched.get.getString("Name"), // name (if applicable)
            resMatched.get.getString("Description"), // description (if applicable)
            resMatched.get.getDouble("Amount"), // amount
            resMatched.get.getDouble("Tax"), // tax (if applicable)
            resMatched.get.getInt("Transaction_ID") // database ID
          )
        )
      }
    }
    (txn, matched)
  }

  /**
   * Grabs all transactions that could be matched to the provided transaction (have the same amount)
   * 
   * @param txnID: Int, ID of transaction to match to
   * @return : Set[Transaction], set of transactions that could be matched to the provided transaction
   */
  def possibleMatches(txnID: Int): Set[Transaction] = {
    val txnRes = DB_Factory.withDB((db, state) => db.getSpecificTxn(state, txnID)).get
    txnRes.next()
    val txnAmount = txnRes.getDouble("Amount")

    val matchRes = DB_Factory.withDB((db, state) => db.getPossibleMatches(state, txnAmount)).get
    val txns = new scala.collection.mutable.HashSet[Transaction]()

    while (matchRes.next()) {txns.add(Transaction.TxnFromPostgres(matchRes))}
    
    txns.filter(txn => txn.getDBID != txnID).toSet

  }

  def matchTxns(txnID: Int, matchID: Int): Boolean = {
    try { DB_Factory.withDB((db, state) => db.matchTxns(state, txnID, matchID)) }
    catch { case e: Exception => return false }
    true
  }
  
  /**
   * Gets all accounts from the database. So far only gathers total of each account.
   * @return : HashMap[String, Double], map of accounts to total balance
   */
  def getReportFromDB (): mutable.HashMap[String, Double] = {
    val result = DB_Factory.withDB { (db, state) => db.getReport(state) }
    if (result.isEmpty) {throw new Error("something went wrong with the getReportFromDB function")}
    val out = new mutable.HashMap[String, Double]()
    while (result.get.next()) { out.put(result.get.getString("from_acc"), result.get.getDouble("total")) }
    out
  }

  def stringifyJsonSet (txns: Set[Transaction]): String =
    txns.foldLeft("{\n\"transactions\":[\n")((acc, txn) => acc + txn.stringifyJson + ",").dropRight(1) + "]}"
}
