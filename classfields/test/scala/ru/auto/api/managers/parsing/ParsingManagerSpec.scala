package ru.auto.api.managers.parsing

import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito._
import org.mockito.internal.verification.Times
import org.scalacheck.Gen
import org.scalatest.exceptions.TestFailedException
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ResponseModel._
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.{ParsedOfferForDealerException, ParsingLoginOrRegisterException}
import ru.auto.api.managers.offers.DraftsManager
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils.RichOfferOrBuilder
import ru.auto.api.model.{CategorySelector, RequestParams}
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.parsing.ParsingClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{RequestImpl, Resources}
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.yandex.passport.model.api.ApiModel.{InternalLoginOrRegisterParameters, UserEssentials}
import ru.yandex.vertis.generators.BasicGenerators.readableString
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by andrey on 11/30/17.
  */
class ParsingManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  trait Fixture {
    val parsingClient: ParsingClient = mock[ParsingClient]

    val draftsManager = mock[DraftsManager]
    val passportManager = mock[PassportManager]

    val draftHandleCrypto = mock[DraftHandleCrypto]

    val vosClient = mock[VosClient]

    val parsingManager = new ParsingManager(draftsManager, parsingClient, passportManager, draftHandleCrypto, vosClient)

    val remoteUrl = "https://www.avito.ru/miass/avtomobili/toyota_land_cruiser_prado_2013_1767143438"
    val remoteId = "avito|cars|1767143438"
    val hash = readableString.next
  }

  trait FailingDraftHandleFixture extends Fixture {
    when(draftHandleCrypto.decrypt(?)).thenReturn(Failure(new RuntimeException("failed to decrypt")))
  }

  trait SuccessfullDraftHandleFixture extends Fixture {
    val category = StrictCategoryGen.next
    val draftId = OfferIDGen.next
    val user = PrivateUserRefGen.next
    val phone = PhoneGen.next
    when(draftHandleCrypto.decrypt(?)).thenReturn(Success(DraftHandle(user, phone, category, draftId)))
  }

  abstract class SuccessfullDraftHandleWithSpecificCategoryFixture(category: CategorySelector.StrictCategory)
    extends Fixture {
    val draftId = OfferIDGen.next
    val user = PrivateUserRefGen.next
    val phone = PhoneGen.next
    when(draftHandleCrypto.decrypt(?)).thenReturn(Success(DraftHandle(user, phone, category, draftId)))
  }

  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
    r.setUser(PrivateUserRefGen.next)
    r.setApplication(Application.iosApp)
    r.setToken(TokenServiceImpl.iosApp)
    r.setTrace(trace)
    r
  }

  "ParsingManager" should {
    "create draft successfully" when {
      "from draft by encrypted draft handle" in new SuccessfullDraftHandleFixture {
        val draft = specificCategoryOfferGen(Gen.const(category.`enum`), Gen.const(user)).next.updated { b =>
          b.setId(draftId.toPlain)
        }
        val draftResponse = DraftResponse.newBuilder().setOffer(draft).build()
        when(draftsManager.getDraft(?, ?, ?, ?)(?)).thenReturn(Future.successful(draftResponse))
        when(draftsManager.changeCurrent(?, ?)(?)).thenReturn(Future.unit)
        val result = parsingManager.createDraftFromParsedOffer(user, hash, None).futureValue
        result shouldBe draftResponse
        verify(draftsManager).getDraft(eeq(category), eeq(user), eeq(draftId), eeq(true))(?)
        verify(draftsManager).changeCurrent(eeq(category), eeq(draftId))(?)
      }

      "from parsed offer" in new FailingDraftHandleFixture {
        val parsedOffer = Resources.toProto[ApiOfferModel.Offer]("/parsing/parsed_offer.json")
        when(parsingClient.getOffer(?, ?, ?)(?)).thenReturnF(parsedOffer)
        when(draftsManager.draftCreateAsCurrent(?, ?, ?)(?)).thenReturnF(DraftResponse.getDefaultInstance)
        parsingManager.createDraftFromParsedOffer(request.user.registeredRef, hash, None).futureValue
        verify(parsingClient).getOffer(eeq(Some(hash)), eeq(None), eeq(None))(?)
        verify(draftsManager).draftCreateAsCurrent(
          eeq(CategorySelector.Trucks),
          eeq(request.user.privateRef),
          eeq(parsedOffer)
        )(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }

      "from parsed offer with custom phone" in new FailingDraftHandleFixture {
        val phone = PhoneGen.next
        val parsedOffer = Resources.toProto[ApiOfferModel.Offer]("/parsing/parsed_offer.json")
        when(parsingClient.getOffer(?, ?, ?)(?)).thenReturnF(parsedOffer)
        when(draftsManager.draftCreateAsCurrent(?, ?, ?)(?)).thenReturnF(DraftResponse.getDefaultInstance)
        parsingManager.createDraftFromParsedOffer(request.user.registeredRef, hash, Some(phone)).futureValue
        verify(parsingClient).getOffer(eeq(Some(hash)), eeq(None), eeq(Some(phone)))(?)
        verify(draftsManager).draftCreateAsCurrent(
          eeq(CategorySelector.Trucks),
          eeq(request.user.privateRef),
          eeq(parsedOffer)
        )(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }

      "from parsed offer by url" in new FailingDraftHandleFixture {
        val url = "https://m.avito.ru/penza/avtomobili/ssangyong_korando_1997_440422701"
        val parsedOffer = Resources.toProto[ApiOfferModel.Offer]("/parsing/parsed_offer.json")
        parsedOffer.getSeller.getPhonesCount shouldBe 1
        when(parsingClient.getOffer(?, ?, ?)(?)).thenReturnF(parsedOffer)
        when(draftsManager.draftCreateAsCurrent(?, ?, ?)(?)).thenReturnF(DraftResponse.getDefaultInstance)
        val response = parsingManager.createDraftFromParsedOfferByUrl(request.user.registeredRef, url).futureValue
        response.getOffer.getSeller.getPhonesCount shouldBe 0 // удаляем телефоны из драфта
        verify(parsingClient).getOffer(eeq(None), eeq(Some(url)), eeq(None))(?)
        val withoutPhones = {
          val b = parsedOffer.toBuilder
          b.getSellerBuilder.clearPhones()
          b.build()
        }
        verify(draftsManager).draftCreateAsCurrent(
          eeq(CategorySelector.Trucks),
          eeq(request.user.privateRef),
          eeq(withoutPhones)
        )(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }
    }

    "throw exception when trying to create draft" when {
      "dealer's draft" in new FailingDraftHandleFixture {
        val dealerRequest = {
          val r = new RequestImpl
          r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
          r.setUser(DealerUserRefGen.next)
          r.setApplication(Application.iosApp)
          r.setToken(TokenServiceImpl.iosApp)
          r.setTrace(trace)
          r
        }
        intercept[ParsedOfferForDealerException] {
          parsingManager.createDraftFromParsedOffer(dealerRequest.user.registeredRef, hash, None)(dealerRequest).await
        }
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }
    }

    "return offer info" when {
      for {
        category <- CategorySelector.categories
      } s"hash is encrypted draft hash (category = $category)" in new SuccessfullDraftHandleWithSpecificCategoryFixture(
        category
      ) {
        val markName = Gen.alphaStr.next
        val modelName = Gen.alphaStr.next
        val draft = specificCategoryOfferGen(Gen.const(category.`enum`), Gen.const(user)).next.updated { b =>
          b.setId(draftId.toPlain)
          b.getCategory match {
            case Category.CARS =>
              b.getCarInfoBuilder.getMarkInfoBuilder.setName(markName)
              b.getCarInfoBuilder.getModelInfoBuilder.setName(modelName)
            case Category.MOTO =>
              b.getMotoInfoBuilder.getMarkInfoBuilder.setName(markName)
              b.getMotoInfoBuilder.getModelInfoBuilder.setName(modelName)
            case Category.TRUCKS =>
              b.getTruckInfoBuilder.getMarkInfoBuilder.setName(markName)
              b.getTruckInfoBuilder.getModelInfoBuilder.setName(modelName)
            case x => sys.error(s"unexpected category $x")
          }
        }
        when(vosClient.getDraft(?, ?, ?, ?)(?)).thenReturn(Future.successful(draft))
        val result = parsingManager.getParsedOfferInfo(hash).futureValue
        result.getName shouldBe markName + " " + modelName
        result.getPhone shouldBe phone
        result.getCanPublish.getValue shouldBe true
        verify(vosClient).getDraft(eeq(category), eeq(user), eeq(draftId), eeq(false))(?)
      }

      "hash is from parsing" in new FailingDraftHandleFixture {
        val response = Resources.toProto[ParsedOfferInfoResponse]("/parsing/parsed_offer_info.json")
        when(parsingClient.getOfferInfo(?)(?)).thenReturnF(response)
        val res = parsingManager.getParsedOfferInfo(hash).futureValue
        verify(parsingClient).getOfferInfo(eeq(hash))(?)
        res shouldBe response
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }
    }

    "set not_published" in new FailingDraftHandleFixture {
      val reason: String = "noanswer"
      val response = SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()
      when(parsingClient.setRejectReason(?, ?)(?)).thenReturn(Future.unit)
      val res = parsingManager.setRejectReason(hash, reason).futureValue
      verify(parsingClient).setRejectReason(eeq(hash), eeq(reason))(?)
      res shouldBe response
      verifyNoMoreInteractions(parsingClient)
      verifyNoMoreInteractions(passportManager)
      verifyNoMoreInteractions(draftsManager)
    }

    "loginOrRegister successfully" when {
      "encrypted draft hash was passed, optPhone is empty, user essentials contain phone from hash" in new SuccessfullDraftHandleFixture {
        when(passportManager.getUserEssentials(?, ?)(?))
          .thenReturn(Future.successful(UserEssentials.newBuilder().addPhones(phone).build()))
        when(passportManager.loginOrRegisterInt(?)(?)).thenReturn(Future.successful(LoginResponse.getDefaultInstance))
        parsingManager.loginOrRegisterOwner(hash, None).futureValue
        verify(passportManager).getUserEssentials(eeq(user), eeq(false))(?)
        verify(passportManager).loginOrRegisterInt(argThat[InternalLoginOrRegisterParameters] { v =>
          v.getIdentity.getPhone == phone
        })(?)
      }

      "encrypted draft hash was passed, optPhone is not empty, user essentials contain optPhone" in new SuccessfullDraftHandleFixture {
        val optPhone = Some(PhoneGen.next)
        when(passportManager.getUserEssentials(?, ?)(?))
          .thenReturn(Future.successful(UserEssentials.newBuilder().addPhones(optPhone.get).build()))
        when(passportManager.loginOrRegisterInt(?)(?)).thenReturn(Future.successful(LoginResponse.getDefaultInstance))
        parsingManager.loginOrRegisterOwner(hash, optPhone).futureValue
        verify(passportManager).loginOrRegisterInt(argThat[InternalLoginOrRegisterParameters] { v =>
          v.getIdentity.getPhone == optPhone.get
        })(?)
      }

      "everything is ok" in new FailingDraftHandleFixture {
        val info = Resources.toProto[ParsedOfferInfoResponse]("/parsing/parsed_offer_info.json")
        when(parsingClient.getOfferInfo(?)(?)).thenReturnF(info)
        when(passportManager.loginOrRegisterInt(?)(?)).thenReturnF(LoginResponse.getDefaultInstance)

        val needParams = {
          val b = InternalLoginOrRegisterParameters.newBuilder()
          b.getIdentityBuilder.setPhone(info.getPhone)
          b.getOptionsBuilder.putPayload("ParsingRemoteUrl", remoteUrl)
          b.getOptionsBuilder.putPayload("ParsingRemoteId", remoteId)
          b.build()
        }

        parsingManager.loginOrRegisterOwner(hash, None).futureValue
        verify(parsingClient).getOfferInfo(eeq(hash))(?)
        verify(passportManager).loginOrRegisterInt(eeq(needParams))(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }

      "no params in the info response" in new FailingDraftHandleFixture {
        val info = Resources
          .toProto[ParsedOfferInfoResponse]("/parsing/parsed_offer_info.json")
          .toBuilder
          .clearRemoteId()
          .clearRemoteUrl()
          .build()
        when(parsingClient.getOfferInfo(?)(?)).thenReturnF(info)
        when(passportManager.loginOrRegisterInt(?)(?)).thenReturnF(LoginResponse.getDefaultInstance)

        val needParams = {
          val b = InternalLoginOrRegisterParameters.newBuilder()
          b.getIdentityBuilder.setPhone(info.getPhone)
          b.build()
        }

        parsingManager.loginOrRegisterOwner(hash, None).futureValue
        verify(parsingClient).getOfferInfo(eeq(hash))(?)
        verify(passportManager).loginOrRegisterInt(eeq(needParams))(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }

      "custom phone was passed" in new FailingDraftHandleFixture {
        val phone = PhoneGen.next
        val info = Resources.toProto[ParsedOfferInfoResponse]("/parsing/parsed_offer_info.json")
        when(parsingClient.getOfferInfo(?)(?)).thenReturnF(info)
        when(passportManager.loginOrRegisterInt(?)(?)).thenReturnF(LoginResponse.getDefaultInstance)

        val needParams = {
          val b = InternalLoginOrRegisterParameters.newBuilder()
          b.getIdentityBuilder.setPhone(phone)
          b.getOptionsBuilder.putPayload("ParsingRemoteUrl", remoteUrl)
          b.getOptionsBuilder.putPayload("ParsingRemoteId", remoteId)
          b.build()
        }

        parsingManager.loginOrRegisterOwner(hash, Some(phone)).futureValue
        verify(parsingClient).getOfferInfo(eeq(hash))(?)
        verify(passportManager).loginOrRegisterInt(eeq(needParams))(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }

      "empty phone but custom phone provided" in new FailingDraftHandleFixture {
        val phone = PhoneGen.next
        val info = {
          val b = Resources.toProto[ParsedOfferInfoResponse]("/parsing/parsed_offer_info.json").toBuilder
          b.setPhone("")
          b.build()
        }
        when(parsingClient.getOfferInfo(?)(?)).thenReturnF(info)

        when(passportManager.loginOrRegisterInt(?)(?)).thenReturnF(LoginResponse.getDefaultInstance)

        parsingManager.loginOrRegisterOwner(hash, Some(phone)).await

        verify(parsingClient).getOfferInfo(eeq(hash))(?)
        val needParams = {
          val b = InternalLoginOrRegisterParameters.newBuilder()
          b.getIdentityBuilder.setPhone(phone)
          b.getOptionsBuilder.putPayload("ParsingRemoteUrl", remoteUrl)
          b.getOptionsBuilder.putPayload("ParsingRemoteId", remoteId)
          b.build()
        }
        verify(passportManager).loginOrRegisterInt(eeq(needParams))(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }
    }

    "loginOrRegisterOwner return error" when {
      "encrypted draft hash was passed, optPhone is not empty, user essentials do not contain optPhone" in new SuccessfullDraftHandleFixture {
        val optPhone = Some(PhoneGen.next)
        when(passportManager.getUserEssentials(?, ?)(?))
          .thenReturn(Future.successful(UserEssentials.newBuilder().addPhones(phone).build()))
        when(passportManager.loginOrRegisterInt(?)(?)).thenReturn(Future.successful(LoginResponse.getDefaultInstance))
        val exception =
          intercept[ParsingLoginOrRegisterException](
            cause(parsingManager.loginOrRegisterOwner(hash, optPhone).futureValue)
          )
        exception.getMessage shouldBe "not own phone"
        verify(passportManager).getUserEssentials(eeq(user), eeq(false))(?)
        verifyNoMoreInteractions(passportManager)
      }

      "encrypted draft hash contains phone from another user" in new SuccessfullDraftHandleFixture {
        val anotherPhone = PhoneGen.next
        when(passportManager.getUserEssentials(?, ?)(?))
          .thenReturn(Future.successful(UserEssentials.newBuilder().addPhones(anotherPhone).build()))
        when(passportManager.loginOrRegisterInt(?)(?)).thenReturn(Future.successful(LoginResponse.getDefaultInstance))
        val exception =
          intercept[ParsingLoginOrRegisterException](cause(parsingManager.loginOrRegisterOwner(hash, None).futureValue))
        exception.getMessage shouldBe "not own phone"
        verify(passportManager).getUserEssentials(eeq(user), eeq(false))(?)
        verifyNoMoreInteractions(passportManager)
      }

      "canPublish=false" in new FailingDraftHandleFixture {
        val phone = PhoneGen.next
        val info = {
          val b = Resources.toProto[ParsedOfferInfoResponse]("/parsing/parsed_offer_info.json").toBuilder
          b.getCanPublishBuilder.setValue(false)
          b.build()
        }
        when(parsingClient.getOfferInfo(?)(?)).thenReturnF(info)

        intercept[ParsingLoginOrRegisterException] {
          parsingManager.loginOrRegisterOwner(hash, None).await
        }

        intercept[ParsingLoginOrRegisterException] {
          parsingManager.loginOrRegisterOwner(hash, Some(phone)).await
        }

        verify(parsingClient, new Times(2)).getOfferInfo(eeq(hash))(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }

      "empty phone, empty custom phone" in new FailingDraftHandleFixture {
        val info = {
          val b = Resources.toProto[ParsedOfferInfoResponse]("/parsing/parsed_offer_info.json").toBuilder
          b.setPhone("")
          b.build()
        }
        when(parsingClient.getOfferInfo(?)(?)).thenReturnF(info)

        intercept[ParsingLoginOrRegisterException] {
          parsingManager.loginOrRegisterOwner(hash, None).await
        }

        verify(parsingClient).getOfferInfo(eeq(hash))(?)
        verifyNoMoreInteractions(parsingClient)
        verifyNoMoreInteractions(passportManager)
        verifyNoMoreInteractions(draftsManager)
      }
    }
  }

  private def cause[A](action: => A): A = {
    try {
      action
    } catch {
      case e: TestFailedException if e.getCause != null =>
        throw e.getCause
    }
  }
}
