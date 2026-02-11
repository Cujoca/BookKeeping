package model.domain.objects

import model.database.DB_Factory

import scala.collection.mutable

/**
 * Object containing functions for generating reports.
 */
object Report {

  private case class Report(delta: Double, txns: Int) {
    def + (other: Double) = Report(delta + other, txns + 1)
  }

  def totalReport () = {

    val txns = Transaction.getTxnsFromDB()
    val accs = new mutable.HashMap[String, Report]

    txns foreach (txn => {
      accs.put(
        txn.getFromAcc,
        accs.getOrElse(txn.getFromAcc, new Report(0, 0))
      )
    })

  }



}
