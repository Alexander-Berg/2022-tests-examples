package ru.yandex.vertis.shark.client.bank

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging._
import io.circe.syntax.EncoderOps
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.RaiffeisenBankConverter
import ru.yandex.vertis.shark.client.bank.converter.RaiffeisenBankConverter.Source
import ru.yandex.vertis.shark.client.bank.converter.impl.raiffeizen.ConvertJson
import ru.yandex.vertis.shark.client.bank.data.raiffeizen.Responses
import ru.yandex.vertis.shark.client.bank.data.raiffeizen.Responses.{DecisionNotFound, DecisionSuccess}
import ru.yandex.vertis.shark.client.bank.dictionary.raiffeizen.{RaiffeisenBankDictionary, StaticRaiffeisenBankResource}
import ru.yandex.vertis.shark.config.RaiffeisenBankClientConfig
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.model.{SenderConverterContext, Tag}
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.TestAspect.ignore
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Task, ZIO, ZLayer}

import scala.concurrent.duration.DurationInt

object RaiffeisenBankClientSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with ConvertJson
  with StaticSamples {

  private lazy val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private lazy val raiffeisenBankClientConfig = RaiffeisenBankClientConfig(
    HttpClientConfig(
      url = "https://api.raiffeisen.ru",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    )
  )

  private lazy val bankHttpClientLayer =
    Blocking.live ++ ZLayer.succeed(raiffeisenBankClientConfig.http) >>> HttpClient.blockingLayer

  private lazy val raiffeisenBankConverterLayer =
    ZLayer.succeed[Resource[Any, RaiffeisenBankDictionary.Service]](new StaticRaiffeisenBankResource) >>>
      RaiffeisenBankConverter.liveAny

  private lazy val raiffeisenBankClientLayer =
    bankHttpClientLayer ++ ZLayer.succeed(raiffeisenBankClientConfig) >>> RaiffeisenBankClient.live

  private lazy val env = Clock.any ++ raiffeisenBankConverterLayer ++ raiffeisenBankClientLayer

  private val autoruCreditApplication = sampleAutoruCreditApplication()
  private val claimId = "f961c29971ddcc5f4e2e998f07d67b76".taggedWith[Tag.CreditApplicationClaimId]
  private val bankClientContext = BankClientContext(autoruCreditApplication, claimId)

  def spec: ZSpec[TestEnvironment, Any] =
    suite("RaiffeisenBankClient")(
      testM("convert") {
        val res = for {
          converter <- ZIO.service[RaiffeisenBankConverter.Service]
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          autoruCreditApplication <- Task.succeed(sampleCreditApplication)
          claimId = "claimId-test-1111".taggedWith[Tag.CreditApplicationClaimId]
          converterContext = AutoConverterContext.forTest(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context, claimId)
          claim <- converter.convert(source)
        } yield {
          val json = claim.asJson
          assertTrue(json.isObject)
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("send claim") {
        val res = for {
          converter <- ZIO.service[RaiffeisenBankConverter.Service]
          client <- ZIO.service[RaiffeisenBankClient.Service]
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          autoruCreditApplication <- Task.succeed(sampleCreditApplication)
          claimId = "claimId-test-2222".taggedWith[Tag.CreditApplicationClaimId]
          converterContext = AutoConverterContext.forTest(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context, claimId)
          claim <- converter.convert(source)
          clientContext = BankClientContext(autoruCreditApplication, claimId)
          res <- client.sendClaim(claim)(clientContext)
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[Responses.CreateSuccess])
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("success decision claim") {
        val res = for {
          client <- ZIO.service[RaiffeisenBankClient.Service]
          res <- client.decisionClaim("OAPI20201207PLL002164654111060".taggedWith[Tag.CreditApplicationBankClaimId])(
            bankClientContext
          )
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[DecisionSuccess])
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("not found decision claim") {
        val res = for {
          client <- ZIO.service[RaiffeisenBankClient.Service]
          res <- client.decisionClaim("OAPI20201207PLL002164654111061".taggedWith[Tag.CreditApplicationBankClaimId])(
            bankClientContext
          )
        } yield {
          println(res)
          assertTrue(res == DecisionNotFound)
        }
        res.provideLayer(env)
      } @@ ignore
    )
}
