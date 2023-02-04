package ru.yandex.vertis.shark.scheduler.sender

import com.softwaremill.tagging.Tagger
import mouse.ignore
import ru.yandex.vertis.shark.{TestSharedConfig, TmsStaticSamples}
import ru.yandex.vertis.shark.client.bank.AlfaBankClient
import ru.yandex.vertis.shark.client.bank.converter.AlfaBankConverter
import ru.yandex.vertis.shark.client.bank.dictionary.alfa.{AlfaBankDictionary, StaticAlfaBankResource}
import ru.yandex.vertis.shark.model.Tag
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.AlfaCreditApplicationBankSenderBase.AlfaBankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import common.id.IdGenerator
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.test.Assertion.isTrue
import zio.{RLayer, Task, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking
import zio.test.{assertM, DefaultRunnableSpec, TestAspect, ZSpec}
import zio.test.environment.TestEnvironment

import java.time.Instant

object ContextDeciderSpec extends DefaultRunnableSpec with TestLayers {

  override lazy val config: TestSharedConfig = TestSharedConfig.local

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {
    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.alfaBankClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.alfaBankClient) >>> AlfaBankClient.live
    val converterLayer = ZLayer.succeed[Resource[Any, AlfaBankDictionary.Service]](new StaticAlfaBankResource) >>>
      AlfaBankConverter.livePil
    val idGeneratorLayer = IdGenerator.snowflake
    val senderLayer: URLayer[AlfaBankSenderEnvironment, CreditApplicationBankSender] = (for {
      bankClient <- ZIO.service[AlfaBankClient.Service]
      converter <- ZIO.service[AlfaBankConverter.Service]
      idGenerator <- ZIO.service[IdGenerator.Service]
    } yield new Alfa1CreditApplicationBankSender(bankClient, converter, idGenerator)).toLayer
    clientLayer ++ converterLayer ++ idGeneratorLayer >>> senderLayer
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ContextDecider") {
      testM("source") {
        val result = for {
          decider <- ZIO.service[ConverterContextDecider.Service]
          aca = TmsStaticSamples.sampleCreditApplication
          res <- decider.source(aca, Instant.now)
          _ <- Task.effect {
            ignore(res.origin)
            ignore(res.byClaimId("56fda7d3-e297-411a-8df5-aaebd20f57fa".taggedWith[Tag.CreditApplicationClaimId]))
            ignore(res.byClaimId("dbe88b72-2154-4f4f-bbfb-0bc33543d2c6".taggedWith[Tag.CreditApplicationClaimId]))
            ignore(res.byClaimId("e81b3437-ebd0-46ab-aaa4-a43136ac9380".taggedWith[Tag.CreditApplicationClaimId]))
          }
        } yield true
        assertM(result)(isTrue).provideLayer(testLayer)
      }
    } @@ TestAspect.ignore

}
