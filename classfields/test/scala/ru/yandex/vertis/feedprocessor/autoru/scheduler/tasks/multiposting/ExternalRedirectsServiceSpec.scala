package ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting

import akka.actor.{ActorSystem, Scheduler}
import com.google.protobuf.util.Timestamps
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.ApiOfferModel._
import ru.auto.calltracking.proto.CallsService.{GetSettingsRequest, SettingsResponse}
import ru.auto.calltracking.proto.Model.Settings
import ru.auto.calltracking.proto.RedirectsServiceOuterClass.{
  GetOrCreateRedirectConfirmationRequest,
  GetOrCreateRedirectConfirmationResponse
}
import ru.auto.dealer_pony.proto.ApiModel.TeleponyInfoBatchRequest.Client
import ru.auto.dealer_pony.proto.ApiModel.{
  TeleponyInfoBatchRequest,
  TeleponyInfoBatchResponse,
  TeleponyInfoRequest,
  TeleponyInfoResponse
}
import ru.auto.dealer_pony.proto.ApiModel.TeleponyInfoRequest.OfferData
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.dealer_pony.DealerPonyClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting.ExternalRedirectsService.PhoneRedirection
import ru.yandex.vertis.feedprocessor.services.calltracking.CalltrackingClient
import ru.yandex.vertis.feedprocessor.services.telepony.TeleponyClient
import ru.yandex.vertis.feedprocessor.services.telepony.TeleponyClient.{
  CreateRequest,
  Domains,
  NoAvailableRedirectPhones,
  PhoneTypes,
  RedirectOptions,
  Redirect => TeleponyRedirect
}
import ru.yandex.vertis.mockito.MockitoSupport

import java.time.{Instant, OffsetDateTime, ZoneId}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting.ExternalRedirectsServiceSpec._
import ru.yandex.vertis.feedprocessor.services.telepony.TeleponyClient.PhoneTypes.PhoneType

import scala.concurrent.Future
import scala.concurrent.duration._

class ExternalRedirectsServiceSpec extends WordSpecBase with ScalaFutures with MockitoSupport {
  implicit val s: Scheduler = ActorSystem("test-system").scheduler

  private val calltracking = mock[CalltrackingClient]
  private val dealerpony = mock[DealerPonyClient]
  private val telepony = mock[TeleponyClient]

  val service = new ExternalRedirectsServiceImpl(
    calltracking,
    dealerpony,
    telepony,
    ExternalRedirectsConfig(Set(ClassifiedName.AVITO))
  )

