package ru.yandex.auto.vin.decoder.manager

import auto.carfax.common.utils.avatars.{CarfaxNamespace, PhotoInfoId}
import auto.carfax.common.utils.tracing.Traced
import com.google.protobuf.BoolValue
import com.google.protobuf.util.Timestamps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.CommonModel.Photo
import ru.auto.api.vin.VinApiModel
import ru.auto.api.vin.VinApiModel.AutoruConfirmed
import ru.auto.api.vin.VinReportModel.HistoryBlock.{HistoryRecord, OwnerHistory}
import ru.auto.api.vin.VinReportModel._
import ru.yandex.auto.vin.decoder.manager.moderation.ModerationManager
import ru.yandex.auto.vin.decoder.manager.vin.{KnownIdentifiers, VinHistoryManager}
import ru.yandex.auto.vin.decoder.model._
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.Moderation.{HiddenPhoto, IdentifierDateModeration, IntModeration}
import ru.yandex.auto.vin.decoder.proto.VinHistory._
import ru.yandex.auto.vin.decoder.providers.bodytype.BodyTypesProvider
import ru.yandex.auto.vin.decoder.report.converters.formats.proto.builder.essentials.PtsOwnersBlockBuilder
import ru.yandex.auto.vin.decoder.utils.ReportUtils.{RichRawVinEssentialsReportBuilder, RichRawVinReportBuilder}
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class ModerationManagerTest extends AnyWordSpecLike with Matchers with MockitoSupport with MockedFeatures {

  private val vinHistoryManager = mock[VinHistoryManager]
  private val bodyTypeProvider = mock[BodyTypesProvider]
  when(bodyTypeProvider.getSelector).thenReturn(BodyTypesSelector(Nil))
  private val ptsBlockBuilder = new PtsOwnersBlockBuilder(features)

  private val moderationManager = new ModerationManager(vinHistoryManager, bodyTypeProvider, ptsBlockBuilder)

  implicit private val t: Traced = Traced.empty

  // VinInfoHistory хранящий модераторские правки
  // Сразу заполняем какими-то правками, чтобы проверять сохранение новых поверх старых
  private val editsVih: VinInfoHistory = VinInfoHistory.newBuilder
    .setRegistration(
      VinHistory.Registration.newBuilder
        .setMark("BMW")
        .setModel("X3")
        .setYear(2010)
        .setPowerHp(200)
        .setDisplacement(13)
        .setColor("бежевый")
        .addAllRegActions((0 until 3).map(_ => buildRegEvent).asJava)
        .addAllPeriods((0 until 300 by 99).map(buildPeriod).asJava)
    )
    .addAllAccidents((0 until 3).map(_ => buildAccident).asJava)
    .addAllWanted((0 until 3).map(_ => buildWanted).asJava)
    .addAllPledges((0 until 3).map(_ => buildPledge).asJava)
    .addAllConstraints((0 until 3).map(_ => buildConstraint).asJava)
    .setModeration(
      Moderation.newBuilder
        .addPtsReceiveDate(
          IdentifierDateModeration.newBuilder
            .setIdentifier("test_pts")
            .setModeration(IntModeration.newBuilder.setOrig(123).setModerated(321).setCreated(1).setTtl(300))
        )
        .addStsReceiveDate(
          IdentifierDateModeration.newBuilder
            .setIdentifier("test_sts")
            .setModeration(IntModeration.newBuilder.setOrig(111).setModerated(222).setCreated(2).setTtl(301))
        )
        .addHiddenPhoto(
          HiddenPhoto.newBuilder
            .setName("autoru-carfax:123-abc")
            .setAlias("orig")
        )
    )
    .build

  private val vin = VinCode("Z0NZWE00054341234")

  // ResolutionData к которой применяются правки
  private val rd: ResolutionData = ResolutionData
    .empty(vin)
    .copy(
      registration = Some(Prepared.simulate(editsVih)), // просто чем-то заполнили
      identifiers = KnownIdentifiers.empty.copy(
        pts = Some(IdentifierContainer(Pts("50ОЕ188701"), Some(666), 100)),
        sts = List(
          IdentifierContainer(Sts("9921478983"), dataReceiveOpt = Some(777), 123),
          IdentifierContainer(Sts("test"), dataReceiveOpt = Some(11), 122),
          IdentifierContainer(Sts("test2"), dataReceiveOpt = Some(12), 121)
        ),
        hasDuplicatePtsOpt = Some(true)
      )
    )

  // RawVinEssentialsReport к которому применяются правки
  private val rawEssentialsReport: RawVinEssentialsReport = {
    val photos = List(
      "autoru-carfax:123-abc",
      "autoru-carfax:123-def"
    )
      .map(name => Photo.newBuilder.setName(name).putAllSizes(buildPhotoSizes(name).asJava).build)
      .asJava
    val offer = OfferRecord.newBuilder
      .setPhoto(photos.get(0))
      .addAllPhotos(photos)
      .addOfferChangesHistoryRecords(
        OfferChangesHistoryRecord.newBuilder.addAllPhotosAdded(photos).addAllPhotosRemoved(photos)
      )
    val vehiclePhoto = VehiclePhotoItem.newBuilder.addGallery(photos.get(0))
    RawVinEssentialsReport.newBuilder
      .setAutoruOffers(AutoruOffersBlock.newBuilder.addOffers(offer))
      .setVehiclePhotos(VehiclePhotos.newBuilder.addRecords(vehiclePhoto))
      .setHistory(
        HistoryBlock.newBuilder.addOwners(
          OwnerHistory.newBuilder.addAllHistoryRecords(
            List(
              HistoryRecord.newBuilder.setPhotoRecord(vehiclePhoto).build,
              HistoryRecord.newBuilder.setOfferRecord(offer).build
            ).asJava
          )
        )
      )
      .build
  }

  // RawVinReport к которому применяются правки
  private val rawReport: RawVinReport = {
    RawVinReport.newBuilder
      .setAutoruOffers(rawEssentialsReport.getAutoruOffers)
      .setVehiclePhotos(rawEssentialsReport.getVehiclePhotos)
      .setHistory(rawEssentialsReport.getHistory)
      .setPhotoBlock(
        PhotoBlock.newBuilder.addAllPhotos(rawEssentialsReport.getAutoruOffers.getOffers(0).getPhotosList)
      )
      .build
  }

  private def buildPhotoSizes(name: String): Map[String, String] = {
    val photo = PhotoInfoId.unapply(name).get
    CarfaxNamespace.Aliases.map { alias =>
      alias -> s"//images.mds-proxy.test.avto.ru/get-${photo.namespace}/${photo.groupId}/${photo.name}/$alias"
    }.toMap
  }

  private def buildRegEvent: RegistrationEvent = {
    RegistrationEvent.newBuilder
      .setFrom(100)
      .setTo(200)
      .setOperationType("TEST OP")
      .setGeo(
        Geo.newBuilder
          .setCityName("Москва")
          .setGeobaseId(12345)
      )
      .setOwner("LEGAL")
      .setOwnerInfo(
        Owner.newBuilder
          .setName("ООО ЖилСтройИнвестАгроРусХолдингСервис")
      )
      .build
  }

  private def buildAccident: Accident = {
    Accident.newBuilder
      .setDate(100)
      .build
  }

  private def buildWanted: Wanted = {
    Wanted.newBuilder
      .setDate(100)
      .build
  }

  private def buildPledge: Pledge = {
    Pledge.newBuilder
      .setDate(100)
      .build
  }

  private def buildConstraint: Constraint = {
    Constraint.newBuilder
      .setDate(100)
      .build
  }

  private def buildPeriod(from: Int): RegistrationEvent = {
    RegistrationEvent.newBuilder
      .setFrom(from)
      .setTo(from + 99)
      .build
  }

  "changeConfirmed" should {

    "correctly update tech params" in {

      def checkReg(reg: Registration): Unit = {
        reg.getMark shouldBe "LADA"
        reg.getModel shouldBe "KALINA"
        reg.getYear shouldBe 2003
        reg.getPowerHp shouldBe 9999
        reg.getDisplacement shouldBe 666
        reg.getColor shouldBe "голубой"
        ()
      }

      // правильно сохранили правки
      val savedEdits = saveEdits(
        _.setTechParams(
          VinApiModel.TechParams.newBuilder
            .setMark("LADA")
            .setModel("KALINA")
            .setYear(2003)
            .setPowerHp(9999)
            .setDisplacement(666)
            .setColor("голубой")
        )
      )
      checkReg(savedEdits.getRegistration)

      // правильно применили правки
      val moderated = moderate(savedEdits)
      assert(moderated.registration.nonEmpty)
      checkReg(moderated.registration.get.data.getRegistration)
    }

    "detect incorrect moderation values" in {

      val editsRaw: AutoruConfirmed = AutoruConfirmed.newBuilder
        .setTechParams(
          VinApiModel.TechParams.newBuilder
            .setYear(32)
            .setPowerHp(-1)
            .setDisplacement(100 * 1000)
            .setColor("RED")
        )
        .setOwnershipInfo(
          VinApiModel.OwnershipInfo.newBuilder
            .setRegistration(
              VinApiModel.ConfirmedRegistration.newBuilder
                .addAllPeriods(
                  List(
                    VinApiModel.RegistrationPeriod.newBuilder
                      .setFrom(Timestamps.fromMillis(100))
                      .setTo(Timestamps.fromMillis(200))
                      .build,
                    VinApiModel.RegistrationPeriod.newBuilder
                      .setFrom(Timestamps.fromMillis(50))
                      .setTo(Timestamps.fromMillis(250)) // периоды пересекаются
                      .build
                  ).reverse.asJava
                )
            )
            .addAllRegActions(
              List(VinApiModel.ConfirmedRegistrationAction.newBuilder.build).asJava
            ) // нет дат и operation
        )
        .build
      val updaters = moderationManager.edits.map(_.validate(editsRaw))
      val updatersSuc = updaters.flatMap(_.toOption)
      val updatersErr = updaters.flatMap(_.left.toOption)

      updatersErr.size shouldBe 6

      val savedEdits = editsVih.toBuilder
      updatersSuc.foreach(_(savedEdits))
      savedEdits.build shouldBe editsVih
    }

    "drop stuff required by moderator" in {

      // правильно сохранили правки
      val savedEdits = saveEdits(
        _.setLegalPurity(
          VinApiModel.LegalPurity.newBuilder
            .setDropWanted(true)
            .setDropPledges(true)
            .setDropConstraints(true)
        )
          .setOwnershipInfo(
            VinApiModel.OwnershipInfo.newBuilder
              .setDropAccidents(true)
              .setDropRegActions(true)
          )
      )
      savedEdits.getRegistration.getRegActionsCount shouldBe 0
      savedEdits.getAccidentsCount shouldBe 0
      savedEdits.getConstraintsCount shouldBe 0
      savedEdits.getWantedCount shouldBe 0
      savedEdits.getPledgesCount shouldBe 0
      assert(savedEdits.getConfirmedTtl.getRegistrationActions > 0)
      assert(savedEdits.getConfirmedTtl.getAccidents > 0)
      assert(savedEdits.getConfirmedTtl.getConstraints > 0)
      assert(savedEdits.getConfirmedTtl.getWanted > 0)
      assert(savedEdits.getConfirmedTtl.getPlegdes > 0)

      // правильно применили правки
      val moderated = moderate(savedEdits)
      assert(moderated.registration.nonEmpty)
      moderated.registration.get.data.getRegistration.getRegActionsCount shouldBe 0
      assert(moderated.accidents.nonEmpty)
      moderated.accidents.get.data.getAccidentsCount shouldBe 0
      assert(moderated.constraints.nonEmpty)
      moderated.constraints.get.data.getAccidentsCount shouldBe 0
      assert(moderated.wanted.nonEmpty)
      moderated.wanted.get.data.getWantedCount shouldBe 0
      moderated.pledges.size shouldBe 0
    }

    "correctly update reg actions" in {

      def checkReg(reg: Registration): Unit = {
        reg.getRegActionsCount shouldBe 2
        reg.getRegActionsList.asScala.foreach { a =>
          a.getFrom shouldBe 101
          a.getTo shouldBe 101
          a.getGeo.getCityName shouldBe "Екатеринбург"
          a.getOperationType shouldBe "Регистрация тс"
        }
      }

      // правильно сохранили правки
      val savedEdits = saveEdits(
        _.setOwnershipInfo(
          VinApiModel.OwnershipInfo.newBuilder
            .addAllRegActions(
              (0 until 2).map { a =>
                VinApiModel.ConfirmedRegistrationAction.newBuilder
                  .setDate(101)
                  .setRegion("Екатеринбург")
                  .setOperation("Регистрация тс")
                  .build
              }.asJava
            )
        )
      )
      checkReg(savedEdits.getRegistration)

      // правильно применили правки
      val moderated = moderate(savedEdits)
      assert(moderated.registration.nonEmpty)
      checkReg(moderated.registration.get.data.getRegistration)
    }

    "correctly update ownership periods" in {

      def checkReg(reg: Registration): Unit = {
        reg.getPeriodsCount shouldBe 2
        reg.getPeriodsList.asScala.head.getTo shouldBe 300000
        ()
      }

      // правильно сохранили правки
      val savedEdits = saveEdits(
        _.setOwnershipInfo(
          VinApiModel.OwnershipInfo.newBuilder
            .setRegistration(
              VinApiModel.ConfirmedRegistration.newBuilder
                .addAllPeriods(
                  (0 until 2)
                    .map { i =>
                      VinApiModel.RegistrationPeriod.newBuilder
                        .setFrom(Timestamps.fromSeconds(100 + i * 100))
                        .setTo(Timestamps.fromSeconds(200 + i * 100))
                        .build
                    }
                    .reverse
                    .asJava
                )
            )
        )
      )
      checkReg(savedEdits.getRegistration)
      assert(savedEdits.getConfirmedTtl.getRegistrationPeriods > 0)

      // правильно применили правки
      val moderated = moderate(savedEdits)
      assert(moderated.registration.nonEmpty)
      checkReg(moderated.registration.get.data.getRegistration)
    }

    "correctly update accidents" in {

      def checkAccidents(vih: VinInfoHistory): Unit = {
        vih.getAccidentsCount shouldBe 2
        vih.getAccidentsList.asScala.foreach { accident =>
          assert(accident.getDate != 100)
          accident.getRegion shouldBe "Москва"
          accident.getAccidentType shouldBe "ДТП"
          assert(accident.getDamagePointsList.asScala.head != 0)
        }
      }

      // правильно сохранили правки
      val savedEdits = saveEdits(
        _.setOwnershipInfo(
          VinApiModel.OwnershipInfo.newBuilder
            .setAccidents(
              VinApiModel.Accidents.newBuilder
                .addAllAccident((0 until 2).map { i =>
                  VinApiModel.Accident.newBuilder
                    .setDate(Timestamps.fromSeconds(666 + 100 * i))
                    .setAccidentType("ДТП")
                    .setRegion("Москва")
                    .addDamagePoints(1 + i)
                    .build
                }.asJava)
            )
        )
      )
      checkAccidents(savedEdits)

      // правильно применили правки
      val moderated = moderate(savedEdits)
      assert(moderated.accidents.nonEmpty)
      checkAccidents(moderated.accidents.get.data)
    }

    "correctly update pts info" in {

      // правильно сохранили правки
      val savedEdits = saveEdits(
        _.setPtsInfo(
          VinApiModel.PtsInfo.newBuilder
            .setHasDuplicatePts(BoolValue.of(false))
            .setPtsDataReceive(
              VinApiModel.IdentifierDateModeration.newBuilder
                .setIdentifier("50ОЕ188701")
                .setDateOrig(666)
                .setDateModerated(665)
            )
            .setStsDataReceive(
              VinApiModel.IdentifierDateModeration.newBuilder
                .setIdentifier("9921478983")
                .setDateOrig(777)
                .setDateModerated(776)
            )
        )
      )
      savedEdits.getVehicleIdentifiers.getHasDuplicatePts.getValue shouldBe false
      assert(savedEdits.getConfirmedTtl.getRegistrationInfo > 0)
      assert(savedEdits.getModeration.getPtsReceiveDateCount == 2)
      assert(savedEdits.getModeration.getStsReceiveDateCount == 2)
      val ptsReceived = savedEdits.getModeration.getPtsReceiveDate(1).getModeration
      val stsReceived = savedEdits.getModeration.getStsReceiveDate(1).getModeration
      ptsReceived.getOrig shouldBe 666
      ptsReceived.getModerated shouldBe 665
      assert(ptsReceived.getTtl > 0)
      assert(ptsReceived.getCreated > 0)
      stsReceived.getOrig shouldBe 777
      stsReceived.getModerated shouldBe 776
      assert(stsReceived.getTtl > 0)
      assert(stsReceived.getCreated > 0)

      // правильно применили правки
      val moderated = moderate(savedEdits)
      assert(moderated.identifiers.hasDuplicatePtsOpt.contains(false))
      assert(moderated.identifiers.pts.nonEmpty)
      assert(moderated.identifiers.pts.get.dataReceiveOpt.contains(665))
      assert(moderated.identifiers.sts.nonEmpty)
      assert(moderated.identifiers.sts.head.dataReceiveOpt.contains(776))

      // правильно применили правки, когда их много по разным стс-ам и птс-ам
      val savedEdits2b = savedEdits.toBuilder
      val now = System.currentTimeMillis
      savedEdits2b.getModerationBuilder
        .clearPtsReceiveDate()
        .addAllPtsReceiveDate(
          List(
            idDateMod("123", 123, 321, 10),
            idDateMod("50ОЕ100000", 123, 16, 10),
            idDateMod("50ОЕ100000", 16, 9, 20),
            idDateMod("50ОЕ188701", 123, 321, 10000),
            idDateMod("50ОЕ188701", 123, 321, now + 1000),
            idDateMod("50ОЕ188701", 666, 321, now + 10000, created = 10),
            idDateMod("50ОЕ188701", 666, 665, now + 1000, created = 20)
          ).asJava
        )
      savedEdits2b.getModerationBuilder
        .clearStsReceiveDate()
        .addAllStsReceiveDate(
          List(
            idDateMod("test", 11, 111, 10),
            idDateMod("test", 111, 1111, 10),
            idDateMod("test2", 12, 122, 10),
            idDateMod("9921478983", 77, 777, now - 1000),
            idDateMod("9921478983", 777, 776, now + 1000)
          ).asJava
        )
      val savedEdits2 = savedEdits2b.build
      val moderated2 = moderate(savedEdits2)
      assert(moderated2.identifiers.pts.nonEmpty)
      assert(moderated2.identifiers.pts.get.dataReceiveOpt.contains(665))
      assert(moderated2.identifiers.sts.nonEmpty)
      assert(moderated2.identifiers.sts.head.dataReceiveOpt.contains(776))
    }

    "correctly update utilization info" in {

      def checkReg(reg: Registration): Unit = {
        reg.getWasUtilization.getValue shouldBe true
        reg.getDateOfUtilizationsCount shouldBe 1
        reg.getDateOfUtilizations(0) shouldBe 128
        ()
      }

      // правильно сохранили правки
      val savedEdits = saveEdits(
        _.setUtilizationInfo(
          VinApiModel.UtilizationInfo.newBuilder
            .setWasUtilization(BoolValue.of(true))
            .setDateOfUtilization(128)
        )
      )
      checkReg(savedEdits.getRegistration)
      assert(savedEdits.getConfirmedTtl.getUtilizationInfo > 0)

      // правильно применили правки
      val moderated = moderate(savedEdits)
      assert(moderated.registration.nonEmpty)
      checkReg(moderated.registration.get.data.getRegistration)
    }

    "correctly hide photo aliases" in {

      def isOrigHidden(p: Photo): Boolean = {
        p.getSizesCount == CarfaxNamespace.Aliases.size - 1 &&
        !p.getSizesMap.asScala.keys.toSet.contains("orig")
      }

      // правильно сохранили правки
      val savedEdits = saveEdits(
        _.addAllHiddenPhoto(
          List(
            VinApiModel.HiddenPhoto.newBuilder
              .setName("autoru-carfax:123-abc")
              .setAlias("orig")
              .setHidden(BoolValue.of(false))
              .build,
            VinApiModel.HiddenPhoto.newBuilder
              .setName("autoru-carfax:123-def")
              .setAlias("orig")
              .setHidden(BoolValue.of(true))
              .build
          ).asJava
        )
      )
      val hiddenAliases = savedEdits.getModeration.getHiddenPhotoList.asScala
      assert(hiddenAliases.size == 1)
      assert(hiddenAliases.head.getName == "autoru-carfax:123-def")
      assert(hiddenAliases.head.getAlias == "orig")

      // правильно применили правки
      val moderated = moderateRawVinReport(savedEdits)
      val moderatedOffer = moderated.getAutoruOffers.getOffers(0)
      val moderatedHistoryOffer = moderated.getHistory.getOwners(0).getHistoryRecords(1).getOfferRecord

      // у фотки abc есть orig
      List(
        moderated.getPhotoBlock.getPhotos(0),
        moderatedOffer.getPhoto,
        moderatedOffer.getPhotos(0),
        moderatedOffer.getOfferChangesHistoryRecords(0).getPhotosAdded(0),
        moderatedOffer.getOfferChangesHistoryRecords(0).getPhotosRemoved(0),
        moderated.getVehiclePhotos.getRecords(0).getGallery(0),
        moderated.getHistory.getOwners(0).getHistoryRecords(0).getPhotoRecord.getGallery(0),
        moderatedHistoryOffer.getPhoto,
        moderatedHistoryOffer.getPhotos(0),
        moderatedHistoryOffer.getOfferChangesHistoryRecords(0).getPhotosAdded(0),
        moderatedHistoryOffer.getOfferChangesHistoryRecords(0).getPhotosRemoved(0)
      ).foreach(photo => assert(!isOrigHidden(photo)))

      // у фотки def скрыт orig
      List(
        moderated.getPhotoBlock.getPhotos(1),
        moderatedOffer.getPhotos(1),
        moderatedOffer.getOfferChangesHistoryRecords(0).getPhotosAdded(1),
        moderatedOffer.getOfferChangesHistoryRecords(0).getPhotosRemoved(1),
        moderatedHistoryOffer.getPhotos(1),
        moderatedHistoryOffer.getOfferChangesHistoryRecords(0).getPhotosAdded(1),
        moderatedHistoryOffer.getOfferChangesHistoryRecords(0).getPhotosRemoved(1)
      ).foreach(photo => assert(isOrigHidden(photo)))
    }

    "correctly hide photos" in {
      val savedEdits = saveEdits(
        _.addAllHiddenPhoto(
          List(
            VinApiModel.HiddenPhoto.newBuilder
              .setName("autoru-carfax:123-abc")
              .setAlias("orig")
              .setHidden(BoolValue.of(false))
              .build,
            VinApiModel.HiddenPhoto.newBuilder
              .setName("autoru-carfax:123-def")
              .setHidden(BoolValue.of(true))
              .build
          ).asJava
        )
      )
      val moderatedEssentialsReport = moderateRawVinEssentialsReport(savedEdits)
      val moderatedReport = moderateRawVinReport(savedEdits)
      assert(!moderatedReport.toBuilder.allPhotoBuilders.exists(_.getName == "autoru-carfax:123-def"))
      assert(
        moderatedEssentialsReport.toBuilder.allPhotoBuilders
          .filter(_.getName == "autoru-carfax:123-def")
          .forall(_.getIsHiddenFromReport)
      )
    }
  }

  private def saveEdits(
      moderationBuildF: AutoruConfirmed.Builder => AutoruConfirmed.Builder): VinInfoHistory = {
    val moderation = moderationBuildF(AutoruConfirmed.newBuilder).build
    val resVihB = editsVih.toBuilder
    moderationManager.edits
      .map(_.validate(moderation))
      .flatMap(_.toOption)
      .foreach(_(resVihB))
    resVihB.build
  }

  private def moderate(edits: VinInfoHistory): ResolutionData = {
    moderationManager.applyModeration(rd.copy(confirmed = Some(edits)))
  }

  private def moderateRawVinEssentialsReport(edits: VinInfoHistory): RawVinEssentialsReport = {
    val repB = rawEssentialsReport.toBuilder
    moderationManager.applyModerationToEssentialsReport(repB, edits)
    repB.build
  }

  private def moderateRawVinReport(edits: VinInfoHistory): RawVinReport = {
    val repB = rawReport.toBuilder
    moderationManager.applyModerationToReport(repB, edits)
    repB.build
  }

  private def idDateMod(
      id: String,
      orig: Long,
      moderated: Long,
      ttl: Long,
      created: Long = System.currentTimeMillis): IdentifierDateModeration = {
    IdentifierDateModeration.newBuilder
      .setIdentifier(id)
      .setModeration(
        IntModeration.newBuilder
          .setOrig(orig)
          .setModerated(moderated)
          .setTtl(ttl)
          .setCreated(created)
      )
      .build
  }
}
