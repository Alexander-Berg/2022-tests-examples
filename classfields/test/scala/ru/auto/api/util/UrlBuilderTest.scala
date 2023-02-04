package ru.auto.api.util

import ru.auto.api.ApiOfferModel.{Category, Offer, Section}
import ru.auto.api.BaseSpec
import ru.auto.api.MotoModel.{MotoCategory, MotoInfo}
import ru.auto.api.TrucksModel.{TruckCategory, TruckInfo}
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelUtils._
import ru.auto.api.util.crypt.TypedCrypto
import ru.auto.api.model.AutoruUser
import ru.yandex.vertis.mockito.MockitoSupport
import org.scalacheck.Arbitrary.arbitrary
import ru.auto.api.util.StringUtils.RichString

/**
  * Created by mcsim-gr on 03.07.17.
  */
class UrlBuilderTest extends BaseSpec with MockitoSupport {
  private val desktopHost = "https://test.avto.ru"
  private val mobileHost = "https://m.test.avto.ru"
  private val partsHost = "https://parts.test.avto.ru"
  private val cryptoUserId = mock[TypedCrypto[AutoruUser]]
  private val urlBuilder = new UrlBuilder(desktopHost, mobileHost, partsHost, cryptoUserId)
  when(cryptoUserId.encrypt(? : AutoruUser)).thenAnswer(_.getArgument[AutoruUser](0).toString.reverse)

  "UrlBuilder" should {
    "generate offer desktop link" in {
      val offer = ModelGenerators.OfferGen.next
      val url = urlBuilder.offerUrl(offer)
      url should startWith(desktopHost)
      url should endWith(s"${offer.getId}/")
    }

    "generate offer mobile link" in {
      val offer = ModelGenerators.OfferGen.next
      val url = urlBuilder.offerUrl(offer, isMobile = true)
      url should startWith(mobileHost)
      url should endWith(s"${offer.getId}/")
    }

    "generate motorcycle offer mobile link" in {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.MOTO)
        .setMotoInfo(
          MotoInfo
            .newBuilder()
            .setMotoCategory(MotoCategory.MOTORCYCLE)
            .setMark("MARK")
            .setModel("MODEL")
            .build()
        )
        .setId("abcd-1234")
        .setSection(Section.USED)
        .build()
      val url = urlBuilder.offerUrl(offer, isMobile = true)
      url shouldBe s"$mobileHost/motorcycle/used/sale/mark/model/abcd-1234/"
    }

    "generate motorcycle offer desktop link" in {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.MOTO)
        .setMotoInfo(
          MotoInfo
            .newBuilder()
            .setMotoCategory(MotoCategory.MOTORCYCLE)
            .setMark("MARK")
            .setModel("MODEL")
            .build()
        )
        .setId("abcd-1234")
        .setSection(Section.USED)
        .build()
      val url = urlBuilder.offerUrl(offer)
      url shouldBe s"$desktopHost/motorcycle/used/sale/mark/model/abcd-1234/"
    }

    "generate lcv offer mobile link" in {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.TRUCKS)
        .setTruckInfo(
          TruckInfo
            .newBuilder()
            .setTruckCategory(TruckCategory.LCV)
            .setMark("MARK")
            .setModel("MODEL")
            .build()
        )
        .setId("abcd-1234")
        .setSection(Section.USED)
        .build()
      val url = urlBuilder.offerUrl(offer, isMobile = true)
      url shouldBe s"$mobileHost/lcv/used/sale/mark/model/abcd-1234/"
    }

    "generate lcv offer desktop link" in {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.TRUCKS)
        .setTruckInfo(
          TruckInfo
            .newBuilder()
            .setTruckCategory(TruckCategory.LCV)
            .setMark("MARK")
            .setModel("MODEL")
            .build()
        )
        .setId("abcd-1234")
        .setSection(Section.USED)
        .build()
      val url = urlBuilder.offerUrl(offer)
      url shouldBe s"$desktopHost/lcv/used/sale/mark/model/abcd-1234/"
    }

    "generate cert mobile link" in {
      val cert = ModelGenerators.CertGen.next
      val url = urlBuilder.certUrl(cert, isMobile = true)
      url should startWith(mobileHost)
      url should endWith(cert.getHash)
    }

    "generate url for special" in {
      val baseTruck = ModelGenerators.TruckOfferGen.next
      val offer = baseTruck.updated { b =>
        b.setSection(Section.USED)
        b.getTruckInfoBuilder.setTruckCategory(TruckCategory.CRANE_HYDRAULICS)
      }

      val url = urlBuilder.offerUrl(offer)
      val mark = offer.getTruckInfo.getMark.toLowerCase
      val model = offer.getTruckInfo.getModel.toLowerCase
      url shouldBe s"$desktopHost/crane_hydraulics/used/sale/$mark/$model/${offer.id}/"
    }

    "generate url for a reseller" in {
      val user = AutoruUser(arbitrary[Int].next)
      val encryptedUser = cryptoUserId.encrypt(user)
      val url = urlBuilder.resellerUrl(user)
      url shouldBe s"$desktopHost/reseller/${encryptedUser.escaped}/all"
    }
  }
}
