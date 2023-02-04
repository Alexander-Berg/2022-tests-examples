package ru.yandex.vertis.tvm

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.io.Source

/**
  * @author alex-kovalenko
  */
trait TvmSpecBase extends AnyWordSpecLike with Matchers {
  val SelfClientId: ServiceId = 2000446
  val ClientSecret: Secret = "DULvoHr3tooV6qeufXuglQ"

  def load(name: String): String =
    Source.fromInputStream(this.getClass.getResourceAsStream(name)).mkString

}
