package ru.yandex.vertis.safe_deal.spec

import java.nio.charset.StandardCharsets

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport

import scala.io.BufferedSource

trait SpecBase
  extends AnyWordSpecLike
  with Matchers
  with ScalaCheckPropertyChecks
  with MockitoSupport
  with BeforeAndAfter
  with BeforeAndAfterAll {

  private def readResourceFile(path: String): BufferedSource =
    scala.io.Source.fromInputStream(getClass.getResourceAsStream(path), StandardCharsets.UTF_8.name())

  protected def readResourceFileAsString(path: String): String = readResourceFile(path).mkString
}
