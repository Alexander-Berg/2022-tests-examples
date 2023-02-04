package ru.yandex.vertis.moderation.photos.multiaddress

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer

import scala.concurrent.ExecutionContext.Implicits.global
import ru.yandex.vertis.moderation.httpclient.cbir.CbirClient
import ru.yandex.vertis.moderation.httpclient.cbir.CbirClient.CbirDoc
import ru.yandex.vertis.moderation.httpclient.wizard.WizardClient
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.realty
import ru.yandex.vertis.moderation.photos.multiaddress.MultiAddressPhotoChecker.Verdict
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials.{CategoryType, OfferType}

import scala.concurrent.Future

/**
  * Specs for [[MultiAddressPhotoChecker]]
  *
  * @author frenki
  */
@RunWith(classOf[JUnitRunner])
class MultiAddressPhotoCheckerSpec extends SpecBase {

  val mockCbirClient: CbirClient = mock[CbirClient]
  doReturn(Future.successful(Set(CbirDoc("a", "ya.ru"), CbirDoc("b", "auto.ru"), CbirDoc("c", "realty.yandex.ru"))))
    .when(mockCbirClient)
    .getImageClones(anyString)

  val mockWizardClient: WizardClient = mock[WizardClient]
  doReturn(Future.successful(Set(1, 2, 3))).when(mockWizardClient).resolveBestGeoIds(anyString)

  private def getMultiAddressPhotosChecker(minWrongPhotos: Int = 3,
                                           minDifferentAddressForPhoto: Int = 3,
                                           minDomains: Int = 3
                                          ): MultiAddressPhotoChecker = {
    new MultiAddressPhotoChecker(
      mockCbirClient,
      mockWizardClient,
      minWrongPhotos,
      minDifferentAddressForPhoto,
      minDomains
    )
  }

  private def getRealtyInstance(photos: Seq[realty.PhotoInfo]): Instance = {
    val essentials =
      RealtyEssentialsGen.next.copy(
        offerType = Some(OfferType.RENT),
        categoryType = Some(CategoryType.ROOMS),
        photos = photos,
        photoUrls = photos.map(_.url)
      )
    InstanceGen.next.copy(essentials = essentials)
  }

