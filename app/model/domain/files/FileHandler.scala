package model.domain.files

import model.database.DB_Factory
import model.domain.objects.Transaction
import org.apache.commons.csv.CSVFormat
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

import java.io.{BufferedReader, FileReader}
import scala.collection.mutable

object FileHandler {
  
  def readFile(filePath: String): Set[Transaction] = {

    val reader = new BufferedReader ( new FileReader (filePath))

    // Configure CSV to use explicit headers since the header row is not the first record
    val format = CSVFormat.EXCEL
      .builder()
      .setHeader(
        "_ignored",
        "Distribution account",
        "Transaction date",
        "Transaction type",
        "#",
        "name",
        "Memo/Description",
        "Split account",
        "Amount",
        "Balance"
      )
      // there is no guaranteed header row to skip
      .setSkipHeaderRecord(false)
      .setIgnoreHeaderCase(true)
      .setTrim(true)
      .get()

    val records = format.parse(reader)

    val txns = records.flatMap( txn => {
      // Filter out non-data lines (including any stray header rows) by ensuring date is present and not the literal header text
      if (txn.size() > 3 && (txn.get("Transaction date").nonEmpty) && !txn.get("Transaction date").equalsIgnoreCase("Transaction date")) {
        Some(Transaction.TxnFromCSV(txn))
      } else None
    }).toSet
    
    // send all to database
    DB_Factory.withDB { (db, statement) =>
      db.initDb(statement)
      db.addTxnBatch(statement, txns.toList)

      val accRes = db.getAccountsFromTxn(statement).get
      val accs = new mutable.HashSet[String]()
      while (accRes.next()) {accs.add(accRes.getString("From_Acc"))}
      db.addAccounts(statement, accs.toSet)
      None
    }
    reader.close()
    txns
  }
}
