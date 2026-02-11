package model.domain.files

import model.domain.objects.Transaction
import org.apache.commons.csv.CSVFormat
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

import java.io.{BufferedReader, FileReader}
import scala.collection.mutable

object FileHandler_Ledger extends FileHandler_Interface {


  override def readFile(filePath: String): Set[Transaction] = {
    val reader = new BufferedReader ( new FileReader (filePath))
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
      .setSkipHeaderRecord(false)
      .setIgnoreHeaderCase(true)
      .setTrim(true)
      .get()

    val records = format.parse(reader)
    val accs = mutable.HashSet[String]()

    val txns = records.flatMap ( txn => {
      if (txn.size() > 3 &&
         (txn.get("Transaction Date").nonEmpty) &&
          !txn.get("Transaction date").equalsIgnoreCase("Transaction date")) {
        accs.add(txn.get("Distribution account"))
        Some(Transaction.TxnFromCSV(txn))
      } else None
    }).toSet
    logger.info(accs.toString())
    sendToDB(txns, accs.toSet)

    reader.close()
    txns
  }

}
