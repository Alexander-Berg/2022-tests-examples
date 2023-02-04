package ru.yandex.vertis.telepony

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}

trait SpecBase extends AnyWordSpecLike with Matchers with ScalaFutures with OptionValues with BeforeAndAfterEach {
  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  private val DefaultPatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Milliseconds))
}
