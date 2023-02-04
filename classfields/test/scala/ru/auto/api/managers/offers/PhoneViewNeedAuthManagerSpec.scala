package ru.auto.api.managers.offers

import cats.implicits.catsSyntaxOptionId
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import ru.auto.api.{BaseSpec, DummyOperationalSupport, GeneratorUtils}
import ru.auto.api.auth.{Application, ApplicationToken, StaticApplication}
import ru.auto.api.exceptions.{NeedAuthActionException, ShowUrlActionException}
import ru.auto.api.geo.Tree
import ru.auto.api.managers.offers.PhoneViewNeedAuthManager.{CanViewResult, LimitExceededResult, NeedAuthResult, NeedGosuslugiResult, Skip}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.{RequestParams, UserRef}
import ru.auto.api.model.bunker.phoneview.{PhoneViewWithAuth, WithGosuslugi, WithoutGosuslugi}
import ru.auto.api.services.hydra.HydraClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.util.RequestImpl
import ru.auto.api.ResponseModel.ErrorResponse.ActionTag
import ru.auto.api.app.redis.RedisCache
import ru.auto.api.features.FeatureManager
import ru.yandex.passport.model.api.ApiModel.{UserEssentials, UserSocialProfile}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.SocialProvider
import ru.yandex.vertis.feature.model.Feature

import java.net.URL
import scala.concurrent.Future

class PhoneViewNeedAuthManagerSpec extends BaseSpec with MockitoSupport with DummyOperationalSupport {

  import PhoneViewNeedAuthManagerSpec._

  private val tree = mock[Tree]
  private val passportClient = mock[PassportClient]
  private val hydraClient = mock[HydraClient]
  private val redisCache = mock[RedisCache]
  private val featureManager = mock[FeatureManager]
  val offerPhoneViewNeedAuthEnabled: Feature[Boolean] = mock[Feature[Boolean]]
  when(offerPhoneViewNeedAuthEnabled.value).thenReturn(true)
  val offerPhoneViewNeedAuthDisabled: Feature[Boolean] = mock[Feature[Boolean]]
  when(offerPhoneViewNeedAuthDisabled.value).thenReturn(false)

  private val manager =
    new PhoneViewNeedAuthManager(
      tree,
      phoneViewWithAuth,
      passportClient,
      hydraClient,
      prometheusRegistryDummy,
      redisCache,
      featureManager
    )

  before {
    reset(tree)
    reset(passportClient)
    reset(hydraClient)
    reset(redisCache)
    reset(featureManager)
  }

  "PhoneViewNeedAuthManager.check" should {
    "not authorized user when check" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = AnonymousUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = NeedAuthResult(
        exception = new NeedAuthActionException(
          descriptionRu = "phone_view_with_auth".some
        )
      )

      actual.isInstanceOf[NeedAuthResult] shouldBe true
      actual.asInstanceOf[NeedAuthResult].exception.descriptionRu shouldBe expected.exception.descriptionRu

      verify(tree).isInside(?, ?)
      verifyNoMoreInteractions(passportClient)
      verifyNoMoreInteractions(hydraClient)
    }

