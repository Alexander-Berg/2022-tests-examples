package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.PsbClient
import ru.yandex.vertis.shark.client.bank.PsbClient.PsbClient
import ru.yandex.vertis.shark.client.bank.converter.PsbConverter
import ru.yandex.vertis.shark.client.bank.converter.PsbConverter.PsbConverter
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import zio.test.environment.TestEnvironment
import zio.{RLayer, URLayer, ZLayer, ZManaged}
import zio.blocking.Blocking

class Psb1CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Psb1CreditApplicationBankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {

    val cfg = config.psbClientConfig

    val clientLayer =
      ZLayer.requires[Blocking] ++
        ZLayer.succeed(cfg.http) >>>
        HttpClient.blockingLayer ++
        ZLayer.succeed(cfg) >>>
        PsbClient.live

    val converterLayer = ZLayer.succeed(cfg) >>> PsbConverter.live

    val senderLayer: URLayer[PsbClient with PsbConverter, CreditApplicationBankSender] = (for {
      client <- ZManaged.service[PsbClient.Service]
      converter <- ZManaged.service[PsbConverter.Service]
    } yield new Psb1CreditApplicationBankSender(client, converter)).toLayer

    clientLayer ++ converterLayer >>> senderLayer
  }
}
