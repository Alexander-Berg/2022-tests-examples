package ru.yandex.auto.vin.decoder.manager.validation

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.validation.data.FileFeedData
import ru.yandex.auto.vin.decoder.manager.validation.download.Feed
import ru.yandex.auto.vin.decoder.manager.validation.results.SaleResultState
import ru.yandex.auto.vin.decoder.manager.validation.results.autoru.AutoruSbResultState
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.services.AutoruFormatSbRawModelManager
import ru.yandex.auto.vin.decoder.raw.autoteka.sale.AutotekaSaleRawModelManager
import ru.yandex.auto.vin.decoder.raw.autoteka.sale.model.AutotekaSaleRawModel
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class FeedValidatorTest extends AnyFunSuite with MockitoSupport {

  implicit val t: Traced = Traced.empty
  val validator = new FeedValidator()

  test("validate autoteka json sales") {
    val feedData = mock[FileFeedData]
    when(feedData.getInputStream).thenReturn(getClass.getResourceAsStream("/autoteka/sales/validation.json"))

    val feed = Feed("", feedData, AutotekaSaleRawModelManager(FileFormats.Json, EventType.UNKNOWN))

    val res = Await
      .result(validator.validateFeed(feed), 1.second)
      .asInstanceOf[SaleResultState[AutotekaSaleRawModel]]

    assert(res.common.count === 4)
    assert(res.common.succesRecordsCount === 3)
    assert(res.common.recordWithErrorsCount === 1)
    assert(res.common.recordWithWarningCount === 1)
    assert(res.common.errorsCount === 2)

    assert(res.common.uniqueIdentifiers === false)
    assert(res.common.distinctVinsCount === 2)
    assert(res.common.mileagesCount === 2)
    assert(res.common.zeroMileagesCount === 1)
    assert(res.common.minTimestamp === Some(1382918400000L))
    assert(res.common.maxTimestamp === Some(1509148800000L))

    assert(res.hasIsNewFlag === 1)
    assert(res.newCars === 1)
    assert(res.usedCars === 0)

    assert(res.hasIsCreditFlag === 3)
    assert(res.creditCars === 2)
    assert(res.notCreditCars === 1)

    assert(res.common.errors.size === 2)
    assert(res.common.warnings.size === 1)
  }

  test("validate xml autoru service books") {
    val feedData = mock[FileFeedData]
    when(feedData.getInputStream).thenReturn(getClass.getResourceAsStream("/autoru/services/validation.xml"))

    val feed = Feed("", feedData, new AutoruFormatSbRawModelManager(EventType.UNKNOWN, FileFormats.Xml))

    val res = Await
      .result(validator.validateFeed(feed), 1.second)
      .asInstanceOf[AutoruSbResultState]

    assert(res.common.count === 4)
    assert(res.common.succesRecordsCount === 3)
    assert(res.common.recordWithErrorsCount === 1)
    assert(res.common.recordWithWarningCount === 0)
    assert(res.common.errorsCount === 1)
    assert(res.common.hasInfoCount === 2)

    assert(res.common.mileagesCount === 2)
    assert(res.common.zeroMileagesCount === 0)

    assert(res.common.maxTimestamp === Some(1492128000000L))
    assert(res.common.minTimestamp === Some(1454544000000L))

    assert(res.recordsWithServicesList === 2)
    assert(res.recordsWithProductsList === 1)
  }

  test("validate csv autoru service books") {
    val feedData = mock[FileFeedData]
    when(feedData.getInputStream).thenReturn(
      getClass.getResourceAsStream("/autoru/services/ROLF_2_20200525-102513.csv")
    )

    val feed = Feed("", feedData, new AutoruFormatSbRawModelManager(EventType.UNKNOWN, FileFormats.Csv))

    val res = Await
      .result(validator.validateFeed(feed), 1.second)
      .asInstanceOf[AutoruSbResultState]

    assert(res.common.count === 2)
    assert(res.common.succesRecordsCount === 2)
    assert(res.common.recordWithErrorsCount === 0)
    assert(res.common.recordWithWarningCount === 0)
    assert(res.common.errorsCount === 0)
    assert(res.common.hasInfoCount === 2)

    assert(res.common.mileagesCount === 2)
    assert(res.common.zeroMileagesCount === 0)

    assert(res.common.maxTimestamp === Some(1492128000000L))
    assert(res.common.minTimestamp === Some(1454544000000L))

    assert(res.recordsWithServicesList === 2)
    assert(res.recordsWithProductsList === 2)
  }

}
