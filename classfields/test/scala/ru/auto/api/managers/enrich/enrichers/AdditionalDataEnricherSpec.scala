package ru.auto.api.managers.enrich.enrichers

import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Multiposting.Classified
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.BaseSpec
import ru.auto.api.auth.{Application, ApplicationToken, StaticApplication}
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.Tree
import ru.auto.api.model.ModelGenerators.{DealerCarsNewOfferGen, DealerCarsUsedOfferGen}
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.moderation.Placeholders
import ru.auto.api.model.uaas.UaasResponse
import ru.auto.api.model.{RequestParams, SessionID, UserRef}
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.util.{RequestImpl, UrlBuilder}
import ru.yandex.vertis.generators.DateTimeGenerators.{dateTimeInFuture, dateTimeInPast}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class AdditionalDataEnricherSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with OptionValues {

  private val dataService = mock[DataService]
  private val urlBuilder = mock[UrlBuilder]
  private val placeholders = mock[Placeholders]
  private val featureManager = mock[FeatureManager]
  private val tree = mock[Tree]

  private val enricher = new AdditionalDataEnricher(dataService, urlBuilder, placeholders, featureManager, tree)

  implicit private val request: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionID("test_session"))))
    r.setUser(UserRef.dealer(20101))
    r.setApplication(Application.desktop)
    r
  }

  def newUserRequest(experiments: Set[String] = Set.empty,
                     uaasExpFlags: String = "",
                     application: StaticApplication = Application.web,
                     token: ApplicationToken = TokenServiceImpl.web): RequestImpl = {
    val req = new RequestImpl
    val uaasResponse =
      if (uaasExpFlags.nonEmpty) UaasResponse.empty.copy(expFlags = Some(uaasExpFlags)) else UaasResponse.empty
    req.setApplication(application)
    req.setToken(token)
    req.setRequestParams(RequestParams.construct("1.1.1.1", experiments = experiments, uaasResponse = uaasResponse))
    req.setUser(UserRef.user(20101))
    req
  }

  "AdditionalDataEnricher.fillDaysOnSale()" should {

    "enrich with days in stock and days on sale" in {
      val b = DealerCarsUsedOfferGen.next.toBuilder

      b.getAdditionalInfoBuilder.setCreationDate {
        DateTime.now().minusDays(3).minusSeconds(30).getMillis
      }

      enricher.fillDaysOnSale(b)
      val enriched = b.build()

      enriched.getAdditionalInfo.getDaysInStock shouldBe 4
      enriched.getAdditionalInfo.getDaysOnSale shouldBe 4
    }

    "enrich classifieds with days days on sale" in {
      val b = DealerCarsUsedOfferGen.next.toBuilder

      b.getMultipostingBuilder
        .clearClassifieds()
        .addClassifieds {
          Classified.newBuilder().setName(ClassifiedName.DROM).setCreateDate {
            DateTime.now().minusDays(4).minusSeconds(30).getMillis
          }
        }
        .addClassifieds {
          Classified.newBuilder().setName(ClassifiedName.AVITO).setCreateDate {
            DateTime.now().minusDays(2).minusSeconds(30).getMillis
          }
        }

      enricher.fillDaysOnSale(b)
      val enriched = b.build()

      enriched.findClassified(ClassifiedName.DROM).value.getDaysOnSale shouldBe 5
      enriched.findClassified(ClassifiedName.AVITO).value.getDaysOnSale shouldBe 3
    }

    "enrich with days in stock and days on sale in multiposting offer" in {
      val b = DealerCarsUsedOfferGen.next.toBuilder

      b.getAdditionalInfoBuilder.setCreationDate {
        DateTime.now().minusDays(5).minusSeconds(30).getMillis
      }

      b.getMultipostingBuilder
        .clearClassifieds()
        .addClassifieds {
          Classified.newBuilder().setCreateDate {
            DateTime.now().minusDays(4).minusSeconds(30).getMillis
          }
        }
        .addClassifieds {
          Classified.newBuilder().setCreateDate {
            DateTime.now().minusDays(2).minusSeconds(30).getMillis
          }
        }

      enricher.fillDaysOnSale(b)
      val enriched = b.build()

      enriched.getAdditionalInfo.getDaysInStock shouldBe 6
      enriched.getAdditionalInfo.getDaysOnSale shouldBe 5
    }

    "enrich with zero days on sale for multiposting offer without classifieds" in {
      val b = DealerCarsUsedOfferGen.next.toBuilder

      b.getAdditionalInfoBuilder.setCreationDate {
        DateTime.now().minusDays(5).minusSeconds(30).getMillis
      }

      b.getMultipostingBuilder
        .setStatus(OfferStatus.ACTIVE)
        .clearClassifieds()

      enricher.fillDaysOnSale(b)
      val enriched = b.build()

      enriched.getAdditionalInfo.getDaysInStock shouldBe 6
      enriched.getAdditionalInfo.getDaysOnSale shouldBe 0
    }

  }

  "AdditionalDataEnricher.fillMultiposting()" should {
    def service(name: String, isActive: Option[Boolean], expireDate: Option[Long]): Classified.Service = {
      val builder = Classified.Service.newBuilder().setService(name)
      isActive.foreach(builder.setIsActive)
      expireDate.foreach(builder.setExpireDate)
      builder.build()
    }

    def classified(services: Classified.Service*): Classified = {
      Classified
        .newBuilder()
        .clearServices()
        .addAllServices(services.asJava)
        .build()
    }

    "actualize is_active flag for classifieds services" in {
      val b = DealerCarsUsedOfferGen.next.toBuilder

      val timestampInFuture = dateTimeInFuture().next.getMillis
      val timestampInPast = dateTimeInPast().next.getMillis

      b.getMultipostingBuilder
        .clearClassifieds()
        .addClassifieds {
          classified(
            service("service_A", isActive = Some(true), expireDate = Some(timestampInPast)),
            service("service_A", isActive = Some(true), expireDate = Some(timestampInFuture)),
            service("service_B", isActive = Some(false), expireDate = Some(timestampInFuture))
          )
        }
        .addClassifieds {
          classified(
            service("service_A", isActive = None, expireDate = Some(timestampInFuture)),
            service("service_A", isActive = Some(true), expireDate = None)
          )
        }

      enricher.fillMultiposting(b)
      val enriched = b.build()

      val expected = Seq(
        classified(
          service(
            "service_A",
            isActive = Some(false), // is_active = false by expire_date in past
            expireDate = Some(timestampInPast)
          ),
          service(
            "service_A",
            isActive = Some(true), // ok
            expireDate = Some(timestampInFuture)
          ),
          service(
            "service_B",
            isActive = Some(true), // is_active = true by expire_date in future
            expireDate = Some(timestampInFuture)
          )
        ),
        classified(
          service(
            "service_A",
            isActive = Some(true), // is_active = true by expire_date in future
            expireDate = Some(timestampInFuture)
          ),
          service(
            "service_A",
            isActive = Some(false), // is_active = false by empty expire_date
            expireDate = None
          )
        )
      )

      enriched.getMultiposting.getClassifiedsList.asScala.toList shouldBe expected
    }

    "AdditionalDataEnricher.enrichChatExps()" should {
      "set chatEnabled=true for new cars under exp" in {

        val uaasExpFlags = new String(
          Base64.encodeBase64(
            """[{"HANDLER": "AUTORU_APP","CONTEXT": {"MAIN": {"AUTORU_APP": {"AUTORUOFFICE-6072-with-chat": true}}},"TESTID": ["234013"]}]"""
              .getBytes("UTF-8")
          ),
          "UTF-8"
        )
        implicit val request: RequestImpl =
          newUserRequest(application = Application.iosApp, token = TokenServiceImpl.iosApp, uaasExpFlags = uaasExpFlags)
        val b = DealerCarsNewOfferGen.next.toBuilder
        b.getSellerBuilder.setChatsEnabled(false)

        when(tree.isInside(?, ?)).thenReturn(true)

        enricher.enrichChatExps(b)
        val enriched = b.build()

        enriched.getSeller.getChatsEnabled shouldBe true
      }

      "set chatEnabled=false for new cars under exp" in {

        val uaasExpFlags = new String(
          Base64.encodeBase64(
            """[{"HANDLER": "AUTORU_APP","CONTEXT": {"MAIN": {"AUTORU_APP": {"AUTORUOFFICE-6072-without-chat": true}}},"TESTID": ["234013"]}]"""
              .getBytes("UTF-8")
          ),
          "UTF-8"
        )
        implicit val request: RequestImpl =
          newUserRequest(application = Application.iosApp, token = TokenServiceImpl.iosApp, uaasExpFlags = uaasExpFlags)
        val b = DealerCarsNewOfferGen.next.toBuilder
        b.getSellerBuilder.setChatsEnabled(true)

        when(tree.isInside(?, ?)).thenReturn(true)

        enricher.enrichChatExps(b)
        val enriched = b.build()

        enriched.getSeller.getChatsEnabled shouldBe false
      }
    }
  }
}
