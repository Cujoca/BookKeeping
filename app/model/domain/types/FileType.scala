package model.domain.types

/**
 * Enum for file types. Used in UploadController to tell
 * FileHandler_Factory to invoke the correct file handler.
 */
enum FileType:
  case Invoice
  case TD
  case Ledger

/**
 * Companion object for FileType, unsure if needed
 */
object FileType {
    
}