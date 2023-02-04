package ru.yandex.auto.vin.decoder.raw.customs

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CustomsRawModelManagerTest extends AnyFunSuite {

  val manager = new CustomsRawModelManager

  test("parse file") {
    val rawInputStream = getClass.getResourceAsStream("/customs/data-20210115-structure-20161230.json")

    val res = manager.parseFile(rawInputStream, "data-20210115-structure-20161230.json").toList

    val successResult = res.collect { case Right(v) =>
      v
    }
    val errorsResult = res.collect { case Left(v) =>
      v
    }

    assert(successResult.size === 2)
    assert(errorsResult.size === 1)

    assert(successResult(0).identifier === VinCode("XUG0500FHGCB06295"))
    assert(successResult(0).groupId === "1483228800000-156")
    assert(successResult(0).record.timestamp === 1483228800000L)
    assert(successResult(0).record.countryId === Some(156))
    assert(successResult(0).record.countryName === Some("КИТАЙСКАЯ НАРОДНАЯ РЕСПУБЛИКА"))

    assert(successResult(1).identifier === VinCode("CHSD23AAJG1001961"))
    assert(successResult(1).groupId === "1514851200000-0")
    assert(successResult(1).record.timestamp === 1514851200000L)
    assert(successResult(1).record.countryId === None)
    assert(successResult(1).record.countryName === None)
  }

  test("convert") {
    val rawInputStream = getClass.getResourceAsStream("/customs/data-20210115-structure-20161230.json")

    val res = manager.parseFile(rawInputStream, "data-20210115-structure-20161230.json").toList

    val successResult = res.collect { case Right(v) =>
      v
    }

    val converted = Future.sequence(successResult.map(manager.convert)).await

    assert(converted(0).getEventType === EventType.CUSTOMS_INFO)
    assert(converted(0).getVin === "XUG0500FHGCB06295")
    assert(converted(0).getCustomsCount === 1)
    assert(converted(0).getCustoms(0).getDate === 1483228800000L)
    assert(converted(0).getCustoms(0).getCountryFromCode === "156")
    assert(converted(0).getCustoms(0).getCountryFromName === "КИТАЙСКАЯ НАРОДНАЯ РЕСПУБЛИКА")

    assert(converted(1).getEventType === EventType.CUSTOMS_INFO)
    assert(converted(1).getVin === "CHSD23AAJG1001961")
    assert(converted(1).getCustomsCount === 1)
    assert(converted(1).getCustoms(0).getDate === 1514851200000L)
    assert(converted(1).getCustoms(0).getCountryFromCode === "")
    assert(converted(1).getCustoms(0).getCountryFromName === "")
  }
}
