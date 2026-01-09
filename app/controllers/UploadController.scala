package controllers

import play.api.{Logger, libs}
import play.api.data.Form
import play.api.data.Forms.*
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents, MultipartFormData}

import javax.inject.Inject

case class FormData(name: String)
object FormData {
  def unapply(formData: FormData): Option[(String)] = Some((formData.name))
}

@Singleton
class UploadController @Inject() (cc: MessagesControllerComponents) 
  extends MessagesAbstractController(cc){
  
  private val logger = Logger(this.getClass)
  
  val form: Form[FormData] = Form(
    mapping("name" -> text)(FormData.apply)(FormData.unapply)
  )

  def goToUpload: Action[AnyContent] = Action { implicit request => 
    Ok(views.html.fileUpload(form))
  }
  
  def operateOnTempFile(file: java.io.File) = {
    logger.info("Now reading file")
    val reportData = FileHandler.readFile(file.toPath.toString)
    logger.info(reportData)
    Files.deleteIfExists(file.toPath)
    reportData.toMap
  }
  
  def upload: Action[MultipartFormData[TemporaryFile]] = Action(parse.multipartFormData) { implicit request => 
    logger.info("File upload started")
    val startTime = System.currentTimeMillis()
    val fileOption = request.body.files.toArray.map {
      case FilePart(key, filename, contentType, tempFile: TemporaryFile, fileSize, dispositionType) =>
        libs.Files.logger.info(
          s"key = $key, filename = $filename, tempFile = ${tempFile.path}, fileSize = $fileSize" +
            s", contentType = $contentType, dispositionType = $dispositionType")
        (filename,
          operateOnTempFile(tempFile.path.toFile)
    }
  }
  
}
