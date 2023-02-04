package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.CommonModel.Damage.{DamageType => CMDamageType}
import ru.auto.api.vin.VinResolutionEnums.{ResolutionPart, Status}
import ru.auto.api.vin.VinResolutionModel.{ResolutionEntry, VinIndexResolution}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.CarState.{Damage, DamageType}
import ru.yandex.vos2.BasicsModel.Photo.RecognizedNumber
import ru.yandex.vos2.autoru.model.TestUtils

import scala.jdk.CollectionConverters._

class EvolveWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val worker = new EvolveWorkerYdb with YdbWorkerTestImpl
    val currentVersion: Int = worker.SchemaVersion

  }

  ("DamageTypeUpgrade") in new Fixture {
    val builder = TestUtils.createOffer()
    builder
      .setSchemaVer(4)
      .getOfferAutoruBuilder
      .getStateBuilder
      .addDamage(
        Damage
          .newBuilder()
          .setCarPart("frontbumper")
          .addAllDamageTypeOBSOLETE(
            Iterable(
              DamageType.UNKNOWN,
              DamageType.DYED,
              DamageType.SCRATCH,
              DamageType.DENT,
              DamageType.CORROSION
            ).asJava
          )
      )

    val offer = builder.build()
    assert(offer.getOfferAutoru.getState.getDamage(0).getDamageTypeOBSOLETECount == 5)
    assert(offer.getOfferAutoru.getState.getDamage(0).getDamageTypeCount == 0)

    val newOffer = worker.process(offer, None).updateOfferFunc.get(offer)

    assert(newOffer.getSchemaVer == currentVersion)
    assert(newOffer.getOfferAutoru.getState.getDamage(0).getDamageTypeCount == 5)
    assert(
      newOffer.getOfferAutoru.getState.getDamage(0).getDamageTypeList.asScala == List(
        CMDamageType.DAMAGE_TYPE_UNKNOWN,
        CMDamageType.DYED,
        CMDamageType.SCRATCH,
        CMDamageType.DENT,
        CMDamageType.CORROSION
      )
    )
  }

  ("DamageType upgrade (just converted offer)") in new Fixture {
    val builder = TestUtils.createOffer()
    builder
      .setSchemaVer(0)
      .getOfferAutoruBuilder
      .getStateBuilder
      .addDamage(
        Damage
          .newBuilder()
          .setCarPart("frontbumper")
          .addAllDamageType(
            Iterable(
              CMDamageType.DAMAGE_TYPE_UNKNOWN,
              CMDamageType.DYED,
              CMDamageType.SCRATCH,
              CMDamageType.DENT,
              CMDamageType.CORROSION
            ).asJava
          )
      )

    val offer = builder.build()
    assume(offer.getOfferAutoru.getState.getDamage(0).getDamageTypeOBSOLETECount == 0)
    assume(offer.getOfferAutoru.getState.getDamage(0).getDamageTypeCount == 5)

    val newOffer = worker.process(offer, None).updateOfferFunc.get(offer)

    assert(newOffer.getSchemaVer == currentVersion)
    assert(newOffer.getOfferAutoru.getState.getDamage(0).getDamageTypeCount == 5)
    assert(
      newOffer.getOfferAutoru.getState.getDamage(0).getDamageTypeList.asScala == List(
        CMDamageType.DAMAGE_TYPE_UNKNOWN,
        CMDamageType.DYED,
        CMDamageType.SCRATCH,
        CMDamageType.DENT,
        CMDamageType.CORROSION
      )
    )
  }

  ("vin resolution tag should be updated for version < 25") in new Fixture {
    val builder = TestUtils.createOffer()

    builder
      .setSchemaVer(24)
      .addAllTag(Seq("some_other_tag", "vin_resolution_ok").asJava)
      .getOfferAutoruBuilder
      .getVinResolutionBuilder
      .setVersion(1)
      .setResolution(
        VinIndexResolution
          .newBuilder()
          .setVersion(1)
          .addEntries(
            ResolutionEntry
              .newBuilder()
              .setPart(ResolutionPart.SUMMARY)
              .setStatus(Status.ERROR)
          )
      )

    val offer = builder.build()

    val newOffer = worker.process(offer, None).updateOfferFunc.get(offer)

    newOffer.getTagList.asScala.toSet shouldBe Set("vin_resolution_error", "some_other_tag")
  }

  ("vin resolution tag should not be updated for version = 25") in new Fixture {
    val builder = TestUtils.createOffer()

    builder
      .setSchemaVer(25)
      .addAllTag(Seq("some_other_tag", "vin_resolution_ok").asJava)
      .getOfferAutoruBuilder
      .getVinResolutionBuilder
      .setVersion(1)
      .setResolution(
        VinIndexResolution
          .newBuilder()
          .setVersion(1)
          .addEntries(
            ResolutionEntry
              .newBuilder()
              .setPart(ResolutionPart.SUMMARY)
              .setStatus(Status.ERROR)
          )
      )

    val offer = builder.build()

    val newOffer = worker.process(offer, None).updateOfferFunc.get(offer)

    newOffer.getTagList.asScala.toSet shouldBe Set("vin_resolution_ok", "some_other_tag")
  }

  ("clean duplicate numbers in photos") in new Fixture {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder
      .addPhotoBuilder()
      .setName("aa")
      .setCreated(0L)
      .setOrder(0)
      .setIsMain(true)
      .addNumbers(RecognizedNumber.newBuilder().setNumber("bbb").setConfidence(0.2d).setWidthPercent(0.5))
      .addNumbers(RecognizedNumber.newBuilder().setNumber("aaa").setConfidence(0.9d).setWidthPercent(0.5))
      .addNumbers(RecognizedNumber.newBuilder().setNumber("aaa").setConfidence(0.5d).setWidthPercent(0.5))
      .addNumbers(RecognizedNumber.newBuilder().setNumber("aaa").setConfidence(0.1d).setWidthPercent(0.5))

    val offer = builder.build()

    val newOffer = worker.process(offer, None).updateOfferFunc.get(offer)

    val photo = newOffer.getOfferAutoru.getPhoto(0)
    photo.getNumbersCount shouldBe 2
    photo.getNumbers(0).getNumber shouldBe "aaa"
    photo.getNumbers(0).getConfidence shouldBe 0.9d
    photo.getNumbers(1).getNumber shouldBe "bbb"
    photo.getNumbers(1).getConfidence shouldBe 0.2d
  }

  ("license plate should replace latin to cyrillic chars") in new Fixture {

    val cyrillicCodes = List(
      1040, //А
      49, //1
      51, //3
      53, //5
      1058, //Т
      1042, //В
      51, //3
      55 //7
    )

    val latinCodes = List(
      65, //A
      49, //1
      51, //3
      53, //5
      84, //T
      66, //B
      51, //3
      55 //7
    )

    val latinLicensePlate = latinCodes.map(_.toChar).mkString

    val builder = TestUtils.createOffer()
    builder
      .setSchemaVer(26)
      .getOfferAutoruBuilder
      .getDocumentsBuilder
      .setLicensePlate(latinLicensePlate)

    val offer = builder.build()

    val newOffer = worker.process(offer, None).updateOfferFunc.get(offer)

    val licensePlate = newOffer.getOfferAutoru.getDocuments.getLicensePlate

    val charCodes = licensePlate.map(_.toInt).toList

    assert(charCodes == cyrillicCodes)
  }
}
