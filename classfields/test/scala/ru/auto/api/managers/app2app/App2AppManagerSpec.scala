package ru.auto.api.managers.app2app

import org.mockito.ArgumentMatchers.{eq => eqq}
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.app2app.App2AppPayloadFields.{Line1, RedirectId}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.managers.offers.OfferLoader
import ru.auto.api.model.ModelGenerators.{ReadableStringGen, SessionResultGen}
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.model.gen.BasicGenerators.readableString
import ru.auto.api.model.{AutoruUser, CategorySelector, ModelGenerators, RequestParams}
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.util.{RequestImpl, UrlBuilder}
import ru.yandex.passport.model.api.ApiModel.VoxBatchResponse
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.auto.api.model.ModelUtils.RichOfferOrBuilder

import scala.jdk.CollectionConverters._
import scala.util.Success

class App2AppManagerSpec extends BaseSpec with ScalaCheckPropertyChecks with MockitoSupport with OptionValues {

  abstract class Fixture(needRegistered: Boolean = false, withAlias: Boolean = true) {
    val passportClient = mock[PassportClient]
    val offerLoader = mock[OfferLoader]
    val enrichManager = mock[EnrichManager]
    val settingsClient = mock[SettingsClient]
    val featureManager = mock[FeatureManager]
    val urlBuilder = mock[UrlBuilder]
    val app2appHandleCrypto = mock[App2AppHandleCrypto]

    val app2AppManager = new App2AppManager(
      passportClient,
      offerLoader,
      enrichManager,
      settingsClient,
      featureManager,
      urlBuilder,
      app2appHandleCrypto
    )

    implicit val request = {
      val trace: Traced = Traced.empty
      val r = new RequestImpl
      r.setTrace(trace)
      r.setRequestParams(RequestParams.construct("1.1.1.1"))
      r.setApplication(Application.iosApp)

      if (needRegistered) {
        val sessionResult = SessionResultGen.next.toBuilder
        if (!withAlias) sessionResult.getUserBuilder.getProfileBuilder.clearAlias()
        else sessionResult.getUserBuilder.getProfileBuilder.setAlias(readableString.next)
        r.setSession(sessionResult.build())
        r.setUser(AutoruUser(r.user.session.get.getSession.getUserId.toLong))
      }
      r
    }
  }

