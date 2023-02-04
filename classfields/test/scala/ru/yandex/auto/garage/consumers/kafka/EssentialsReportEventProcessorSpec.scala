package ru.yandex.auto.garage.consumers.kafka

import com.google.protobuf.util.Timestamps
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.VinReportModel
import ru.auto.api.vin.VinReportModel.InsuranceType._
import ru.auto.api.vin.VinReportModel.PtsBlock.{IntItem, StringItem}
import ru.auto.api.vin.VinReportModel.{InsurancesBlock, PtsBlock, RawVinEssentialsReport}
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.auto.api.vin.garage.GarageApiModel.InsuranceSource
import ru.yandex.auto.garage.dao.CardsService
import ru.yandex.auto.vin.decoder.events.Events.EssentialsReportEvent
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, InsuranceInfo}
import ru.yandex.auto.vin.decoder.utils.RequestInfo
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class EssentialsReportEventProcessorSpec extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter {

  private val cardsService = mock[CardsService]
  implicit private val ec: ExecutionContext = ExecutionContext.global
  implicit private val r: RequestInfo = RequestInfo.Empty
  private val emptyCard = GarageCard.newBuilder().build()
  private val now = System.currentTimeMillis()
  implicit val m = TestOperationalSupport

  private val processor = new EssentialsReportEventProcessor(cardsService)

  private def essentialReport(insurancesBlockOpt: Option[InsurancesBlock]): EssentialsReportEvent = {
    val pts =
      PtsBlock
        .newBuilder()
        .setMark(StringItem.newBuilder().setValueText("mark"))
        .setModel(StringItem.newBuilder().setValueText("model"))
        .setYear(IntItem.newBuilder().setValue(2020))

    val report =
      RawVinEssentialsReport
        .newBuilder()
        .setPtsInfo(pts)

    insurancesBlockOpt.foreach(report.setInsurances)

    EssentialsReportEvent.newBuilder().setReport(report).build()
  }

  "InsurancesProcessor" should {
    "process event without insurance in report" in {
      assert(processor.processCard(emptyCard, essentialReport(None)).isEmpty)
    }

    "process event with insurance in report" in {
      val insuranceBlock = buildInsurancesBlock(
        List(
          buildInsuranceItem(
            serialOpt = Some("OSAGO-XXX"),
            numberOpt = Some("120 23 432"),
            fromOpt = Some(now - 240.day.toMillis),
            toOpt = Some(now + 125.day.toMillis)
          ),
          buildInsuranceItem(
            serialOpt = Some("OSAGO-22"),
            numberOpt = Some("33 00 00"),
            fromOpt = Some(now - 605.day.toMillis),
            toOpt = Some(now - 240.day.toMillis)
          ),
          buildInsuranceItem(
            serialOpt = Some("KASKO-XXX"),
            numberOpt = Some("120 23 432"),
            fromOpt = Some(now - 245.day.toMillis),
            toOpt = Some(now + 130.day.toMillis),
            insuranceType = KASKO
          ),
          buildInsuranceItem(
            serialOpt = Some("KASKO-22"),
            numberOpt = Some("33 00 00"),
            fromOpt = Some(now - 610.day.toMillis),
            toOpt = Some(now - 245.day.toMillis),
            insuranceType = KASKO
          ),
          buildInsuranceItem(
            serialOpt = Some("KASKO-22"),
            numberOpt = Some("33 44 00"),
            fromOpt = Some(now),
            toOpt = None,
            insuranceType = KASKO
          )
        )
      )
      val result: Option[(GarageCard, Long)] = processor.processCard(emptyCard, essentialReport(Some(insuranceBlock)))

      assert(result.nonEmpty)
      val resultInsurances = result.get._1.getInsuranceInfo.getInsurancesList.asScala
      assert(resultInsurances.size == 5)
      assert(resultInsurances.exists(_.getTo.getSeconds == Timestamps.fromMillis(now + 125.day.toMillis).getSeconds))
      assert(resultInsurances.exists(_.getTo.getSeconds == Timestamps.fromMillis(now - 240.day.toMillis).getSeconds))
      assert(resultInsurances.exists(_.getTo.getSeconds == Timestamps.fromMillis(now + 130.day.toMillis).getSeconds))
      assert(resultInsurances.exists(_.getTo.getSeconds == Timestamps.fromMillis(now - 245.day.toMillis).getSeconds))
      assert(resultInsurances.exists(_.getTo.getSeconds == Timestamps.fromMillis(now + 364.day.toMillis).getSeconds))
      assert(resultInsurances.forall(_.getSource == InsuranceSource.RSA))
    }
    "process event with insurance in report with wrong" in {
      val insuranceBlock = buildInsurancesBlock(
        List(
          buildInsuranceItem(
            serialOpt = Some("some_serial"),
            numberOpt = Some("some_number"),
            fromOpt = None,
            toOpt = Some(now + 980.day.toMillis)
          ),
          buildInsuranceItem(
            serialOpt = Some(" "),
            numberOpt = Some("kasko_without_serial"),
            fromOpt = Some(now + 400.day.toMillis),
            toOpt = Some(now + 600.day.toMillis),
            insuranceType = KASKO
          ),
          buildInsuranceItem(
            serialOpt = Some(" "),
            numberOpt = Some(" "),
            fromOpt = Some(now - 240.day.toMillis),
            toOpt = Some(now + 125.day.toMillis)
          ),
          buildInsuranceItem(
            serialOpt = Some("ОSАGО-22"),
            numberOpt = Some("33 00 00"),
            fromOpt = Some(now - 605.day.toMillis),
            toOpt = Some(now - 240.day.toMillis)
          ),
          buildInsuranceItem(
            serialOpt = Some("KASKO-XXX"),
            numberOpt = Some("120 23 432"),
            fromOpt = Some(now - 245.day.toMillis),
            toOpt = Some(now + 130.day.toMillis),
            insuranceType = KASKO
          ),
          buildInsuranceItem(
            serialOpt = Some("KASKO-22"),
            numberOpt = Some("33 00 00"),
            fromOpt = Some(now - 610.day.toMillis),
            toOpt = Some(now - 245.day.toMillis),
            insuranceType = KASKO
          ),
          buildInsuranceItem(
            serialOpt = Some("KASKO-66"),
            numberOpt = Some("32 11 43"),
            fromOpt = Some(now),
            toOpt = None,
            insuranceType = KASKO
          )
        )
      )
      val result: Option[(GarageCard, Long)] = processor.processCard(emptyCard, essentialReport(Some(insuranceBlock)))
      assert(result.nonEmpty)
      val resultInsurances = result.get._1.getInsuranceInfo.getInsurancesList.asScala
      assert(resultInsurances.size == 5)
      assert(
        resultInsurances.exists(ins =>
          ins.getTo.getSeconds == Timestamps
            .fromMillis(now + 364.day.toMillis)
            .getSeconds && ins.getNumber == "32 11 43"
        )
      )
      assert(
        resultInsurances.exists(ins =>
          ins.getFrom.getSeconds == Timestamps.fromMillis(now + 400.day.toMillis).getSeconds &&
            ins.getTo.getSeconds == Timestamps.fromMillis(now + 600.day.toMillis).getSeconds &&
            ins.getSerial == "" && ins.getNumber == "kasko_without_serial"
        )
      )
      assert(!resultInsurances.exists(ins => ins.getSerial == "some_serial" && ins.getNumber == "some_number"))
    }

    "process event with insurance in report and insurances in garage card" in {
      val insuranceBlock = buildInsurancesBlock(
        List(
          buildInsuranceItem(
            serialOpt = Some("ОSАGО-FIRST"),
            numberOpt = Some("00 11 22"),
            fromOpt = None,
            toOpt = None
          ),
          buildInsuranceItem(
            serialOpt = Some("OSAGO-XXX"),
            numberOpt = Some("120 23 432"),
            fromOpt = Some(now - 240.day.toMillis),
            toOpt = Some(now + 125.day.toMillis)
          ),
          buildInsuranceItem(
            serialOpt = Some("ОSАGО-22"),
            numberOpt = Some("33 00 00"),
            fromOpt = Some(now - 605.day.toMillis),
            toOpt = Some(now - 240.day.toMillis)
          ),
          buildInsuranceItem(
            serialOpt = Some("KASKO-XXX"),
            numberOpt = Some("120 23 432"),
            fromOpt = Some(now - 245.day.toMillis),
            toOpt = Some(now + 130.day.toMillis),
            insuranceType = KASKO
          ),
          buildInsuranceItem(
            serialOpt = Some("KASKO-22"),
            numberOpt = Some("33 00 00"),
            fromOpt = Some(now - 610.day.toMillis),
            toOpt = Some(now - 245.day.toMillis),
            insuranceType = KASKO
          ),
          buildInsuranceItem(
            serialOpt = Some("ОSАGО-22"),
            numberOpt = Some("1234567890"),
            fromOpt = Some(now + 10.day.toMillis),
            toOpt = None,
            insuranceType = OSAGO
          ),
          buildInsuranceItem(
            serialOpt = None,
            numberOpt = Some("33"),
            fromOpt = Some(now - 610.day.toMillis),
            toOpt = Some(now - 245.day.toMillis),
            insuranceType = KASKO
          )
        )
      )
      val existedInsurances =
        for (i <- 0 to 54)
          yield GarageSchema.Insurance
            .newBuilder()
            .setNumber(i.toString)
            .setInsuranceType(KASKO)
            .setSource(InsuranceSource.MANUAL)
            .build()

      val garageCard = emptyCard.toBuilder
        .setInsuranceInfo(
          InsuranceInfo
            .newBuilder()
            .addAllInsurances(existedInsurances.asJava)
            .build()
        )
        .build()
      val result = processor.processCard(garageCard, essentialReport(Some(insuranceBlock)))
      assert(result.nonEmpty)
      val resultInsurances = result.get._1.getInsuranceInfo.getInsurancesList.asScala
      assert(resultInsurances.size == 60)
      assert(
        resultInsurances.exists(ins =>
          ins.getTo.getSeconds == Timestamps.fromMillis(now + 374.day.toMillis).getSeconds
            && ins.getNumber == "1234567890"
            && ins.getSerial == "OSAGO-22"
        )
      )
      assert(
        !resultInsurances.exists(ins =>
          ins.getNumber == "00 11 22"
            && ins.getSerial == "OSAGO-FIRST"
        )
      )
      assert(
        resultInsurances
          .find(ins => ins.getNumber == "33" && ins.getSerial.isEmpty)
          .get
          .getSource == InsuranceSource.MANUAL
      )
    }
  }

  private def buildInsurancesBlock(insurances: List[VinReportModel.InsuranceItem]): VinReportModel.InsurancesBlock = {
    VinReportModel.InsurancesBlock
      .newBuilder()
      .addAllInsurances(insurances.asJava)
      .build()
  }

  private def buildInsuranceItem(
      serialOpt: Option[String],
      numberOpt: Option[String],
      fromOpt: Option[Long] = Some(now - 240.day.toMillis),
      toOpt: Option[Long] = Some(now + 125.day.toMillis),
      insuranceType: VinReportModel.InsuranceType = OSAGO,
      insuranceStatus: VinReportModel.InsuranceStatus = VinReportModel.InsuranceStatus.ACTIVE,
      eventType: EventType = EventType.SH_RSA_INSURANCE_DETAILS): VinReportModel.InsuranceItem = {
    val insuranceItemBuilder = VinReportModel.InsuranceItem
      .newBuilder()
      .setInsuranceType(insuranceType)
      .setInsuranceStatus(insuranceStatus)
      .setMeta(
        VinReportModel.RecordMeta
          .newBuilder()
          .setSource(
            VinReportModel.RecordMeta.SourceMeta
              .newBuilder()
              .setIsAnonymous(false)
              .setEventType(eventType)
              .addAllAutoruClientIds(List.empty.asJava)
          )
          .build()
      )
      .setDate(now)
      .setInsurerName("""АО "АльфаСтрахование"""")
      .setPartnerName("Партнёр Авто.ру")
      .setRegionName("Ставропольский край, г Ставрополь")
      .setPolicyStatus("Выдан страхователю")

    numberOpt.foreach(insuranceItemBuilder.setNumber)
    serialOpt.foreach(insuranceItemBuilder.setSerial)
    toOpt.foreach(insuranceItemBuilder.setTo)
    fromOpt.foreach(insuranceItemBuilder.setFrom)

    insuranceItemBuilder.build()

  }

}
