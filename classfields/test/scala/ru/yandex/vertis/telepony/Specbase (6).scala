package ru.yandex.vertis.telepony

import org.scalatest.Inside
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * @author neron
  */
trait Specbase
  extends AnyWordSpecLike
  with Matchers
  with Inside
  with ScalaFutures
  with Eventually
  with ScalaCheckDrivenPropertyChecks {

//  System.setProperty("config.resource", "application-test.conf")

  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  private val DefaultPatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Milliseconds))
}
