package ru.yandex.vertis.moderation.client

/**
  * Parameters for testing moderation client
  *
  * @author semkagtn
  */
trait TestParams {

  /**
    * Client host
    */
  val host = "alesavin-01-sas.dev.vertis.yandex.net"

  /**
    * Client port
    */
  val port = 37158
}

object TestParams extends TestParams
