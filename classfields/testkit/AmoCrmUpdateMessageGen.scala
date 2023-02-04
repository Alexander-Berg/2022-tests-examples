package auto.dealers.amoyak.storage.testkit

import auto.dealers.amoyak.model.{AmoMessageType, ClientType, UnknownMessage}
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia._
import auto.dealers.amoyak.storage.testkit.zonedDateTimeGen

object AmoCrmUpdateMessageGen {

  val clientTypeGen: Gen[Random, ClientType] = Gen.oneOf(Gen.const(ClientType.Client), Gen.const(ClientType.Agency))
  // FIXME
//  val amoCrmUpdateMessageGen: Gen[Random with Sized, AmoCrmUpdateMessage with AmoMessageType] =
//    for {
//      message <- DeriveGen[AmoCrmUpdateMessage]
//      clientType <- clientTypeGen
//    } yield new AmoCrmUpdateMessage(
//      clientId = message.clientId.copy(clientType = clientType),
//      main = message.main,
//      tariffs = message.tariffs,
//      loyalty = message.loyalty,
//      moderation = message.moderation,
//      finance = message.finance,
//      event = message.event,
//      expenses = message.expenses,
//      offers = message.offers,
//      timestamp = message.timestamp
//    ) with UnknownMessage
}
