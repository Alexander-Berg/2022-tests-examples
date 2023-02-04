package ru.yandex.realty.managers.users

import akka.http.scaladsl.server.RequestContext
import com.google.protobuf.BoolValue
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.ProtoResponse
import ru.yandex.realty.api.model.user.{
  User => ApiUser,
  UserContacts => ApiUserContacts,
  UserInfo => ApiUserInfo,
  UserUpdate => ApiUserUpdate
}
import ru.yandex.realty.auth.AuthInfo
import ru.yandex.realty.clients.billing.{Balance, Order, OrderProperties}
import ru.yandex.realty.clients.capa.CapaClient
import ru.yandex.realty.clients.social.SocialClient
import ru.yandex.realty.clients.vos.ng.{CrmOffersFilter, VosClientNG}
import ru.yandex.realty.controllers.lk.BootstrapRequest
import ru.yandex.realty.converters.VosUserToApiUserConverter
import ru.yandex.realty.event.VertisEventManager
import ru.yandex.realty.managers.banker.BankerManager
import ru.yandex.realty.managers.billing.BillingBootstrapManager
import ru.yandex.realty.managers.tuz.TuzManager
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.promocoder.PromocoderAsyncClient
import ru.yandex.realty.promocoder.model.{CreateFeatureRequest, FeatureInstance}
import ru.yandex.realty.proto.offer.PaymentType
import ru.yandex.realty.proto.social.trusted.TrustedUserInfo
import ru.yandex.realty.request.{Request, RequestImpl}
import ru.yandex.realty.vos.model.agency.AgencyProfile
import ru.yandex.realty.vos.model.common.Timestamps
import ru.yandex.realty.vos.model.request.RequestMeta
import ru.yandex.realty.vos.model.user.{
  UserSource,
  UserStatus,
  UserType,
  User => VosUser,
  UserContacts => VosUserContacts,
  UserInfo => VosUserInfo,
  UserSettings => VosUserSettings,
  UserUpdate => VosUserUpdate
}
import ru.yandex.vertis.banker.model.ApiModel
import ru.yandex.vertis.banker.model.ApiModel.AccountInfo
import ru.yandex.realty.tracing.Traced

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class PersonalAccountUsersManagerSpec extends AsyncSpecBase {
  private def getApiUserUpdate(name: String, paymentType: PaymentType) = {
    val apiUserContacts = ApiUserContacts
      .newBuilder()
      .setName(name)
      .build()
    val apiUserInfo = ApiUserInfo
      .newBuilder()
      .setPaymentType(paymentType)
      .setUserSource(UserSource.DEFAULT)
      .setUserType(UserType.OWNER)
      .build()
    val trustedUserInfo = TrustedUserInfo.newBuilder().build()
    ApiUserUpdate
      .newBuilder()
      .setUserContacts(apiUserContacts)
      .setUserInfo(apiUserInfo)
      .setTrustedUserInfo(trustedUserInfo)
      .build()
  }

  private def getRequest(implicit traced: Traced) = {
    val request = new RequestImpl
    request.setTrace(traced)
    request.setIp("localhost")
    request.setProxyIp(Some("localhost"))
    request.setUserAgent(None)
    request.setAuthInfo(AuthInfo())
    request
  }

  private def getVosUserUpdate(vosUserContacts: VosUserContacts, vosUserInfo: VosUserInfo) = {
    val requestMeta = RequestMeta
      .newBuilder()
      .setUserIp("localhost")
      .setProxyIp("localhost")
      .build()
    val trustedUserInfo = TrustedUserInfo.newBuilder().build()
    VosUserUpdate
      .newBuilder()
      .setUserContacts(vosUserContacts)
      .setUserInfo(vosUserInfo)
      .setRequestMeta(requestMeta)
      .setUserSettings(VosUserSettings.newBuilder().build())
      .setTrustedUserInfo(trustedUserInfo)
      .build()
  }

  private def getVosUserContacts(name: String) = {
    VosUserContacts
      .newBuilder()
      .setName(name)
      .build()
  }

  private def getVosUserInfo(paymentType: PaymentType) = {
    VosUserInfo
      .newBuilder()
      .setPaymentType(paymentType)
      .setUserSource(UserSource.DEFAULT)
      .setUserType(UserType.OWNER)
      .build()
  }

  private def getResponse = {
    ProtoResponse.UserUpdateResponse
      .newBuilder()
      .setUserRef("uid:42")
      .build()
  }

  private def getReturnedVosUser(vosUserContacts: VosUserContacts, vosUserInfo: VosUserInfo) = {
    val timestamps = Timestamps
      .newBuilder()
      .setCreateTime(1234L)
      .setUpdateTime(4321L)
      .build()
    val trustedUserInfo = TrustedUserInfo.newBuilder().build()
    VosUser
      .newBuilder()
      .setId(42)
      .setUserContacts(vosUserContacts)
      .setUserInfo(vosUserInfo)
      .setUserSettings(VosUserSettings.newBuilder().build())
      .setUserStatus(UserStatus.ACTIVE)
      .setTimestamps(timestamps)
      .setTrustedUserInfo(trustedUserInfo)
      .build()
  }

  private def getExistingVosUser(vosUserContacts: VosUserContacts, vosUserInfo: VosUserInfo) = {
    VosUser
      .newBuilder()
      .setId(42L)
      .setUserContacts(vosUserContacts)
      .setUserInfo(vosUserInfo)
      .build()
  }

  private def getBankerAccount = {
    ApiModel.Account
      .newBuilder()
      .setId("42")
      .setUser("Новый пользователь banker")
      .build()
  }

  private def getOrder(): Order = Order(42, 42, None, OrderProperties(None, None, None), Balance(0L, 0L, 0L, 0L))

  abstract private class Fixture {
    val vosClientNG = mock[VosClientNG]
    val bankerManager = mock[BankerManager]
    val billingBootstrapManager = mock[BillingBootstrapManager]
    val promocoderClient = mock[PromocoderAsyncClient]
    val vertisEventManager = mock[VertisEventManager]
    val capaClient = mock[CapaClient]
    val requestContext = mock[RequestContext]
    val socialClient = mock[SocialClient]
    val tuzManager = mock[TuzManager]

    lazy val manager = new PersonalAccountUsersManager(
      vosClientNG,
      bankerManager,
      billingBootstrapManager,
      promocoderClient,
      vertisEventManager,
      capaClient,
      "",
      "",
      tuzManager = tuzManager
    )
  }

  "PersonalAccountUsersManager" should {
    "Call VosClient and convert vos user to api user" in new Fixture {
      val vosUserToApiUserConverter = mock[VosUserToApiUserConverter]
      override lazy val manager: PersonalAccountUsersManager = new PersonalAccountUsersManager(
        vosClientNG,
        bankerManager,
        billingBootstrapManager,
        promocoderClient,
        vertisEventManager,
        capaClient,
        "",
        "",
        tuzManager,
        vosUserToApiUserConverter
      )
      val uid = 4815162342L

      val vosUser = VosUser
        .newBuilder()
        .setId(uid)
        .build()

      val apiUser = ApiUser
        .newBuilder()
        .setId(uid)
        .build()

      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(uid, *, *)
        .returning(Future.successful(Some(vosUser)))

      val filterSell =
        new CrmOffersFilter(
          offerType = Some(OfferType.SELL.toString),
          category = Set(CategoryType.APARTMENT.toString),
          showStatusV2 = Set("PUBLISHED", "UNPUBLISHED")
        )

      (vosUserToApiUserConverter
        .fromVos(_: VosUser))
        .expects(vosUser)
        .returning(apiUser)

      val result = manager.getUserByUid(uid)(Traced.empty).futureValue
      result.getResponse.getId shouldBe uid
    }

    "updateUser should work correctly when no user is present, and natural person is added" in new Fixture {
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(None)
        }

      val apiUserUpdate = getApiUserUpdate("Какое-то имя", PaymentType.PT_NATURAL_PERSON)
      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest

      val vosUserContacts = getVosUserContacts("Какое-то имя")
      val vosUserInfo = getVosUserInfo(PaymentType.PT_NATURAL_PERSON)
      val vosUserUpdate = getVosUserUpdate(vosUserContacts, vosUserInfo)
      val response = getResponse
      (vosClientNG
        .updateUser(_: Long, _: VosUserUpdate)(_: Traced))
        .expects(42L, vosUserUpdate, *)
        .returning {
          Future.successful(response)
        }
      val returnedUser = getReturnedVosUser(vosUserContacts, vosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(returnedUser))
        }
      val bankerAccount = getBankerAccount
      (bankerManager
        .getOrCreateAccount(_: VosUser)(_: Traced))
        .expects(returnedUser, *)
        .returning {
          Future.successful(bankerAccount)
        }
      manager.updateUser(42L, apiUserUpdate, requestContext).futureValue shouldBe response
    }

    "updateUser should work correctly when no user is present, and juridical person is added" in new Fixture {
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(None)
        }

      val apiUserUpdate = getApiUserUpdate("Какое-то имя", PaymentType.PT_JURIDICAL_PERSON)
      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest

      val vosUserContacts = getVosUserContacts("Какое-то имя")
      val vosUserInfo = getVosUserInfo(PaymentType.PT_JURIDICAL_PERSON)
      val vosUserUpdate = getVosUserUpdate(vosUserContacts, vosUserInfo)
      val response = getResponse
      (vosClientNG
        .updateUser(_: Long, _: VosUserUpdate)(_: Traced))
        .expects(42L, vosUserUpdate, *)
        .returning {
          Future.successful(response)
        }
      val returnedUser = getReturnedVosUser(vosUserContacts, vosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(returnedUser))
        }
      (billingBootstrapManager
        .billingBootstrap(_: String, _: BootstrapRequest)(_: Traced))
        .expects("42", *, *)
        .returning(Future.successful(getOrder))
      (capaClient.getPartners(_: String)(_: Traced)).expects("42", *).returning(Future.successful(Nil))
      manager.updateUser(42L, apiUserUpdate, requestContext).futureValue shouldBe response
    }

    "updateUser should work correctly when payment type is changed from juridical person to natural person" in new Fixture {
      val vosUserContacts = getVosUserContacts("Какое-то имя")
      val vosUserInfo = getVosUserInfo(PaymentType.PT_JURIDICAL_PERSON)
      val vosUser = getExistingVosUser(vosUserContacts, vosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(vosUser))
        }

      val apiUserUpdate = getApiUserUpdate("Новое имя", PaymentType.PT_NATURAL_PERSON)
      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest
      a[IllegalArgumentException] should be thrownBy {
        Await.result(manager.updateUser(42L, apiUserUpdate, requestContext), Duration.Inf)
      }
    }

    "updateUser should work correctly when user with unknown payment type is being inserted" in new Fixture {

      val apiUserUpdate = getApiUserUpdate("Новое имя", PaymentType.PT_UNKNOWN)
      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest
      a[IllegalArgumentException] should be thrownBy {
        Await.result(manager.updateUser(42L, apiUserUpdate, requestContext), Duration.Inf)
      }
    }

    "updateUser should work correctly when natural person is updated" in new Fixture {
      val oldVosUserContacts = getVosUserContacts("Какое-то имя")
      val oldVosUserInfo = getVosUserInfo(PaymentType.PT_NATURAL_PERSON)
      val oldVosUser = getExistingVosUser(oldVosUserContacts, oldVosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(oldVosUser))
        }

      val apiUserUpdate = getApiUserUpdate("Какое-то новое имя", PaymentType.PT_NATURAL_PERSON)
      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest

      val vosUserContacts = getVosUserContacts("Какое-то новое имя")
      val vosUserInfo = getVosUserInfo(PaymentType.PT_NATURAL_PERSON)
      val vosUserUpdate = getVosUserUpdate(vosUserContacts, vosUserInfo)
      val response = getResponse
      (vosClientNG
        .updateUser(_: Long, _: VosUserUpdate)(_: Traced))
        .expects(42L, vosUserUpdate, *)
        .returning {
          Future.successful(response)
        }
      val returnedUser = getReturnedVosUser(vosUserContacts, vosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(returnedUser))
        }
      val bankerAccount = getBankerAccount
      (bankerManager
        .getOrCreateAccount(_: VosUser)(_: Traced))
        .expects(returnedUser, *)
        .returning {
          Future.successful(bankerAccount)
        }
      manager.updateUser(42L, apiUserUpdate, requestContext).futureValue shouldBe response
    }

    "updateUser should work correctly when juridical person is updated" in new Fixture {
      val oldVosUserContacts = getVosUserContacts("Какое-то имя")
      val oldVosUserInfo = getVosUserInfo(PaymentType.PT_JURIDICAL_PERSON)
      val oldVosUser = getExistingVosUser(oldVosUserContacts, oldVosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(oldVosUser))
        }

      val apiUserUpdate = getApiUserUpdate("Какое-то новое имя", PaymentType.PT_JURIDICAL_PERSON)
      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest

      val vosUserContacts = getVosUserContacts("Какое-то новое имя")
      val vosUserInfo = getVosUserInfo(PaymentType.PT_JURIDICAL_PERSON)
      val vosUserUpdate = getVosUserUpdate(vosUserContacts, vosUserInfo)
      val response = getResponse
      (vosClientNG
        .updateUser(_: Long, _: VosUserUpdate)(_: Traced))
        .expects(42L, vosUserUpdate, *)
        .returning {
          Future.successful(response)
        }
      val returnedUser = getReturnedVosUser(vosUserContacts, vosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(returnedUser))
        }
      (capaClient.getPartners(_: String)(_: Traced)).expects("42", *).returning(Future.successful(Nil))
      manager.updateUser(42L, apiUserUpdate, requestContext).futureValue shouldBe response
    }

    "updateUser should work correctly when user is updated from natural person to juridical person" in new Fixture {
      val oldVosUserContacts = getVosUserContacts("Какое-то имя")
      val oldVosUserInfo = getVosUserInfo(PaymentType.PT_NATURAL_PERSON)
      val oldVosUser = getExistingVosUser(oldVosUserContacts, oldVosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(oldVosUser))
        }
      val apiUserUpdate = getApiUserUpdate("Какое-то новое имя", PaymentType.PT_JURIDICAL_PERSON)
      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest

      val vosUserContacts = getVosUserContacts("Какое-то новое имя")
      val vosUserInfo = getVosUserInfo(PaymentType.PT_JURIDICAL_PERSON)
      val vosUserUpdate = getVosUserUpdate(vosUserContacts, vosUserInfo)
      val response = getResponse
      (vosClientNG
        .updateUser(_: Long, _: VosUserUpdate)(_: Traced))
        .expects(42L, vosUserUpdate, *)
        .returning {
          Future.successful(response)
        }
      val returnedUser = getReturnedVosUser(vosUserContacts, vosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(returnedUser))
        }

      (billingBootstrapManager
        .billingBootstrap(_: String, _: BootstrapRequest)(_: Traced))
        .expects("42", *, *)
        .returning(Future.successful(getOrder))

      (capaClient.getPartners(_: String)(_: Traced)).expects("42", *).returning(Future.successful(Nil))

      val accountInfo = AccountInfo
        .newBuilder()
        .setBalance(1337)
        .build()
      (bankerManager
        .getAccountInfo(_: String, _: Option[String], _: Option[DateTime], _: Option[DateTime])(_: Traced))
        .expects("42", Some("42"), None, None, *)
        .returning {
          Future.successful {
            Some(accountInfo)
          }
        }
      val featuresRequest = CreateFeatureRequest(
        tag = "money",
        lifetime = (3 * 365).days,
        count = 13
      )
      val featureInstance = FeatureInstance(
        id = "instance_id",
        tag = "tag",
        createTs = DateTime.now(),
        deadline = DateTime.now().plusYears(3),
        user = "some_user",
        count = 13,
        jsonPayload = None
      )
      (promocoderClient
        .createFeatures(_: String, _: String, _: Seq[CreateFeatureRequest])(_: Traced))
        .expects("42", "migration", Seq(featuresRequest), *)
        .returning {
          Future.successful {
            Seq(featureInstance)
          }
        }
      manager.updateUser(42L, apiUserUpdate, requestContext).futureValue shouldBe response
    }

    "updateUser should work correctly when agency profile info not specified" in new Fixture {
      val uid = 42

      val vosUser = VosUser
        .newBuilder()
        .setId(uid)
        .setAgencyProfileEnabled(true)
        .setCurrentAgencyProfile(AgencyProfile.newBuilder().setName("Какое-то имя").setOgrn("111222"))
        .build()

      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(uid, *, *)
        .returning(Future.successful(Some(vosUser)))

      (billingBootstrapManager
        .billingBootstrap(_: String, _: BootstrapRequest)(_: Traced))
        .expects("42", *, *)
        .returning(Future.successful(getOrder))

      val vosUserContacts = getVosUserContacts("Какое-то имя")
      val vosUserInfo = getVosUserInfo(PaymentType.PT_JURIDICAL_PERSON)
      val vosUserUpdate = getVosUserUpdate(vosUserContacts, vosUserInfo)
      val response = getResponse

      vosUserUpdate.hasAgencyProfileEnabled shouldBe false
      vosUserUpdate.hasAgencyProfile shouldBe false

      (vosClientNG
        .updateUser(_: Long, _: VosUserUpdate)(_: Traced))
        .expects(uid, vosUserUpdate, *)
        .returning(Future.successful(response))
      val returnedUser = getReturnedVosUser(vosUserContacts, vosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(returnedUser))
        }
      (capaClient.getPartners(_: String)(_: Traced)).expects("42", *).returning(Future.successful(Nil))

      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest

      val userUpdate = getApiUserUpdate("Какое-то имя", PaymentType.PT_JURIDICAL_PERSON)
      val result = manager.updateUser(uid, userUpdate, requestContext).futureValue

      result.getUserRef shouldNot be(empty)
      result.hasError shouldBe false
    }

    "updateUser should pass filled agency profile" in new Fixture {
      val uid = 42

      val vosUser = VosUser
        .newBuilder()
        .setId(uid)
        .build()

      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(uid, *, *)
        .returning(Future.successful(Some(vosUser)))

      (billingBootstrapManager
        .billingBootstrap(_: String, _: BootstrapRequest)(_: Traced))
        .expects("42", *, *)
        .returning(Future.successful(getOrder))

      val vosUserContacts = getVosUserContacts("Какое-то имя")
      val vosUserInfo = getVosUserInfo(PaymentType.PT_JURIDICAL_PERSON)
      val vosUserUpdateBuilder = getVosUserUpdate(vosUserContacts, vosUserInfo).toBuilder
      val response = getResponse

      vosUserUpdateBuilder.setAgencyProfileEnabled(BoolValue.of(true))
      vosUserUpdateBuilder.getAgencyProfileBuilder.setName("Какое-то новое имя").setOgrn("111222")

      (vosClientNG
        .updateUser(_: Long, _: VosUserUpdate)(_: Traced))
        .expects(uid, vosUserUpdateBuilder.build(), *)
        .returning(Future.successful(response))

      val returnedUser = getReturnedVosUser(vosUserContacts, vosUserInfo)
      (vosClientNG
        .getUser(_: Long, _: Boolean)(_: Traced))
        .expects(42, *, *)
        .returning {
          Future.successful(Some(returnedUser))
        }
      (capaClient.getPartners(_: String)(_: Traced)).expects("42", *).returning(Future.successful(Nil))

      implicit val traced: Traced = Traced.empty
      implicit val request: Request = getRequest

      val userUpdateBuilder = getApiUserUpdate("Какое-то имя", PaymentType.PT_JURIDICAL_PERSON).toBuilder
      userUpdateBuilder.setAgencyProfileEnabled(BoolValue.of(true))
      userUpdateBuilder.getAgencyProfileBuilder.setName("Какое-то новое имя").setOgrn("111222")
      val result = manager.updateUser(uid, userUpdateBuilder.build(), requestContext).futureValue

      result.getUserRef shouldNot be(empty)
      result.hasError shouldBe false
    }
  }
}
