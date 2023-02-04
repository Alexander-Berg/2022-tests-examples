package ru.yandex.vertis.picapica.model.auto

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata.OcrResult
import ru.yandex.vertis.picapica.model.realty.MetaFactoryImpl
import spray.json.JsonParser

import scala.collection.JavaConversions._

/**
  * @author pnaydenov
  */
@RunWith(classOf[JUnitRunner])
class AutoAvatarsMetaSpec extends WordSpecLike with Matchers {
  val config = ConfigFactory.parseString("""{"flat_fields": [], "nested_fields": {}}""")

  def ocrResult(confidence: Float, text: String, textType: String): OcrResult =
    OcrResult.newBuilder().setConfidence(confidence).setText(text).setType(textType).build()

  "auto json reader" should {
    "read ocr results" in {
      val jsonStr = IOUtils.toString(getClass.getResourceAsStream("/autoru_ocr_meta.json"))
      val reader = MetaFactoryImpl(config, saveRawMeta = false).format
      val (meta, metadata) = reader.read(JsonParser(jsonStr))

      //scalastyle:off
      val platesEntry = metadata.getOcrResultsList.filter(_.getName == "auto_plate")
      val contactsEntry = metadata.getOcrResultsList.filter(_.getName == "auto_contacts")
      platesEntry should have size 1
      contactsEntry should have size 1
      val plates = platesEntry.head.getValuesList
      val contacts = contactsEntry.head.getValuesList
      contacts should have size 5
      contacts(0) shouldEqual ocrResult(0.61842f, "+7 495 695-17-24", "phone")
      contacts(1) shouldEqual ocrResult(0f, "\n", "text")
      contacts(2) shouldEqual ocrResult(0.729544f, "8495-223-33-22", "phone")
      contacts(3) shouldEqual ocrResult(0f, "\n", "text")
      contacts(4) shouldEqual ocrResult(0.727022f, "Wolozh@yandex-team.ru", "email")
      plates should have size 1
      plates(0) shouldEqual ocrResult(0.518982f, "E270EM50\n", "text")
      //scalastyle:on
    }
  }
}
