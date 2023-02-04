package amogus.logic.webhook

import amogus.logic.AmoClientMock
import amogus.model.AmogusConfig
import amogus.model.AmogusConfig.{AmogusCredentialsConfig, AmogusServiceConfig}
import amogus.model.ValueTypes.{AccessToken, IntegrationId, RefreshToken, ServiceId, ServiceName}
import amogus.model.webhook.Webhook
import ru.yandex.vertis.amogus.model.AmoEventType
import ru.yandex.vertis.amogus.model.AmoEventType._
import amogus.storage.amoClient.AmoClient.AmoClient
import amogus.storage.tokenRepository.DefaultTokenRepository
import amogus.storage.tokenRepository.TokenRepository.TokenRepository
import common.zio.logging.Logging
import common.zio.redis.sentinel.RedisSentinelClient.RedisSentinelClient
import common.zio.redis.sentinel.testkit.InMemoryRedisSentinelClient
import common.zio.sttp.endpoint.Endpoint
import zio.magic._
import zio.test.Assertion.{equalTo, isUnit}
import zio.test.TestAspect.{after, sequential}
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.unit
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.{Has, ULayer, URLayer, ZIO, ZLayer}

import java.util.UUID

object WebhookManagerSpec extends DefaultRunnableSpec {

  private val uuid = UUID.fromString("1f5c13dc-a793-47cd-b616-8def01003244")
  private val testEndpoint = Endpoint(host = "autorutesting.amocrm.ru", port = 443, schema = "https")
  private val accessToken = AccessToken("some_token")
  private val serviceName = ServiceName("some-service")
  private val listenEvents = Set[AmoEventType](ADD_LEAD, DELETE_CUSTOMER)
  val topic = "topic"

  private val config = AmogusConfig(
    Seq(
      AmogusServiceConfig(
        serviceId = ServiceId(uuid),
        serviceName = serviceName,
        host = testEndpoint,
        topic = topic,
        webhooks = listenEvents,
        credentials = Seq(
          AmogusCredentialsConfig(
            integrationId = IntegrationId(uuid),
            clientSecret = "EmYUk7ygjjD6mxNwSL3nFEuWX6igoaHAKRWCHQLbaoWDG0J6IJG4UlUmV7GuBaaa",
            forceRefreshToken = Some(RefreshToken("force_refresh_token"))
          )
        ),
        robotManagerEmail = None
      )
    )
  )

  private val externalAddress = Endpoint(
    host = "ext.host",
    port = 123
  )

  private val testWebhook = Webhook(
    // corresponds to amogus.api.http.routes.webhook.WebhookHandler.webhookHandlerEndpoint
    destination = s"${externalAddress.toString}/api/services/${uuid.toString}/webhooks",
    settings = listenEvents.toSeq
  )

  private val amoClientUpsertWebhook =
    AmoClientMock.UpsertWebhook(equalTo((testEndpoint, accessToken, testWebhook)), unit)

  type CustomTestLayer = Has[WebhookManager] with RedisSentinelClient with Has[InMemoryRedisSentinelClient]

  type SharedEnv =
    Has[AmogusConfig]
      with TokenRepository
      with Has[Endpoint]
      with RedisSentinelClient
      with Has[InMemoryRedisSentinelClient]
      with Logging.Logging

  private def addMocks(amoClientMock: ULayer[AmoClient]): URLayer[SharedEnv, CustomTestLayer] =
    ZLayer.fromSomeMagic[SharedEnv, CustomTestLayer](
      amoClientMock,
      WebhookManagerLive.layer
    )

  val sharedLayer: ULayer[SharedEnv] = ZLayer.fromMagic[SharedEnv](
    Logging.live,
    ZLayer.succeed(config),
    ZLayer.succeed(externalAddress),
    InMemoryRedisSentinelClient.test,
    DefaultTokenRepository.live
  )

  override def spec: ZSpec[TestEnvironment, Any] = (suite("WebhookManager")(
    testM("upserts webhook from config") {
      for {
        redis <- ZIO.service[InMemoryRedisSentinelClient]
        _ <- redis.set(s"access-key:$uuid".getBytes, accessToken.value.getBytes, None)
        res <- WebhookManager(_.syncWebhookConfigs())
          .provideLayer(addMocks(amoClientUpsertWebhook))
      } yield assert(res)(isUnit)
    }
  )
    @@ after {
      for {
        redis <- ZIO.service[InMemoryRedisSentinelClient]
        _ <- redis.clear
      } yield ()
    }
    @@ sequential)
    .provideCustomLayerShared(sharedLayer)
}
