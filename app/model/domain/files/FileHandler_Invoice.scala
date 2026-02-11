package model.domain.files

import model.domain.objects.Transaction
import org.apache.commons.csv.CSVFormat
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

import java.io.{BufferedReader, FileReader}
import scala.collection.mutable

/**
 * FileHandler version for invoice files, invoked by FileHandler_Factory
 */
object FileHandler_Invoice extends FileHandler_Interface {

  /**
   * Reads provided file and returns a set of transactions.
   * this specific one is tailored to the invoice format provided by Excel
   * 
   * @param filePath: String, path to file
   * @return : Set[Transaction], parsed set of transactions
   */
  override def readFile(filePath: String): Set[Transaction] = {

    val reader = new BufferedReader ( new FileReader (filePath))

    // manually read and format header line since shitass excel puts random spaces
    val head = reader.readLine()
      .split(",")
      .map(_.trim)


    val data = CSVFormat.EXCEL.builder()
      .setHeader(head*)
      // we don't skip header line through csv parse since we already
      // read through it manually in the buffered reader
      .setSkipHeaderRecord(false) 
      .get()
      .parse(reader)

    val accs = mutable.HashSet[String]()
    accs.add("Expenses")

    val txns = data.flatMap(record => {
      Some(Transaction.TxnFromCSV_Invoice(record))
    }).toSet

    reader.close()
    sendToDB(txns, accs.toSet)
    txns
  }

}
