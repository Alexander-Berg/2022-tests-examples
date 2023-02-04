package ru.yandex.vertis.moderation.client

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

/**
  * @author semkagtn
  */
trait SpecBase
  extends WordSpec
    with Matchers
    with ScalaFutures {

  private val DefaultPatienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(50, Millis))

  override implicit def patienceConfig: PatienceConfig = DefaultPatienceConfig
}
