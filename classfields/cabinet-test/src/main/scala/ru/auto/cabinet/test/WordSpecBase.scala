package ru.auto.cabinet.test

import org.scalacheck.ShrinkLowPriority
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}

import scala.concurrent.ExecutionContext

trait WordSpecBase
    extends Matchers
    with AnyWordSpecLike
    with OptionValues
    with ScalaFutures
    with PropertyChecks
    with ShrinkLowPriority {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  implicit val instr: Instr = new EmptyInstr("test")
}