  "App2AppManager" should {
    "correctly pass userId to encrypt" in new Fixture() {
      val voxUsername = "vox" + BasicGenerators.readableString.next
      val userId = ModelGenerators.PrivateUserRefGen.next
      val handle = BasicGenerators.readableString.next
      val category = CategorySelector.Cars
      val offerId = ModelGenerators.OfferIDGen.next
      when(passportClient.voxBatchRequest(?)(?))
        .thenReturnF(
          VoxBatchResponse
            .newBuilder()
            .putUserIdByVoxUsername(voxUsername, userId.uid.toString)
            .putVoxUsernameByUserId(userId.uid.toString, voxUsername)
            .build()
        )
      when(app2appHandleCrypto.encrypt(eqq(userId), ?, eqq(offerId))).thenReturn(handle)
      val response = app2AppManager.getHandlesByVoxUsernames(Seq(voxUsername), category, offerId).futureValue
      response.size shouldBe 1
      response.get(voxUsername).value shouldBe handle
    }

    "skip offer with App2AppCallsDisabled=true" in new Fixture(needRegistered = true) {
      val calleeHandle = "handleCallee" + BasicGenerators.readableString.next
      val calleeRef = ModelGenerators.PrivateUserRefGen.next
      val callee = ModelGenerators.UserEssentialsGen.next
      val calleeVoxName = "voxCallee" + BasicGenerators.readableString.next
      val offerId = ModelGenerators.OfferIDGen.next
      val category = CategorySelector.Cars
      val offer = ModelGenerators.offerGen(category, offerId.toPlain, Gen.const(calleeRef)).next.updated { b =>
        b.setStatus(OfferStatus.ACTIVE)
        b.getAdditionalInfoBuilder.setApp2AppCallsDisabled(true)
      }
      val voxCheckFeatureTrue: Feature[Boolean] = mock[Feature[Boolean]]
      val callerRef: AutoruUser = request.user.ref.asPrivate
      when(app2appHandleCrypto.decrypt(eqq(calleeHandle))).thenReturn(Success((calleeRef, category, offerId)))
      when(passportClient.getUserEssentials(eqq(calleeRef), eqq(false))(?)).thenReturnF(callee)
      when(voxCheckFeatureTrue.value).thenReturn(true)
      when(featureManager.voxCheck).thenReturn(voxCheckFeatureTrue)
      when(settingsClient.getSettings(eqq(SettingsClient.SettingsDomain), eqq(callerRef))(?))
        .thenReturnF(Map("vox_username" -> calleeVoxName))
      when(offerLoader.findRawOffer(eqq(category), eqq(offerId), eqq(true), ?)(?)).thenReturnF(offer)
      val callInfo = app2AppManager.getCallInfo(calleeHandle).futureValue
      callInfo.getApp2AppCallAvailable shouldBe false
    }

    "call unavailable: caller is callee" in new Fixture(needRegistered = true) {
      val calleeHandle = "handleCallee" + BasicGenerators.readableString.next
      val callerHandle = "handleCaller" + BasicGenerators.readableString.next
      val calleeRef = request.user.ref.asPrivate
      val offerId = ModelGenerators.OfferIDGen.next
      val category = CategorySelector.Cars
      val callee = ModelGenerators.UserEssentialsGen.next
      val calleeVoxName = "voxCallee" + BasicGenerators.readableString.next
      val offer = ModelGenerators.offerGen(category, offerId.toPlain, Gen.const(calleeRef)).next.updated {
        _.setStatus(OfferStatus.ACTIVE)
      }
      val offerLink = "offerLink" + BasicGenerators.readableString
      val callerRef: AutoruUser = request.user.ref.asPrivate
      when(app2appHandleCrypto.decrypt(eqq(calleeHandle))).thenReturn(Success((calleeRef, category, offerId)))
      when(passportClient.getUserEssentials(eqq(calleeRef), eqq(false))(?)).thenReturnF(callee)
      val voxCheckFeatureTrue: Feature[Boolean] = mock[Feature[Boolean]]
      when(voxCheckFeatureTrue.value).thenReturn(true)
      when(featureManager.voxCheck).thenReturn(voxCheckFeatureTrue)
      when(settingsClient.getSettings(eqq(SettingsClient.SettingsDomain), eqq(callerRef))(?))
        .thenReturnF(Map("vox_username" -> calleeVoxName))
      when(offerLoader.findRawOffer(eqq(category), eqq(offerId), eqq(true), ?)(?)).thenReturnF(offer)
      when(app2appHandleCrypto.encrypt(eqq(callerRef), eqq(category), eqq(offerId))).thenReturn(callerHandle)
      val callInfo = app2AppManager.getCallInfo(calleeHandle).futureValue
      withClue(callInfo) {
        callInfo.getApp2AppCallAvailable shouldBe false
        callInfo.getCallAvailable shouldBe false
        callInfo.getCallUnavailableReason shouldBe "Звонок самому себе не поддерживается"
      }
    }

    "call unavailable: no redirectId" in new Fixture(needRegistered = true) {
      val calleeHandle = "handleCallee" + BasicGenerators.readableString.next
      val callerHandle = "handleCaller" + BasicGenerators.readableString.next
      val calleeRef = ModelGenerators.PrivateUserRefGen.next
      val offerId = ModelGenerators.OfferIDGen.next
      val category = CategorySelector.Cars
      val callee = ModelGenerators.UserEssentialsGen.next
      val calleeVoxName = "voxCallee" + BasicGenerators.readableString.next
      val offer = ModelGenerators.offerGen(category, offerId.toPlain, Gen.const(calleeRef)).next.updated {
        _.setStatus(OfferStatus.ACTIVE)
      }
      val enriched: ApiOfferModel.Offer = offer
      val offerLink = "offerLink" + BasicGenerators.readableString
      val callerRef: AutoruUser = request.user.ref.asPrivate
      when(app2appHandleCrypto.decrypt(eqq(calleeHandle))).thenReturn(Success((calleeRef, category, offerId)))
      when(passportClient.getUserEssentials(eqq(calleeRef), eqq(false))(?)).thenReturnF(callee)
      val voxCheckFeatureTrue: Feature[Boolean] = mock[Feature[Boolean]]
      when(voxCheckFeatureTrue.value).thenReturn(true)
      when(featureManager.voxCheck).thenReturn(voxCheckFeatureTrue)
      when(settingsClient.getSettings(eqq(SettingsClient.SettingsDomain), eqq(callerRef))(?))
        .thenReturnF(Map("vox_username" -> calleeVoxName))
      when(offerLoader.findRawOffer(eqq(category), eqq(offerId), eqq(true), ?)(?)).thenReturnF(offer)
      when(
        enrichManager.enrich(
          eqq(offer),
          eqq(
            EnrichOptions(
              phoneRedirects = true,
              techParams = true,
              userPic = true,
              required = Set(ApiOfferModel.EnrichFailedFlag.PHONE_REDIRECTS)
            )
          )
        )(?)
      ).thenReturnF(enriched)
      when(urlBuilder.offerUrl(eqq(enriched), eqq(false), eqq(true))).thenReturn(offerLink)
      when(app2appHandleCrypto.encrypt(eqq(callerRef), eqq(category), eqq(offerId))).thenReturn(callerHandle)
      val callInfo = app2AppManager.getCallInfo(calleeHandle).futureValue
      withClue(callInfo) {
        callInfo.getApp2AppCallAvailable shouldBe false
        callInfo.getCallAvailable shouldBe false
        callInfo.getCallUnavailableReason shouldBe "В объявлении не включен подменный номер"
      }
    }

    "get markInfo.name and modelInfo.name" in new Fixture(needRegistered = true) {
      val calleeHandle = "handleCallee" + BasicGenerators.readableString.next
      val callerHandle = "handleCaller" + BasicGenerators.readableString.next
      val calleeRef = ModelGenerators.PrivateUserRefGen.next
      val offerId = ModelGenerators.OfferIDGen.next
      val category = CategorySelector.Cars
      val callee = ModelGenerators.UserEssentialsGen.next
      val calleeVoxName = "voxCallee" + BasicGenerators.readableString.next
      val offer = ModelGenerators.offerGen(category, offerId.toPlain, Gen.const(calleeRef)).next.updated {
        _.setStatus(OfferStatus.ACTIVE)
      }
      val markName = "markName" + BasicGenerators.readableString.next
      val modelName = "modelName" + BasicGenerators.readableString.next
      val superGenName = "generationName" + BasicGenerators.readableString.next
      val redirectId = ReadableStringGen.next
      val enriched: ApiOfferModel.Offer = offer.updated { builder =>
        builder.getCarInfoBuilder.getMarkInfoBuilder.setName(markName)
        builder.getCarInfoBuilder.getModelInfoBuilder.setName(modelName)
        builder.getCarInfoBuilder.getSuperGenBuilder.setName(superGenName)
        builder.getSellerBuilder.getPhonesBuilderList.asScala.foreach { phoneBuilder =>
          phoneBuilder.putApp2AppPayload(RedirectId, redirectId)
        }
      }
      val offerLink = "offerLink" + BasicGenerators.readableString
      val callerRef: AutoruUser = request.user.ref.asPrivate
      when(app2appHandleCrypto.decrypt(eqq(calleeHandle))).thenReturn(Success((calleeRef, category, offerId)))
      when(passportClient.getUserEssentials(eqq(calleeRef), eqq(false))(?)).thenReturnF(callee)
      val voxCheckFeatureTrue: Feature[Boolean] = mock[Feature[Boolean]]
      when(voxCheckFeatureTrue.value).thenReturn(true)
      when(featureManager.voxCheck).thenReturn(voxCheckFeatureTrue)
      when(settingsClient.getSettings(eqq(SettingsClient.SettingsDomain), eqq(callerRef))(?))
        .thenReturnF(Map("vox_username" -> calleeVoxName))
      when(offerLoader.findRawOffer(eqq(category), eqq(offerId), eqq(true), ?)(?)).thenReturnF(offer)
      when(
        enrichManager.enrich(
          eqq(offer),
          eqq(
            EnrichOptions(
              phoneRedirects = true,
              techParams = true,
              userPic = true,
              required = Set(ApiOfferModel.EnrichFailedFlag.PHONE_REDIRECTS)
            )
          )
        )(?)
      ).thenReturnF(enriched)
      when(urlBuilder.offerUrl(eqq(enriched), eqq(false), eqq(true))).thenReturn(offerLink)
      when(app2appHandleCrypto.encrypt(eqq(callerRef), eqq(category), eqq(offerId))).thenReturn(callerHandle)
      val callInfo = app2AppManager.getCallInfo(calleeHandle).futureValue
      withClue(callInfo) {
        callInfo.getApp2AppPayloadMap.asScala.get(Line1).value shouldBe s"$markName $modelName $superGenName"
      }
    }
  }
}
