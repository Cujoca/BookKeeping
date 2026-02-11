package model.domain.types

/**
  * Enum for account types, because personally keeping track of them is annoying
  * TODO: clean up list of types for what is actually needed
  * TODO: perhaps add some modularity? allow user to add account type?
  */
enum AccountType:
  case Bank
  case Cash
  case Credit
  case Investment
  case Loan
  case Asset
  case Liability
  case Equity
  case Income
  case Expense
  case Other
  case Unknown

/**
 * Companion object for AccountType, provides utility methods for parsing and displaying. */
object AccountType:
  /** 
   * Canonical, user-facing display name for each enum value. */
  def displayName(at: AccountType): String = at match
    case AccountType.Bank       => "Bank"
    case AccountType.Cash       => "Cash"
    case AccountType.Credit     => "Credit"
    case AccountType.Investment => "Investment"
    case AccountType.Loan       => "Loan"
    case AccountType.Asset      => "Asset"
    case AccountType.Liability  => "Liability"
    case AccountType.Equity     => "Equity"
    case AccountType.Income     => "Income"
    case AccountType.Expense    => "Expense"
    case AccountType.Other      => "Other"
    case AccountType.Unknown    => "?"

  /** 
   * All values as a List, useful for dropdowns. */
  val all: List[AccountType] = AccountType.values.toList

  /**
    * Parse from a canonical string (case-insensitive) into the enum.
    * Accepts either the enum entry name or the display name.
    */
  def parse(s: String): AccountType =
    if s == null then Unknown
    else {
      val norm = s.trim
      if norm.isEmpty then Unknown
      else {
        val byEntryName = values.find(_.toString.equalsIgnoreCase(norm))
        byEntryName.orElse {
          values.find(at => displayName(at).equalsIgnoreCase(norm))
        }.get
      }
    }

  /** 
   * Default DB representation equals the display name. */
  def toDbString(at: AccountType): String = displayName(at)

  /** 
   * Default parser from DB representation; case-insensitive match on display name. */
  def fromDbString(s: String): AccountType = parse(s)
