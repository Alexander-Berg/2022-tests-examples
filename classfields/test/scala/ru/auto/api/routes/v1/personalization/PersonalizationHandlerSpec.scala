package ru.auto.api.routes.v1.personalization

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, Uri}
import org.mockito.Mockito.{reset, times, verify, verifyNoMoreInteractions}
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.RecommendedOffersResponse
import ru.auto.api.directives.AuthDirectives
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.managers.personalization.PersonalizationManager
import ru.auto.api.routes.v1.chat.ChatHandlerSpec.SupportedAccepts
import ru.auto.api.services.MockedClients
import ru.auto.api.services.bigbrother.BigBrotherSearchParams

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class PersonalizationHandlerSpec extends ApiSpec with MockedClients with BeforeAndAfter with AuthDirectives {
  override lazy val personalizationManager: PersonalizationManager = mock[PersonalizationManager]

  before {
    when(personalizationManager.getRecommendedOffers(?, ?, ?, ?, ?)(?))
      .thenReturn(Future.successful(None.orNull))
  }

  after {
    verifyNoMoreInteractions(personalizationManager)
    reset(personalizationManager)
  }

  "/1.0/personalization/get-recommended-offers" should {
    "return recommended offers" in {
      val yandexUid = "1234567890"
      val offer = OfferGen.next

      val resp = RecommendedOffersResponse.newBuilder().addOffers(offer).build()

      when(
        personalizationManager
          .getRecommendedOffers(eq(BigBrotherSearchParams(yandexUid = Some(yandexUid))), ?, ?, ?, ?)(?)
      ).thenReturn(Future.successful(resp))

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)

      SupportedAccepts.foreach { accept =>
        Get(Uri("/1.0/personalization/get-recommended-offers"))
          .withHeaders(accept, testAuthorizationHeader, RawHeader(XYandexUidHeaderName, yandexUid)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[RecommendedOffersResponse].getOffersList.asScala should contain(offer)
          }
      }

      verify(personalizationManager, times(SupportedAccepts.length)).getRecommendedOffers(
        eq(BigBrotherSearchParams(yandexUid = Some(yandexUid))),
        ?,
        eq(false),
        eq(15),
        ?
      )(
        ?
      )
    }

    "return recommended offers without last viewed" in {
      val yandexUid = "1234567890"
      val offer = OfferGen.next

      val resp = RecommendedOffersResponse.newBuilder().addOffers(offer).build()

      when(
        personalizationManager
          .getRecommendedOffers(eq(BigBrotherSearchParams(yandexUid = Some(yandexUid))), ?, ?, ?, ?)(?)
      ).thenReturn(Future.successful(resp))

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)

      SupportedAccepts.foreach { accept =>
        Get(Uri("/1.0/personalization/get-recommended-offers?do_not_add_last_viewed=true"))
          .withHeaders(accept, testAuthorizationHeader, RawHeader(XYandexUidHeaderName, yandexUid)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[RecommendedOffersResponse].getOffersList.asScala should contain(offer)
          }
      }

      verify(personalizationManager, times(SupportedAccepts.length)).getRecommendedOffers(
        eq(BigBrotherSearchParams(yandexUid = Some(yandexUid))),
        ?,
        eq(true),
        eq(15),
        ?
      )(
        ?
      )
    }
  }
}