  "ExternalRedirectsService" should {
    "do nothing if setting disabled" in {
      val clientId = 20101L
      val settingsReq = GetSettingsRequest.newBuilder().setClientId(clientId).build()
      when(calltracking.getSettings(eq(settingsReq))(?)).thenReturn(
        Future.successful(
          SettingsResponse
            .newBuilder()
            .setSettings(Settings.newBuilder().setCalltrackingClassifiedsEnabled(false))
            .build()
        )
      )

      val result = service.redirectsForOffers(clientId, ClassifiedName.AUTORU, Nil).futureValue

      verify(calltracking).getSettings(eq(settingsReq))(?)
      verifyNoMoreInteractions(calltracking, dealerpony, telepony)
      clearInvocations(calltracking, dealerpony, telepony)

      assert(result.isEmpty)
    }

    "fetch redirects" in {
      val clientId = 20101L
      val settingsReq = GetSettingsRequest.newBuilder().setClientId(clientId).build()
      when(calltracking.getSettings(eq(settingsReq))(?)).thenReturn(
        Future.successful(
          SettingsResponse
            .newBuilder()
            .setSettings(Settings.newBuilder().setCalltrackingClassifiedsEnabled(true))
            .build()
        )
      )

      val req1 =
        TeleponyInfoRequest
          .newBuilder()
          .setPlatform("autoru")
          .setOfferData(OfferData.newBuilder().setCategory(Category.CARS).setSection(Section.NEW))
      val req2 =
        TeleponyInfoRequest
          .newBuilder()
          .setPlatform("autoru")
          .setOfferData(OfferData.newBuilder().setCategory(Category.CARS).setSection(Section.USED))
      val teleponyReq = TeleponyInfoBatchRequest
        .newBuilder()
        .setClient(Client.newBuilder().setClientId(clientId))
        .addRequests(req1)
        .addRequests(req2)
        .build()
      when(dealerpony.getTeleponyInfoBatch(eq(teleponyReq))(?)).thenReturn(
        Future.successful(
          TeleponyInfoBatchResponse
            .newBuilder()
            .addResponses(
              TeleponyInfoResponse
                .newBuilder()
                .setRequest(req1)
                .setResponse(
                  TeleponyInfo
                    .newBuilder()
                    .setObjectId(s"dealer-$clientId")
                    .setTag("category=CARS#section=NEW#platform=autoru")
                )
            )
            .addResponses(
              TeleponyInfoResponse
                .newBuilder()
                .setRequest(req2)
                .setResponse(
                  TeleponyInfo
                    .newBuilder()
                    .setObjectId(s"dealer-$clientId")
                    .setTag("category=CARS#section=USED#platform=autoru")
                )
            )
            .build()
        )
      )

      val phone1 = "+79991112233"
      val redirectPhone1 = "+71111111111"
      val createReq1 = CreateRequest(
        target = TeleponyClient.formatPhone(phone1),
        phoneType = None,
        geoId = None,
        ttl = Some(30.days.toSeconds.toInt),
        antifraud = Some("enable"),
        tag = Some("category=CARS#section=NEW#platform=autoru"),
        options = RedirectOptions(None),
        voxUsername = None
      ).fold(throw _, identity)
      val redirect1 = TeleponyRedirect(
        id = "",
        objectId = s"dealer-$clientId",
        OffsetDateTime.MIN,
        source = redirectPhone1,
        target = phone1,
        options = None,
        deadline = None,
        tag = None
      )
      when(telepony.getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq1)))
        .thenReturn(Future.successful(redirect1))
      val phone2 = "+78881112233"
      val redirectPhone2 = "+72221111111"
      val createReq2 = CreateRequest(
        target = TeleponyClient.formatPhone(phone2),
        phoneType = None,
        geoId = None,
        ttl = Some(30.days.toSeconds.toInt),
        antifraud = Some("enable"),
        tag = Some("category=CARS#section=USED#platform=autoru"),
        options = RedirectOptions(None),
        voxUsername = None
      ).fold(throw _, identity)
      val redirect2 = TeleponyRedirect(
        id = "",
        objectId = s"dealer-$clientId",
        OffsetDateTime.MIN,
        source = redirectPhone2,
        target = phone1,
        options = None,
        deadline = None,
        tag = None
      )
      when(telepony.getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq2)))
        .thenReturn(Future.successful(redirect2))

      val offer1 = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone(phone1)))
        .build()
      val offer2 = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone(phone2)))
        .build()
      val offers = List(offer1, offer2)

      val result = service.redirectsForOffers(clientId, ClassifiedName.AUTORU, offers).futureValue

      assert(
        result.contains(
          Map(
            offer1 -> List(PhoneRedirection(phone = redirect1.source, originalPhone = redirect1.target)),
            offer2 -> List(PhoneRedirection(phone = redirect2.source, originalPhone = redirect2.target))
          )
        )
      )

      verify(calltracking).getSettings(eq(settingsReq))(?)
      verify(dealerpony).getTeleponyInfoBatch(eq(teleponyReq))(?)
      verify(telepony).getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq1))
      verify(telepony).getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq2))
      verifyNoMoreInteractions(calltracking, dealerpony, telepony)
      clearInvocations(calltracking, dealerpony, telepony)
    }

    "fetch PhoneType.Local redirects for Avito" in {
      val clientId = 20101L
      val settingsReq = GetSettingsRequest.newBuilder().setClientId(clientId).build()
      when(calltracking.getSettings(eq(settingsReq))(?)).thenReturn(
        Future.successful(
          SettingsResponse
            .newBuilder()
            .setSettings(Settings.newBuilder().setCalltrackingClassifiedsEnabled(true))
            .build()
        )
      )

      val req1 =
        TeleponyInfoRequest
          .newBuilder()
          .setPlatform("avito")
          .setOfferData(OfferData.newBuilder().setCategory(Category.CARS).setSection(Section.NEW))
      val req2 =
        TeleponyInfoRequest
          .newBuilder()
          .setPlatform("avito")
          .setOfferData(OfferData.newBuilder().setCategory(Category.CARS).setSection(Section.USED))
      val teleponyReq = TeleponyInfoBatchRequest
        .newBuilder()
        .setClient(Client.newBuilder().setClientId(clientId))
        .addRequests(req1)
        .addRequests(req2)
        .build()
      when(dealerpony.getTeleponyInfoBatch(eq(teleponyReq))(?)).thenReturn(
        Future.successful(
          TeleponyInfoBatchResponse
            .newBuilder()
            .addResponses(
              TeleponyInfoResponse
                .newBuilder()
                .setRequest(req1)
                .setResponse(
                  TeleponyInfo
                    .newBuilder()
                    .setObjectId(s"dealer-$clientId")
                    .setTag("category=CARS#section=NEW#platform=AVITO")
                )
            )
            .addResponses(
              TeleponyInfoResponse
                .newBuilder()
                .setRequest(req2)
                .setResponse(
                  TeleponyInfo
                    .newBuilder()
                    .setObjectId(s"dealer-$clientId")
                    .setTag("category=CARS#section=USED#platform=AVITO")
                )
            )
            .build()
        )
      )

      val phone1 = "+79991112233"
      val redirectPhone1 = "+71111111111"
      val createReq1 = CreateRequest(
        target = TeleponyClient.formatPhone(phone1),
        phoneType = Some(PhoneTypes.Local),
        geoId = None,
        ttl = Some(30.days.toSeconds.toInt),
        antifraud = Some("enable"),
        tag = Some("category=CARS#section=NEW#platform=AVITO"),
        options = RedirectOptions(None),
        voxUsername = None
      ).fold(throw _, identity)
      val redirect1 = TeleponyRedirect(
        id = "",
        objectId = s"dealer-$clientId",
        OffsetDateTime.MIN,
        source = redirectPhone1,
        target = phone1,
        options = None,
        deadline = Some(OffsetDateTime.ofInstant(Instant.EPOCH.plusSeconds(10), ZoneId.of("Z"))),
        tag = None
      )
      when(telepony.getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq1)))
        .thenReturn(Future.successful(redirect1))
      val phone2 = "+78881112233"
      val redirectPhone2 = "+72221111111"
      val createReq2 = CreateRequest(
        target = TeleponyClient.formatPhone(phone2),
        phoneType = Some(PhoneTypes.Local),
        geoId = None,
        ttl = Some(30.days.toSeconds.toInt),
        antifraud = Some("enable"),
        tag = Some("category=CARS#section=USED#platform=AVITO"),
        options = RedirectOptions(None),
        voxUsername = None
      ).fold(throw _, identity)
      val redirect2 = TeleponyRedirect(
        id = "",
        objectId = s"dealer-$clientId",
        OffsetDateTime.MIN,
        source = redirectPhone2,
        target = phone2,
        options = None,
        deadline = Some(OffsetDateTime.ofInstant(Instant.EPOCH.plusSeconds(10), ZoneId.of("Z"))),
        tag = None
      )
      when(telepony.getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq2)))
        .thenReturn(Future.successful(redirect2))

      val getOrCreateReq1 = GetOrCreateRedirectConfirmationRequest
        .newBuilder()
        .setClientId(clientId)
        .setDeadline(Timestamps.fromSeconds(10))
        .setPlatform("avito")
        .setRedirectPhone(redirect1.source)
        .build()
      val getOrCreateReq2 = GetOrCreateRedirectConfirmationRequest
        .newBuilder()
        .setClientId(clientId)
        .setDeadline(Timestamps.fromSeconds(10))
        .setPlatform("avito")
        .setRedirectPhone(redirect2.source)
        .build()
      when(calltracking.getOrCreateRedirectConfirmation(eq(getOrCreateReq1))(?))
        .thenReturn(Future.successful(GetOrCreateRedirectConfirmationResponse.newBuilder().setConfirmed(true).build()))
      when(calltracking.getOrCreateRedirectConfirmation(eq(getOrCreateReq2))(?))
        .thenReturn(Future.successful(GetOrCreateRedirectConfirmationResponse.newBuilder().setConfirmed(true).build()))

      val offer1 = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone(phone1)))
        .build()
      val offer2 = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone(phone2)))
        .build()
      val offers = List(offer1, offer2)

      val result = service.redirectsForOffers(clientId, ClassifiedName.AVITO, offers).futureValue

      assert(
        result.contains(
          Map(
            offer1 -> List(PhoneRedirection(phone = redirect1.source, originalPhone = redirect1.target)),
            offer2 -> List(PhoneRedirection(phone = redirect2.source, originalPhone = redirect2.target))
          )
        )
      )

      verify(calltracking).getSettings(eq(settingsReq))(?)
      verify(calltracking).getOrCreateRedirectConfirmation(eq(getOrCreateReq1))(?)
      verify(calltracking).getOrCreateRedirectConfirmation(eq(getOrCreateReq2))(?)
      verify(dealerpony).getTeleponyInfoBatch(eq(teleponyReq))(?)
      verify(telepony).getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq1))
      verify(telepony).getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq2))
      verifyNoMoreInteractions(calltracking, dealerpony, telepony)
      clearInvocations(calltracking, dealerpony, telepony)
    }

    "drop unconfirmed redirects" in {
      val clientId = 20101L
      val settingsReq = GetSettingsRequest.newBuilder().setClientId(clientId).build()
      when(calltracking.getSettings(eq(settingsReq))(?)).thenReturn(
        Future.successful(
          SettingsResponse
            .newBuilder()
            .setSettings(Settings.newBuilder().setCalltrackingClassifiedsEnabled(true))
            .build()
        )
      )

      val req1 =
        TeleponyInfoRequest
          .newBuilder()
          .setPlatform("avito")
          .setOfferData(OfferData.newBuilder().setCategory(Category.CARS).setSection(Section.NEW))
      val req2 =
        TeleponyInfoRequest
          .newBuilder()
          .setPlatform("avito")
          .setOfferData(OfferData.newBuilder().setCategory(Category.CARS).setSection(Section.USED))
      val teleponyReq = TeleponyInfoBatchRequest
        .newBuilder()
        .setClient(Client.newBuilder().setClientId(clientId))
        .addRequests(req1)
        .addRequests(req2)
        .build()
      when(dealerpony.getTeleponyInfoBatch(eq(teleponyReq))(?)).thenReturn(
        Future.successful(
          TeleponyInfoBatchResponse
            .newBuilder()
            .addResponses(
              TeleponyInfoResponse
                .newBuilder()
                .setRequest(req1)
                .setResponse(
                  TeleponyInfo
                    .newBuilder()
                    .setObjectId(s"dealer-$clientId")
                    .setTag("category=CARS#section=NEW#platform=avito")
                )
            )
            .addResponses(
              TeleponyInfoResponse
                .newBuilder()
                .setRequest(req2)
                .setResponse(
                  TeleponyInfo
                    .newBuilder()
                    .setObjectId(s"dealer-$clientId")
                    .setTag("category=CARS#section=USED#platform=avito")
                )
            )
            .build()
        )
      )

      val phone1 = "+79991112233"
      val redirectPhone1 = "+71111111111"
      val createReq1 = CreateRequest(
        target = TeleponyClient.formatPhone(phone1),
        phoneType = Some(PhoneTypes.Local),
        geoId = None,
        ttl = Some(30.days.toSeconds.toInt),
        antifraud = Some("enable"),
        tag = Some("category=CARS#section=NEW#platform=avito"),
        options = RedirectOptions(None),
        voxUsername = None
      ).fold(throw _, identity)
      val redirect1 = TeleponyRedirect(
        id = "",
        objectId = s"dealer-$clientId",
        OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("Z")),
        source = redirectPhone1,
        target = phone1,
        options = None,
        deadline = Some(OffsetDateTime.ofInstant(Instant.EPOCH.plusSeconds(10), ZoneId.of("Z"))),
        tag = None
      )
      when(telepony.getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq1)))
        .thenReturn(Future.successful(redirect1))
      val phone2 = "+78881112233"
      val redirectPhone2 = "+72221111111"
      val createReq2 = CreateRequest(
        target = TeleponyClient.formatPhone(phone2),
        phoneType = Some(PhoneTypes.Local),
        geoId = None,
        ttl = Some(30.days.toSeconds.toInt),
        antifraud = Some("enable"),
        tag = Some("category=CARS#section=USED#platform=avito"),
        options = RedirectOptions(None),
        voxUsername = None
      ).fold(throw _, identity)
      val redirect2 = TeleponyRedirect(
        id = "",
        objectId = s"dealer-$clientId",
        OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("Z")),
        source = redirectPhone2,
        target = phone2,
        options = None,
        deadline = Some(OffsetDateTime.ofInstant(Instant.EPOCH.plusSeconds(10), ZoneId.of("Z"))),
        tag = None
      )
      when(telepony.getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq2)))
        .thenReturn(Future.successful(redirect2))

      val getOrCreateReq1 = GetOrCreateRedirectConfirmationRequest
        .newBuilder()
        .setClientId(clientId)
        .setDeadline(Timestamps.fromSeconds(10))
        .setPlatform("avito")
        .setRedirectPhone(redirect1.source)
        .build()
      val getOrCreateReq2 = GetOrCreateRedirectConfirmationRequest
        .newBuilder()
        .setClientId(clientId)
        .setDeadline(Timestamps.fromSeconds(10))
        .setPlatform("avito")
        .setRedirectPhone(redirect2.source)
        .build()
      when(calltracking.getOrCreateRedirectConfirmation(eq(getOrCreateReq1))(?))
        .thenReturn(Future.successful(GetOrCreateRedirectConfirmationResponse.newBuilder().setConfirmed(true).build()))
      when(calltracking.getOrCreateRedirectConfirmation(eq(getOrCreateReq2))(?))
        .thenReturn(Future.successful(GetOrCreateRedirectConfirmationResponse.newBuilder().setConfirmed(false).build()))

      val offer1 = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone(phone1)))
        .build()
      val offer2 = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .setSeller(Seller.newBuilder().addPhones(Phone.newBuilder().setPhone(phone2)))
        .build()
      val offers = List(offer1, offer2)

      val result = service.redirectsForOffers(clientId, ClassifiedName.AVITO, offers).futureValue

      assert(
        result.contains(
          Map(
            offer1 -> List(PhoneRedirection(phone = redirect1.source, originalPhone = redirect1.target)),
            offer2 -> List(PhoneRedirection(phone = phone2, originalPhone = phone2))
          )
        )
      )

      verify(calltracking).getSettings(eq(settingsReq))(?)
      verify(dealerpony).getTeleponyInfoBatch(eq(teleponyReq))(?)
      verify(telepony).getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq1))
      verify(telepony).getOrCreate(eq(Domains.AutoDealers), eq(s"dealer-$clientId"), eq(createReq2))
      verify(calltracking).getOrCreateRedirectConfirmation(eq(getOrCreateReq1))(?)
      verify(calltracking).getOrCreateRedirectConfirmation(eq(getOrCreateReq2))(?)
      verifyNoMoreInteractions(calltracking, dealerpony, telepony)
      clearInvocations(calltracking, dealerpony, telepony)
    }
  }

  "ExternalRedirectsService.callTelepony" should {
    "recover on NoAvailableRedirectPhones with some PhoneType in request" in {
      val TestDomain = Domains.AutoDealers
      val TestObjectId = "dealer-123"
      val TestPhone = "+70000000000"

      val CreateRequestWithPhoneType = tpRequest(TestPhone, phoneType = Some(PhoneTypes.Local))
      when(telepony.getOrCreate(TestDomain, TestObjectId, CreateRequestWithPhoneType)).thenReturn {
        Future.failed(new NoAvailableRedirectPhones)
      }

      val CreateRequestWithoutPhoneType = tpRequest(TestPhone, phoneType = None)
      val redirect = tpRedirect(TestObjectId, TestPhone, "+79999999999")
      when(telepony.getOrCreate(TestDomain, TestObjectId, CreateRequestWithoutPhoneType)).thenReturn {
        Future.successful(redirect)
      }

      val result = service.callTelepony(CreateRequestWithPhoneType, TestDomain, TestObjectId).futureValue
      assert(result.contains(redirect))

      verify(telepony).getOrCreate(TestDomain, TestObjectId, CreateRequestWithPhoneType)
      verify(telepony).getOrCreate(TestDomain, TestObjectId, CreateRequestWithoutPhoneType)
      verifyNoMoreInteractions(telepony)
      clearInvocations(telepony)
    }

    "recover request without PhoneType" in {
      val TestDomain = Domains.AutoDealers
      val TestObjectId = "dealer-123"
      val TestPhone = "+70000000000"

      val CreateRequestWithPhoneType = tpRequest(TestPhone, phoneType = Some(PhoneTypes.Local))
      when(telepony.getOrCreate(TestDomain, TestObjectId, CreateRequestWithPhoneType)).thenReturn {
        Future.failed(new NoAvailableRedirectPhones)
      }

      val CreateRequestWithoutPhoneType = tpRequest(TestPhone, phoneType = None)
      when(telepony.getOrCreate(TestDomain, TestObjectId, CreateRequestWithoutPhoneType)).thenReturn {
        Future.failed(new NoAvailableRedirectPhones)
      }

      val result = service.callTelepony(CreateRequestWithPhoneType, TestDomain, TestObjectId).futureValue
      assert(result.isEmpty)

      verify(telepony).getOrCreate(TestDomain, TestObjectId, CreateRequestWithPhoneType)
      verify(telepony).getOrCreate(TestDomain, TestObjectId, CreateRequestWithoutPhoneType)
      verifyNoMoreInteractions(telepony)
      clearInvocations(telepony)
    }

    "do only one request if has redirect phones with needed PhoneType" in {
      val TestDomain = Domains.AutoDealers
      val TestObjectId = "dealer-123"
      val TestPhone = "+70000000000"

      val CreateRequestWithPhoneType = tpRequest(TestPhone, phoneType = Some(PhoneTypes.Local))
      val redirect = tpRedirect(TestObjectId, TestPhone, "+79999999999")
      when(telepony.getOrCreate(TestDomain, TestObjectId, CreateRequestWithPhoneType)).thenReturn {
        Future.successful(redirect)
      }

      val result = service.callTelepony(CreateRequestWithPhoneType, TestDomain, TestObjectId).futureValue
      assert(result.contains(redirect))

      verify(telepony).getOrCreate(TestDomain, TestObjectId, CreateRequestWithPhoneType)
      verifyNoMoreInteractions(telepony)
      clearInvocations(telepony)
    }
  }
}

object ExternalRedirectsServiceSpec {

  def tpRequest(target: String, phoneType: Option[PhoneType]): CreateRequest = {
    CreateRequest(
      target = target,
      phoneType = phoneType,
      geoId = None,
      ttl = None,
      antifraud = None,
      tag = None,
      options = RedirectOptions(None),
      voxUsername = None
    ).fold(throw _, identity)
  }

  def tpRedirect(objectId: String, target: String, source: String): TeleponyRedirect = {
    TeleponyRedirect(
      id = s"Redirect-$target",
      objectId = objectId,
      createTime = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("Z")),
      source = source,
      target = target,
      options = None,
      deadline = None,
      tag = None
    )
  }

}
