package ru.yandex.auto.vin.decoder.report.processors

import auto.carfax.common.utils.tracing.Traced
import cats.syntax.option._
import com.google.protobuf.util.Timestamps
import org.scalatest.enablers.Emptiness.{emptinessOfGenTraversable, emptinessOfOption}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model._
import ru.yandex.auto.vin.decoder.model.vin.{AutocodeTaxi, Offer}
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex.AudatexPartner
import ru.yandex.auto.vin.decoder.proto.CommonModels.{OriginalSize, PhotoInfo}
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.Auction.Lot
import ru.yandex.auto.vin.decoder.proto.VinHistory.ServiceBook.Order
import ru.yandex.auto.vin.decoder.proto.VinHistory._
import ru.yandex.auto.vin.decoder.providers.certification.BrandCertifications
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.history.entities._
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.taxi.{RsaTaxi, TaxiEntity}
import ru.yandex.auto.vin.decoder.report.processors.entities.MarkModelSupport.MarkModelSource
import ru.yandex.auto.vin.decoder.report.processors.entities.OfferEntity.OfferOptSource
import ru.yandex.auto.vin.decoder.report.processors.entities.audatex.AudatexReportEntity
import ru.yandex.auto.vin.decoder.report.processors.entities.autoru.alyansauto.AlyansAutoSaleEntity
import ru.yandex.auto.vin.decoder.report.processors.entities.partner.DkpHistoryEntity
import ru.yandex.auto.vin.decoder.report.processors.entities.taxi.TaxiHistoryEntity
import ru.yandex.auto.vin.decoder.report.processors.entities.{OfferEntity, TechInspectionEntity}
import ru.yandex.auto.vin.decoder.report.processors.resolution.ResolutionWrappers.KmageHistory
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.mockito.MockitoSupport

