package ru.yandex.auto.vin.decoder.raw.autoru

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.CommonModel.Damage.CarPart
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.estimate.AutoruFormatEstimateRawModelManager
import ru.yandex.auto.vin.decoder.raw.autoru.estimate.model.AutoruFormatEstimateRawModel
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{
  FieldRangeBorderError,
  NumberFieldError,
  RequiredFieldError
}

class AutoruFormatEstimateRawModelManagerTest extends AnyFunSuite {

  private val jsonManager =
    new AutoruFormatEstimateRawModelManager(EventType.UNDEFINED, FileFormats.Json)

  private val xmlManager =
    new AutoruFormatEstimateRawModelManager(EventType.UNDEFINED, FileFormats.Xml)

  private val csvManager =
    new AutoruFormatEstimateRawModelManager(EventType.UNDEFINED, FileFormats.Csv)

  test("parse json") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/estimate/estimate_test.json")

    val parsed = jsonManager.parseFile(rawInputStream, "").toList.collect { case Right(v) =>
      v
    }
    testParsed(parsed)
  }

  test("parse xml") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/estimate/estimate_test.xml")

    val parsed = xmlManager.parseFile(rawInputStream, "").toList.collect { case Right(v) =>
      v
    }
    testParsed(parsed)
  }

  test("parse json with errors") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/estimate/estimate_test.json")

    val errors = jsonManager.parseFile(rawInputStream, "").toList.collect { case Left(v) =>
      v
    }

    assert(errors.size === 1)
    assert(errors(0).asInstanceOf[NumberFieldError].field === "PART")
  }

  test("parse json with warnings") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/estimate/estimate_warning.json")

    val warnings = jsonManager
      .parseFile(rawInputStream, "")
      .toList
      .collect { case Right(v) =>
        v.warnings
      }
      .flatten

    assert(warnings.size === 1)
    assert(warnings.head.asInstanceOf[FieldRangeBorderError[Int]].field === "PRICE_FROM")
  }

  test("parse xml with errors") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/estimate/estimate_test.xml")

    val errors = xmlManager.parseFile(rawInputStream, "").toList.collect { case Left(v) =>
      v
    }

    assert(errors.size === 1)
    assert(errors(0).asInstanceOf[RequiredFieldError].field === "PRICE_TO")
  }

  test("parse csv") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/estimate/estimate.csv")

    val parsed = csvManager.parseFile(rawInputStream, "").toList.collect { case Right(v) =>
      v
    }
    assert(parsed.size === 1)
    assert(parsed(0).identifier.toString === "X4XYB81160D247134")
    assert(parsed(0).groupId === "AMV0378172")
    assert(parsed(0).model.info.get.eventTimestamp === 1454544000000L)

    // images
    val images = parsed(0).model.info.get.images
    assert(images.size === 3)
    assert(images(0).origUrl === "https://xn----etbpba5admdlad.xn--p1ai/report/photo/id1")

    // videos
    val videos = parsed(0).model.info.get.videos
    assert(videos.size === 3)
    assert(videos(0).url === "https://xn----etbpba5admdlad.xn--p1ai/report/video/id1")

    // damages
    assert(parsed(0).model.info.get.damages.nonEmpty)
    val damages = parsed(0).model.info.get.damages.get
    val damageElems = damages.damages
    assert(damageElems.size === 1)

    // results
    assert(parsed(0).model.info.get.results.nonEmpty)
    val results = parsed(0).model.info.get.results.get
    assert(results.reportUrl.nonEmpty)
    assert(results.repair.nonEmpty)
    val repairResults = results.repair.get
    assert(repairResults.priceTo === 45000)
    assert(repairResults.priceFrom.exists(_ === 30000))
    assert(results.inspectionDetails.size === 3)
    assert(results.inspectionDetails.forall(_.nonEmpty))
    assert(results.summary.exists(_.nonEmpty))
    assert(results.priceFrom.contains(600000))
    assert(results.priceTo.contains(800000))

  }

  def testParsed(parsed: Seq[AutoruFormatEstimateRawModel]): Unit = {
    assert(parsed.size === 1)
    assert(parsed(0).identifier.toString === "X4XYB81160D247134")
    assert(parsed(0).groupId === "AMV0378172")
    assert(parsed(0).model.info.get.eventTimestamp === 1454544000000L)

    // images
    val images = parsed(0).model.info.get.images
    assert(images.size === 3)
    assert(images(0).origUrl === "https://xn----etbpba5admdlad.xn--p1ai/report/photo/id1")
    assert(images(0).imageType.nonEmpty)
    assert(images(0).imageType.get == "DAMAGE")
    assert(images(1).origUrl === "https://xn----etbpba5admdlad.xn--p1ai/report/photo/id2")
    assert(images(1).imageType.isEmpty)
    assert(images(2).origUrl === "https://xn----etbpba5admdlad.xn--p1ai/report/photo/id3")
    assert(images(2).imageType.isEmpty)

    // videos
    val videos = parsed(0).model.info.get.videos
    assert(videos.size === 3)
    assert(videos(0).url === "https://xn----etbpba5admdlad.xn--p1ai/report/video/id1")
    assert(videos(0).videoType.nonEmpty)
    assert(videos(0).videoType.get == "OVERVIEW")
    assert(videos(1).url === "https://xn----etbpba5admdlad.xn--p1ai/report/video/id2")
    assert(videos(1).videoType.isEmpty)
    assert(videos(2).url === "https://xn----etbpba5admdlad.xn--p1ai/report/video/id3")
    assert(videos(2).videoType.isEmpty)

    // damages
    assert(parsed(0).model.info.get.damages.nonEmpty)
    val damages = parsed(0).model.info.get.damages.get
    assert(damages.description.nonEmpty)
    val damageElems = damages.damages
    assert(damageElems.size === 3)
    assert(damageElems(0).description.nonEmpty)
    assert(damageElems(0).part.nonEmpty)
    assert(damageElems(0).part.get == CarPart.ROOF)
    assert(damageElems(0).image.nonEmpty)
    val damageElemImage1 = damageElems(0).image.get
    assert(damageElemImage1.origUrl === "https://xn----etbpba5admdlad.xn--p1ai/report/photo/id1")
    assert(damageElemImage1.imageType.nonEmpty)
    assert(damageElemImage1.imageType.get == "DAMAGE")

    assert(damageElems(1).description.nonEmpty)
    assert(damageElems(1).part.nonEmpty)
    assert(damageElems(1).part.get == CarPart.TRUNK_DOOR)
    assert(damageElems(1).image.nonEmpty)
    val damageElemImage2 = damageElems(1).image.get
    assert(damageElemImage2.origUrl === "https://xn----etbpba5admdlad.xn--p1ai/report/photo/id2")
    assert(damageElemImage2.imageType.nonEmpty)
    assert(damageElemImage2.imageType.get == "DAMAGE")

    assert(damageElems(2).description.nonEmpty)
    assert(damageElems(2).part.nonEmpty)
    assert(damageElems(2).part.get == CarPart.CAR_PART_UNKNOWN)
    assert(damageElems(2).image.nonEmpty)
    val damageElemImage3 = damageElems(2).image.get
    assert(damageElemImage3.origUrl === "https://xn----etbpba5admdlad.xn--p1ai/report/photo/id3")
    assert(damageElemImage3.imageType.nonEmpty)
    assert(damageElemImage3.imageType.get == "DAMAGE")

    // results
    assert(parsed(0).model.info.get.results.nonEmpty)
    val results = parsed(0).model.info.get.results.get
    assert(results.reportUrl.nonEmpty)
    assert(results.repair.nonEmpty)
    val repairResults = results.repair.get
    assert(repairResults.priceTo === 45000)
    assert(repairResults.priceFrom.exists(_ === 30000))
    assert(results.priceFrom.contains(600000))
    assert(results.priceTo.contains(800000))

    assert(results.inspectionDetails.size === 3)
    assert(results.inspectionDetails.forall(_.nonEmpty))
    assert(results.summary.exists(_.nonEmpty))
    ()
  }
}
