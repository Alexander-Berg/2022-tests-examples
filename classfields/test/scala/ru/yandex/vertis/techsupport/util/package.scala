package ru.yandex.vertis.vsquality.techsupport

import io.circe.Decoder
import io.circe.parser.decode
import ru.yandex.vertis.vsquality.techsupport.service.bot.ExternalScenario
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario

import scala.io.{BufferedSource, Codec, Source}

/**
  * @author potseluev
  */
package object util {

  def readResourceFile(path: String): BufferedSource =
    Source.fromInputStream(getClass.getResourceAsStream(path))(Codec.UTF8)

  def readResourceFileAsString(path: String): String = readResourceFile(path).mkString

  def scenarioFromFile(fileName: String)(implicit decoder: Decoder[ExternalGraphScenario]): ExternalGraphScenario =
    decode[ExternalGraphScenario](readResourceFileAsString(s"/$fileName")).toTry.get
}
