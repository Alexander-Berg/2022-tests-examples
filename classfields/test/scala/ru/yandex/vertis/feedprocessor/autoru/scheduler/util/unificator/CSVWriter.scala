package ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator

import java.io.{OutputStream, OutputStreamWriter}

import org.apache.commons.lang3.StringEscapeUtils.escapeCsv
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator.UnificatorChecker.UnifyData

/**
  * Created by sievmi on 20.04.18
  */

class CSVWriter(stream: OutputStream) {
  private val writer = new OutputStreamWriter(stream)
  private val cntFields: Int = 9
  private val csvLine: Array[String] = Array.fill[String](cntFields)("")

  def startLine(): Unit = {
    writer.write(
      "real_tech_param_id\tmark\tmodel\tmodification\tbody_type" +
        "\tyear\tunificator_tech_param_id\terror\thttp_error"
    )
    writer.write("\n")
  }

  def writeLine(unifyData: UnifyData, unificatorTechParamId: String, error: String, httpError: String): Unit = {

    csvLine(0) = unifyData.catalogData.techParamId
    csvLine(1) = unifyData.catalogData.mark
    csvLine(2) = unifyData.catalogData.model
    csvLine(3) = unifyData.catalogData.modification
    csvLine(4) = unifyData.catalogData.bodyType
    csvLine(5) = unifyData.offer.year.toString
    csvLine(6) = unificatorTechParamId
    csvLine(7) = error
    csvLine(8) = httpError

    writer.write(csvLine.map(escapeCsv).mkString("\t"))
    writer.write("\n")
  }

  def finish(): Unit = {
    writer.close()
  }
}
