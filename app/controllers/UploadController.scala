package controllers

import model.domain.files.{FileHandler, FileHandler_Factory}
import model.domain.objects.Transaction
import model.domain.types.FileType
import play.api.{Logger, libs}
import play.api.data.Form
import play.api.data.Forms.*

import java.io.File
import play.api.mvc.MultipartFormData.FilePart

import java.nio.file.{Files, Path, Paths}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents, MultipartFormData}

import javax.inject.Inject

case class FormData(name: String)
object FormData {
  def unapply(formData: FormData): Option[(String)] = Some((formData.name))
}

class UploadController @Inject() (cc: MessagesControllerComponents) 
  extends MessagesAbstractController(cc){
  
  private val logger = Logger(this.getClass)
  
  val form: Form[FormData] = Form(
    mapping("name" -> text)(FormData.apply)(FormData.unapply)
  )
  
  def goToUpload: Action[AnyContent] = Action { implicit request => 
    Ok(views.html.files.fileUpload(form))
  }
  
  private def operateOnTempFile(file: File, fileType: FileType): Set[Transaction] = {
    logger.info("Now reading file")

    //val reportData = FileHandler.readFile(file.toPath.toString)

    val reportData = FileHandler_Factory.handleFile(file.toPath.toString, fileType)

    Files.deleteIfExists(file.toPath)
    reportData
  }
  
  def upload: Action[MultipartFormData[TemporaryFile]] = Action(parse.multipartFormData) { implicit request => 
    logger.info("Ledger File upload started")
    val startTime = System.currentTimeMillis()
    val fileOption = request.body.files.toArray.map {
      case FilePart(key, filename, contentType, tempFile: TemporaryFile, fileSize, dispositionType, _) => {
        logger.info(
          s"key = $key, filename = $filename, tempFile = ${tempFile.path}, fileSize = $fileSize" +
            s", contentType = $contentType, dispositionType = $dispositionType")
        (filename,
          operateOnTempFile(tempFile.path.toFile, FileType.Ledger))
      }
    }
    Ok(views.html.files.fileStatus(fileOption.nonEmpty, fileOption))
  }

  def uploadInvoice: Action[MultipartFormData[TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
    logger.info("Invoice File upload started")
    val startTime = System.currentTimeMillis()
    val fileOption = request.body.files.toArray.map {
      case FilePart(key, filename, contentType, tempFile: TemporaryFile, fileSize, dispositionType, _) => {
        logger.info(
          s"key = $key, filename = $filename, tempFile = ${tempFile.path}, fileSize = $fileSize" +
            s", contentType = $contentType, dispositionType = $dispositionType")
        (filename,
          operateOnTempFile(tempFile.path.toFile, FileType.Invoice))
      }
    }
    Ok(views.html.files.fileStatus(fileOption.nonEmpty, fileOption))
  }

  def uploadTD: Action[MultipartFormData[TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
    logger.info("TD File upload started")
    val startTime = System.currentTimeMillis()
    val fileOption = request.body.files.toArray.map {
      case FilePart(key, filename, contentType, tempFile: TemporaryFile, fileSize, dispositionType, _) => {
        logger.info(
          s"key = $key, filename = $filename, tempFile = ${tempFile.path}, fileSize = $fileSize" +
            s", contentType = $contentType, dispositionType = $dispositionType")
        (filename,
          operateOnTempFile(tempFile.path.toFile, FileType.TD))
      }
    }
    Ok(views.html.files.fileStatus(fileOption.nonEmpty, fileOption))
  }
}
