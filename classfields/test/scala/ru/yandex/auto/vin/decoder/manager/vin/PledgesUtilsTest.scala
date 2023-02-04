package ru.yandex.auto.vin.decoder.manager.vin

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Pledge, VinInfoHistory}

import scala.jdk.CollectionConverters.ListHasAsScala

class PledgesUtilsTest extends AnyWordSpecLike with MockitoSugar with Matchers {

  val vin = "X4XXG55470DS40452"

  def buildPledge(status: VinInfoHistory.Status): VinInfoHistory.Builder = {
    VinInfoHistory
      .newBuilder()
      .setEventType(EventType.AUTOCODE_PLEDGE)
      .setVin(vin)
      .setStatus(status)
  }

  "Pledges utils" should {
    "group and filter out pledges which are closed" in {
      val historyBuilder = buildPledge(VinInfoHistory.Status.OK)

      // two pledges with same number and undefined getInPledge
      val pledge1 = Pledge.newBuilder().setNumber("2017-001-790661-534/1").setInPledge(true)
      val pledge2 = Pledge.newBuilder().setNumber("2017-001-790661-534/2").setInPledge(true)
      val pledge3 = Pledge.newBuilder().setNumber("2017-001-790661-534/3").setInPledge(true)
      val pledge4 = Pledge.newBuilder().setNumber("2017-001-790661-534/4").setInPledge(false)
      val pledge5 = Pledge.newBuilder().setNumber("2017-001-790661-534").setInPledge(true)
      val pledge6 = Pledge.newBuilder().setNumber("2017-001-790661-535/1").setInPledge(false)
      val pledge7 = Pledge.newBuilder().setNumber("2017-001-790661-535").setInPledge(true)

      historyBuilder
        .addPledges(pledge1)
        .addPledges(pledge2)
        .addPledges(pledge3)
        .addPledges(pledge4)
        .addPledges(pledge5)
        .addPledges(pledge6)
        .addPledges(pledge7)
      val history = historyBuilder.build()

      val res = PledgesUtils.gluePledges(history)
      res.getPledgesCount shouldBe 0
    }

    "not filter pledges which are open" in {
      val historyBuilder = buildPledge(VinInfoHistory.Status.OK)

      // two pledges with same number and undefined getInPledge
      val pledge1 = Pledge.newBuilder().setNumber("2017-001-790661-534/1").setInPledge(false)
      val pledge2 = Pledge.newBuilder().setNumber("2017-001-790661-534").setInPledge(true)
      val pledge3 = Pledge.newBuilder().setNumber("2017-001-790661-535/1").setInPledge(true)
      val pledge4 = Pledge.newBuilder().setNumber("2017-001-790661-535").setInPledge(true)

      historyBuilder
        .addPledges(pledge1)
        .addPledges(pledge2)
        .addPledges(pledge3)
        .addPledges(pledge4)
      val history = historyBuilder.build()

      val res = PledgesUtils.gluePledges(history)
      res.getPledgesCount shouldBe 2
      res.getPledgesList.asScala.exists(_.getNumber == "2017-001-790661-535/1") shouldBe true
    }

    "glue combined pledges" in {
      val historyBuilder = buildPledge(VinInfoHistory.Status.OK)
      val pledge1 = Pledge.newBuilder().setDate(1L).setPerformanceDate(2000L).setInPledge(false)
      val pledge2 = Pledge.newBuilder().setDate(0L).setPerformanceDate(2000L).setInPledge(true)

      val pledge3 = Pledge.newBuilder().setNumber("2017-001-790661-535/1").setInPledge(true)
      val pledge4 = Pledge.newBuilder().setNumber("2017-001-790661-535").setInPledge(false)

      val pledge5 = Pledge.newBuilder().setNumber("2017-001").setInPledge(true)
      val pledge6 = Pledge.newBuilder().setNumber("2018-001").setInPledge(false)

      historyBuilder
        .addPledges(pledge1)
        .addPledges(pledge2)
        .addPledges(pledge3)
        .addPledges(pledge4)
        .addPledges(pledge5)
        .addPledges(pledge6)
      val history = historyBuilder.build()

      val res = PledgesUtils.gluePledges(history)
      res.getPledgesCount shouldBe 2
    }

    "glue pledges based on dates" in {
      val historyBuilder = buildPledge(VinInfoHistory.Status.OK)
      val pledge1 = Pledge.newBuilder().setDate(0L).setPerformanceDate(2000L).setInPledge(false)
      val pledge2 = Pledge.newBuilder().setDate(0L).setPerformanceDate(2000L).setInPledge(true)
      val pledge3 = Pledge.newBuilder().setDate(1L).setPerformanceDate(20L).setInPledge(false)
      val pledge4 = Pledge.newBuilder().setDate(2L).setPerformanceDate(20L).setInPledge(true)
      val pledge5 = Pledge.newBuilder().setDate(10L).setPerformanceDate(100L).setInPledge(false)
      val pledge6 = Pledge.newBuilder().setDate(10L).setPerformanceDate(20L).setInPledge(true)

      historyBuilder
        .addPledges(pledge1)
        .addPledges(pledge2)
        .addPledges(pledge3)
        .addPledges(pledge4)
        .addPledges(pledge5)
        .addPledges(pledge6)
      val history = historyBuilder.build()

      val res = PledgesUtils.gluePledges(history)
      res.getPledgesCount shouldBe 2
    }

    "do not glue different pledges" in {
      val historyBuilder = buildPledge(VinInfoHistory.Status.OK)

      // two pledges with same number and undefined getInPledge
      val pledge1 = Pledge.newBuilder().setNumber("2017-001-790661-531/1").setInPledge(false)
      val pledge2 = Pledge.newBuilder().setNumber("2017-001-790661-532").setInPledge(true)
      val pledge3 = Pledge.newBuilder().setNumber("2017-001-790661-533/1").setInPledge(true)
      val pledge4 = Pledge.newBuilder().setNumber("2017-001-790661-534").setInPledge(true)

      historyBuilder
        .addPledges(pledge1)
        .addPledges(pledge2)
        .addPledges(pledge3)
        .addPledges(pledge4)
      val history = historyBuilder.build()

      val res = PledgesUtils.gluePledges(history)
      res.getPledgesCount shouldBe 3
      res.getPledgesList.asScala.exists(_.getNumber == "2017-001-790661-532") shouldBe true

    }

  }

}
