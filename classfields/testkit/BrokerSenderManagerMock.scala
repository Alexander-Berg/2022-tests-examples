package auto.c2b.lotus.logic.testkit

import auto.c2b.common.proposition.Proposition
import auto.c2b.lotus.logic.BrokerSendManager
import auto.c2b.lotus.model.Lot
import zio.{Has, Task, ULayer, ZLayer}

class BrokerSenderManagerMock extends BrokerSendManager.Service {
  override def sendLot(lot: Lot, propositions: Seq[Proposition] = Seq.empty): Task[Unit] = Task.unit
}

object BrokerSenderManagerMock {

  val live: ULayer[Has[BrokerSendManager.Service]] =
    ZLayer.succeed(new BrokerSenderManagerMock: BrokerSendManager.Service)
}
