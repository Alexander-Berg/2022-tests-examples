package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.RosgosstrahBankClient
import ru.yandex.vertis.shark.client.bank.converter.RosgosstrahBankConverter
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.Rosgosstrah1CreditApplicationBankSender.RosgosstrahBankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import common.id.IdGenerator
import zio.test.environment.TestEnvironment
import zio.{RLayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Rosgosstrah1CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Rosgosstrah1CreditApplicationBankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {
    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.rosgosstrahBankClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.rosgosstrahBankClient) >>> RosgosstrahBankClient.live

    val converterLayer = IdGenerator.snowflake ++ dadataLayer >>> RosgosstrahBankConverter.live

    val senderLayer: URLayer[RosgosstrahBankSenderEnvironment, CreditApplicationBankSender] = (for {
      bankClient <- ZIO.service[RosgosstrahBankClient.Service]
      converter <- ZIO.service[RosgosstrahBankConverter.Service]
    } yield new Rosgosstrah1CreditApplicationBankSender(bankClient, converter)).toLayer

    clientLayer ++ converterLayer >>> senderLayer
  }
}
