package ru.yandex.vertis.telepony.api

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.BeforeAndAfterEach

/**
  * Base for all specs for avoid 'extends' same things in each spec.
  *
  * @author dimas
  */
trait SpecBase
  extends AnyWordSpecLike
  with Matchers
  with ScalaFutures
  with BeforeAndAfterEach
  with ScalaCheckDrivenPropertyChecks {
//  System.setProperty("config.resource", "application.test.conf")

  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  private val DefaultPatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Milliseconds))
}
