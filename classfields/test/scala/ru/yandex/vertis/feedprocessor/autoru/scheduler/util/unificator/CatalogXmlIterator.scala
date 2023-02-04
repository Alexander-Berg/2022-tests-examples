package ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator

import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator.CatalogXmlIterator.CatalogData

import java.io.{InputStream, InputStreamReader}
import java.time.LocalDate
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants}
import scala.collection.mutable.ListBuffer

/**
  * Created by sievmi on 20.04.18
  */

class CatalogXmlIterator(inputStream: InputStream) {

  private val reader = XMLInputFactory.newFactory().createXMLStreamReader(new InputStreamReader(inputStream))
  private val parsedData: ListBuffer[CatalogData] = ListBuffer.empty

  private var mark: String = _
  private var model: String = _
  private var modification: String = _
  private var bodyType: String = _
  private var startYear: String = _
  private var endYear: String = _
  private var techParamId: String = _

  private val elementText = new StringBuilder()
  private val currentYear = LocalDate.now().getYear

  def parserCatalog(): ListBuffer[CatalogData] = {
    while (reader.hasNext) {
      val elem = reader.next()
      elem match {
        case XMLStreamConstants.CHARACTERS =>
          elementText.append(reader.getText)
        case XMLStreamConstants.END_ELEMENT =>
          reader.getLocalName.toLowerCase() match {
            case "modification_id" => modification = elementText.toString.trim
            case "body_type" => bodyType = elementText.toString.trim
            case "years" =>
              val years = elementText.toString().split("-")
              try {
                startYear = years.head.trim.toInt.toString
              } catch {
                case e: Exception => startYear = currentYear.toString
              }
              try {
                endYear = years(1).trim.toInt.toString
              } catch {
                case e: Exception => endYear = currentYear.toString
              }

            case "folder_id" => model = elementText.toString.trim
            case "mark_id" => mark = elementText.toString.trim
            case "tech_param_id" => techParamId = elementText.toString().trim
            case "modification" =>
              parsedData += CatalogData(mark, model, modification, bodyType, startYear, endYear, techParamId)
              modification = ""
              model = ""
              bodyType = ""
              startYear = ""
              endYear = ""
              techParamId = ""

            case _ =>
          }
          elementText.clear()
        case _ =>
      }
    }

    println(s"Found ${parsedData.size} cars modifications")
    parsedData
  }
}

object CatalogXmlIterator {

  case class CatalogData(
      mark: String,
      model: String,
      modification: String,
      bodyType: String,
      startYear: String,
      endYear: String,
      techParamId: String)

}