import java.time.Year
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class KmageHistoryBuilderSpec extends AnyWordSpecLike with Matchers with MockitoSupport with MockedFeatures {

  private val vinCode = "JTMHT05J605098077"
  private val EmptyResolutionData = ResolutionData.empty(VinCode(vinCode))

  implicit val t: Traced = Traced.empty

  "KmageHistoryBuilder" should {

    "build empty raw history if resolution data is empty" in {
      val history = buildRawHistoryEntities(EmptyResolutionData)
      history shouldBe empty
    }

    "hide mileage if it is 1" in {
      val mileage = 1
      val event = MileageEvent(mileage, System.currentTimeMillis)
      val manufacturedYear = Year.now().minusYears(10).getValue
      val data = resolutionData(
        autotekaServices = autotekaServices(List(event)),
        manufacturedYear = manufacturedYear.some
      )
      val history = buildRawHistoryEntities(data)

      history.head.mileage shouldBe empty
    }

    "do not hide low mileage if manufactured year is unknown" in {
      val mileage = 1000
      val event = MileageEvent(mileage, System.currentTimeMillis)
      val data = resolutionData(
        autotekaServices = autotekaServices(List(event)),
        manufacturedYear = None
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe mileage
    }

    "do not hide mileage if it is big enough" in {
      val mileage = 1001
      val event = MileageEvent(mileage, System.currentTimeMillis)
      val manufacturedYear = Year.now().getValue
      val data = resolutionData(
        autotekaServices = autotekaServices(List(event)),
        manufacturedYear = manufacturedYear.some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe mileage
    }

    "do not hide low mileage if date with manufactured year diff is less then 3 years" in {
      val mileage = 1000
      val event = MileageEvent(mileage, System.currentTimeMillis)
      val manufacturedYear = Year.now().minusYears(2).getValue
      val data = resolutionData(
        autotekaServices = autotekaServices(List(event)),
        manufacturedYear = manufacturedYear.some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe mileage
    }

    "hide mileage if it is not big enough and date with manufactured year diff is greater then 3 years" in {
      val mileage = 1000
      val event = MileageEvent(mileage, System.currentTimeMillis)
      val manufacturedYear = Year.now().minusYears(3).getValue
      val data = resolutionData(
        autotekaServices = autotekaServices(List(event)),
        manufacturedYear = manufacturedYear.some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage shouldBe empty
    }

    "do not hide mileage for offer entities even if it is 1" in {
      val mileage = 1
      val event = MileageEvent(mileage, System.currentTimeMillis)
      val data = resolutionData(offers = offers(event).some)
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[OfferPreparedHistoryEntity]
      history.head.mileage.get shouldBe 1
    }

    "highlight entity if it mileage value is less than previous entity mileage value more than 10000 mileages" in {
      val now = System.currentTimeMillis
      val currentMileage = 1000
      val currentEvent = MileageEvent(currentMileage, now)
      val previousMileage = 11000
      val previousEvent = MileageEvent(previousMileage, now - 1)
      val data = resolutionData(
        offers = offers(currentEvent).some,
        autotekaServices = autotekaServices(List(previousEvent))
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe previousMileage
      history(1) shouldBe a[OfferPreparedHistoryEntity]
      history(1).mileage.get shouldBe currentMileage
      history(1).isRed shouldBe true
    }

    "do not highlight entity if it mileage value is not less than previous entity mileage value more than 10000 mileages" in {
      val now = System.currentTimeMillis
      val currentMileage = 1000
      val currentEvent = MileageEvent(currentMileage, now)
      val previousMileage = 10000
      val previousEvent = MileageEvent(previousMileage, now - 1)
      val data = resolutionData(
        offers = offers(currentEvent).some,
        autotekaServices = autotekaServices(List(previousEvent))
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe previousMileage
      history(1) shouldBe a[OfferPreparedHistoryEntity]
      history(1).mileage.get shouldBe currentMileage
      history(1).isRed shouldBe false
    }

    "do not highlight entity if it mileage value is 0" in {
      val now = System.currentTimeMillis
      val currentMileage = 0
      val currentEvent = MileageEvent(currentMileage, now)
      val previousMileage = 11000
      val previousEvent = MileageEvent(previousMileage, now - 1)
      val data = resolutionData(
        offers = offers(currentEvent).some,
        autotekaServices = autotekaServices(List(previousEvent))
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe previousMileage
      history(1) shouldBe a[OfferPreparedHistoryEntity]
      history(1).mileage.get shouldBe currentMileage
      history(1).isRed shouldBe false
    }

    "do not highlight entity if previous entity timestamp value is 0" in {
      val currentMileage = 1000
      val currentEvent = MileageEvent(currentMileage, System.currentTimeMillis)
      val previousMileage = 11000
      val previousEvent = MileageEvent(previousMileage, 0)
      val data = resolutionData(
        offers = offers(currentEvent).some,
        autotekaServices = autotekaServices(List(previousEvent))
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe previousMileage
      history(1) shouldBe a[OfferPreparedHistoryEntity]
      history(1).mileage.get shouldBe currentMileage
      history(1).isRed shouldBe false
    }

    "hide technical inspection mileages if it is highlighted" in {
      val now = System.currentTimeMillis
      val currentMileage = 1000
      val currentEvent = MileageEvent(currentMileage, now)
      val previousEvent = MileageEvent(12000, now - 1)
      val data = resolutionData(
        autotekaServices = autotekaServices(List(previousEvent)),
        techInspections = autocodeMileages(List(currentEvent)).some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe 12000
      history(1) shouldBe a[TechInspectionPreparedHistoryEntity]
      history(1).mileage shouldBe empty
      history(1).isRed shouldBe false
    }

    "hide technical inspection mileages if it causes mileage highlighting for next entities" in {
      val now = System.currentTimeMillis
      val currentMileage = 120000
      val currentEvent = MileageEvent(currentMileage, now - 1.day.toMillis * 80)
      val nextEvents =
        List(MileageEvent(10000, now - 1.day.toMillis * 40), MileageEvent(13000, now))
      val data = resolutionData(
        techInspections = autocodeMileages(List(currentEvent)).some,
        autotekaServices = autotekaServices(nextEvents)
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 3
      history.head shouldBe a[TechInspectionPreparedHistoryEntity]
      history.head.mileage shouldBe empty
      history(1) shouldBe a[ServicePreparedHistoryEntity]
      history(1).mileage.get shouldBe nextEvents.head.mileage
      history(1).isRed shouldBe false
      history(2) shouldBe a[ServicePreparedHistoryEntity]
      history(2).mileage.get shouldBe nextEvents(1).mileage
      history(2).isRed shouldBe false
    }

    "hide technical inspection mileages if it causes mileage highlighting for next entities, even if next entity without kmage" in {
      val now = System.currentTimeMillis
      val currentMileage = 120000
      val currentEvent = MileageEvent(currentMileage, now - 1.day.toMillis * 120)
      val nextEvents =
        List(MileageEvent(10000, now - 1.day.toMillis * 40), MileageEvent(13000, now))

      val data = resolutionData(
        techInspections = autocodeMileages(List(currentEvent)).some,
        autotekaServices = sbWithoutKmage(now - 1.day.toMillis * 80) +: autotekaServices(nextEvents)
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 4
      history.head shouldBe a[TechInspectionPreparedHistoryEntity]
      history.head.mileage shouldBe empty
      history(1) shouldBe a[ServicePreparedHistoryEntity]
      history(1).mileage shouldBe empty
      history(1).isRed shouldBe false
      history(2) shouldBe a[ServicePreparedHistoryEntity]
      history(2).mileage.get shouldBe nextEvents.head.mileage
      history(2).isRed shouldBe false
      history(3) shouldBe a[ServicePreparedHistoryEntity]
      history(3).mileage.get shouldBe nextEvents(1).mileage
      history(3).isRed shouldBe false
    }

    "do not use mileages without date for technical inspection" in {
      val data = resolutionData(
        techInspections = autocodeMileages(List(MileageEvent(120000, 0))).some
      )
      val history = buildRawHistoryEntities(data)

      history.count(_.isInstanceOf[TechInspectionEntity]) shouldBe 0
    }

    "do not skip mark-model entity if it mark and model contains in current entity mark and model (case insensitive)" in {
      val event = MileageEvent(1000, System.currentTimeMillis)
      val offer = offers(event, "bmw-cars".some, "m5i".some)
      val data = resolutionData(offers = offer.some)
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[OfferPreparedHistoryEntity]
      history.head.mileage.get shouldBe 1000
    }

    "skip dkp entity if it mileage value diff with previous entity mileage value greater than 30k per year" in {
      val now = System.currentTimeMillis
      val currentMileage = 40000
      val event = MileageEvent(currentMileage, now)
      val dkp = List(dkpMileages(event))
      val previousMileage = 10000
      val previousEvent = MileageEvent(previousMileage, now - 1.day.toMillis * 365)
      val data = resolutionData(dkpMileages = dkp, autotekaServices = autotekaServices(List(previousEvent)))
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe previousMileage
    }

    "do not skip dkp entity if it mileage value diff with previous entity mileage value less than 30k per year" in {
      val now = System.currentTimeMillis
      val currentMileage = 30000
      val event = MileageEvent(currentMileage, now)
      val dkp = List(dkpMileages(event))
      val previousMileage = 10000
      val previousEvent = MileageEvent(previousMileage, now - 1.day.toMillis * 365)
      val data = resolutionData(dkpMileages = dkp, autotekaServices = autotekaServices(List(previousEvent)))
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe previousMileage
      history(1) shouldBe a[PartnerPreparedHistoryEntity]
      history(1).mileage.get shouldBe currentMileage
    }

    "skip editable entity if it is hidden" in {
      val event = MileageEvent(1000, System.currentTimeMillis)
      val autocode = autocodeMileages(List(event))
      val id = s"${event.timestamp}:${event.mileage}"
      val confirmed = confirmedKmage(id, EventType.AUTOCODE_DIAGNOSTIC_CARDS, isHidden = true)
      val data = resolutionData(techInspections = autocode.some, confirmed = confirmed.some)
      val history = buildRawHistoryEntities(data)

      history shouldBe empty
    }

    "skip PhotoEvent entity if it is hidden" in {
      val skipMark = "suzuki"
      val skipModel = "sx4"
      val skipData = System.currentTimeMillis
      val skipExternalPhotoUrl = "some_url"
      val skipId = s"$skipExternalPhotoUrl:$skipExternalPhotoUrl-$skipData"

      val noSkipMark = "kia"
      val noSkipModel = "rio"
      val noSkipData = System.currentTimeMillis - 1.day.toMillis

      val confirmed = confirmedKmage(skipId, EventType.AVTONOMER_PHOTO, isHidden = true)

      val photoInfo = PhotoInfo
        .newBuilder()
        .setExternalPhotoUrl(skipExternalPhotoUrl)
        .setMdsPhotoInfo(
          MdsPhotoInfo
            .newBuilder()
            .setName("name")
            .setNamespace("namespace")
            .setGroupId(23)
            .build()
        )
        .setOriginalSize(OriginalSize.newBuilder().setX(100).setY(200).build())
        .build()

      val skipEvent =
        PhotoEventEntity(skipData, skipMark, skipModel, s"${skipMark}_$skipModel", Seq(photoInfo, photoInfo))
      val noSkipEvent =
        PhotoEventEntity(noSkipData, noSkipMark, noSkipModel, s"${noSkipMark}_$noSkipModel", Seq(photoInfo, photoInfo))

      val avtonomerEntities: List[VinInfoHistory] = avtonomerEntity(List(skipEvent, noSkipEvent))

      val data = resolutionData(photo = avtonomerEntities, confirmed = confirmed.some)

      val history = buildRawHistoryEntities(data)

      history.size shouldBe 1
      history.head shouldBe a[PhotoPreparedHistoryEntity]
      history.head.asInstanceOf[PhotoPreparedHistoryEntity].event.getMark shouldBe noSkipMark
      history.head.asInstanceOf[PhotoPreparedHistoryEntity].event.getModel shouldBe noSkipModel
      history.head.asInstanceOf[PhotoPreparedHistoryEntity].event.getDate shouldBe noSkipData
    }

    "do not skip editable entity if it is not hidden" in {
      val mileage = 1000
      val event = MileageEvent(mileage, System.currentTimeMillis)
      val autocode = autocodeMileages(List(event))
      val id = s"${event.timestamp}:${event.mileage}"
      val confirmed = confirmedKmage(id, EventType.AUTOCODE_MILEAGE, isHidden = false)
      val data = resolutionData(techInspections = autocode.some, confirmed = confirmed.some)
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[TechInspectionPreparedHistoryEntity]
      history.head.mileage.get shouldBe mileage
    }

    "hide mileage if it mileage value diff with previous entity mileage value greater than 1m per year" in {
      val now = System.currentTimeMillis
      val currentMileage = 30000
      val middleMileage = 1020000
      val currentEvent = MileageEvent(currentMileage, now + 1.day.toMillis * 200)
      val middleEvent = MileageEvent(middleMileage, now)
      val previousMileage = 10000
      val previousEvent = MileageEvent(previousMileage, now - 1.day.toMillis * 365)
      val data = resolutionData(
        autotekaServices = autotekaServices(List(previousEvent, middleEvent, currentEvent))
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 3
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history(1) shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage.get shouldBe previousMileage
      history(1).mileage shouldBe empty
      history(2) shouldBe a[ServicePreparedHistoryEntity]
      history(2).mileage.get shouldBe currentMileage
    }

    "do not hide mileage if it mileage value diff with previous entity mileage value less than 1m per year" in {
      val now = System.currentTimeMillis
      val currentMileage = 1000000
      val currentEvent = MileageEvent(currentMileage, now)
      val previousMileage = 10000
      val previousEvent = MileageEvent(previousMileage, now - 1.day.toMillis * 365)
      val data = resolutionData(
        offers = offers(currentEvent).some,
        autotekaServices = autotekaServices(List(previousEvent))
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history(1) shouldBe a[OfferPreparedHistoryEntity]
      history.head.mileage.get shouldBe previousMileage
      history(1).mileage.get shouldBe currentMileage
    }

    "hide mileage if it first mileage value and mileage greater than 1m per year and manufacture year equals mileage set year" in {
      val now = System.currentTimeMillis
      val firstMileage = 1000001
      val firstEvent = MileageEvent(firstMileage, now)
      val manufacturedYear = Year.now().getValue
      val currentMileage = 30000
      val currentEvent = MileageEvent(currentMileage, now + 1.day.toMillis * 200)
      val data = resolutionData(
        autotekaServices = autotekaServices(List(firstEvent, currentEvent)),
        manufacturedYear = manufacturedYear.some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage shouldBe empty
      history(1) shouldBe a[ServicePreparedHistoryEntity]
      history(1).mileage.get shouldBe currentMileage
    }

    "do not hide mileage if it first mileage value and mileage less than 1m per year and manufacture year equals mileage set year" in {
      val now = System.currentTimeMillis
      val currentMileage = 999998
      val currentEvent = MileageEvent(currentMileage, now)
      val manufacturedYear = Year.now().getValue
      val data = resolutionData(
        offers = offers(currentEvent, manufacturedYear = manufacturedYear.some).some,
        manufacturedYear = manufacturedYear.some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[OfferPreparedHistoryEntity]
      history.head.mileage.get shouldBe currentMileage
    }

    "hide mileage if it first mileage value and mileage GREATER than 1m per year and manufacture year do NOT equals mileage set year" in {
      val now = System.currentTimeMillis
      val currentMileage = 30000
      val currentEvent = MileageEvent(currentMileage, now + 1.day.toMillis * 200)
      val firstMileage = 3000001
      val firstEvent = MileageEvent(firstMileage, now)
      val manufacturedYear = Year.now().minusYears(3).getValue
      val data = resolutionData(
        autotekaServices = autotekaServices(List(firstEvent, currentEvent)),
        manufacturedYear = manufacturedYear.some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head shouldBe a[ServicePreparedHistoryEntity]
      history.head.mileage shouldBe empty
      history(1) shouldBe a[ServicePreparedHistoryEntity]
      history(1).mileage.get shouldBe currentMileage
    }

    "do not hide mileage if it first mileage value and mileage LESS than 1m per year and manufacture year do NOT equals mileage set year" in {
      val now = System.currentTimeMillis
      val currentMileage = 2999000
      val currentEvent = MileageEvent(currentMileage, now)
      val manufacturedYear = Year.now().minusYears(3).getValue
      val data = resolutionData(
        offers = offers(currentEvent, manufacturedYear = manufacturedYear.some).some,
        manufacturedYear = manufacturedYear.some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head shouldBe a[OfferPreparedHistoryEntity]
      history.head.mileage.get shouldBe currentMileage
    }

    "hide low mileage LESS than 131" in {
      val mileage = 130
      val currentEvent = MileageEvent(mileage, System.currentTimeMillis)
      val data = resolutionData(
        autotekaServices = autotekaServices(List(currentEvent)),
        techInspections = autocodeMileages(List(currentEvent)).some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.flatMap(_.mileage) shouldBe empty
    }

    "do not hide low mileage GREATER than 130" in {
      val mileage = 131
      val currentEvent = MileageEvent(mileage, System.currentTimeMillis)
      val data = resolutionData(
        autotekaServices = autotekaServices(List(currentEvent)),
        techInspections = autocodeMileages(List(currentEvent)).some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 2
      history.head.mileage.get shouldBe mileage
      history(1).mileage.get shouldBe mileage
    }

    "do not skip offer with mileage GREATER than 130" in {
      val mileage = 131
      val currentEvent = MileageEvent(mileage, System.currentTimeMillis)
      val data = resolutionData(
        offers = offers(currentEvent).some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 1
      history.head.mileage.get shouldBe mileage
    }

    "hide twisting mileage if prev and next mileage ok by kmage and date" in {
      val prevMileage = 124000
      val currentMileage = 40000
      val nextMileage = 131340
      val prevEvent = MileageEvent(prevMileage, System.currentTimeMillis - 1.day.toMillis * 60)
      val currentEvent = MileageEvent(currentMileage, System.currentTimeMillis)
      val nextEvent = MileageEvent(nextMileage, System.currentTimeMillis + 1.day.toMillis * 60)
      val data = resolutionData(
        dkpMileages = List(dkpMileages(prevEvent)),
        autotekaServices = autotekaServices(List(currentEvent)),
        techInspections = autocodeMileages(List(nextEvent)).some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 3
      history.head.mileage.get shouldBe prevMileage
      history(1).mileage shouldBe empty
      history(2).mileage.get shouldBe nextMileage
    }

    "do not hide twisting mileage if prev and next mileage ok by kmage and date" in {
      val prevMileage = 124000
      val currentMileage = 90000
      val nextMileage = 81340
      val prevEvent = MileageEvent(prevMileage, System.currentTimeMillis - 1.day.toMillis * 60)
      val currentEvent = MileageEvent(currentMileage, System.currentTimeMillis)
      val nextEvent = MileageEvent(nextMileage, System.currentTimeMillis + 1.day.toMillis * 60)
      val data = resolutionData(
        dkpMileages = List(dkpMileages(prevEvent)),
        autotekaServices = autotekaServices(List(currentEvent)),
        techInspections = autocodeMileages(List(nextEvent)).some
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 3
      history.head.mileage.get shouldBe prevMileage
      history(1).mileage.get shouldBe currentMileage
      history(2).mileage.get shouldBe nextMileage
    }

    "do not hide twisting mileage for offers" in {
      val prevMileage = 124000
      val currentMileage = 40000
      val nextMileage = 131340
      val prevEvent = MileageEvent(prevMileage, System.currentTimeMillis - 1.day.toMillis * 60)
      val currentEvent = MileageEvent(currentMileage, System.currentTimeMillis)
      val nextEvent = MileageEvent(nextMileage, System.currentTimeMillis + 1.day.toMillis * 60)
      val data = resolutionData(
        dkpMileages = List(dkpMileages(prevEvent)),
        offers = offers(currentEvent).some,
        autotekaServices = autotekaServices(List(nextEvent))
      )
      val history = buildRawHistoryEntities(data)

      history.length shouldBe 3
      history.head.mileage.get shouldBe prevMileage
      history(1).mileage.get shouldBe currentMileage
      history(2).mileage.get shouldBe nextMileage
    }

    "group dkp mileages and filter only most recent from every group" in {
      val now = System.currentTimeMillis
      val monthAgo = now - (30.days).toMillis
      val weekAgo = now - (7.days).toMillis
      val nextWeek = now + (7.days).toMillis
      val secondNextWeek = now + (14.days).toMillis
      Seq(
        // 1 группа пробегов: все месяц назад
        (monthAgo, 100),
        (monthAgo + 10, 110),
        (monthAgo + 20, 110),
        // 2 группа пробегов: трёхнедельный отрезок с интервалами <= 14 дням
        (weekAgo, 150),
        (now, 160),
        (nextWeek, 170),
        (secondNextWeek, 180)
      ).map { case (date, kmAge) =>
        DkpHistoryEntity(
          Mileage.newBuilder().setDate(date).setValue(kmAge).build(),
          Map.empty
        )
      }.permutations
        .take(5)
        .foreach { entities =>
          val entitiesFiltered = kmageBuilder.prepareDkp(entities)
          entitiesFiltered.length shouldBe 3
          entitiesFiltered.head.mileage.getValue shouldBe 110
          entitiesFiltered(1).mileage.getValue shouldBe 170
          entitiesFiltered(2).mileage.getValue shouldBe 180
        }
    }

    "preserve hidden entities" in {
      val offerId = "1102967469-9aa41466"
      val offers = VinInfo
        .newBuilder()
        .setOfferId(offerId)
        .setMark("BMW")
        .setModel("X5")
        .build()
      val confirmed = VinInfoHistory
        .newBuilder()
        .addConfirmedKmages(
          KmAgeConfirmed
            .newBuilder()
            .setEventType(OfferEntity.eventType)
            .setId(offerId)
            .setIsHidden(true)
        )
        .build()
      val rd = EmptyResolutionData.copy(offers = List(offers), confirmed = confirmed.some)
      val entities = kmageBuilder.buildEntities(entity, rd, vinCode)
      entities.size shouldBe 1
    }

    "skip hidden entities" in {
      val offerId = "1102967469-9aa41466"
      val offers = VinInfo
        .newBuilder()
        .setOfferId(offerId)
        .setMark("BMW")
        .setModel("X5")
        .build()
      val confirmed = VinInfoHistory
        .newBuilder()
        .addConfirmedKmages(
          KmAgeConfirmed
            .newBuilder()
            .setEventType(OfferEntity.eventType)
            .setId(offerId)
            .setIsHidden(true)
        )
        .build()
      val rd = EmptyResolutionData.copy(offers = List(offers), confirmed = confirmed.some)
      val entities = kmageBuilder.buildEntities(entity, rd, vinCode)
      val preparedEntities =
        kmageBuilder.prepareEntities(entities, 2018.some, None, None, AudatexDealers(List.empty[AudatexPartner]))
      preparedEntities.isEmpty shouldBe true
    }

    "skip audatex entities for non-authorized dealer" in {
      val prepared = Prepared(1, 1, 1, audatexReport, "")
      val rd = EmptyResolutionData.copy(audatex = Some(prepared))
      val entities = kmageBuilder.buildEntities(entity, rd, vinCode)
      val preparedEntities =
        kmageBuilder.prepareEntities(entities, 2018.some, Some(123L), None, AudatexDealers(List.empty[AudatexPartner]))
      preparedEntities.isEmpty shouldBe true
    }

//    "skip audatex entities for non-supported mark even if dealer is authorized" in {
//      val prepared = Prepared(1, 1, 1, audatexReport, "")
//      val rd = EmptyResolutionData.copy(audatex = Some(prepared))
//      val entities = kmageBuilder.buildEntities(entity, rd, vinCode)
//      val preparedEntities = kmageBuilder.prepareEntities(
//        entities,
//        2018.some,
//        Some(262L),
//        Some("VAZ"),
//        AudatexDealers(List.empty[AudatexPartner])
//      )
//      preparedEntities.isEmpty shouldBe true
//    }

    "not skip audatex entities for authorized dealer" in {
      val prepared = Prepared(1, 1, 1, audatexReport, "")
      val rd = EmptyResolutionData.copy(audatex = Some(prepared))
      val entities = kmageBuilder.buildEntities(entity, rd, vinCode)
      val preparedEntities =
        kmageBuilder.prepareEntities(
          entities,
          2018.some,
          Some(262L),
          None,
          AudatexDealers(List(AudatexPartner(Set(262L), "", Audatex.Credentials("", ""))))
        )
      preparedEntities.nonEmpty shouldBe true
    }

    "not skip audatex entities for non-dealer user" in {
      val prepared = Prepared(1, 1, 1, audatexReport, "")
      val rd = EmptyResolutionData.copy(audatex = Some(prepared))
      val entities = kmageBuilder.buildEntities(entity, rd, vinCode)
      val preparedEntities =
        kmageBuilder.prepareEntities(entities, 2018.some, None, None, AudatexDealers(List.empty[AudatexPartner]))
      preparedEntities.nonEmpty shouldBe true
    }

    "build taxi entities" must {
      def convert(taxiEntities: List[TaxiEntity]) = {
        val rd = EmptyResolutionData.copy(taxi = Some(taxiEntities))
        val entities = kmageBuilder.buildEntities(entity, rd, vinCode)
        kmageBuilder.prepareEntities(entities, 2018.some, None, None, AudatexDealers(List.empty[AudatexPartner]))
      }
      "preserve order for non-overlaps" in {
        val result = convert(
          List(
            rsaTaxi(1, 2),
            autocodeTaxi(4, 10),
            rsaTaxi(12, 15)
          )
        )
        assert(result.size == 3)
        val firstRsa = result.head.entity.asInstanceOf[TaxiHistoryEntity].taxiEntity
        val lastRsa = result(2).entity.asInstanceOf[TaxiHistoryEntity].taxiEntity
        assert(firstRsa.dateStart.contains(days(1)))
        assert(firstRsa.dateEnd.contains(days(2)))
        assert(lastRsa.dateStart.contains(days(12)))
        assert(lastRsa.dateEnd.contains(days(15)))
      }

      "correctly cut overlapping rsa periods" in {
        val result = convert(
          List(
            rsaTaxi(1, 6),
            autocodeTaxi(4, 10),
            rsaTaxi(8, 15)
          )
        )
        assert(result.size == 3)
        val firstRsa = result.head.entity.asInstanceOf[TaxiHistoryEntity].taxiEntity
        val lastRsa = result(2).entity.asInstanceOf[TaxiHistoryEntity].taxiEntity

        assert(firstRsa.dateStart.contains(days(1)))
        assert(firstRsa.dateEnd.contains(days(3)))

        assert(lastRsa.dateStart.contains(days(11)))
        assert(lastRsa.dateEnd.contains(days(15)))
      }

      "correctly cut encompassing rsa periods" in {
        val result = convert(
          List(
            rsaTaxi(1, 20),
            autocodeTaxi(4, 10)
          )
        )
        assert(result.size == 3)
        val firstRsa = result.head.entity.asInstanceOf[TaxiHistoryEntity].taxiEntity
        val lastRsa = result(2).entity.asInstanceOf[TaxiHistoryEntity].taxiEntity

        assert(firstRsa.dateStart.contains(days(1)))
        assert(firstRsa.dateEnd.contains(days(3)))

        assert(lastRsa.dateStart.contains(days(11)))
        assert(lastRsa.dateEnd.contains(days(20)))
      }

      "correctly cut rsa within encompassing autocode periods" in {
        val result = convert(
          List(
            autocodeTaxi(1, 20),
            rsaTaxi(4, 10)
          )
        )
        assert(result.size == 1)
        val autocode = result.head.entity.asInstanceOf[TaxiHistoryEntity].taxiEntity

        assert(autocode.dateStart.contains(days(1)))
        assert(autocode.dateEnd.contains(days(20)))
      }

      "correctly map only autocode" in {
        val result = convert(
          List(
            autocodeTaxi(1, 20)
          )
        )
        assert(result.size == 1)
        val autocode = result.head.entity.asInstanceOf[TaxiHistoryEntity].taxiEntity

        assert(autocode.dateStart.contains(days(1)))
        assert(autocode.dateEnd.contains(days(20)))
      }

      "correctly map only rsa" in {
        val result = convert(
          List(
            rsaTaxi(1, 20)
          )
        )
        assert(result.size == 1)
        val autocode = result.head.entity.asInstanceOf[TaxiHistoryEntity].taxiEntity

        assert(autocode.dateStart.contains(days(1)))
        assert(autocode.dateEnd.contains(days(20)))
      }

      "correctly filter invalid dates" in {
        val result = convert(
          List(
            autocodeTaxi(3, 5, 2)
          )
        )
        assert(result.isEmpty)
      }
    }

    "skip audatex auctions only for non-authorized dealers" in {

      val enabledDealer = 41549L
      val disabledDealer = 41114L

      val rd = EmptyResolutionData.copy(
        auctions = List(
          buildAuction(EventType.UREMONT_AUCTION, lotsAmount = 4),
          buildAuction(EventType.AUDATEX_FRAUD_CHECK, lotsAmount = 4)
        )
      )

      def getEntities(dealerId: Option[Long]): List[PreparedHistoryEntity] = {
        kmageBuilder.buildRawHistoryEntities(
          entity,
          rd,
          vinCode,
          clientId = dealerId,
          mark = None,
          AudatexDealers(List(AudatexPartner(Set(enabledDealer), "", Audatex.Credentials("", ""))))
        )
      }

      getEntities(dealerId = None).size shouldBe 8
      getEntities(dealerId = Some(enabledDealer)).size shouldBe 8

      val entities = getEntities(dealerId = Some(disabledDealer))
      entities.size shouldBe 4
      entities.foreach(_.meta.source.eventType shouldBe EventType.UREMONT_AUCTION)
    }

    "hide audatex mileage spikes" in {

      val entities = List(
        AlyansAutoSaleEntity(Sale.newBuilder.setMileage(100000).setDate(10).build, Map.empty),
        AudatexReportEntity(
          AdaperioAudatex.Report.newBuilder.setMileage(20000).setCalculated(20).build,
          Map.empty
        ), // spike
        AlyansAutoSaleEntity(Sale.newBuilder.setMileage(101000).setDate(30).build, Map.empty),
        AlyansAutoSaleEntity(
          Sale.newBuilder.setMileage(20000).setDate(40).build,
          Map.empty
        ), // тоже spike, но не аудатекс
        AlyansAutoSaleEntity(Sale.newBuilder.setMileage(102000).setDate(5000000000L).build, Map.empty),
        AudatexReportEntity(
          AdaperioAudatex.Report.newBuilder.setMileage(20000).setCalculated(5000000010L).build,
          Map.empty
        ), // похоже на spike, но не дотягивает
        AlyansAutoSaleEntity(Sale.newBuilder.setMileage(70999).setDate(5000000020L).build, Map.empty)
      )
      val res = kmageBuilder.prepareEntities(entities, None, None, None, AudatexDealers(List.empty[AudatexPartner]))
      res.size shouldBe 7
      res(1).isSuitableMileage shouldBe false
      res(3).isSuitableMileage shouldBe true
      res(3).isHighlighted shouldBe true
      res(5).isSuitableMileage shouldBe true
      res(5).isHighlighted shouldBe true
    }

    "hide obvious fakes in offers and audatex" in {

      val entities = List(
        AlyansAutoSaleEntity(Sale.newBuilder.setMileage(111111).setDate(10).build, Map.empty),
        AudatexReportEntity(AdaperioAudatex.Report.newBuilder.setMileage(111111).setCalculated(20).build, Map.empty),
        AudatexReportEntity(AdaperioAudatex.Report.newBuilder.setMileage(456789).setCalculated(30).build, Map.empty),
        buildOfferEntity(kmage = 1234567, date = 40),
        buildOfferEntity(kmage = 7777777, date = 50)
      )

      val res = kmageBuilder.prepareEntities(entities, None, None, None, AudatexDealers(List.empty[AudatexPartner]))
      res.size shouldBe 5
      res.head.isSuitableMileage shouldBe true
      res.drop(1).foreach(_.isSuitableMileage shouldBe false)
    }
  }

  case class MileageEvent(mileage: Int, timestamp: Long)

  case class PhotoEventEntity(
      data: Long,
      mark: String,
      model: String,
      raw_mark_model: String,
      images: Iterable[PhotoInfo])

  def confirmedKmage(id: String, eventType: EventType, isHidden: Boolean): VinInfoHistory = {
    val data = List(KmAgeConfirmed.newBuilder().setId(id).setEventType(eventType).setIsHidden(isHidden).build())
    VinInfoHistory.newBuilder().addAllConfirmedKmages(data.asJava).build()
  }

  def dkpMileages(mileage: MileageEvent): VinInfoHistory = {
    val data = List(Mileage.newBuilder().setDate(mileage.timestamp).setValue(mileage.mileage).build())
    VinInfoHistory.newBuilder().setEventType(EventType.DKP_AUTORU).addAllMileage(data.asJava).build()
  }

  def autocodeMileages(mileages: List[MileageEvent]): VinInfoHistory = {
    val data = mileages.map { case MileageEvent(mileage, date) =>
      Mileage.newBuilder().setDate(date).setValue(mileage).build()
    }
    VinInfoHistory.newBuilder().setEventType(EventType.AUTOCODE_MILEAGE).addAllMileage(data.asJava).build()
  }

  def avtonomerEntity(photoEvents: List[PhotoEventEntity]): List[VinInfoHistory] = {
    val data = photoEvents.map { case PhotoEventEntity(data, mark, model, raw_mark_model, images) =>
      PhotoEvent
        .newBuilder()
        .setDate(data)
        .setMark(mark)
        .setModel(model)
        .setRawMarkModel(raw_mark_model)
        .addAllImages(images.asJava)
        .build()
    }
    data.map { photo =>
      VinInfoHistory
        .newBuilder()
        .setEventType(EventType.AVTONOMER_PHOTO)
        .addPhotoEvents(photo)
        .build()
    }
  }

  def buildOfferEntity(kmage: Int, date: Long): OfferEntity[Offer] = {
    OfferEntity(
      offer.OfferGroup(
        List(
          VinInfo.newBuilder
            .addKmageHistory(
              KmageInfo.newBuilder.setKmage(kmage).setUpdateTimestamp(Timestamps.fromMillis(date))
            )
            .build
        ),
        EventType.AUTORU_OFFER
      ),
      Offer.Empty,
      Map.empty,
      vinCode
    )(OfferEntity.OfferToOpt)
  }

  def offers(
      mileage: MileageEvent,
      mark: Option[String] = None,
      model: Option[String] = None,
      manufacturedYear: Option[Int] = None): VinInfoHistory = {
    val b = VinInfo
      .newBuilder()
      .setKmage(mileage.mileage)
      .setDateOfPlacement(mileage.timestamp)

    manufacturedYear.foreach(b.setYear)
    mark.foreach(b.setMark)
    model.foreach(b.setModel)
    val vinInfo = List(b.build())
    VinInfoHistory.newBuilder().addAllRecords(vinInfo.asJava).build()
  }

  def autotekaServices(mileages: List[MileageEvent]): List[VinInfoHistory] = {
    val orders =
      mileages.map(mileage => Order.newBuilder().setMileage(mileage.mileage).setOrderDate(mileage.timestamp).build())
    val serviceBook = ServiceBook.newBuilder().addAllOrders(orders.asJava)

    List(VinInfoHistory.newBuilder().setEventType(EventType.KOMOSAUTO_SERVICES).setServiceBook(serviceBook).build())
  }

  def sbWithoutKmage(timestamp: Long): VinInfoHistory = {
    val order = Order.newBuilder().setOrderDate(timestamp).build()
    val serviceBook = ServiceBook.newBuilder().addAllOrders(List(order).asJava).build()
    VinInfoHistory.newBuilder().setEventType(EventType.KOMOSAUTO_SERVICES).setServiceBook(serviceBook).build()
  }

  def audatexReport: VinInfoHistory =
    VinInfoHistory
      .newBuilder()
      .addAdaperioAudatex(
        AdaperioAudatex
          .newBuilder()
          .setReport(AdaperioAudatex.Report.getDefaultInstance)
      )
      .build()

  private def buildAuction(eventType: EventType, lotsAmount: Int): VinInfoHistory = {
    VinInfoHistory.newBuilder
      .setEventType(eventType)
      .setUremontAuction(
        Auction.newBuilder
          .addAllLot((0 until lotsAmount).map(_ => Lot.getDefaultInstance).asJava)
      )
      .build
  }

  private def days(num: Long) = TimeUnit.DAYS.toMillis(num)

  private def autocodeTaxi(from: Long, to: Long, cancel: Long = 0): AutocodeTaxi = {
    AutocodeTaxi(
      VinHistory.Taxi
        .newBuilder()
        .setFrom(days(from))
        .setTo(days(to))
        .setCancel(days(cancel))
        .build()
    )
  }

  def rsaTaxi(from: Long, to: Long): RsaTaxi = {
    RsaTaxi(
      VinHistory.Insurance
        .newBuilder()
        .setFrom(days(from))
        .setTo(days(to))
        .build()
    )
  }

  def resolutionData(
      offers: Option[VinInfoHistory] = None,
      autotekaServices: List[VinInfoHistory] = Nil,
      techInspections: Option[VinInfoHistory] = None,
      dkpMileages: List[VinInfoHistory] = Nil,
      confirmed: Option[VinInfoHistory] = None,
      manufacturedYear: Option[Int] = None,
      photo: List[VinInfoHistory] = Nil): ResolutionData = {

    val registration = manufacturedYear
      .map { year =>
        VinInfoHistory
          .newBuilder()
          .setRegistration(Registration.newBuilder().setYear(year))
          .build()
      }
      .map(Prepared.simulate(_))

    EmptyResolutionData.copy(
      registration = registration,
      offers = offers.toList.flatMap(_.getRecordsList.asScala),
      confirmed = confirmed,
      techInspections = techInspections,
      dkpRecords = dkpMileages,
      autotekaServices = autotekaServices,
      photo = photo
    )
  }

  case class SomeEntity(vinInfo: VinInfo)

  val vinInfo: VinInfo = VinInfo.newBuilder().setMark("BMW").setModel("M5").build()

  def entity: SomeEntity = SomeEntity(vinInfo)

  private def buildRawHistoryEntities(data: ResolutionData): List[PreparedHistoryEntity] = {
    kmageBuilder.buildRawHistoryEntities(
      entity,
      data,
      vinCode,
      41114L.some,
      "BMW".some,
      AudatexDealers(List.empty[AudatexPartner])
    )
  }

  val mms: MarkModelSource[SomeEntity] = new MarkModelSource[SomeEntity] {
    override def getMark(t: SomeEntity): Option[String] = t.vinInfo.getMark.some
    override def getModel(t: SomeEntity): Option[String] = t.vinInfo.getModel.some
  }

  val oos: OfferOptSource[SomeEntity] = new OfferOptSource[SomeEntity] {
    override def toOffer(t: SomeEntity): Option[Offer] = Offer(t.vinInfo).some
  }

  def kmageBuilder: KmageHistoryBuilder[SomeEntity, KmageHistory] = {
    new KmageHistoryBuilder[SomeEntity, KmageHistory]()(mms, oos) {

      override protected def buildInternal(
          current: SomeEntity,
          vin: String,
          entities: List[PreparedEntity]): KmageHistory = ???

      override protected val brandCertifications: BrandCertifications = BrandCertifications(Map.empty)

    }
  }
}