    "authorized user when check (has gosuslugi & has requests)" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = PrivateUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF {
        UserEssentials.getDefaultInstance.withGosuslugi()
      }
      when(hydraClient.limiter(?, ?, ?, eq(200.some))(?)).thenReturnF(100)
      when(redisCache.getTtl(?)(?)).thenReturnF(-2L)
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.unit)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = CanViewResult("need_auth_supported".some)

      actual shouldBe expected

      verify(tree).isInside(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verify(hydraClient).limiter(?, ?, ?, ?)(?)
    }

    "authorized user when check (has not trusted gosuslugi & hasn't requests)" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = PrivateUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturn {
        Future.successful(UserEssentials.getDefaultInstance.withGosuslugi(trusted = false))
      }
      when(hydraClient.limiter(?, ?, ?, eq(100.some))(?)).thenReturn {
        Future.successful(0)
      }
      when(redisCache.getTtl(?)(?)).thenReturnF(-2L)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = NeedGosuslugiResult(
        exception = new ShowUrlActionException(
          url = new URL("https://auto.ru/1/"),
          descriptionRu = "without_gosuslugi".some,
          actionTags = Seq(ActionTag.GOSUSLUGI_REQUIRED)
        )
      )

      actual.isInstanceOf[NeedGosuslugiResult] shouldBe true
      val actualCheck = actual.asInstanceOf[NeedGosuslugiResult]
      actualCheck.exception.url shouldBe expected.exception.url
      actualCheck.exception.descriptionRu shouldBe expected.exception.descriptionRu
      actualCheck.exception.actionTags shouldBe expected.exception.actionTags

      verify(tree).isInside(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verify(hydraClient).limiter(?, ?, ?, ?)(?)
      verify(redisCache).getTtl(?)(?)
    }

    "authorized user when check (has gosuslugi & hasn't requests)" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = PrivateUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF {
        UserEssentials.getDefaultInstance.withGosuslugi()
      }
      when(hydraClient.limiter(?, ?, ?, ?)(?)).thenReturnF(0)
      when(redisCache.getTtl(?)(?)).thenReturnF(-2L)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = LimitExceededResult(
        exception = new ShowUrlActionException(
          url = new URL("https://auto.ru/2/"),
          descriptionRu = "phone_view_with_auth".some,
          actionTags = Seq(ActionTag.CONTACTS_LIMITED)
        )
      )

      actual.isInstanceOf[LimitExceededResult] shouldBe true
      val actualCheck = actual.asInstanceOf[LimitExceededResult]
      actualCheck.exception.url shouldBe expected.exception.url
      actualCheck.exception.descriptionRu shouldBe expected.exception.descriptionRu
      actualCheck.exception.actionTags shouldBe expected.exception.actionTags

      verify(tree).isInside(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verify(hydraClient).limiter(?, ?, ?, ?)(?)
      verify(redisCache).getTtl(?)(?)
    }

    "authorized user when check (hasn't gosuslugi & has requests)" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = PrivateUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF {
        UserEssentials.getDefaultInstance
      }
      when(hydraClient.limiter(?, ?, ?, ?)(?)).thenReturnF(100)
      when(redisCache.getTtl(?)(?)).thenReturnF(-2L)
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.unit)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = CanViewResult("need_auth_supported".some)

      actual shouldBe expected

      verify(tree).isInside(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verify(hydraClient).limiter(?, ?, ?, ?)(?)
      verify(redisCache).getTtl(?)(?)
      verify(redisCache).set(?, ?, ?)(?, ?)
    }

    "authorized user when check (hasn't gosuslugi & hasn't requests)" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = PrivateUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF {
        UserEssentials.getDefaultInstance
      }
      when(hydraClient.limiter(?, ?, ?, ?)(?)).thenReturnF(0)
      when(redisCache.getTtl(?)(?)).thenReturnF(-2L)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = NeedGosuslugiResult(
        exception = new ShowUrlActionException(
          url = new URL("https://auto.ru/1/"),
          descriptionRu = "without_gosuslugi".some,
          actionTags = Seq(ActionTag.GOSUSLUGI_REQUIRED)
        )
      )

      actual.isInstanceOf[NeedGosuslugiResult] shouldBe true
      val actualCheck = actual.asInstanceOf[NeedGosuslugiResult]
      actualCheck.exception.url shouldBe expected.exception.url
      actualCheck.exception.descriptionRu shouldBe expected.exception.descriptionRu
      actualCheck.exception.actionTags shouldBe expected.exception.actionTags

      verify(tree).isInside(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verify(hydraClient).limiter(?, ?, ?, ?)(?)
      verify(redisCache).getTtl(?)(?)
    }

    "not authorized user when check is not required by app version" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.14".some
        ),
        user = AnonymousUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = CanViewResult("need_auth_not_supported".some)

      actual shouldBe expected

      verify(tree).isInside(?, ?)
      verifyNoMoreInteractions(passportClient)
      verifyNoMoreInteractions(hydraClient)
      verifyNoMoreInteractions(redisCache)
    }

    "not authorized user when check is not required by feature" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = AnonymousUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthDisabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = Skip

      actual shouldBe expected

      verify(tree).isInside(?, ?)
      verifyNoMoreInteractions(passportClient)
      verifyNoMoreInteractions(hydraClient)
      verifyNoMoreInteractions(redisCache)
    }

    "not suitable dealer offer" in {
      when(tree.isInside(?, ?)).thenReturn(true)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(dealerOffer).await
      val expected = Skip

      actual shouldBe expected

      verify(tree).isInside(?, ?)
      verifyNoMoreInteractions(passportClient)
      verifyNoMoreInteractions(hydraClient)
      verifyNoMoreInteractions(redisCache)
    }

    "authorized user when check (has gosuslugi & has requests) with phone_view_ttl" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = PrivateUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF {
        UserEssentials.getDefaultInstance.withGosuslugi()
      }
      when(redisCache.getTtl(?)(?)).thenReturnF(100L)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = CanViewResult("need_auth_supported".some)

      actual shouldBe expected

      verify(tree).isInside(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verifyNoMoreInteractions(hydraClient)
      verify(redisCache).getTtl(?)(?)
    }

    "authorized user when check (hasn't gosuslugi & has requests) with phone_view_ttl" in {
      val req = generateReq(
        requestParams = RequestParams.construct(
          ip = "1.1.1.1",
          iosAppVersion = "11.16".some
        ),
        user = PrivateUserRefGen.next
      )

      when(tree.isInside(?, ?)).thenReturn(true)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF {
        UserEssentials.getDefaultInstance
      }
      when(redisCache.getTtl(?)(?)).thenReturnF(100L)
      when(featureManager.offerPhoneViewNeedAuth).thenReturn(offerPhoneViewNeedAuthEnabled)

      val actual = manager.check(privateOffer)(req).await
      val expected = CanViewResult("need_auth_supported".some)

      actual shouldBe expected

      verify(tree).isInside(?, ?)
      verify(passportClient).getUserEssentials(?, ?)(?)
      verifyNoMoreInteractions(hydraClient)
      verify(redisCache).getTtl(?)(?)
    }
  }
}

