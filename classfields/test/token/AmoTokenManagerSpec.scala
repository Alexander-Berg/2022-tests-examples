package amogus.logic.token

import amogus.logic.AmoClientMock
import amogus.logic.token.AmoTokenManager.AmoTokenManager
import amogus.model.AmogusConfig
import amogus.model.AmogusConfig.{AmogusCredentialsConfig, AmogusServiceConfig}
import amogus.model.ValueTypes.{AccessToken, IntegrationId, RefreshToken, ServiceId, ServiceName}
import amogus.model.token._
import amogus.storage.tokenRepository.DefaultTokenRepository
import amogus.storage.tokenRepository.TokenRepository.RefreshTokenEmpty
import common.zio.features.Features
import common.zio.features.Features.Features
import common.zio.logging.Logging
import common.zio.redis.sentinel.RedisSentinelClient.RedisSentinelClient
import common.zio.redis.sentinel.testkit.InMemoryRedisSentinelClient
import common.zio.sttp.endpoint.Endpoint
import ru.yandex.vertis.amogus.model.AmoEventType
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import zio.{Has, ZIO, ZLayer}
import zio.magic._
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test.TestAspect.{after, sequential}
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._

import java.util.UUID
import scala.concurrent.duration.{FiniteDuration, SECONDS}

//noinspection TypeAnnotation
object AmoTokenManagerSpec extends DefaultRunnableSpec {

  val refreshToken = RefreshToken("refresh_token")
  val forceRefreshToken = Some(RefreshToken("force_refresh_token"))
  val accessToken = AccessToken("access_token")
  val uuid = UUID.fromString("1f5c13dc-a793-47cd-b616-8def01003244")
  val integrationId = IntegrationId(uuid)
  val topic = "topic"

  val testCredentials = AmogusCredentialsConfig(
    integrationId,
    clientSecret = "EmYUk7ygjjD6mxNwSL3nFEuWX6igoaHAKRWCHQLbaoWDG0J6IJG4UlUmV7GuBaaa",
    forceRefreshToken = forceRefreshToken
  )

  val testServiceHost = "autorutesting.amocrm.ru"

  val testServiceConfig = AmogusServiceConfig(
    serviceId = ServiceId(uuid),
    serviceName = ServiceName("some-service"),
    topic = topic,
    host = Endpoint(host = testServiceHost, port = 443, schema = "https"),
    webhooks = Set(AmoEventType.ADD_COMPANY),
    credentials = Seq(testCredentials),
    robotManagerEmail = None
  )

  val testConfig = AmogusConfig(Seq(testServiceConfig))

  val amoClientIssueAccessToken = AmoClientMock
    .IssueAccessToken(
      anything,
      valueM { case (_: Endpoint, accessTokenRequest: AccessTokenRequest) =>
        ZIO.succeed(
          AccessTokenResponse(
            tokenType = "Bearer",
            expiresIn = FiniteDuration(1234, SECONDS),
            accessToken = accessToken,
            refreshToken = accessTokenRequest.refreshToken
          )
        )
      }
    )
    .atLeast(0) // мок пока общий для всех тестов, поэтому позволим выполняться ему любое количество раз

  val redisLayer = InMemoryRedisSentinelClient.test
  val featuresLayer = Features.liveInMemory

  type CustomTestLayer = AmoTokenManager with RedisSentinelClient with Has[InMemoryRedisSentinelClient] with Features

  val testEnv = ZLayer.fromMagic[CustomTestLayer](
    featuresLayer,
    amoClientIssueAccessToken,
    ZLayer.succeed(testConfig),
    redisLayer,
    DefaultTokenRepository.live,
    Logging.live,
    AmoTokenManager.live
  )

  override def spec: ZSpec[TestEnvironment, Any] = (suite("AmoTokenManager")(
    testM("update tokens") {
      for {
        features <- ZIO.service[Features.Service]
        _ <- features.register(s"amo_token_manager_integration_$uuid", initialValue = false)
        redis <- ZIO.service[InMemoryRedisSentinelClient]
        _ <- redis.set(s"refresh-key:$uuid".getBytes, refreshToken.value.getBytes, None)
        _ <- AmoTokenManager.updateAccessTokens()
        refreshTokenSaved <- redis.get(s"refresh-key:$uuid".getBytes)
        accessTokenSaved <- redis.get(s"access-key:$uuid".getBytes)
      } yield {
        assertTrue(refreshTokenSaved.map(new String(_)).contains(refreshToken.value)) &&
        assertTrue(accessTokenSaved.map(new String(_)).contains(accessToken.value))
      }
    },
    testM("when refresh token is empty raise error") {
      for {
        features <- ZIO.service[Features.Service]
        _ <- features.register(s"amo_token_manager_integration_$uuid", initialValue = false)
        res <- AmoTokenManager.updateAccessTokens().either
      } yield {
        assertTrue(
          res == Left(
            TokenUpdatesException(
              List(
                TokenUpdateException(integrationId, testServiceHost, RefreshTokenEmpty(integrationId))
              )
            )
          )
        )
      }
    },
    testM("force refresh token") {
      for {
        features <- ZIO.service[Features.Service]
        _ <- features.register(s"amo_token_manager_integration_$uuid", initialValue = false)
        _ <- features.updateFeature(s"amo_token_manager_integration_$uuid", true)
        redis <- ZIO.service[InMemoryRedisSentinelClient]
        _ <- AmoTokenManager.updateAccessTokens()
        refreshTokenSaved <- redis.get(s"refresh-key:$uuid".getBytes)
        accessTokenSaved <- redis.get(s"access-key:$uuid".getBytes)
      } yield {
        assertTrue(refreshTokenSaved.map(new String(_)).contains(forceRefreshToken.get.value)) &&
        assertTrue(accessTokenSaved.map(new String(_)).contains(accessToken.value))
      }
    }
  )
    @@ after {
      for {
        redis <- ZIO.service[InMemoryRedisSentinelClient]
        _ <- redis.clear
      } yield ()
    }
    @@ sequential)
    .provideCustomLayerShared(testEnv)
}
