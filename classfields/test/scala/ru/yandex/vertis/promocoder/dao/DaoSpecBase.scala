package ru.yandex.vertis.promocoder.dao

import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.promocoder.WordSpecBase

/** Basic trait for all dao specs
  *
  * @author alex-kovalenko
  */
trait DaoSpecBase extends WordSpecBase {

  /** Default 150 millis is too small for database tests
    */
  implicit override val patienceConfig = PatienceConfig(scaled(Span(5, Seconds)))

}
