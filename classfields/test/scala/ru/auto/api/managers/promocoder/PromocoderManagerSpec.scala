package ru.auto.api.managers.promocoder

import org.mockito.Mockito.{reset, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.PromocodeModel.PromocodeActivationRequest
import ru.auto.api.exceptions.{NeedAuthentication, PromocodeActivationException, UnknownProductException}
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.PromocoderModelGenerators
import ru.auto.api.services.promocoder.PromocoderClient
import ru.auto.api.services.promocoder.PromocoderServices
import ru.auto.api.services.promocoder.model.PromocoderUser
import ru.auto.api.util.Request
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class PromocoderManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with PromocoderModelGenerators
  with BeforeAndAfterEach
  with TestRequest {

  private val promocoderClient = mock[PromocoderClient]
  private val promocoderManager = new PromocoderManager(promocoderClient)

  implicit override val request: Request = super.request

  override protected def beforeEach(): Unit = {
    reset(promocoderClient)
    super.beforeEach()
  }

  "PromocoderManager.bonusMoney()" should {
    "get count" in {
      val bonusBalanceFeature = moneyFeatureInstanceGen.next

      forAll(PrivateUserRefGen) { (user) =>
        val promocoderUser = PromocoderUser(user)
        val service = PromocoderServices.AutoRuUsers

        when(promocoderClient.getFeatures(?, ?)(?)).thenReturnF(List(bonusBalanceFeature))

        val result = promocoderManager.bonusBalance(user).futureValue
        result shouldBe bonusBalanceFeature.count

        verify(promocoderClient).getFeatures(promocoderUser, service)
      }
    }

    "return zero if no money features" in {
      val promoFeature = promoFeatureInstanceGen.next

      forAll(PrivateUserRefGen) { (user) =>
        val promocoderUser = PromocoderUser(user)
        val service = PromocoderServices.AutoRuUsers

        when(promocoderClient.getFeatures(?, ?)(?)).thenReturnF(List(promoFeature))

        val result = promocoderManager.bonusBalance(user).futureValue
        result shouldBe 0L

        verify(promocoderClient).getFeatures(promocoderUser, service)
      }
    }
  }

  val validate = PromocodeActivationRequest.Validate.newBuilder.addAllProducts(List("placement").asJava)
  val placementValidation = PromocodeActivationRequest.newBuilder.setValidation(validate).build

  val validateOffersHistoryReports =
    PromocodeActivationRequest.Validate.newBuilder.addAllProducts(List("offers-history-reports-10").asJava)

  val offersHistoryReports10Validation =
    PromocodeActivationRequest.newBuilder.setValidation(validateOffersHistoryReports).build

  val noValidation = PromocodeActivationRequest.getDefaultInstance

  "PromocoderManager.activatePromocode" should {
    "activate for user without validation" in {
      forAll(PrivateUserRefGen) { user =>
        val service = PromocoderServices.AutoRuUsers

        when(promocoderClient.activatePromocode(eq(PromocoderUser(user)), eq(service), eq("test_promocode"))(?))
          .thenReturnF(())
        when(promocoderClient.getPromocodeTags(eq(service), eq("test_promocode"))(?)).thenReturnF(List("placement"))

        promocoderManager.activatePromocode(user, "test_promocode", noValidation).futureValue
      }
    }

    "activate for user and validation" in {
      forAll(PrivateUserRefGen) { user =>
        val service = PromocoderServices.AutoRuUsers

        when(promocoderClient.activatePromocode(eq(PromocoderUser(user)), eq(service), eq("test_promocode"))(?))
          .thenReturnF(())
        when(promocoderClient.getPromocodeTags(eq(service), eq("test_promocode"))(?)).thenReturnF(List("placement"))

        promocoderManager.activatePromocode(user, "test_promocode", placementValidation).futureValue
      }
    }

    "activate countable product" in {
      forAll(PrivateUserRefGen) { user =>
        val service = PromocoderServices.AutoRuUsers

        when(promocoderClient.activatePromocode(eq(PromocoderUser(user)), eq(service), eq("test_promocode"))(?))
          .thenReturnF(())
        when(promocoderClient.getPromocodeTags(eq(service), eq("test_promocode"))(?))
          .thenReturnF(List("offers-history-reports-10"))

        promocoderManager.activatePromocode(user, "test_promocode", offersHistoryReports10Validation).futureValue
      }
    }

    "fail validation if validation product doesn't match promocode product" in {
      forAll(PrivateUserRefGen) { user =>
        val service = PromocoderServices.AutoRuUsers

        when(promocoderClient.activatePromocode(eq(PromocoderUser(user)), eq(service), eq("test_promocode"))(?))
          .thenReturnF(())
        when(promocoderClient.getPromocodeTags(eq(service), eq("test_promocode"))(?)).thenReturnF(List("boost"))

        promocoderManager
          .activatePromocode(user, "test_promocode", placementValidation)
          .failed
          .futureValue shouldBe an[PromocodeActivationException]
      }
    }

    "activate for dealer" in {
      forAll(DealerUserRefGen) { dealer =>
        val service = PromocoderServices.AutoRu

        when(promocoderClient.activatePromocode(eq(PromocoderUser(dealer)), eq(service), eq("test_promocode"))(?))
          .thenReturnF(())
        when(promocoderClient.getPromocodeTags(eq(service), eq("test_promocode"))(?)).thenReturnF(List("placement"))

        promocoderManager.activatePromocode(dealer, "test_promocode", placementValidation).futureValue
      }
    }

    "fail on user which isn't user or dealer" in {
      forAll(AnonymousUserRefGen) { anon =>

        promocoderManager
          .activatePromocode(anon, "test_promocode", placementValidation)
          .failed
          .futureValue shouldBe an[NeedAuthentication]
      }
    }

    "fail on unknown product in, don't activate promocode" in {
      forAll(PrivateUserRefGen) { user =>
        val service = PromocoderServices.AutoRuUsers

        when(promocoderClient.getPromocodeTags(eq(service), eq("test_promocode"))(?))
          .thenReturnF(List("wrong_product_name"))

        promocoderManager
          .activatePromocode(user, "test_promocode", placementValidation)
          .failed
          .futureValue shouldBe an[UnknownProductException]
      }
    }
  }
}
