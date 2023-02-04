package ru.yandex.vertis.feedprocessor

import org.scalacheck.ShrinkLowPriority
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

import scala.concurrent.ExecutionContext

trait AsyncWordSpecBase
  extends Matchers
  with AsyncWordSpecLike
  with OptionValues
  with ScalaFutures
  with ShrinkLowPriority {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

}
