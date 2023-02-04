package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.SravniRuClient
import ru.yandex.vertis.shark.client.bank.converter.SravniRuConverter
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.SravniRu1Sender.SravniRuSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import zio.test.environment.TestEnvironment
import zio.{RLayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Sravni1CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = SravniRu1Sender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {

    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.sravniRuClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.sravniRuClient) >>> SravniRuClient.live
    val converterLayer = SravniRuConverter.live
    val senderLayer: URLayer[SravniRuSenderEnvironment, CreditApplicationBankSender] = (for {
      bankClient <- ZIO.service[SravniRuClient.Service]
      converter <- ZIO.service[SravniRuConverter.Service]
    } yield new SravniRu1Sender(bankClient, converter)).toLayer
    clientLayer ++ converterLayer >>> senderLayer
  }

}
