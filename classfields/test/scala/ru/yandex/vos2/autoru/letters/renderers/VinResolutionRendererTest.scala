package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.vin.VinResolutionEnums.{ResolutionPart, Status}
import ru.auto.api.vin.VinResolutionModel.ResolutionEntry
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport.{mock, when}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.OfferService
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.letters.vin.{GoodVinResolutionAdded, LegalErrorVinResolutionAdded, TechParamErrorVinResolutionAdded}

import scala.jdk.CollectionConverters._
import ru.yandex.vos2.autoru.model.extdata.VinResolutionPushes

@RunWith(classOf[JUnitRunner])
class VinResolutionRendererTest extends AnyFunSuite with InitTestDbs with BeforeAndAfter {
  implicit val trace = Traced.empty

  private val isSpamalotFeature: Feature[Boolean] = mock[Feature[Boolean]]
  when(isSpamalotFeature.value).thenReturn(true)

  private def makeRenderer(): VinResolutionRenderer = {
    val vinResolutionPushes = VinResolutionPushes.from(components.extDataEngine)
    new VinResolutionRenderer(components.carsCatalog, vinResolutionPushes, isSpamalotFeature)
  }

  private def testOffer: OfferModel.Offer.Builder = {
    OfferModel.Offer
      .newBuilder()
      .setOfferAutoru(
        AutoruOffer
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .setVersion(0)
      )
      .setOfferID("1111-aaa")
      .setOfferService(OfferService.OFFER_AUTO)
      .setTimestampUpdate(0)
      .setUserRef("a_1")
  }

  test("skip not auto and not used offers") {
    val offer1 = testOffer
    offer1.getOfferAutoruBuilder.setCategory(Category.TRUCKS)

    val renderer = makeRenderer()

    intercept[IllegalArgumentException] {
      renderer.render(offer1.build())
    }

    val offer2 = testOffer
    offer2.getOfferAutoruBuilder.setSection(Section.NEW)

    intercept[IllegalArgumentException] {
      renderer.render(offer2.build())
    }
  }

  test("provide url") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.OK))

    val renderer = makeRenderer()

    val result = renderer.render(offer.build())

    assert((result.mail.get.payload \ "offer_url_vin_checked").as[String] == "https://auto.ru/cars/used/sale/1111-aaa")
  }

  test("good resolution without dtp") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addAllEntries(
        Seq(
          buildEntry(ResolutionPart.SUMMARY, Status.OK)
        ).asJava
      )

    val renderer = makeRenderer()

    val result = renderer.render(offer.build())

    assert(result.isInstanceOf[GoodVinResolutionAdded])
    assert(result.mail.get.name == "front.vin_checked_has_history")
  }

  test("good resolution with dtp") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addAllEntries(
        Seq(
          buildEntry(ResolutionPart.SUMMARY, Status.OK),
          buildEntry(ResolutionPart.RP_ACCIDENTS, Status.ERROR)
        ).asJava
      )

    val renderer = makeRenderer()

    val result = renderer.render(offer.build())

    assert(result.isInstanceOf[GoodVinResolutionAdded])
    assert(result.mail.get.name == "front.vin_checked_has_accidents")
  }

  test("resolve mark/model") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getCarInfoBuilder
      .setTechParamId(2309943)
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.OK))

    val renderer = makeRenderer()

    val result = renderer.render(offer.build())

    assert((result.mail.get.payload \ "mark").as[String] == "Volkswagen")
    assert((result.mail.get.payload \ "model").as[String] == "Touareg")
  }

  test("select ok mail if resolution is ok") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.OK))

    val renderer = makeRenderer()

    assert(renderer.render(offer.build()).isInstanceOf[GoodVinResolutionAdded])
  }

  test("select tech params error mail if resolution has problem with tech params") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.UNKNOWN))
      .addEntries(buildEntry(ResolutionPart.RP_OWNERS, Status.UNKNOWN))

    val renderer = makeRenderer()

    assert(renderer.render(offer.build()).isInstanceOf[TechParamErrorVinResolutionAdded])

    val offer1 = testOffer
    offer1.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.ERROR))
      .addEntries(buildEntry(ResolutionPart.RP_TECH_PARAMS_GROUP, Status.ERROR))
      .addEntries(buildEntry(ResolutionPart.RP_DISPLACEMENT, Status.ERROR))
      .addEntries(buildEntry(ResolutionPart.RP_YEAR, Status.UNKNOWN))
      .addEntries(buildEntry(ResolutionPart.RP_POWER, Status.ERROR))

    assert(renderer.render(offer1.build()).isInstanceOf[TechParamErrorVinResolutionAdded])
  }

  test("select tech params error mail if resolution has problem with owners") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.ERROR))
      .addEntries(buildEntry(ResolutionPart.RP_OWNERS, Status.ERROR))

    val renderer = makeRenderer()

    assert(renderer.render(offer.build()).isInstanceOf[TechParamErrorVinResolutionAdded])
  }

  test("select legal error mail if resolution has problem with legal purity") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.ERROR))
      .addEntries(buildEntry(ResolutionPart.RP_LEGAL_GROUP, Status.ERROR))
      .addEntries(buildEntry(ResolutionPart.RP_PLEDGE, Status.ERROR))

    val renderer = makeRenderer()

    assert(renderer.render(offer.build()).isInstanceOf[LegalErrorVinResolutionAdded])
  }

  test("select none mail if resolution is somehow empty") {
    val offer = testOffer

    val renderer = makeRenderer()

    assert(renderer.render(offer.build()).mail.isEmpty)
  }

  test("select none mail if resolution legal block is unknown") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.UNKNOWN))
      .addEntries(buildEntry(ResolutionPart.RP_PLEDGE, Status.UNKNOWN))

    val renderer = makeRenderer()

    assert(renderer.render(offer.build()).mail.isEmpty)
  }

  test("select none mail if resolution tech block is unknown") {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .getResolutionBuilder
      .addEntries(buildEntry(ResolutionPart.SUMMARY, Status.UNKNOWN))
      .addEntries(buildEntry(ResolutionPart.RP_TECH_PARAMS_GROUP, Status.UNKNOWN))

    val renderer = makeRenderer()

    assert(renderer.render(offer.build()).mail.isEmpty)
  }

  private def buildEntry(part: ResolutionPart, status: Status) = {
    ResolutionEntry.newBuilder().setPart(part).setStatus(status).build()
  }

}
