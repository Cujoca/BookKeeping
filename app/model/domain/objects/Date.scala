package model.domain.objects

import java.time.LocalDate

/**
 * Utility object for handling date conversions between String and LocalDate formats.
 */
class Date (date: LocalDate){
}

object Date {
  
  def fromString(date: String): LocalDate = LocalDate.parse(date)
  def toSql(date: LocalDate): String = date.toString
}
