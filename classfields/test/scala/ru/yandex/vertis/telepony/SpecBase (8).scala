package ru.yandex.vertis.telepony

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * Base for all specs for avoid 'extends' same things in each spec.
  *
  * @author dimas
  */
trait SpecBase extends AnyWordSpecLike with Matchers with ScalaFutures with BeforeAndAfterEach {
  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  private val DefaultPatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Milliseconds))
}
