package ru.yandex.vos2.api.directives.utils

import java.util.concurrent.ThreadLocalRandom
import org.junit.runner.RunWith
import org.scalacheck.Arbitrary
import org.scalatest.Inspectors.forAll
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.Checkers
import ru.yandex.realty.model.user.UserRefGenerators
import ru.yandex.vos2.BasicsModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.api.model.{PhotoInfo, RealtyOfferCommonInformation, RealtyOfferPrice, UpdateOfferInfo}
import ru.yandex.vos2.features.SimpleFeatures
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.model.user.UserGenerator
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.services.mds.MdsPhotoUtils
import org.scalatest.prop.TableDrivenPropertyChecks._
import ru.yandex.vos2.realty.features.RealtyFeatures

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RealtyOfferExtractorTest extends WordSpec with Matchers with Checkers with UserRefGenerators {

  implicit val arbOffer = Arbitrary(RealtyOfferGenerator.offerGen().map { offer =>
    offer.toBuilder.build()
  })
  val features = new SimpleFeatures with RealtyFeatures

  private def createPhotoInfo(): PhotoInfo = {
    val id = ThreadLocalRandom.current.nextLong(Long.MaxValue)
    PhotoInfo(s"https://avatars.yandex.net/get-realty/12345/$id/orig", None)
  }

  private def initBuilderWithImages(images: Option[Seq[PhotoInfo]]): Offer.Builder = {
    val builder = TestUtils.createOffer()
    require(builder.getImageRefList.isEmpty)
    require(builder.getOfferRealtyBuilder.getPhotoList.isEmpty)
    RealtyOfferExtractor.setImages(builder, builder.getOfferRealtyBuilder, images)
    builder
  }

  private def createTestOffer(): Offer = {
    val photoInfo = createPhotoInfo()
    val builder = initBuilderWithImages(Some(Seq(createPhotoInfo(), photoInfo))).build.toBuilder
    RealtyOfferExtractor.setImages(builder, builder.getOfferRealtyBuilder, Some(Seq(photoInfo, createPhotoInfo())))
    builder.build()
  }

  private def getOfferTuple(newImages: Option[Seq[PhotoInfo]]): (Offer, Offer) = {
    val testOffer = createTestOffer()
    val builder = testOffer.toBuilder
    RealtyOfferExtractor.setImages(builder, builder.getOfferRealtyBuilder, newImages)
    (testOffer, builder.build())
  }

  private def getPhotoList(offer: Offer): Seq[BasicsModel.Photo] = {
    offer.getOfferRealty.getPhotoList.asScala
  }

  private def getDummyReqForUpdateOrderInfo: UpdateOfferInfo =
    UpdateOfferInfo(
      callCenter = Some(true),
      location = Some(
        ru.yandex.vos2.api.model.Location(
          country = Some("Russia"),
          address = Some("Pushkina 12"),
          rgid = Some(1L)
        )
      )
    )

  "RealtyOfferExtractor" should {

    "do nothing with image refs when new images are None" in {
      val (oldOffer, newOffer) = getOfferTuple(None)
      oldOffer.getImageRefList shouldBe newOffer.getImageRefList
    }

    "do nothing with mds names when new images are None" in {

      val (oldOffer, newOffer) = getOfferTuple(None)
      getPhotoList(oldOffer).map(_.getName) shouldBe getPhotoList(newOffer).map(_.getName)
    }

    "do nothing with deleted when new images are None" in {
      val (oldOffer, newOffer) = getOfferTuple(None)
      getPhotoList(oldOffer).map(_.getDeleted) shouldBe getPhotoList(newOffer).map(_.getDeleted)
    }

    "clear image refs when new images are emtpy" in {
      val (oldOffer, newOffer) = getOfferTuple(Some(Seq.empty))
      newOffer.getImageRefList shouldBe empty
    }

    "do nothing with mds names when new images are emtpy" in {
      val (oldOffer, newOffer) = getOfferTuple(Some(Seq.empty))
      getPhotoList(oldOffer).map(_.getName) shouldBe getPhotoList(newOffer).map(_.getName)
    }

    "set deleted to all when new images are empty" in {
      val (oldOffer, newOffer) = getOfferTuple(Some(Seq.empty))
      getPhotoList(newOffer).forall(_.getDeleted) shouldBe true
    }

    "replace image refs when new images aren't empty" in {
      val photos = Seq(createPhotoInfo())
      val (oldOffer, newOffer) = getOfferTuple(Some(photos))
      newOffer.getImageRefList.asScala.map(_.getUrl) shouldBe photos.map(_.url)
    }

    "add new mds names when new images aren't empty" in {
      val photos = Seq(createPhotoInfo())
      val (oldOffer, newOffer) = getOfferTuple(Some(photos))
      forAll(photos.map(_.url).flatMap(MdsPhotoUtils.parseMDSPhoto)) {
        getPhotoList(newOffer).map(_.getName) should contain(_)
      }
    }

    "do nothing with old mds names when new images aren't empty" in {
      val photos = Seq(createPhotoInfo())
      val (oldOffer, newOffer) = getOfferTuple(Some(photos))
      forAll(getPhotoList(oldOffer).map(_.getName)) {
        getPhotoList(newOffer).map(_.getName) should contain(_)
      }
    }

    "set deleted to old images only when new images aren't empty" in {
      val photos = Seq(createPhotoInfo())
      val (oldOffer, newOffer) = getOfferTuple(Some(photos))
      forAll(getPhotoList(newOffer).filter(_.getDeleted).map(_.getName)) {
        getPhotoList(oldOffer).map(_.getName) should contain(_)
      }
    }

    "extractOffer with empty stored offer with correct fields" in {
      val emptyString = ""
      val req = getDummyReqForUpdateOrderInfo
      val userRef = UserGenerator.AidRefGen.next
      val extractedOffer = RealtyOfferExtractor.extractOffer(req, userRef, features = features)

      // updated address
      extractedOffer.getOfferRealty.getAddress.getCountry shouldBe req.location.get.country.get
      extractedOffer.getOfferRealty.getAddress.getAddress shouldBe req.location.get.address.get
      extractedOffer.getOfferRealty.getAddress.getRgid shouldBe req.location.get.rgid.get

      // empty values, cause stored offer not provided
      extractedOffer.getOfferRealty.getAddress.getAddressDescription shouldBe emptyString
      extractedOffer.getOfferRealty.getAddress.getHouseNumber shouldBe emptyString
      extractedOffer.getOfferRealty.getAddress.getStreet shouldBe emptyString
    }

    "extractOffer only for specific field" in { o: Offer ⇒
      val req = getDummyReqForUpdateOrderInfo
      val userRef = UserRef(o.getUser)
      // updated field: location
      val extractedOffer = RealtyOfferExtractor.extractOffer(req, userRef, o.toBuilder, features)
      extractedOffer.getOfferRealty.getAddress.getCountry shouldBe req.location.get.country.get
      extractedOffer.getOfferRealty.getAddress.getAddress shouldBe req.location.get.address.get
      extractedOffer.getOfferRealty.getAddress.getRgid shouldBe req.location.get.rgid.get

      // not updated field: price, category
      extractedOffer.getOfferRealty.getPrice shouldBe o.getOfferRealty.getPrice
      extractedOffer.getOfferRealty.getCategory shouldBe o.getOfferRealty.getCategory
      extractedOffer.getOfferRealty.getCategory shouldBe o.getOfferRealty.getCategory
    }

    "extractOffer with old and new rent fields" in {
      val req = UpdateOfferInfo(
        price = Some(
          RealtyOfferPrice(
            value = Some(38.0)
          )
        ),
        common = Some(
          RealtyOfferCommonInformation(
            rentPledge = Some(true),
            rentDeposit = Some(19L),
            utilitiesIncluded = Some(true),
            utilitiesFee = Some("INCLUDED")
          )
        )
      )
      val extractedOffer = RealtyOfferExtractor.extractOffer(req, UserGenerator.AidRefGen.next, features = features)

      extractedOffer.getOfferRealty.getPrice.getRentDeposit shouldBe req.common.get.rentDeposit.get
      extractedOffer.getOfferRealty.getFacilities.getFlagRentPledge shouldBe req.common.get.rentPledge.get
      extractedOffer.getOfferRealty.getPrice.getUtilitiesFee.name() shouldBe req.common.get.utilitiesFee.get
      extractedOffer.getOfferRealty.getPrice.getIncludedUtilities shouldBe req.common.get.utilitiesIncluded.get
    }

    val offerTestData =
      Table(
        (
          "rentDeposit",
          "utilitiesFee",
          "rentPledgeInit",
          "utilitiesIncludedInit",
          "rentPledgeCalc",
          "utilitiesIncludedCalc"
        ),
        (Option.empty[Long], Option.empty[String], true, true, true, true),
        (Option.empty[Long], Option.empty[String], false, false, false, false),
        (Option(0L), Option.empty[String], true, false, false, false),
        (Option(0L), Option.empty[String], false, false, false, false),
        (Option(1L), Option.empty[String], true, false, true, false),
        (Option(1L), Option.empty[String], false, false, true, false),
        (Option.empty[Long], Option("INCLUDED"), false, false, false, true),
        (Option.empty[Long], Option("INCLUDED"), false, true, false, true),
        (Option.empty[Long], Option("METER"), false, true, false, false),
        (Option.empty[Long], Option("METER"), false, false, false, false),
        (Option.empty[Long], Option("NOT_INCLUDED"), false, true, false, false),
        (Option(5L), Option("INCLUDED"), false, false, true, true),
        (Option(5L), Option("INCLUDED"), true, true, true, true),
        (Option(0L), Option("METER"), true, true, false, false),
        (Option(0L), Option("METER"), false, false, false, false)
      )

    "extractOffer when new rent fields are mapped to old fields" in {
      forAll(offerTestData) {
        case (
            rentDeposit,
            utilitiesFee,
            rentPledgeInit,
            utilitiesIncludedInit,
            rentPledgeCalc,
            utilitiesIncludedCalc
            ) =>
          println(
            s"case  $rentDeposit, $utilitiesFee, $rentPledgeInit, $rentPledgeCalc, $utilitiesIncludedInit, $utilitiesIncludedCalc"
          )
          val req = UpdateOfferInfo(
            price = Some(
              RealtyOfferPrice(
                value = Some(38.0)
              )
            ),
            common = Some(
              RealtyOfferCommonInformation(
                rentDeposit = rentDeposit,
                utilitiesFee = utilitiesFee,
                rentPledge = Some(rentPledgeInit),
                utilitiesIncluded = Some(utilitiesIncludedInit)
              )
            )
          )
          val extractedOffer = RealtyOfferExtractor.extractOffer(req, UserGenerator.AidRefGen.next, features = features)

          if (rentDeposit.isDefined) {
            extractedOffer.getOfferRealty.getPrice.getRentDeposit shouldBe req.common.get.rentDeposit.get
          } else {
            extractedOffer.getOfferRealty.getPrice.hasRentDeposit shouldBe false
          }
          extractedOffer.getOfferRealty.getFacilities.getFlagRentPledge shouldBe rentPledgeCalc
          if (utilitiesFee.isDefined) {
            extractedOffer.getOfferRealty.getPrice.getUtilitiesFee.name() shouldBe req.common.get.utilitiesFee.get
          } else {
            extractedOffer.getOfferRealty.getPrice.hasUtilitiesFee shouldBe false
          }
          extractedOffer.getOfferRealty.getPrice.getIncludedUtilities shouldBe utilitiesIncludedCalc
      }
    }
  }
}
