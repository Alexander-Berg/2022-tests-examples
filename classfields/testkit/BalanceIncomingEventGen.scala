package auto.dealers.amoyak.storage.testkit

import auto.dealers.amoyak.model._
import auto.dealers.amoyak.model.blocks.Finance
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia.DeriveGen
import auto.dealers.amoyak.storage.testkit.zonedDateTimeGen

object BalanceIncomingEventGen {
  val eventGen: Gen[Random with Sized, Event] = DeriveGen[Event]
  val clientIdGen: Gen[Random with Sized, AmoClientId] = DeriveGen[AmoClientId]
  // FIXME
//  val financeGen: Gen[Random with Sized, Finance] = DeriveGen[Finance]
//  val balanceIncomingEventGen: Gen[Random with Sized, BalanceIncomingEvent with AmoMessageType] = for {
//    event <- eventGen
//    clientId <- clientIdGen
//    finance <- financeGen
//    timestamp <- Gen.anyInstant.map(_.toString)
//  } yield BalanceIncomingEvent(event, clientId, finance, timestamp)
}
