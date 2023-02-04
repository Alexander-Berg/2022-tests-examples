package ru.yandex.vertis.telepony.api

import akka.util.Timeout
import scala.concurrent.duration._

/**
  * @author evans
  */
package object service {
  implicit val timeout = Timeout(3.seconds)

}
