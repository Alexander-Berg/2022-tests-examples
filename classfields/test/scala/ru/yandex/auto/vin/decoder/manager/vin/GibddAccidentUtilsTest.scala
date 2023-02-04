package ru.yandex.auto.vin.decoder.manager.vin

import org.scalatest.enablers.Emptiness.emptinessOfGenTraversable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Accident, VinInfoHistory}

import scala.jdk.CollectionConverters.IterableHasAsJava

class GibddAccidentUtilsTest extends AnyWordSpecLike with Matchers {

  "GibddDeletedAccidentsUtils" must {

    "get GIBDD deleted accidents" in {
      val autocode = buildVinInfoHistory(EventType.AUTOCODE_ACCIDENT, List("1", "2", "003"))
      val adaperio = buildVinInfoHistory(EventType.ADAPERIO_ACCIDENT, List("3", "04", "5"))
      val scrapinghub = buildVinInfoHistory(EventType.SH_GIBDD_ACCIDENTS, List("2", "3", "4"))

      val history = List(autocode, adaperio)

      val number1 = buildAccident("1")
      val number5 = buildAccident("5")

      val deleted = GibddAccidentUtils.getGibddDeletedAccidents(scrapinghub, history)

      deleted.length shouldBe 2
      deleted should contain(number1)
      deleted should contain(number5)
    }

    "don't return any GIBDD deleted accidents if there is no deleted accidents" in {
      val autocode = buildVinInfoHistory(EventType.AUTOCODE_ACCIDENT, List("001", "2", "0003"))
      val adaperio = buildVinInfoHistory(EventType.ADAPERIO_ACCIDENT, List("2", "3", "4"))
      val scrapinghub = buildVinInfoHistory(EventType.SH_GIBDD_ACCIDENTS, List("1", "02", "3", "0004", "5"))

      val history = List(autocode, adaperio)

      val deleted = GibddAccidentUtils.getGibddDeletedAccidents(scrapinghub, history)

      deleted shouldBe empty
    }

    "add GIBDD deleted accidents to the history" in {
      val history = buildVinInfoHistory(EventType.AUTOCODE_ACCIDENT, List("1", "2", "3"))
      val deletedAccidents = List(buildAccident("4"), buildAccident("5"))

      val number1 = buildAccident("1")
      val number2 = buildAccident("2")
      val number3 = buildAccident("3")
      val number4 = buildAccident("4", true)
      val number5 = buildAccident("5", true)

      val res = addGibddDeletedAccidents(history, deletedAccidents)
      val accidents = res.getAccidentsList

      accidents.size shouldBe 5
      accidents should contain(number1)
      accidents should contain(number2)
      accidents should contain(number3)
      accidents should contain(number4)
      accidents should contain(number5)
    }

    "don't modify accidents history if there is no deleted accidents" in {
      val history = buildVinInfoHistory(EventType.AUTOCODE_ACCIDENT, List("1", "2", "3"))

      val res = addGibddDeletedAccidents(history, List())

      res shouldBe history
    }
  }

  private def buildAccident(number: String, deletedInGibdd: Boolean = false): Accident = {
    Accident
      .newBuilder()
      .setNumber(number)
      .setDeletedInGibdd(deletedInGibdd)
      .build()
  }

  private def buildVinInfoHistory(eventType: EventType, numbers: List[String]): VinInfoHistory = {
    val builder = VinInfoHistory
      .newBuilder()
      .setEventType(eventType)

    numbers.foreach { n =>
      val accident = builder.addAccidentsBuilder()
      accident
        .setNumber(n)
        .setDeletedInGibdd(false)
    }

    builder.getStatusesBuilder.setAccidentsStatus(VinInfoHistory.Status.OK)
    builder.build()
  }

  private def addGibddDeletedAccidents(history: VinInfoHistory, deletedAccidents: List[Accident]) =
    if (deletedAccidents.nonEmpty) {
      val accidents = deletedAccidents.map(_.toBuilder.setDeletedInGibdd(true).build()).asJava
      history.toBuilder.addAllAccidents(accidents).build()
    } else {
      history
    }
}
