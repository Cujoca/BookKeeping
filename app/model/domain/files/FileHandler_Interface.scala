package model.domain.files

import model.database.DB_Factory
import model.domain.objects.{Account, Transaction}
import scala.collection.mutable

/**
 * Strategy pattern for handling different file formats. Everything is assumed to be csv
 * the specific concrete class will have specialized versions of readFile to handle
 * the specific file format.
 */
trait FileHandler_Interface {

  val logger = play.api.Logger(this.getClass)

  /**
   * Reads provided file and returns a set of transactions.
   *
   * @param filePath: String, path to file
   * @return : Set[Transaction], parsed set of transactions
   */
  def readFile(filePath: String): Set[Transaction]

  /**
   * Pre defined function to send data to database.
   * haven't encountered any cases yet where needs to be redefined in concrete class
   *
   * TODO:  when uploading same file why not duplicating transactions?
   *        fixed dupe accounts but transactions are not getting duped
   *
   * @param txns: Set[Transaction], set of transactions to be sent to database
   * @param accs: Set[String], set of accounts to be sent to database
   */
  def sendToDB(txns: Set[Transaction], accs: Set[String]): Unit = {

    DB_Factory.withDB { (db, statement) => {
      db.initDb(statement)
      db.addAccounts(statement, accs)
      val accRes = db.getAccounts(statement).get
      val accountIds = new mutable.HashMap[String, Int]()
      // grab all accounts currently in db (including those from cur file upload)
      while (accRes.next()) {
        accountIds.put(
          accRes.getString("Account_Name"),
          accRes.getInt("Account_ID")
        )
      }
      logger.info(accountIds.toString())
      // specifically send in accountID so can be used as foreign key for txns
      db.addTxnBatch(statement, txns.toList, accountIds.toMap)
      // doesn't do anything, returns None
      None
    }
    }
  }
}
