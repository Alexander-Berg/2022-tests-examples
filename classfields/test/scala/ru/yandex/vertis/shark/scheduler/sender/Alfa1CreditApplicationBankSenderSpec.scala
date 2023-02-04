package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.AlfaBankClient
import ru.yandex.vertis.shark.client.bank.converter.AlfaBankConverter
import ru.yandex.vertis.shark.client.bank.dictionary.alfa.{AlfaBankDictionary, StaticAlfaBankResource}
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.AlfaCreditApplicationBankSenderBase.AlfaBankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import common.id.IdGenerator
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.test.environment.TestEnvironment
import zio.{RLayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Alfa1CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Alfa1CreditApplicationBankSender.CreditProductId

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

  override protected def testCases: Seq[TestCases.TestCase] = customTestCases ++ super.testCases

  private val customTestCases: Seq[TestCases.TestCase] = Seq(
    TestCases.testIsSome(
      name = "income proof without 2NDFL",
      builder = _.setIncomeProof(proto.Block.IncomeBlock.IncomeProof.WITHOUT)
    ),
    TestCases.testIsSome(
      name = "income proof with 2NDFL",
      builder = _.setIncomeProof(proto.Block.IncomeBlock.IncomeProof.BY_2NDFL)
    )
  )
}
