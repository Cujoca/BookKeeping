package model.domain.files
import model.domain.objects.Transaction
import model.domain.types.FileType

object FileHandler_Factory {

  def handleFile (filePath: String, style: FileType): Set[Transaction] = {
    
    style match {
      case FileType.Invoice => FileHandler_Invoice.readFile(filePath)
      case FileType.TD => ???
      case FileType.Ledger => FileHandler_Ledger.readFile(filePath)
    }
  }

}