  "MultiAddressPhotosChecker" should {
    "return None for any instance except realty and auto offer" in {
      EssentialsGenerators
        .filterNot(gen => (gen.equals(RealtyEssentialsGen)) || (gen.equals(AutoruEssentialsGen)))
        .foreach(generator => {
          val essentials = generator.next
          val instance = InstanceGen.next.copy(essentials = essentials)

          val actualResult = getMultiAddressPhotosChecker().check(instance).futureValue
          val expectedResult = None
          actualResult shouldBe expectedResult
        })
    }

    "return None for any offer type except rent and photos is empty" in {
      OfferType.values
        .filterNot(_.equals(OfferType.RENT))
        .foreach(offerType => {
          val essentials =
            RealtyEssentialsGen.next
              .copy(offerType = Some(offerType), categoryType = Some(CategoryType.ROOMS), photoUrls = Seq.empty)
          val instance = InstanceGen.next.copy(essentials = essentials)

          val actualResult = getMultiAddressPhotosChecker().check(instance).futureValue
          val expectedResult = None
          actualResult shouldBe expectedResult
        })
    }

    "return None for any rent offer with different from 'CheckCategoryTypes' category type" in {
      CategoryType.values
        .filterNot(MultiAddressPhotoChecker.CheckCategoryTypes.contains)
        .foreach(categoryType => {
          val essentials =
            RealtyEssentialsGen.next
              .copy(offerType = Some(OfferType.RENT), categoryType = Some(categoryType), photoUrls = Seq.empty)
          val instance = InstanceGen.next.copy(essentials = essentials)

          val actualResult = getMultiAddressPhotosChecker().check(instance).futureValue
          val expectedResult = None
          actualResult shouldBe expectedResult
        })
    }

    "return None for undefined offer type and empty photos" in {
      val essentials = RealtyEssentialsGen.next.copy(offerType = None, photoUrls = Seq.empty)
      val instance = InstanceGen.next.copy(essentials = essentials)

      val actualResult = getMultiAddressPhotosChecker().check(instance).futureValue
      val expectedResult = None
      actualResult shouldBe expectedResult
    }

    "return none verdict when offer has not enough photos to check" in {
      val photos = Seq(RealtyPhotoInfoGen.next.copy(url = "url1"), RealtyPhotoInfoGen.next.copy(url = "url2"))
      val instance = getRealtyInstance(photos)

      val actualResult = getMultiAddressPhotosChecker().check(instance).futureValue
      val expectedResult = None
      actualResult shouldBe expectedResult
    }

    "return ok verdict when photos in offer have less geo ids then limit" in {
      val photos =
        Seq(
          RealtyPhotoInfoGen.next.copy(url = "url1"),
          RealtyPhotoInfoGen.next.copy(url = "url2"),
          RealtyPhotoInfoGen.next.copy(url = "url3")
        )
      val instance = getRealtyInstance(photos)

      val actualResult = getMultiAddressPhotosChecker(3, 4).check(instance).futureValue
      val expectedResult = Some(Verdict.Ok)
      actualResult shouldBe expectedResult
    }

    "return ok verdict when offer has less multi-address photos then limit" in {
      val photos =
        Seq(
          RealtyPhotoInfoGen.next.copy(url = "url1"),
          RealtyPhotoInfoGen.next.copy(url = "url2"),
          RealtyPhotoInfoGen.next.copy(url = "")
        )
      val instance = getRealtyInstance(photos)

      doReturn(Future.successful(Set.empty)).when(mockCbirClient).getImageClones("http:orig")

      val actualResult = getMultiAddressPhotosChecker().check(instance).futureValue
      val expectedResult = Some(Verdict.Ok)
      actualResult shouldBe expectedResult
    }

    "return error verdict when offer has more multi-address photos then limit" in {
      val photos =
        Seq(
          RealtyPhotoInfoGen.next.copy(url = "//avatars/url1/"),
          RealtyPhotoInfoGen.next.copy(url = "//avatars/url2/"),
          RealtyPhotoInfoGen.next.copy(url = "//avatars/url3/"),
          RealtyPhotoInfoGen.next.copy(url = "//avatars/url4/"),
          RealtyPhotoInfoGen.next.copy(url = "//avatars/url5/")
        )
      val instance = getRealtyInstance(photos)

      val actualResult = getMultiAddressPhotosChecker().check(instance).futureValue
      val actualResultCheck =
        actualResult match {
          case Some(Verdict.Error(_)) => true
          case _                      => false
        }
      actualResultCheck shouldBe true
    }

    "return error verdict when offer has one multi-domain photos then limit" in {
      val realtyPhotos = Seq(RealtyPhotoInfoGen.next.copy(url = "//avatars/url1/"))
      val realtyInstance = getRealtyInstance(realtyPhotos)
      val autoruPhotos = Seq(AutoPhotoInfoOptGen.suchThat(_.picaInfo.nonEmpty).next)
      val autoruInstance = InstanceGen.next.copy(essentials = AutoruEssentialsGen.next.copy(photos = autoruPhotos))

      val realtyActualResult = getMultiAddressPhotosChecker().check(realtyInstance).futureValue
      val autoruActualResult = getMultiAddressPhotosChecker().check(autoruInstance).futureValue

      val realtyResultCheck =
        realtyActualResult match {
          case Some(Verdict.Error(_)) => true
          case _                      => false
        }

      val autoruResultCheck =
        autoruActualResult match {
          case Some(Verdict.Error(_)) => true
          case _                      => false
        }
      realtyResultCheck shouldBe true
      autoruResultCheck shouldBe true
    }
  }
}
