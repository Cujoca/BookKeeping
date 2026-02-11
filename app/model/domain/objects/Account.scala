package model.domain.objects

import model.database.DB_Factory
import model.domain.types.AccountType
import play.api.Logger

import scala.collection.mutable

class Account (ID: String, name: String, accType: AccountType) {
  def getID: String = ID
  def getName: String = name
  def getType: AccountType = accType
}

object Account {
  
  private val logger = Logger(this.getClass)

  def getAccountsFromDB: Set[Account] = {

    val accRes = DB_Factory.withDB { (db, state) => db.getAccounts(state) }
    val accs = new mutable.HashSet[Account]()
    while (accRes.get.next()) {
      accs add(new Account(
        accRes.get.getString("Account_ID"),
        accRes.get.getString("Account_Name"),
        AccountType.parse(accRes.get.getString("Account_Type"))
      ))
    }
    accs.toSet
  }

  def getSpecificAccount(accountID: String) = {
    val accRes = DB_Factory.withDB((db, state) => db.getAccountByID(state, accountID))
    accRes.get.next()
    new Account(
      "" + accRes.get.getInt("Account_ID"),
      accRes.get.getString("Account_Name"),
      AccountType.parse(accRes.get.getString("Account_Type"))
    )
  }
  
  def updateAccountType (accountID: String, newType: AccountType) = {
    DB_Factory.withDB { (db, state) => db.updateAccountType(state, accountID, newType.toString) }
  }
  

}
