package ru.yandex.vertis.picapica.model.realty


import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata.OcrResult
import spray.json.JsonParser

import scala.collection.JavaConversions._

/**
  * @author @logab
  */
//scalastyle:off
@RunWith(classOf[JUnitRunner])
class RealtyAvatarsMetaSpec extends WordSpecLike with Matchers {
  val config = ConfigFactory.load("meta.conf").getConfig("picapica.avatars.realty.meta")

  def ocrResult(confidence: Float, text: String, textType: String): OcrResult =
    OcrResult.newBuilder().setConfidence(confidence).setText(text).setType(textType).build()

  val validJson =
    """{
      |    "GlobalSemidupDescriptor64": "MC9C0847E7EFF2D28",
      |    "NNetClassifiersTopClasses": {
      |        "pool7_imagenet": [
      |            [
      |                3159,
      |                28
      |            ],
      |            [
      |                2987,
      |                14
      |            ],
      |            [
      |                5480,
      |                13
      |            ],
      |            [
      |                2500,
      |                12
      |            ],
      |            [
      |                3494,
      |                11
      |            ],
      |            [
      |                4348,
      |                8
      |            ],
      |            [
      |                4164,
      |                8
      |            ],
      |            [
      |                3161,
      |                8
      |            ],
      |            [
      |                3583,
      |                8
      |            ],
      |            [
      |                3886,
      |                7
      |            ]
      |        ]
      |    },
      |    "NeuralNetClasses": {
      |        "aesthetic": 0,
      |        "gruesome": 0,
      |        "ocr_text": 1,
      |        "perversion": 0,
      |        "porno": 0,
      |        "realty_docs_with_plans": 0,
      |        "realty_docs_wo_plans": 1,
      |        "realty_entrance_stairs": 0,
      |        "realty_interior": 232,
      |        "realty_kitchen": 20,
      |        "realty_maps": 0,
      |        "realty_other": 0,
      |        "realty_outside": 0,
      |        "realty_spam": 0,
      |        "realty_wc": 0,
      |        "wallpaper": 0
      |    },
      |    "md5": "476c477ecd5e725b9ce638f801c36342",
      |    "orig-format": "JPEG",
      |    "orig-size": {
      |        "x": 579,
      |        "y": 1024
      |    },
      |    "processed_by_computer_vision": true,
      |    "processing": "finished",
      |    "r-orig-size": {
      |        "x": 579,
      |        "y": 1024
      |    },
      |    "SmartCrop": {
      |        "0x0": {
      |            "FitLevel": 0.997623,
      |            "Rect": [
      |                26,
      |                415,
      |                2701,
      |                2943
      |            ]
      |        }
      |    }
      |}""".stripMargin

  val partialJson =
    """{
      |    "orig-size": {
      |        "x": 579,
      |        "y": 1024
      |    }
      |}""".stripMargin
  val log = LoggerFactory.getLogger(classOf[RealtyAvatarsMetaSpec])
  "realty json reader" should {
    val metaFactory = MetaFactoryImpl(config, saveRawMeta = false)
    val reader = metaFactory.format
    "contain all requiered fields in meta" in {
      metaFactory.requiredFields should contain theSameElementsAs Seq(
        "cv_hash", "docsWithPlans", "docsWoPlans",
        "height", "width", "stairs",
        "wc", "porno", "out",
        "kitchen", "int", "ocr_text",
        "spam", "maps", "other")
    }
    "read valid data" in {
      val read = reader.read(JsonParser(validJson))
      read._1 shouldEqual Map(
        "cv_hash" -> "MC9C0847E7EFF2D28",
        "docsWithPlans" -> "0",
        "docsWoPlans" -> "1",
        "height" -> "1024",
        "width" -> "579",
        "stairs" -> "0",
        "wc" -> "0",
        "porno" -> "0",
        "out" -> "0",
        "kitchen" -> "20",
        "int" -> "232",
        "ocr_text" -> "1",
        "spam" -> "0",
        "maps" -> "0",
        "other" -> "0"
      )

      val metadata = read._2
      metadata.getGlobalSemidupDescriptor64 shouldEqual "MC9C0847E7EFF2D28"
      metadata.getNnetClassifiersTopClassesCount shouldEqual 1
      metadata.getNnetClassifiersTopClasses(0).getName shouldEqual "pool7_imagenet"
      metadata.getNnetClassifiersTopClasses(0).getValues(0).getId shouldEqual 3159
      metadata.getNeuralNetClassesList.toList.find(_.getName == "realty_interior").get.getWeight shouldEqual 232
      metadata.getIsFinished should be (true)
      metadata.getOrigSize shouldEqual Metadata.Size.newBuilder().setX(579).setY(1024).build()
      metadata.getSmartCrop(0).getKey shouldEqual "0x0"
      metadata.getSmartCrop(0).getMinY shouldEqual 415
      metadata.getSmartCrop(0).getMaxY shouldEqual 2943
    }

    "read ocr results" in {
      val jsonStr = IOUtils.toString(getClass.getResourceAsStream("/realty_ocr_meta.json"))
      val reader = MetaFactoryImpl(config, saveRawMeta = false).format
      val (meta, metadata) = reader.read(JsonParser(jsonStr))

      val fullOcrEntry = metadata.getOcrResultsList.filter(_.getName == "full_ocr")
      fullOcrEntry should have size 1
      val ocrEntries = fullOcrEntry.head.getValuesList
      fullOcrEntry.head.getName shouldEqual "full_ocr"
      ocrEntries(0) shouldEqual ocrResult(0.724816f, "Full text sample", "text")
      ocrEntries(1) shouldEqual ocrResult(0.682417f, "+7-937-380-21-75", "phone")
    }

    "read empty object" in {
      val r = reader.read(JsonParser("{}"))
      r._2.getIsFinished shouldBe (false)
    }

    "read partial" in {
      reader.read(JsonParser(partialJson))._1 shouldEqual Map("height" -> "1024", "width" -> "579")
    }
  }
}