object PhoneViewNeedAuthManagerSpec extends GeneratorUtils {

  private val withoutGosuslugi =
    WithoutGosuslugi(
      descriptionRu = "without_gosuslugi".some,
      limit = 100.some,
      url = new URL("https://auto.ru/1/").some
    )

  private val withGosuslugi =
    WithGosuslugi(
      descriptionRu = None,
      limit = 200.some,
      url = new URL("https://auto.ru/2/").some
    )

  private val phoneViewWithAuth =
    PhoneViewWithAuth(
      descriptionRu = "phone_view_with_auth".some,
      geobaseIds = Set(1L),
      withoutGosuslugi = withoutGosuslugi.some,
      withGosuslugi = withGosuslugi.some
    )

  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = generateReq()

  private def generateReq(requestParams: RequestParams = RequestParams.construct("1.1.1.1"),
                          application: StaticApplication = Application.iosApp,
                          token: ApplicationToken = TokenServiceImpl.iosApp,
                          user: UserRef = PersonalUserRefGen.next): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(requestParams)
    r.setApplication(application)
    r.setToken(token)
    r.setUser(user)
    r.setTrace(trace)
    r
  }

  private val dealerOffer = {
    val b = DealerOfferGen.next.toBuilder
    b.getPrivateSellerBuilder.getLocationBuilder.setGeobaseId(1L)
    b.build
  }

  private val privateOffer = {
    val b = PrivateOfferGen.next.toBuilder
    b.getPrivateSellerBuilder.getLocationBuilder.setGeobaseId(1L)
    b.build
  }

  implicit private class RichUserEssentials(val value: UserEssentials) extends AnyVal {

    def withGosuslugi(trusted: Boolean = true): UserEssentials =
      value.toBuilder
        .addSocialProfiles(
          UserSocialProfile.newBuilder
            .setProvider(SocialProvider.GOSUSLUGI)
            .setTrusted(trusted)
            .build
        )
        .build
  }
}
