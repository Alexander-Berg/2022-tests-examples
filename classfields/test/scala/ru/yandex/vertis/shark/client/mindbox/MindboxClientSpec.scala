package ru.yandex.vertis.shark.client.mindbox

import java.time.Instant
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.client.mindbox.MindboxClient._
import ru.yandex.vertis.shark.config.MindboxClientConfig
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.AutoUser
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import zio.test.Assertion.{equalTo, isUnit}
import zio.test.TestAspect.{ignore, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.ZLayer
import zio.blocking.Blocking

import scala.concurrent.duration._

object MindboxClientSpec extends DefaultRunnableSpec {

  private val proxyConfig: ProxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private val clientConfig: MindboxClientConfig = MindboxClientConfig(
    HttpClientConfig(
      url = "https://api.mindbox.ru:443",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    ),
    "autoru",
    "[... paste secret key here ...]"
  )

  private lazy val httpClientBackendLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(clientConfig.http) >>> HttpClient.blockingLayer

  private lazy val clientLayer = httpClientBackendLayer ++ ZLayer.succeed(clientConfig) >>> MindboxClient.live

  private val creditApplicationId: CreditApplicationId =
    "some-credit-application-id".taggedWith[Tag.CreditApplicationId]

  private val creditProductId: CreditProductId =
    "some-credit-product-id".taggedWith[Tag.CreditProductId]

  private val user = AutoUser("444".taggedWith[zio_baker.Tag.UserId])

  private val event: Event = Event(
    customer = Event.Customer(
      ids = Event.Customer.Ids(autoruUserId = user.id, clientRequestId = creditApplicationId.some),
      lastName = "Фамилия",
      firstName = "Имя",
      middleName = "Отчество",
      email = "mailbox@host.com",
      mobilePhone = "+79991234567",
      subscriptions = Seq(
        Event.Customer.Subscription(pointOfContact = Event.Customer.Subscription.PointOfContact.Email),
        Event.Customer.Subscription(pointOfContact = Event.Customer.Subscription.PointOfContact.Sms)
      )
    ),
    order = Event.Order(
      ids = Event.Order.Ids(requestId = creditApplicationId),
      customFields = Event.Order.CustomFields(
        requestEnv = "testing",
        requestOfferUrl =
          "https://auto.ru/cars/new/group/mercedes/s_klasse_amg/21017880/21017885/1102223820-0841a5b5/".some,
        requestPriceRub = 16794800L.some,
        requestMark = "Mercedes".some,
        requestModel = "S-Klasse AMG".some,
        requestMaxAmount = 15000000L.some,
        requestInitialFee = 5000000L.some,
        requestTermMonths = 36.some,
        requestGeobaseIds = Seq(1L),
        requestState = proto.CreditApplication.State.ACTIVE,
        requestCompletnessState = proto.CreditApplication.Communication.AutoruExternal.CompletenessState.MINIMUM,
        requestObjectState = proto.CreditApplication.Communication.AutoruExternal.ObjectCommunicationState.SELECTED,
        photo320x240Url = "http://host.com/photo/1.jpg".some,
        photo1200x900Url = None
      ),
      lines = Seq(
        Event.Order.Line(
          product = Event.Order.Line.ProductItem(
            ids = Event.Order.Line.ProductItem.Ids(
              creditProductId = creditProductId
            )
          ),
          status = proto.CreditApplication.Communication.AutoruExternal.ClaimCommunicationState.SENT.toString
        )
      )
    ),
    executionDateTimeUtc = Instant.now()
  )

  def spec: ZSpec[TestEnvironment, Any] =
    suite("MindboxClient")(
      testM("websiteCartUpdate") {
        assertM(MindboxClient.websiteCartUpdate(event))(isUnit).provideLayer(clientLayer)
      },
      testM("unsubscribe") {
        assertM(MindboxClient.unsubscribe(user, proto.Subscription.Channel.PUSH))(isUnit)
          .provideLayer(clientLayer)
      },
      testM("subscribe") {
        assertM(MindboxClient.subscribe(user, proto.Subscription.Channel.EMAIL))(isUnit)
          .provideLayer(clientLayer)
      },
      testM("subscriptions") {
        import proto.Subscription.Channel._
        val expected = Seq(
          Subscription(channel = ALL, isSubscribed = true),
          Subscription(channel = CALL, isSubscribed = true),
          Subscription(channel = SMS, isSubscribed = true),
          Subscription(channel = EMAIL, isSubscribed = true),
          Subscription(channel = MESSAGE, isSubscribed = true),
          Subscription(channel = PUSH, isSubscribed = false)
        )
        assertM(MindboxClient.subscriptions(user))(equalTo(expected))
          .provideLayer(clientLayer)
      }
    ) @@ sequential @@ ignore
}
