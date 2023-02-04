package ru.yandex.vertis.passport.service.promocoder

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.scalatest.{BeforeAndAfterEach, WordSpec}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.service.pushnoy.PushnoyClient
import ru.yandex.vertis.passport.service.user.UserService
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.concurrent.Future

class PromoCodeServiceSpec extends WordSpec with SpecBase with BeforeAndAfterEach with MockitoSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  class Context {
    val promoCoderClient = mock[PromoCoderClient]
    val pushnoyClient = mock[PushnoyClient]
    val userService = mock[UserService]

    val service = new PromoCodeServiceImpl(promoCoderClient, pushnoyClient, userService)
  }

  def context(f: Context => Unit): Unit = {
    f(new Context)
  }

  "PromoCodeService" should {
    "should apply placement promocode" in context { ctx =>
      val userId = ModelGenerators.userId.next
      when(ctx.promoCoderClient.applyPlacementPromocode(?, ?, ?, ?)(?)).thenReturn(Future.unit)
      when(ctx.pushnoyClient.pushToUser(?)(?)).thenReturn(Future.unit)
      val profile = ModelGenerators.userProfile.next
      when(ctx.userService.getProfile(?)(?)).thenReturn(Future.successful(profile))
      ctx.service.applyPlacementPromoCode(userId, 1, 30).futureValue
      verify(ctx.promoCoderClient).applyPlacementPromocode(
        eq(userId),
        eq(1),
        eq(PromocoderServices.AutoRuUsers),
        eq(30)
      )(?)
      verify(ctx.pushnoyClient).pushToUser(?)(?)
    }
    "should not apply placement promocode in case of promoCoderClient call is failed" in context { ctx =>
      val userId = ModelGenerators.userId.next
      val profile = ModelGenerators.userProfile.next
      when(ctx.userService.getProfile(?)(?)).thenReturn(Future.successful(profile))
      when(ctx.promoCoderClient.applyPlacementPromocode(?, ?, ?, ?)(?))
        .thenReturn(Future.failed(new RuntimeException()))
      ctx.service.applyPlacementPromoCode(userId, 1, 30).futureValue
      verify(ctx.promoCoderClient).applyPlacementPromocode(
        eq(userId),
        eq(1),
        eq(PromocoderServices.AutoRuUsers),
        eq(30)
      )(?)
      verifyZeroInteractions(ctx.pushnoyClient)
    }
  }
}
