package ru.yandex.auto.vin.decoder.hydra

import auto.carfax.common.clients.hydra.HydraClient
import auto.carfax.common.utils.tracing.Traced

import scala.concurrent.Future

class HydraClientStub(maxClicks: Int) extends HydraClient {

  var curHits = 0

  override def getRemainingHits(component: String, userId: String)(implicit t: Traced): Future[Int] =
    Future.successful(maxClicks - curHits)
  override def getClicker(component: String, id: String)(implicit t: Traced): Future[Int] = Future.successful(curHits)

  override def incClicker(component: String, id: String)(implicit t: Traced): Future[Unit] = {
    this.curHits = curHits + 1
    Future.unit
  }
}
