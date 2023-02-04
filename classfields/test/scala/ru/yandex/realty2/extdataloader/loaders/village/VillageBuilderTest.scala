package ru.yandex.realty2.extdataloader.loaders.village

import java.util.Collections
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.proto.village.raw.{RawVillage, RawVillageOfferType}
import ru.yandex.realty.proto.village._
import ru.yandex.realty.sites.CompaniesStorage
import ru.yandex.realty.telepony.RedirectPhoneBuilder
import ru.yandex.realty.unification.unifier.processor.enrichers.BuildingEnricher

@RunWith(classOf[JUnitRunner])
class VillageBuilderTest extends WordSpec with MockFactory with Matchers with OneInstancePerTest {

  class MockCompaniesStorage extends CompaniesStorage(Collections.emptyList())

  private val companiesStorage = mock[MockCompaniesStorage]
  private val villageImageUnifier = mock[VillagesImageUnifier]
  private val villageLocationBuilder = mock[VillageLocationBuilder]
  private val redirectPhoneBuilder = mock[RedirectPhoneBuilder]

  val builder = new VillageBuilder(
    companiesStorage,
    villageImageUnifier,
    villageLocationBuilder,
    redirectPhoneBuilder
  )

  "Builder" should {
    "work correct" in {
      (villageImageUnifier.process _).expects(*)
      (villageLocationBuilder.build _).expects(*).returns(Location.newBuilder().build())

      val raw = RawVillage.newBuilder()
      raw.setId(1L)
      raw.setName("TheVillage")
      raw.setDescription("SomeDescription")
      raw.getMainPhotoBuilder.setOrigUrl("http://main.photo")
      raw.setVillageType(VillageType.VILLAGE_TYPE_COMMON)

      val infoBuilder1 = raw.addVillageTypeInfoBuilder()
      infoBuilder1.setOfferType(RawVillageOfferType.RAW_VILLAGE_OFFER_TYPE_TOWNHOUSE)
      infoBuilder1.setOfferStatus(VillageOfferStatus.VILLAGE_OFFER_STATUS_ON_SALE)
      infoBuilder1.getLotAreaBuilder.getFromBuilder.setValue(2f)
      infoBuilder1.getLotAreaBuilder.getToBuilder.setValue(5f)
      infoBuilder1.getTotalPriceBuilder.getFromBuilder.setValue(10f)
      infoBuilder1.getTotalPriceBuilder.getToBuilder.setValue(20f)
      infoBuilder1.addCottageStatus(CottageStatus.COTTAGE_STATUS_READY)

      val infoBuilder2 = raw.addVillageTypeInfoBuilder()
      infoBuilder2.setOfferType(RawVillageOfferType.RAW_VILLAGE_OFFER_TYPE_DUPLEX)
      infoBuilder2.setOfferStatus(VillageOfferStatus.VILLAGE_OFFER_STATUS_ALL_SOLD)
      infoBuilder2.getLotAreaBuilder.getFromBuilder.setValue(1f)
      infoBuilder2.getLotAreaBuilder.getToBuilder.setValue(3f)
      infoBuilder2.getTotalPriceBuilder.getFromBuilder.setValue(15f)
      infoBuilder2.getTotalPriceBuilder.getToBuilder.setValue(25f)
      infoBuilder2.addCottageStatus(CottageStatus.COTTAGE_STATUS_PROJECT)

      val photo = raw.addPhotoBuilder()
      photo.setType(VillagePhotoType.VILLAGE_PHOTO_TYPE_COMMON)
      photo.getPhotoBuilder.setUrl("http://someurl.org")

      val constructionPhoto = raw.addConstructionPhotoBuilder()
      constructionPhoto.getTimestampBuilder.setSeconds(1000000)
      constructionPhoto.addPhotoBuilder().setUrl("http://someurl.org")

      val village = builder.build(Seq(raw.build())).head

      village.getId shouldEqual 1L
      village.getName shouldEqual "TheVillage"
      village.getInfo.getDescription shouldEqual "SomeDescription"
      village.getPhotoCount shouldEqual 1
      village.getConstructionPhotoCount shouldEqual 1
      village.getVillageTypeInfoCount shouldEqual 1
      village.getInfo.getVillageType shouldBe VillageType.VILLAGE_TYPE_COMMON

      val info = village.getVillageTypeInfo(0)
      info.getOfferType shouldBe VillageOfferType.VILLAGE_OFFER_TYPE_TOWNHOUSE
      info.getOfferStatus shouldBe VillageOfferStatus.VILLAGE_OFFER_STATUS_ON_SALE
      info.getArea.getFrom shouldBe 1f
      info.getArea.getTo shouldBe 5f
      info.getTotalPrice.getFrom shouldBe 10
      info.getTotalPrice.getTo shouldBe 25
      info.getCottage.getCottageStatusList should (contain(CottageStatus.COTTAGE_STATUS_PROJECT)
        and contain(CottageStatus.COTTAGE_STATUS_READY))
      village.getMainPhoto.getRawImage.getOrigUrl shouldBe "http://main.photo"
    }
  }
}
