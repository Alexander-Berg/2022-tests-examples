package ru.auto.salesman.service.impl.user.notify.impl

import org.joda.time.{DateTime, DateTimeUtils}
import org.scalacheck.Gen.delay
import org.scalatest.OneInstancePerTest
import ru.auto.api.ApiOfferModel
import ru.auto.salesman.Task
import ru.auto.salesman.client.sms.model.{
  PhoneDeliveryParams,
  SmsMessage,
  SmsParams,
  SmsSender
}
import ru.auto.salesman.client.{PassportClient, SenderClient, VosClient}
import ru.auto.salesman.exceptions.CompositeException
import ru.auto.salesman.model.geo.{Region, RegionTypes}
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.Products
import ru.auto.salesman.model.user.{PaidOfferProduct, PaidProduct}
import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.geoservice.RegionService
import ru.auto.salesman.service.impl.user.notify.RateLimiter
import ru.auto.salesman.service.user.{ProlongationFailedPushService, UserFeatureService}
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.auto.salesman.util.AutomatedContext
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import zio.{Exit, ZIO}

import scala.collection.JavaConverters._

class NotificationSenderImplSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators
    with OneInstancePerTest
    with IntegrationPropertyCheckConfig {

  private val prolongationFailedPushService =
    mock[ProlongationFailedPushService]
  private val featureService = mock[UserFeatureService]
  private val smsSender = mock[SmsSender]
  private val passportClient = mock[PassportClient]
  private val senderClient = mock[SenderClient]
  private val rateLimiter: RateLimiter[Unit] = mock[RateLimiter[Unit]]
  private val vocClient: VosClient = mock[VosClient]
  private val regionService: RegionService = mock[RegionService]

  private val notificationService =
    new NotificationSenderImpl(
      prolongationFailedPushService = prolongationFailedPushService,
      featureService = featureService,
      smsSender = smsSender,
      passportClient = passportClient,
      senderClient = senderClient,
      rateLimiter = rateLimiter,
      vosClient = vocClient,
      regionService = regionService
    )

  implicit val rc = AutomatedContext("test")

  "AutoProlongNotificationServiceImpl" should {

    "don't send notifications if all features turn off" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        )
      ) { placement =>
        featuresMock(push = false, sms = false, email = false)
        mockCurrentTime()
        mockSuccessfulPassportClient(phones = Nil, email = None)
        mockVosClient(placement.offer)
        mockSuccessfulRateLimiter()
        successExecuteNotificationService(placement)

      }
    }

    "send push notification if feature prolongationFailedPushNotificationEnabled turn on" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        )
      ) { placement =>
        mockCurrentTime()
        mockRegionService()
        featuresMock(push = true, email = false, sms = false)
        mockSuccessfulProlongationFailedPushService(placement)
        mockVosClient(placement.offer)
        mockSuccessfulPassportClient(phones = Nil, email = None)
        mockSuccessfulRateLimiter()
        successExecuteNotificationService(placement)

      }
    }

    "send sms notification if feature prolongationFailedSmsNotificationEnabled turn on" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        featuresMock(push = false, email = false, sms = true)
        mockCurrentTime()
        mockVosClient(placement.offer)
        mockRegionService()
        mockSuccessfulPassportClient(
          phones = List("+79192034411"),
          email = None
        )
        mockSuccessfulRateLimiter()
        mockSuccessfulSmsClient("+79192034411")
        successExecuteNotificationService(placement)

      }
    }

    "send sms notification for few phone numbers if feature prolongationFailedSmsNotificationEnabled turn on" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        featuresMock(push = false, email = false, sms = true)
        mockCurrentTime()
        mockVosClient(placement.offer)
        mockRegionService()
        mockSuccessfulPassportClient(
          phones = List("+79192034411", "+7123456"),
          email = None
        )
        mockSuccessfulRateLimiter()
        mockSuccessfulSmsClient("+79192034411")
        mockSuccessfulSmsClient("+7123456")

        successExecuteNotificationService(placement)

      }
    }

    "prefer phone numbers in the offer where available" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        featuresMock(push = false, email = false, sms = true)
        mockCurrentTime()
        val phonesInOffer = List(
          "+78122128506",
          "+78122128507"
        )
        mockVosClient(
          placement.offer,
          offer = ApiOfferModel.Offer
            .newBuilder()
            .setSeller(
              ApiOfferModel.Seller
                .newBuilder()
                .addAllPhones(
                  phonesInOffer.map { phone =>
                    ApiOfferModel.Phone.newBuilder().setOriginal(phone).build()
                  }.asJava
                )
            )
            .build()
        )
        mockRegionService()
        mockSuccessfulPassportClient(
          phones = List("+79192034411"),
          email = None
        )
        mockSuccessfulRateLimiter()
        phonesInOffer.foreach(mockSuccessfulSmsClient)
        successExecuteNotificationService(placement)
      }
    }

    "send email notification if feature prolongationFailedEmailNotificationEnabled turn on" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        )
      ) { placement =>
        val email = "temp@yandex.ru"
        featuresMock(push = false, sms = false, email = true)
        mockCurrentTime()
        mockVosClient(placement.offer)
        mockSuccessfulPassportClient(phones = Nil, email = Some(email))
        mockSuccessfulRateLimiter()
        mockSuccessfulSenderClient(email)

        successExecuteNotificationService(placement)

      }
    }

    "don't send email notification if feature prolongationFailedEmailNotificationEnabled turn on, " +
    "and email string is empty" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        val email = ""
        mockSuccessfulPassportClient(phones = Nil, email = Some(email))
        mockCurrentTime()
        mockVosClient(placement.offer)
        mockSuccessfulRateLimiter()
        featuresMock(push = false, sms = false, email = true)

        failureExecuteNotificationService(placement) ===
          List(
            CompositeException(
              new IllegalArgumentException(
                "prolongation failed email not send: user email is empty"
              )
            )
          )

      }
    }

    "checking exception heading in passport client and pushnoy service" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        val exception = new Exception("ttt")
        (passportClient.userEssentials _)
          .expects(*)
          .throwingZ(exception)

        failureExecuteNotificationService(placement) shouldBe List(exception)

      }
    }

    "checking exception heading in one of channels" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        val email = "temp@yandex.ru"
        featuresMock(push = true, sms = true, email = true)
        mockVosClient(placement.offer)
        mockRegionService()
        mockCurrentTime()
        mockRegionService()
        val exception = new Exception("test exception")
        (prolongationFailedPushService
          .prolongationFailedPush(_: PaidOfferProduct))
          .expects(placement)
          .throwingZ(exception)

        mockSuccessfulPassportClient(
          phones = List("123455"),
          email = Some(email)
        )
        mockSuccessfulRateLimiter()
        mockSuccessfulSenderClient(email)
        mockSuccessfulSmsClient("123455")

        failureExecuteNotificationService(placement) shouldBe List(
          CompositeException(exception)
        )

      }
    }

    "not send prolongation failed notification if the attempt to send a notification has failed due to RateLimiter" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        mockSuccessfulPassportClient(phones = Nil, email = None)
        mockCurrentTime()
        mockVosClient(placement.offer)
        val exceptionInRateLimiter = new Exception("ttxz")

        (rateLimiter
          .limit(_: AutoruUser)(_: Task[Unit]))
          .expects(*, *)
          .throwingZ(exceptionInRateLimiter)
        notificationService
          .notifyProlongationFailed(
            placement
          )
          .failure
          .cause
          .failures shouldBe List(exceptionInRateLimiter)

      }
    }
    "check send notifyProlongDisableAfterPriceChange" in {
      val user = AutoruUser("user:123")
      val phone = "143542"
      val email = "ttt.@ddd.dd"
      mockSuccessfulPassportClient(
        phones = List(phone),
        email = Some(email)
      )
      mockSuccessfulSenderClient(email)
      mockSuccessfulSmsClient(phone)

      notificationService
        .notifyProlongDisableAfterPriceChange(user)
        .success

    }

    "send only email if curent time is not work time" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        val email = "temp@yandex.ru"
        featuresMock(push = true, sms = true, email = true)
        mockVosClient(placement.offer)
        mockRegionService()
        DateTimeUtils.setCurrentMillisFixed(
          DateTime.parse("2021-08-06T03:41:11").getMillis
        )
        mockRegionService()

        mockSuccessfulPassportClient(
          phones = List("123455"),
          email = Some(email)
        )
        mockSuccessfulRateLimiter()
        mockSuccessfulSenderClient(email)
        val resultException = new IllegalArgumentException(
          "Current user time is not included work time"
        ) ::
          CompositeException(
            new IllegalArgumentException(
              "Current user time is not included work time"
            )
          )

        val result = failureExecuteNotificationService(placement)
        result.size shouldBe 1
        result.head === resultException
        DateTimeUtils.setCurrentMillisSystem()
      }

    }

    "send only email if user not set region and offer region in vldivostok and curent time is not work time" in {
      forAll(
        delay(
          concreteProlongableGoodsGen(
            Products.allOfType[Products.Goods].filterNot(_ == Placement)
          )
        ),
        minSuccessful(1)
      ) { placement =>
        val email = "temp@yandex.ru"
        featuresMock(push = true, sms = true, email = true)
        val vladivostokGeoId = 76
        val valdivostokRegion = Region(
          id = 76,
          parentId = 1,
          `type` = RegionTypes.Other,
          ruName = "адивосток",
          tzOffset = 36000
        )
        val location = ApiOfferModel.Location
          .newBuilder()
          .setGeobaseId(vladivostokGeoId)
          .build()
        val sealer = ApiOfferModel.Seller
          .newBuilder()
          .setLocation(location)
          .build()
        val vosOferResponse = ApiOfferModel.Offer
          .newBuilder()
          .setSeller(sealer)
          .build()

        (vocClient.getOffer _)
          .expects(placement.offer, *)
          .returningZ(vosOferResponse)
        mockRegionService(
          geoId = vladivostokGeoId,
          result = Some(
            valdivostokRegion
          )
        )
        DateTimeUtils.setCurrentMillisFixed(
          DateTime.parse("2021-08-06T17:55:11").getMillis
        )
        mockRegionService(
          geoId = vladivostokGeoId,
          result = Some(
            valdivostokRegion
          )
        )
        mockSuccessfulPassportClient(
          phones = List("123455"),
          email = Some(email)
        )
        mockSuccessfulRateLimiter()
        mockSuccessfulSenderClient(email)

        val result = failureExecuteNotificationService(placement)
        result.size shouldBe 1
        DateTimeUtils.setCurrentMillisSystem()
      }

    }

  }

  private def successExecuteNotificationService(
      placement: PaidProduct
  ): Exit.Success[Unit] =
    notificationService
      .notifyProlongationFailed(
        placement
      )
      .success

  private def failureExecuteNotificationService(placement: PaidProduct): List[Throwable] =
    notificationService
      .notifyProlongationFailed(
        placement
      )
      .failure
      .cause
      .failures

  private def mockSuccessfulProlongationFailedPushService(
      product: PaidOfferProduct
  ): Unit =
    (prolongationFailedPushService
      .prolongationFailedPush(_: PaidOfferProduct))
      .expects(product)
      .returningZ(1)

  private def mockSuccessfulSmsClient(phone: String): Unit =
    (smsSender
      .send(_: SmsMessage, _: SmsParams))
      .expects(*, PhoneDeliveryParams(phone))
      .returningZ("ok")

  private def mockSuccessfulSenderClient(email: String): Unit =
    (senderClient
      .sendLetter(_: String, _: String, _: Map[String, String]))
      .expects(*, email, *)
      .returning(ZIO.unit)

  private def mockSuccessfulPassportClient(
      phones: List[String],
      email: Option[String]
  ): Unit = {
    val userEssentialsBuilder = UserEssentials
      .newBuilder()
    userEssentialsBuilder.addAllPhones(phones.asJava)
    email.foreach(email => userEssentialsBuilder.setEmail(email))
    (passportClient.userEssentials _)
      .expects(*)
      .returningZ(
        userEssentialsBuilder.build()
      )
  }

  private def mockSuccessfulRateLimiter(): Unit =
    (rateLimiter
      .limit(_: AutoruUser)(_: Task[Unit]))
      .expects(*, *)
      .onCall((_: AutoruUser, f: Task[Unit]) => f)

  private def featuresMock(push: Boolean, sms: Boolean, email: Boolean): Unit = {
    (featureService.prolongationFailedPushNotificationEnabled _)
      .expects()
      .returning(push)
    (featureService.prolongationFailedSmsNotificationEnabled _)
      .expects()
      .returning(sms)
    (featureService.prolongationFailedEmailNotificationEnabled _)
      .expects()
      .returning(email)
  }

  private def mockVosClient(
      offerId: OfferIdentity,
      offer: ApiOfferModel.Offer = ApiOfferModel.Offer.newBuilder().build()
  ): Unit =
    (vocClient.getOffer _)
      .expects(offerId, *)
      .returningZ(offer)

  private def mockCurrentTime(): Unit = {
    val dateTime = DateTime.parse("2021-08-06T15:41:11")

    DateTimeUtils.setCurrentMillisFixed(dateTime.getMillis)
  }

  private def mockRegionService(geoId: Long = 0, result: Option[Region] = None): Unit =
    (regionService.getRegion _)
      .expects(geoId)
      .returningZ(result)

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
