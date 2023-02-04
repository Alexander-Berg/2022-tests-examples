package ru.yandex.vertis.chat.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

trait HttpConfig {

//  val TargetHost = "localhost"
//  val TargetPort = 1610
  val TargetHost = "chat-api-auto-server.vrts-slb.test.vertis.yandex.net"
  val TargetPort = 80

  val httpConf =
    http
      .baseUrl(s"http://$TargetHost:$TargetPort/api/1.x/auto/")
      .userAgentHeader("Gatling")
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")
      .shareConnections
}
