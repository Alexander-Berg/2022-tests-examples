package ru.yandex.realty.managers.draft

import akka.http.scaladsl.server.RequestContext
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.{JsObject, JsValue, Json}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.auth.AuthInfo
import ru.yandex.realty.clients.hydra.HydraClient
import ru.yandex.realty.clients.vos.RequestModel.VosUser
import ru.yandex.realty.clients.vos.VosClient
import ru.yandex.realty.event.VertisEventManager
import ru.yandex.realty.event.model.OfferId
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.managers.drafts.{DraftManager, DraftManagerImpl}
import ru.yandex.realty.managers.karma.KarmaManager
import ru.yandex.realty.managers.phone.RedirectPhonesManager
import ru.yandex.realty.model.exception.{NeedAuthenticationException, TooManyRequestsException}
import ru.yandex.realty.model.offer.SalesAgentCategory
import ru.yandex.realty.model.user.{UserRef, UserRefGenerators}
import ru.yandex.realty.platform.PlatformInfo
import ru.yandex.realty.request.{Request, RequestImpl}
import ru.yandex.realty.transformers.FiltersStateToOfferDataTransformer
import ru.yandex.realty.validators.{OfferValidator, ValidationResponse}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class DraftManagerSpec
  extends AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with UserRefGenerators
  with PrivateMethodTester {

  val vosClient: VosClient = mock[VosClient]
  val vertisEventManager: VertisEventManager = mock[VertisEventManager]
  val transformer: FiltersStateToOfferDataTransformer = mock[FiltersStateToOfferDataTransformer]
  val hydraClient: HydraClient = mock[HydraClient]
  val validator: OfferValidator = mock[OfferValidator]
  val redirectPhonesManager: RedirectPhonesManager = mock[RedirectPhonesManager]
  val karmaManager: KarmaManager = mock[KarmaManager]

  private val usersWithIds: Gen[UserRef] = Gen.oneOf(webUserGen, passportUserGen)
  private val usersWithoutIds: Gen[UserRef] = Gen.oneOf(appUserGen, noUserGen)

  val reqCtx: RequestContext = mock[RequestContext]

  val manager: DraftManager = new DraftManagerImpl(
    vosClient,
    vertisEventManager,
    transformer,
    hydraClient,
    validator,
    redirectPhonesManager,
    karmaManager,
    TestOperationalSupport
  )

  val getDraftOwner: PrivateMethod[String] = PrivateMethod[String]('getDraftOwner)

  val androidPlatformInfo = PlatformInfo("android", "dpixxx")
  val authInfo = AuthInfo()

  implicit private val r: Request = new RequestImpl

  "DraftManager createDraft" should {

    "work with no body and app platform" in {
      forAll(passportUserGen) { user =>
        val vosUser = VosUser(
          createTime = DateTime.now(),
          updateTime = DateTime.now(),
          vertical = "realty",
          id = s"uid_${user.uid.toString}",
          status = "active",
          capaUser = false,
          login = "vasya",
          `type` = SalesAgentCategory.OWNER,
          email = None,
          name = None,
          organization = None,
          ogrn = None,
          agencyId = None,
          callCenter = None,
          url = None,
          photoUrl = None,
          phones = Seq("333222333"),
          licenseAgreement = None,
          redirectPhones = None,
          paymentType = None,
          overQuota = None,
          mosRuStatus = None,
          mosRuAvailable = None,
          allowedCommunicationChannels = None
        )

        val userId = manager.invokePrivate(getDraftOwner(user))
        val body: JsValue = Json.parse("""
            |{
            |	"offers": [{
            |		"platform": "ANDROID"
            |	}]
            |}
          """.stripMargin)

        (hydraClient
          .limiter(_: String, _: String, _: Option[Int])(_: Traced))
          .expects(HydraClient.OfferCreateComponent, userId, *, *)
          .returning(Future.successful(100))

        (vertisEventManager
          .createOfferEvent(_: OfferId, _: RequestContext, _: AuthInfo, _: PlatformInfo, _: Seq[String])(_: Traced))
          .expects("7705036036560018432", reqCtx, authInfo, androidPlatformInfo, Seq("333222333"), *)
          .returning(Future.unit)

        (vosClient
          .getUser(_: String)(_: Traced))
          .expects(user.uid.toString, *)
          .returning(Future.successful(Some(vosUser)))

        (vosClient
          .createDraft(_: String, _: JsValue, _: RequestContext)(_: Traced))
          .expects(userId, body, *, *)
          .returning(Future.successful(Json.parse("""
              |{
              |  "status": "OK",
              |  "id": [
              |    "7705036036560018432"
              |  ],
              |  "details": [
              |    {
              |      "id": "7705036036560018432",
              |      "isEligible": false
              |    }
              |  ]
              |}
            """.stripMargin)))

        (karmaManager
          .checkKarma(_: AuthInfo))
          .expects(authInfo)
          .returning(())

        val resp = manager.createDraft(reqCtx, authInfo, androidPlatformInfo, None, user)

        resp.futureValue should be(Json.parse("""
            |{
            |	"response": {
            |		"status": "OK",
            |		"id": "7705036036560018432",
            |		"details": [{
            |			"id": "7705036036560018432",
            |			"isEligible": false
            |		}]
            |	}
            |}
          """.stripMargin))
      }
    }

    "work with no body and no platform" in {
      forAll(passportUserGen) { user =>
        val vosUser = VosUser(
          createTime = DateTime.now(),
          updateTime = DateTime.now(),
          vertical = "realty",
          id = s"uid_${user.uid.toString}",
          status = "active",
          capaUser = false,
          login = "vasya",
          `type` = SalesAgentCategory.OWNER,
          email = None,
          name = None,
          organization = None,
          ogrn = None,
          agencyId = None,
          callCenter = None,
          url = None,
          photoUrl = None,
          phones = Seq("333222333"),
          licenseAgreement = None,
          redirectPhones = None,
          paymentType = None,
          overQuota = None,
          mosRuStatus = None,
          mosRuAvailable = None,
          allowedCommunicationChannels = None
        )

        val userId = manager.invokePrivate(getDraftOwner(user))
        val platformInfo = PlatformInfo("", "")
        val body: JsValue = Json.parse("""
            |{
            |	"offers": [{}]
            |}
          """.stripMargin)

        (hydraClient
          .limiter(_: String, _: String, _: Option[Int])(_: Traced))
          .expects(HydraClient.OfferCreateComponent, userId, *, *)
          .returning(Future.successful(100))

        (vertisEventManager
          .createOfferEvent(_: OfferId, _: RequestContext, _: AuthInfo, _: PlatformInfo, _: Seq[String])(_: Traced))
          .expects("7705036036560018432", reqCtx, authInfo, platformInfo, Seq("333222333"), *)
          .returning(Future.unit)

        (vosClient
          .getUser(_: String)(_: Traced))
          .expects(user.uid.toString, *)
          .returning(Future.successful(Some(vosUser)))

        (vosClient
          .createDraft(_: String, _: JsValue, _: RequestContext)(_: Traced))
          .expects(userId, body, *, *)
          .returning(Future.successful(Json.parse("""
              |{
              |  "status": "OK",
              |  "id": [
              |    "7705036036560018432"
              |  ],
              |  "details": [
              |    {
              |      "id": "7705036036560018432",
              |      "isEligible": false
              |    }
              |  ]
              |}
            """.stripMargin)))

        (karmaManager
          .checkKarma(_: AuthInfo))
          .expects(authInfo)
          .returning(())

        val resp = manager.createDraft(reqCtx, authInfo, platformInfo, None, user)

        resp.futureValue should be(Json.parse("""
            |{
            |	"response": {
            |		"status": "OK",
            |		"id": "7705036036560018432",
            |		"details": [{
            |			"id": "7705036036560018432",
            |			"isEligible": false
            |		}]
            |	}
            |}
          """.stripMargin))
      }
    }

    "work with body and platform" in {
      forAll(passportUserGen) { user =>
        val userId = manager.invokePrivate(getDraftOwner(user))

        val vosUser = VosUser(
          createTime = DateTime.now(),
          updateTime = DateTime.now(),
          vertical = "realty",
          id = s"uid_${user.uid.toString}",
          status = "active",
          capaUser = false,
          login = "vasya",
          `type` = SalesAgentCategory.OWNER,
          email = None,
          name = None,
          organization = None,
          ogrn = None,
          agencyId = None,
          callCenter = None,
          url = None,
          photoUrl = None,
          phones = Seq("333222333"),
          licenseAgreement = None,
          redirectPhones = None,
          paymentType = None,
          overQuota = None,
          mosRuStatus = None,
          mosRuAvailable = None,
          allowedCommunicationChannels = None
        )
        val body: JsValue = Json.parse("""
            |{
            |	"offers": [{
            |		"redirectPhones": false,
            |   "location": {
            |      "rgid": 100,
            |      "country": "Россия",
            |      "localityName": "Санкт-Петербург"
            |    },
            |    "platform": "ANDROID"
            |	},
            | {
            |		"redirectPhones": true,
            |   "location": {
            |      "rgid": 1,
            |      "country": "Россия",
            |      "localityName": "Москва"
            |    },
            |    "platform": "ANDROID"
            |	}]
            |}
          """.stripMargin)

        val inputBody: JsValue = Json.parse("""
            |{
            |	"offers": [{
            |		"redirectPhones": false,
            |   "location": {
            |      "rgid": 100,
            |      "country": "Россия",
            |      "localityName": "Санкт-Петербург"
            |    }
            |	},
            | {
            |		"redirectPhones": true,
            |   "location": {
            |      "rgid": 1,
            |      "country": "Россия",
            |      "localityName": "Москва"
            |    }
            |	}]
            |}
          """.stripMargin)

        (hydraClient
          .limiter(_: String, _: String, _: Option[Int])(_: Traced))
          .expects(HydraClient.OfferCreateComponent, userId, *, *)
          .returning(Future.successful(100))

        (vertisEventManager
          .createOfferEvent(_: OfferId, _: RequestContext, _: AuthInfo, _: PlatformInfo, _: Seq[String])(_: Traced))
          .expects("7705036036560018432", reqCtx, authInfo, androidPlatformInfo, Seq("333222333"), *)
          .returning(Future.unit)

        (vosClient
          .getUser(_: String)(_: Traced))
          .expects(user.uid.toString, *)
          .returning(Future.successful(Some(vosUser)))

        (vosClient
          .createDraft(_: String, _: JsValue, _: RequestContext)(_: Traced))
          .expects(userId, body, *, *)
          .returning(Future.successful(Json.parse("""
              |{
              |  "status": "OK",
              |  "id": [
              |    "7705036036560018432",
              |    "1016489155713981696"
              |  ],
              |  "details": [
              |    {
              |      "id": "7705036036560018432",
              |      "isEligible": false
              |    }, {
              |      "id": "1016489155713981696",
              |      "isEligible": false
              |    }
              |  ]
              |}
            """.stripMargin)))

        (karmaManager
          .checkKarma(_: AuthInfo))
          .expects(authInfo)
          .returning(())

        val resp = manager.createDraft(reqCtx, authInfo, androidPlatformInfo, Some(inputBody), user)

        resp.futureValue should be(Json.parse("""
            |{
            |	"response": {
            |		"status": "OK",
            |		"id": "7705036036560018432",
            |		"details": [{
            |			"id": "7705036036560018432",
            |			"isEligible": false
            |		}, {
            |      "id": "1016489155713981696",
            |      "isEligible": false
            |    }]
            |	}
            |}
          """.stripMargin))
      }
    }

    "throw an exception if request limit exceeded" in {
      forAll(usersWithIds) { user =>
        val userId = manager.invokePrivate(getDraftOwner(user))

        (karmaManager
          .checkKarma(_: AuthInfo))
          .expects(authInfo)
          .returning(())

        (hydraClient
          .limiter(_: String, _: String, _: Option[Int])(_: Traced))
          .expects(HydraClient.OfferCreateComponent, userId, *, *)
          .returning(Future.successful(-1))

        val platformInfo = PlatformInfo("android", "dpixxx")
        ScalaFutures.whenReady(manager.createDraft(reqCtx, authInfo, platformInfo, None, user).failed) { e =>
          e shouldBe a[TooManyRequestsException]
        }
      }
    }

    "throw an exception if user has no UID" in {
      forAll(usersWithoutIds) { user =>
        val platformInfo = PlatformInfo("android", "dpixxx")

        (karmaManager
          .checkKarma(_: AuthInfo))
          .expects(authInfo)
          .returning(())

        ScalaFutures.whenReady(manager.createDraft(reqCtx, authInfo, platformInfo, None, user).failed) { e =>
          e shouldBe a[NeedAuthenticationException]
        }
      }
    }
  }

  "DraftManager updateDraft" should {
    "set redirectPhones for passport user from app" in {
      forAll(passportUserGen) { user =>
        val inputBody = Json.parse("""
            |{
            |  "offer": {
            |		 "redirectPhones": false,
            |    "location": {
            |      "rgid": 100,
            |      "country": "Россия",
            |      "localityName": "Санкт-Петербург"
            |   }
            | }
            |}
          """.stripMargin).as[JsObject]

        val vosBody = (inputBody \ "offer").as[JsObject]

        val vosUser = VosUser(
          createTime = DateTime.now(),
          updateTime = DateTime.now(),
          vertical = "realty",
          id = s"uid_${user.uid.toString}",
          status = "active",
          capaUser = false,
          login = "vasya",
          `type` = SalesAgentCategory.OWNER,
          email = None,
          name = None,
          organization = None,
          ogrn = None,
          agencyId = None,
          callCenter = None,
          url = None,
          photoUrl = None,
          phones = Seq("333222333"),
          licenseAgreement = None,
          redirectPhones = None,
          paymentType = None,
          overQuota = None,
          mosRuStatus = None,
          mosRuAvailable = None,
          allowedCommunicationChannels = None
        )

        val offerId = "3524142647241"
        val userId = manager.invokePrivate(getDraftOwner(user))

        val expectingBody = Json.parse("""
            | {
            | 	"location": {
            | 		"rgid": 100,
            | 		"country": "Россия",
            | 		"localityName": "Санкт-Петербург"
            | 	},
            |  "status": "active",
            |  "redirectPhones": true
            | }
          """.stripMargin)

        val resp = Json.parse("""
            |{
            |  "status": "OK"
            |}
          """.stripMargin)

        (validator
          .validateOffer(_: JsObject, _: Option[Long])(_: Request))
          .expects(inputBody, None, *)
          .returning(ValidationResponse(None, valid = true))

        (transformer
          .transform(_: JsObject))
          .expects(vosBody)
          .returning(vosBody)

        (vosClient
          .getUser(_: String)(_: Traced))
          .expects(user.uid.toString, *)
          .returning(Future.successful(Some(vosUser)))

        (redirectPhonesManager
          .canCreateRedirectForVosUpdate(_: JsValue, _: Seq[String])(_: Traced))
          .expects(vosBody, Seq("333222333"), *)
          .returning(Future.successful(true))

        (vertisEventManager
          .updateOfferEvent(_: String, _: RequestContext, _: AuthInfo, _: PlatformInfo, _: Seq[String])(_: Traced))
          .expects(offerId, reqCtx, authInfo, androidPlatformInfo, Seq("333222333"), *)
          .returning(Future.unit)

        (vosClient
          .updateDraft(_: String, _: JsValue, _: String, _: RequestContext)(_: Traced))
          .expects(userId, expectingBody, offerId, *, *)
          .returning(Future.successful(resp))

        (karmaManager
          .checkKarma(_: AuthInfo))
          .expects(authInfo)
          .returning(())

        manager
          .updateDraft(reqCtx, authInfo, androidPlatformInfo, inputBody, offerId, publish = true, user)
          .futureValue should be(Json.obj("response" -> resp))
      }
    }

    "omit redirectPhones for anon users" in {
      forAll(webUserGen) { user =>
        val inputBody = Json.parse("""
            |{
            |  "offer": {
            |		 "redirectPhones": false,
            |    "location": {
            |      "rgid": 100,
            |      "country": "Россия",
            |      "localityName": "Санкт-Петербург"
            |   }
            | }
            |}
          """.stripMargin).as[JsObject]

        val vosBody = (inputBody \ "offer").as[JsObject]

        val offerId = "3524142647241"
        val userId = manager.invokePrivate(getDraftOwner(user))

        val expectingBody = Json.parse("""
            | {
            | 	"redirectPhones": false,
            | 	"location": {
            | 		"rgid": 100,
            | 		"country": "Россия",
            | 		"localityName": "Санкт-Петербург"
            | 	},
            |  "status": "inactive"
            | }
          """.stripMargin)

        val resp = Json.parse("""
            |{
            |  "status": "OK"
            |}
          """.stripMargin)

        (validator
          .validateOffer(_: JsObject, _: Option[Long])(_: Request))
          .expects(inputBody, None, *)
          .returning(ValidationResponse(None, valid = true))

        (transformer
          .transform(_: JsObject))
          .expects(vosBody)
          .returning(vosBody)

        (vertisEventManager
          .updateOfferEvent(_: String, _: RequestContext, _: AuthInfo, _: PlatformInfo, _: Seq[String])(_: Traced))
          .expects(offerId, reqCtx, authInfo, androidPlatformInfo, Seq.empty[String], *)
          .returning(Future.unit)

        (vosClient
          .updateDraft(_: String, _: JsValue, _: String, _: RequestContext)(_: Traced))
          .expects(userId, expectingBody, offerId, *, *)
          .returning(Future.successful(resp))

        (karmaManager
          .checkKarma(_: AuthInfo))
          .expects(authInfo)
          .returning(())

        manager
          .updateDraft(reqCtx, authInfo, androidPlatformInfo, inputBody, offerId, publish = false, user)
          .futureValue should be(Json.obj("response" -> resp))
      }
    }

    "omit redirectPhones for passport users from web" in {
      forAll(webUserGen) { user =>
        val inputBody = Json.parse("""
            |{
            |  "offer": {
            |		 "redirectPhones": false,
            |    "location": {
            |      "rgid": 100,
            |      "country": "Россия",
            |      "localityName": "Санкт-Петербург"
            |   }
            | }
            |}
          """.stripMargin).as[JsObject]

        val vosBody = (inputBody \ "offer").as[JsObject]
        val platformInfo = PlatformInfo("", "")

        val offerId = "3524142647241"
        val userId = manager.invokePrivate(getDraftOwner(user))

        val expectingBody = Json.parse("""
            | {
            | 	"redirectPhones": false,
            | 	"location": {
            | 		"rgid": 100,
            | 		"country": "Россия",
            | 		"localityName": "Санкт-Петербург"
            | 	},
            |  "status": "inactive"
            | }
          """.stripMargin)

        val resp = Json.parse("""
            |{
            |  "status": "OK"
            |}
          """.stripMargin)

        (validator
          .validateOffer(_: JsObject, _: Option[Long])(_: Request))
          .expects(inputBody, None, *)
          .returning(ValidationResponse(None, valid = true))

        (transformer
          .transform(_: JsObject))
          .expects(vosBody)
          .returning(vosBody)

        (vertisEventManager
          .updateOfferEvent(_: String, _: RequestContext, _: AuthInfo, _: PlatformInfo, _: Seq[String])(_: Traced))
          .expects(offerId, reqCtx, authInfo, platformInfo, Seq.empty[String], *)
          .returning(Future.unit)

        (vosClient
          .updateDraft(_: String, _: JsValue, _: String, _: RequestContext)(_: Traced))
          .expects(userId, expectingBody, offerId, *, *)
          .returning(Future.successful(resp))

        (karmaManager
          .checkKarma(_: AuthInfo))
          .expects(authInfo)
          .returning(())

        manager
          .updateDraft(reqCtx, authInfo, platformInfo, inputBody, offerId, publish = false, user)
          .futureValue should be(Json.obj("response" -> resp))
      }
    }
  }
}
