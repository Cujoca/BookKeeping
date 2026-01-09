package model.domain.objects

import java.time.LocalDate

class Transaction ( fromAcc: String, 
                    toAcc: String, 
                    date: LocalDate, 
                    txnType: String, 
                    id: Int, 
                    name: String, 
                    desc: String,
                    amount: Double) {

  /**
   * Factory method for creating a Transaction object from a CSV file.
   * @param args: Array of strings containing CSV data, in the order
   *              0 -> from Account
   *              1 -> to Account
   *              2 -> Date
   *              3 -> Type
   *              4 -> ID
   *              5 -> Name
   *              6 -> Description
   *              7 -> Amount
   * @return
   */
  def TxnFromCSV(args: Array[String]): Transaction = {
    new Transaction(args(0), args(1), 
                    Date.fromString(args(2)), 
                    args(3), args(4).toInt, 
                    args(5), args(6), 
                    args(7).toDouble)
  }
  
  

}
