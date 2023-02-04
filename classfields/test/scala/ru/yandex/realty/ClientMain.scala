package ru.yandex.realty

import _root_.akka.actor.ActorSystem
import ru.yandex.realty.extdata.ClientBuilder
import ru.yandex.extdata.core.DataType
import ru.yandex.realty.extdata.ClientDataSpec

import scala.concurrent.duration._

/**
  * @author evans
  */
object ClientMain extends App {

  val controller = new ClientBuilder()
    .setRemoteUrl("http://localhost:1234")
    .setSystem(ActorSystem("extdata"))
    .setExtDataPath("extdata")
    .setReplicatePeriod(5.seconds)
    .build()
  controller.extDataService.register(ClientDataSpec(DataType("agregate_building_info", 4)))
  controller.start()
}
