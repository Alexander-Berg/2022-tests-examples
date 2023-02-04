package ru.yandex.vertis.shark.client.bank

import baker.common.client.dadata.{DadataClient, DadataClientConfig}
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import io.circe.syntax.EncoderOps
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.RosgosstrahBankConverter
import ru.yandex.vertis.shark.client.bank.converter.RosgosstrahBankConverter.Source
import ru.yandex.vertis.shark.client.bank.converter.impl.rosgosstrah.ConvertJson
import ru.yandex.vertis.shark.client.bank.data.rosgosstrah.Entities.RosgosstrahStepClaim
import ru.yandex.vertis.shark.client.bank.data.rosgosstrah.Responses.ClaimConditionResponse.SuccessConditionResponse
import ru.yandex.vertis.shark.client.bank.data.rosgosstrah.Responses.{ClaimStatusResponse, FormData, SendResponse}
import ru.yandex.vertis.shark.config.{AuthConfig, RosgosstrahBankClientConfig}
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.{SenderConverterContext, Tag}
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import _root_.common.id.IdGenerator
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.test.TestAspect.ignore
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.{Task, ZIO, ZLayer}

import scala.concurrent.duration.DurationInt

object RosgosstrahBankClientSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with ConvertJson
  with StaticSamples {

  private lazy val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private lazy val rosgosstrahBankClientConfig = RosgosstrahBankClientConfig(
    HttpClientConfig(
      url = "https://product-uat.rgsb.balance-pl.ru/",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    ),
    AuthConfig(
      email = "web_operator.autoru@hardkoded.mail",
      password = ""
    )
  )

  private lazy val dadataClientConfig = DadataClientConfig(
    HttpClientConfig(
      url = "https://suggestions.dadata.ru:443",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    ),
    ""
  )

  private lazy val idGeneratorLayer = Clock.live >+> Random.live >+> IdGenerator.snowflake

  private lazy val bankHttpClientLayer =
    Blocking.live ++ ZLayer.succeed(rosgosstrahBankClientConfig.http) >>> HttpClient.blockingLayer

  private lazy val dadataHttpClientBackendLayer =
    Blocking.live ++ ZLayer.succeed(dadataClientConfig.http) >>> HttpClient.blockingLayer

  private lazy val dadataClientLayer =
    dadataHttpClientBackendLayer ++ ZLayer.succeed(dadataClientConfig) >>> DadataClient.live

  private lazy val rosgosstrahBankConverterLayer =
    idGeneratorLayer ++ dadataClientLayer >+> RosgosstrahBankConverter.live

  private lazy val rosgosstrahBankClientLayer =
    bankHttpClientLayer ++ ZLayer.succeed(rosgosstrahBankClientConfig) >>> RosgosstrahBankClient.live

  private lazy val env = rosgosstrahBankConverterLayer ++ rosgosstrahBankClientLayer

  private val autoruCreditApplication = sampleAutoruCreditApplication()
  private val claimId = "f961c29971ddcc5f4e2e998f07d67b76".taggedWith[Tag.CreditApplicationClaimId]
  private val bankClientContext = BankClientContext(autoruCreditApplication, claimId)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("RaiffeisenBankClient")(
      testM("convert") {
        val res = for {
          converter <- ZIO.service[RosgosstrahBankConverter.Service]
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          autoruCreditApplication <- Task.succeed(sampleCreditApplication)
          converterContext = AutoConverterContext.forTest(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context)
          claim <- converter.convert(source)
        } yield {
          val json = claim.asJson
          assertTrue(json.isObject)
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("send step1 claim") {
        val res = for {
          autoruCreditApplication <- Task.succeed(sampleCreditApplication)
          claimId = "claimId-test-2222".taggedWith[Tag.CreditApplicationClaimId]
          clientContext = BankClientContext(autoruCreditApplication, claimId)
          client <- ZIO.service[RosgosstrahBankClient.Service]
          converter <- ZIO.service[RosgosstrahBankConverter.Service]
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          converterContext = AutoConverterContext.forTest(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context)
          claim <- converter.convert(source)
          signResponse <- client.signIn()(clientContext)
          res <- client.sendClaim(RosgosstrahStepClaim(claim.step1, formId = None))(signResponse.token, clientContext)
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[SendResponse])
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("send step2 claim") {
        val res = for {
          autoruCreditApplication <- Task.succeed(sampleCreditApplication)
          claimId = "claimId-test-2222".taggedWith[Tag.CreditApplicationClaimId]
          clientContext = BankClientContext(autoruCreditApplication, claimId)
          client <- ZIO.service[RosgosstrahBankClient.Service]
          converter <- ZIO.service[RosgosstrahBankConverter.Service]
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          converterContext = AutoConverterContext.forTest(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context)
          claim <- converter.convert(source)
          signResponse <- client.signIn()(clientContext)
          res1 <- client.sendClaim(RosgosstrahStepClaim(claim.step1, formId = None))(signResponse.token, clientContext)
          formId = formIdFromResp(res1)
          res2 <- client.sendClaim(RosgosstrahStepClaim(claim.step2, formId))(signResponse.token, clientContext)
        } yield {
          println(res2)
          assertTrue(res2.isInstanceOf[SendResponse])
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("send step3 claim") {
        val res = for {
          autoruCreditApplication <- Task.succeed(sampleCreditApplication)
          claimId = "claimId-test-2222".taggedWith[Tag.CreditApplicationClaimId]
          clientContext = BankClientContext(autoruCreditApplication, claimId)
          client <- ZIO.service[RosgosstrahBankClient.Service]
          converter <- ZIO.service[RosgosstrahBankConverter.Service]
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          converterContext = AutoConverterContext.forTest(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context)
          claim <- converter.convert(source)
          signResponse <- client.signIn()(clientContext)
          res1 <- client.sendClaim(RosgosstrahStepClaim(claim.step1, formId = None))(signResponse.token, clientContext)
          formId = formIdFromResp(res1)
          res3 <- client.sendClaim(RosgosstrahStepClaim(claim.step3, formId))(signResponse.token, clientContext)
        } yield {
          println(res3)
          assertTrue(res3.isInstanceOf[SendResponse])
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("get claim status") {
        val res = for {
          client <- ZIO.service[RosgosstrahBankClient.Service]
          signResponse <- client.signIn()(bankClientContext)
          res <- client.getStatus("claimId-test".taggedWith)(signResponse.token, bankClientContext)
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[ClaimStatusResponse])
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("get claim conditions") {
        val res = for {
          client <- ZIO.service[RosgosstrahBankClient.Service]
          signResponse <- client.signIn()(bankClientContext)
          res <- client.getConditions("claimId-test".taggedWith)(signResponse.token, bankClientContext)
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[SuccessConditionResponse])
        }
        res.provideLayer(env)
      } @@ ignore
    )
  }

  private def formIdFromResp(res: SendResponse): Option[Int] = res.data.collectFirst { case FormData(_, id, _) => id }
}
