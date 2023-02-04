package ru.auto.api.managers.cme

import com.twitter.finagle.stats.NullStatsReceiver
import org.mockito.ArgumentMatchers.{eq => eqq}
import org.mockito.Mockito.{times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.time.{Days, Span}
import ru.auto.api.ApiOfferModel._
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.OfferIdsByVinsResponse
import ru.auto.api.ResponseModel.OfferIdsByVinsResponse.OfferIdByVin
import ru.auto.api.auth.Application
import ru.auto.api.cme.Cme.CmeWidgetAppraisalResponse
import ru.auto.api.exceptions.ExternalIdAliasNotFoundError
import ru.auto.api.features.FeatureManager
import ru.auto.api.http.{FinagleHttpClient, HttpClient, HttpClientConfig}
import ru.auto.api.managers.aliases.AliasesManager
import ru.auto.api.managers.counters.CountersManager
import ru.auto.api.managers.metrics.MetricsManager
import ru.auto.api.managers.offers.{OfferCardManager, PhoneRedirectManager}
import ru.auto.api.model.{CategorySelector, OfferID, RequestParams}
import ru.auto.api.services.cme.CmeWidgetApiClient
import ru.auto.api.services.telepony.TeleponyClient.Domains
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.TimeUtils.DefaultTimeProvider
import ru.auto.dealer_aliases.proto.v2.DealerAliasesService.Alias
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.statist.model.api.ApiModel.MultipleDailyValues
import ru.yandex.vertis.tracing.Traced

class CmeManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with BeforeAndAfter {

  trait mocks {
    val aliasesManager = mock[AliasesManager]

    val redirectManager = mock[PhoneRedirectManager]
    val featureManager = mock[FeatureManager]
    val cmeWidgetApiClient: CmeWidgetApiClient = mock[CmeWidgetApiClient]

    val cmeWidgetAddress = "https://widget.api.preproduction.cm.expert"

    val metricsManager = mock[MetricsManager]
    val countersManager = mock[CountersManager]

    val offerCardManager = mock[OfferCardManager]

    lazy val manager =
      new CmeManager(
        cmeWidgetAddress,
        cmeWidgetApiClient,
        aliasesManager,
        redirectManager,
        featureManager,
        metricsManager,
        countersManager,
        offerCardManager,
        DefaultTimeProvider
      )
    implicit val trace: Traced = Traced.empty

    implicit val request: RequestImpl = {
      val req = new RequestImpl
      req.setApplication(Application.desktop)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty)
      req
    }
  }

  "Get redirect" should {
    "return mock if feature is disabled" in new mocks {
      val cmeRedirectRequest = CmeRedirectRequest
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setExternalClientId("cme-id")
        .setPhoneNumber("7123456789")
        .setPlatform("autoru")
        .build()

      val alias = Alias.newBuilder().setDealerId(1).build()
      val feature = mock[Feature[Boolean]]
      val offerIds = OfferIdsByVinsResponse
        .newBuilder()
        .putResult(
          "VIN",
          OfferIdByVin
            .newBuilder()
            .setCategory(Category.CARS)
            .setSection(Section.NEW)
            .setOfferId("offer-id")
            .build()
        )
        .build()
      val offer = Offer.getDefaultInstance

      when(feature.value).thenReturn(false)
      when(featureManager.returnRealPhoneRedirectForCme).thenReturn(feature)
      when(aliasesManager.lookupRoutes(?, ?)(?)).thenReturnF(List(alias))

      manager.getRedirect(cmeRedirectRequest)

      verify(featureManager).returnRealPhoneRedirectForCme
      verify(aliasesManager, times(0)).lookupRoutes(?, ?)(?)
    }

    "return autoru redirect" in new mocks {
      val cmeRedirectRequest = CmeRedirectRequest
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setExternalClientId("cme-id")
        .setPhoneNumber("+79031234455")
        .setPlatform("autoru")
        .setVin("VIN")
        .build()

      val alias = Alias.newBuilder().setDealerId(1).build()
      val feature = mock[Feature[Boolean]]

      when(feature.value).thenReturn(true)
      when(featureManager.returnRealPhoneRedirectForCme).thenReturn(feature)
      when(aliasesManager.lookupRoutes(?, ?)(?)).thenReturnF(List(alias))

      val result = manager.getRedirect(cmeRedirectRequest)

      verify(featureManager).returnRealPhoneRedirectForCme
      verify(aliasesManager).lookupRoutes(?, ?)(?)

      result.await shouldBe Phone
        .newBuilder()
        .setPlatform("autoru")
        .setRedirect("79031234455")
        .setPhone("79031234455")
        .build()
    }

    "autoru client id not found" in new mocks {
      val cmeRedirectRequest = CmeRedirectRequest
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setExternalClientId("cme-id")
        .setPhoneNumber("7123456789")
        .setPlatform("autoru")
        .build()

      val feature = mock[Feature[Boolean]]

      when(feature.value).thenReturn(true)
      when(featureManager.returnRealPhoneRedirectForCme).thenReturn(feature)
      when(aliasesManager.lookupRoutes(?, ?)(?)).thenReturnF(List.empty)

      manager.getRedirect(cmeRedirectRequest).failed.futureValue shouldBe a[ExternalIdAliasNotFoundError]
    }

    "return external redirect" in new mocks {
      val cmeRedirectRequest = CmeRedirectRequest
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setExternalClientId("cme-id")
        .setPhoneNumber("7123456789")
        .setPlatform("avito")
        .build()

      val alias = Alias.newBuilder().setDealerId(1).build()
      val feature = mock[Feature[Boolean]]
      val offerIds = OfferIdsByVinsResponse
        .newBuilder()
        .putResult(
          "VIN",
          OfferIdByVin
            .newBuilder()
            .setCategory(Category.CARS)
            .setSection(Section.NEW)
            .setOfferId("offer-id")
            .build()
        )
        .build()
      val offer = Offer.getDefaultInstance
      val phone = Phone.getDefaultInstance

      when(feature.value).thenReturn(true)
      when(featureManager.returnRealPhoneRedirectForCme).thenReturn(feature)
      when(redirectManager.getPhoneWithRedirectForCme(?, ?, ?)(?)).thenReturnF(phone)
      when(aliasesManager.lookupRoutes(?, ?)(?)).thenReturnF(List(alias))

      manager.getRedirect(cmeRedirectRequest)

      verify(featureManager).returnRealPhoneRedirectForCme
      verify(redirectManager).getPhoneWithRedirectForCme(?, eqq(Domains.CMExpert), eqq(None))(?)
      verify(aliasesManager, times(0)).lookupRoutes(?, ?)(?)
    }
  }

  "GetCmeAppraisalWidget" should {
    "return appraisal widget url" in new mocks {
      val offerId = OfferID.parse("11111-fff")

      when(cmeWidgetApiClient.getAppraisalWidgetTicket(?, ?)(?)).thenReturnF {
        "test-ticket"
      }

      val expected = CmeWidgetAppraisalResponse
        .newBuilder()
        .setUrl {
          s"$cmeWidgetAddress/autoru-reappraisal?ticket=test-ticket&readonly=true"
        }
        .build()

      val result = manager.getCmeAppraisalWidget(offerId, 200000, readOnly = true)(request).await
      result shouldBe expected

      verify(cmeWidgetApiClient).getAppraisalWidgetTicket(eqq(offerId), eqq(200000))(eqq(request))
    }

  }

  "integration spec" ignore new mocks {

    val clientId = "47094"
    val offerId = "1106540735-ef3c7534"

    val httpClient: HttpClient = {
      val config = HttpClientConfig(host = "localhost", port = 3000)
      new FinagleHttpClient(config, NullStatsReceiver)
    }

    val offer = Offer
      .newBuilder()
      .setId(offerId)
      .setSalon(
        Salon
          .newBuilder()
          .setClientId(clientId)
      )
      .build()

    when(offerCardManager.getRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
    when(countersManager.getByPlatformCounters(?, ?, ?, ?)(?)).thenReturnF(MultipleDailyValues.getDefaultInstance)
    when(countersManager.getCounters(?, ?, ?, ?)(?)).thenReturnF(Map.empty)
    when(countersManager.getViewsPhone(?, ?, ?, ?)(?)).thenReturnF(MultipleDailyValues.getDefaultInstance)

    val myManager = new CmeManager(
      cmeWidgetAddress = "",
      cmeWidgetApiClient = null,
      aliasesManager = null,
      redirectManager = null,
      featureManager = featureManager,
      metricsManager = new MetricsManager(
        dealerStatsClient = null
      ),
      countersManager = countersManager,
      offerCardManager = offerCardManager,
      timeProvider = DefaultTimeProvider
    )

    val metricsF = myManager.getAllOfferMetrics(offerId = OfferID.parse(offerId), category = CategorySelector.Cars)
    val timeout = Timeout.apply(Span.apply(1, Days))
    val res = metricsF.futureValue(timeout)
    res.getPlacements shouldBe (1)
  }

}
