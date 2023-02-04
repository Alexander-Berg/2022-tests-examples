package ru.yandex.vertis.billing

import scala.util.control.NoStackTrace

/**
  * @author ruslansd
  */
object Exceptions {
  case class ArtificialInternalException(msg: String = "artificial") extends RuntimeException with NoStackTrace

  case class ArtificialNoSuchElementException(msg: String = "artificial")
    extends NoSuchElementException(msg)
    with NoStackTrace
}
