package ru.auto.api.managers.enrich.enrichers

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import org.apache.commons.codec.binary.Base64
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.TeleponyInfo
import ru.auto.api.auth.{Application, ApplicationToken, StaticApplication}
import ru.auto.api.exceptions.TeleponyInvalidRequest
import ru.auto.api.experiments.SourceLastCalls
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.model.ModelGenerators.DealerCarOfferGen
import ru.auto.api.model.ModelUtils.RichOfferOrBuilder
import ru.auto.api.model.uaas.UaasResponse
import ru.auto.api.model.{RequestParams, UserRef}
import ru.auto.api.services.dealer_pony.DealerPonyClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.telepony.TeleponyClient
import ru.auto.api.services.telepony.TeleponyClient.Domains
import ru.auto.api.util.RequestImpl
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.yandex.passport.model.api.ApiModel.{SessionResult, UserEssentials}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.model.proto.SimplifiedCallResultEnum.SimplifiedCallResult
import ru.yandex.vertis.telepony.model.proto.TeleponyCall.CallType
import ru.yandex.vertis.telepony.model.proto.{SourceLastCallsRequest, SourceLastCallsResponse}
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class SourceLastCallsEnricherSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  private val teleponyClient = mock[TeleponyClient]
  private val dealerPonyClient = mock[DealerPonyClient]
  private val featureManager = mock[FeatureManager]
  private val allowEnrichment = mock[Feature[Boolean]]
  private val allowEnrichmentAll = mock[Feature[Boolean]]
  when(allowEnrichment.value).thenReturn(true)
  when(featureManager.allowSourceLastCallsEnrichment).thenReturn(allowEnrichment)
  when(featureManager.allowSourceLastCallsEnrichmentAll).thenReturn(allowEnrichmentAll)

  implicit val trace: Traced = Traced.empty

  val userRef: UserRef = UserRef.parse("user:123")

  private val userPhones = List("79876543210", "79876543211")

  def userRequest(phones: List[String] = userPhones,
                  experiments: Set[String] = Set(SourceLastCalls.desktopExp),
                  uaasExpFlags: String = "",
                  application: StaticApplication = Application.web,
                  token: ApplicationToken = TokenServiceImpl.web): RequestImpl = {
    val req = new RequestImpl
    val uaasResponse =
      if (uaasExpFlags.nonEmpty) UaasResponse.empty.copy(expFlags = Some(uaasExpFlags)) else UaasResponse.empty
    req.setApplication(application)
    req.setToken(token)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.construct("1.1.1.1", experiments = experiments, uaasResponse = uaasResponse))
    req.setSession(
      SessionResult
        .newBuilder()
        .setUser(
          UserEssentials
            .newBuilder()
            .addAllPhones(phones.asJava)
        )
        .build()
    )
    req.setUser(userRef)
    req
  }

  private val enricher = new SourceLastCallsEnricher(teleponyClient, dealerPonyClient, featureManager)

  "SourceLastCallsEnricher enricher" should {
    "enrich offer with source last calls" in {
      implicit val req = userRequest()
      val offer = DealerCarOfferGen.next
      val toEnrich = offer.toBuilder.setDescription("text").build()

      reset(teleponyClient, dealerPonyClient)
      val teleponyInfo = TeleponyInfo.newBuilder().setDomain(Domains.AutoUsers.toString).build()
      val sourceLastCalls = Seq(
        sourceLastCall(
          "123",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        ),
        sourceLastCall(
          "124",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        )
      )

      val expectedResult = userPhones.flatMap { phone =>
        sourceLastCalls.map { call =>
          ApiOfferModel.SourceLastCall
            .newBuilder()
            .setSourcePhone(phone)
            .setResult(call.getResult)
            .setType(call.getCallType)
            .setTime(call.getTime)
            .build()
        }
      }

      when(dealerPonyClient.getTeleponyInfoByOffer(?)(?)).thenReturnF(teleponyInfo)
      when(teleponyClient.getSourceLastCalls(?, ?, ?)(?)).thenReturnF(sourceLastCalls)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(toEnrich)
      enriched.getDescription shouldBe "text"
      enriched.getSourceLastCallsList.asScala shouldBe expectedResult

      verify(dealerPonyClient).getTeleponyInfoByOffer(offer)(req)
      userPhones.foreach { phone =>
        verify(teleponyClient)
          .getSourceLastCalls(phone, Domains.AutoUsers.toString, Seq(offer.id.id.toString))(
            req.trace
          )
      }
    }

    "enrich offer with source last calls for ios" in {
      val uaasExpFlags = new String(
        Base64.encodeBase64(
          """[{"HANDLER": "AUTORU_APP","CONTEXT": {"MAIN": {"AUTORU_APP": {"call_badge_enabled": true}}},"TESTID": ["234013"]}]"""
            .getBytes("UTF-8")
        ),
        "UTF-8"
      )
      implicit val req =
        userRequest(application = Application.iosApp, token = TokenServiceImpl.iosApp, uaasExpFlags = uaasExpFlags)
      val offer = DealerCarOfferGen.next
      val toEnrich = offer.toBuilder.setDescription("text").build()

      reset(teleponyClient, dealerPonyClient)
      val teleponyInfo = TeleponyInfo.newBuilder().setDomain(Domains.AutoUsers.toString).build()
      val sourceLastCalls = Seq(
        sourceLastCall(
          "123",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        ),
        sourceLastCall(
          "124",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        )
      )

      val expectedResult = userPhones.flatMap { phone =>
        sourceLastCalls.map { call =>
          ApiOfferModel.SourceLastCall
            .newBuilder()
            .setSourcePhone(phone)
            .setResult(call.getResult)
            .setType(call.getCallType)
            .setTime(call.getTime)
            .build()
        }
      }

      when(dealerPonyClient.getTeleponyInfoByOffer(?)(?)).thenReturnF(teleponyInfo)
      when(teleponyClient.getSourceLastCalls(?, ?, ?)(?)).thenReturnF(sourceLastCalls)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(toEnrich)
      enriched.getDescription shouldBe "text"
      enriched.getSourceLastCallsList.asScala shouldBe expectedResult

      verify(dealerPonyClient).getTeleponyInfoByOffer(offer)(req)
      userPhones.foreach { phone =>
        verify(teleponyClient)
          .getSourceLastCalls(phone, Domains.AutoUsers.toString, Seq(offer.id.id.toString))(
            req.trace
          )
      }
    }

    "enrich not failing when no domain found" in {
      implicit val req = userRequest()
      val offer = DealerCarOfferGen.next

      offer.getSeller.getTeleponyInfo.toBuilder.setDomain("")
      val toEnrich = offer.toBuilder.setDescription("text").build()

      reset(teleponyClient, dealerPonyClient)
      val teleponyInfo = TeleponyInfo.newBuilder().setDomain("").build()
      val sourceLastCalls = Seq(
        sourceLastCall(
          "123",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        ),
        sourceLastCall(
          "124",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        )
      )

      val expectedResult = userPhones.flatMap { phone =>
        sourceLastCalls.map { call =>
          ApiOfferModel.SourceLastCall
            .newBuilder()
            .setSourcePhone(phone)
            .setResult(call.getResult)
            .setType(call.getCallType)
            .setTime(call.getTime)
            .build()
        }
      }

      when(dealerPonyClient.getTeleponyInfoByOffer(?)(?)).thenReturnF(teleponyInfo)
      when(teleponyClient.getSourceLastCalls(?, ?, ?)(?)).thenReturnF(sourceLastCalls)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(toEnrich)
      enriched.getDescription shouldBe "text"
      enriched.getSourceLastCallsList.asScala shouldBe expectedResult

      verify(dealerPonyClient).getTeleponyInfoByOffer(offer)(req)
      userPhones.foreach { phone =>
        verify(teleponyClient)
          .getSourceLastCalls(phone, Domains.AutoUsers.toString, Seq(offer.id.id.toString))(
            req.trace
          )
      }
    }

    "enrich returns successful requests when some fails" in {
      implicit val req = userRequest()
      val offer = DealerCarOfferGen.next

      val toEnrich = offer.toBuilder.setDescription("text").build()

      reset(teleponyClient, dealerPonyClient)
      val teleponyInfo = TeleponyInfo.newBuilder().setDomain(Domains.AutoUsers.toString).build()
      val sourceLastCalls = Seq(
        sourceLastCall(
          "123",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        ),
        sourceLastCall(
          "124",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        )
      )

      val List(phone1, phone2) = userPhones: @unchecked

      val expectedResult =
        sourceLastCalls.map { call =>
          ApiOfferModel.SourceLastCall
            .newBuilder()
            .setSourcePhone(phone2)
            .setResult(call.getResult)
            .setType(call.getCallType)
            .setTime(call.getTime)
            .build()
        }

      val firstReq = sourceLastCallsRequest(phone1, Seq(offer.id.id.toString))
      val secondReq = sourceLastCallsRequest(phone2, Seq(offer.id.id.toString))

      when(dealerPonyClient.getTeleponyInfoByOffer(?)(?)).thenReturnF(teleponyInfo)
      when(teleponyClient.getSourceLastCalls(eq(phone1), ?, eq(Seq(offer.id.id.toString)))(?))
        .thenReturn(Future.failed(new TeleponyInvalidRequest(Some("error"))))
      when(teleponyClient.getSourceLastCalls(eq(phone2), ?, eq(Seq(offer.id.id.toString)))(?))
        .thenReturnF(sourceLastCalls)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(toEnrich)
      enriched.getDescription shouldBe "text"
      enriched.getSourceLastCallsList.asScala shouldBe expectedResult

      verify(dealerPonyClient).getTeleponyInfoByOffer(offer)(req)
      userPhones.foreach { phone =>
        verify(teleponyClient)
          .getSourceLastCalls(phone, Domains.AutoUsers.toString, Seq(offer.id.id.toString))(
            req.trace
          )
      }
    }

    "dont enrich offer with enrich options sourcelastcalls = false" in {
      implicit val req = userRequest()
      val offer = DealerCarOfferGen.next

      reset(teleponyClient, dealerPonyClient)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions()).futureValue
      val enriched = enrich(offer)
      enriched shouldBe offer

      verifyNoMoreInteractions(dealerPonyClient)
      verifyNoMoreInteractions(teleponyClient)
    }

    "dont enrich offer without user phones" in {
      implicit val req = userRequest(phones = Nil)
      val offer = DealerCarOfferGen.next

      reset(teleponyClient, dealerPonyClient)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(offer)
      enriched shouldBe offer

      verifyNoMoreInteractions(dealerPonyClient)
      verifyNoMoreInteractions(teleponyClient)
    }

    "dont enrich offer without sourceLastCalls experiment" in {
      implicit val req = userRequest(experiments = Set.empty)
      val offer = DealerCarOfferGen.next

      reset(teleponyClient, dealerPonyClient)
      when(allowEnrichmentAll.value).thenReturn(false)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(offer)
      enriched shouldBe offer

      verifyNoMoreInteractions(dealerPonyClient)
      verifyNoMoreInteractions(teleponyClient)
    }

    "dont enrich offer with disabled sourceLastCalls experiment on ios" in {
      val uaasExpFlags = new String(
        Base64.encodeBase64(
          """[{"HANDLER": "AUTORU_APP","CONTEXT": {"MAIN": {"AUTORU_APP": {"call_badge_enabled": false}}},"TESTID": ["234013"]}]"""
            .getBytes("UTF-8")
        ),
        "UTF-8"
      )
      implicit val req =
        userRequest(
          experiments = Set.empty,
          application = Application.iosApp,
          token = TokenServiceImpl.iosApp,
          uaasExpFlags = uaasExpFlags
        )
      val offer = DealerCarOfferGen.next

      reset(teleponyClient, dealerPonyClient)
      when(allowEnrichmentAll.value).thenReturn(false)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(offer)
      enriched shouldBe offer

      verifyNoMoreInteractions(dealerPonyClient)
      verifyNoMoreInteractions(teleponyClient)
    }

    "enrich offer without sourceLastCalls experiment with all feature" in {
      implicit val req = userRequest(experiments = Set.empty)
      val offer = DealerCarOfferGen.next
      val toEnrich = offer.toBuilder.setDescription("text").build()

      reset(teleponyClient, dealerPonyClient)
      val teleponyInfo = TeleponyInfo.newBuilder().setDomain(Domains.AutoUsers.toString).build()
      val sourceLastCalls = Seq(
        sourceLastCall(
          "123",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        ),
        sourceLastCall(
          "124",
          offer.id.id.toString,
          None,
          Timestamps.fromMillis(System.currentTimeMillis()),
          CallType.REDIRECT_CALL,
          SimplifiedCallResult.SUCCESS
        )
      )
      when(allowEnrichmentAll.value).thenReturn(true)

      val expectedResult = userPhones.flatMap { phone =>
        sourceLastCalls.map { call =>
          ApiOfferModel.SourceLastCall
            .newBuilder()
            .setSourcePhone(phone)
            .setResult(call.getResult)
            .setType(call.getCallType)
            .setTime(call.getTime)
            .build()
        }
      }

      when(dealerPonyClient.getTeleponyInfoByOffer(?)(?)).thenReturnF(teleponyInfo)
      when(teleponyClient.getSourceLastCalls(?, ?, ?)(?)).thenReturnF(sourceLastCalls)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(toEnrich)
      enriched.getDescription shouldBe "text"
      enriched.getSourceLastCallsList.asScala shouldBe expectedResult

      verify(dealerPonyClient).getTeleponyInfoByOffer(offer)(req)
      userPhones.foreach { phone =>
        verify(teleponyClient)
          .getSourceLastCalls(phone, Domains.AutoUsers.toString, Seq(offer.id.id.toString))(
            req.trace
          )
      }
    }

    "dont enrich offer without source last calls enrichment feature" in {
      implicit val req = userRequest()
      val offer = DealerCarOfferGen.next

      reset(teleponyClient, dealerPonyClient, allowEnrichment)
      when(allowEnrichment.value).thenReturn(false)

      val enrich = enricher.getFunction(Seq(offer), EnrichOptions(sourceLastCalls = true)).futureValue
      val enriched = enrich(offer)
      enriched shouldBe offer

      verifyNoMoreInteractions(dealerPonyClient)
      verifyNoMoreInteractions(teleponyClient)
    }
  }

  private def sourceLastCallsRequest(phone: String, objectIds: Seq[String]) = {
    SourceLastCallsRequest
      .newBuilder()
      .setSourcePhone(phone)
      .addAllObjectId(objectIds.asJava)
      .build()
  }

  private def sourceLastCall(id: String,
                             objectId: String,
                             tag: Option[String],
                             time: Timestamp,
                             callType: CallType,
                             result: SimplifiedCallResult) = {
    val builder = SourceLastCallsResponse.Call
      .newBuilder()
      .setId(id)
      .setObjectId(objectId)

    tag.foreach(builder.setTag)

    builder
      .setTime(time)
      .setCallType(callType)
      .setResult(result)
      .build()
  }

}
