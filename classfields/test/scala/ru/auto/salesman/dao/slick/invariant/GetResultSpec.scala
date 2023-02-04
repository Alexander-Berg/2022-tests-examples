package ru.auto.salesman.dao.slick.invariant

import ru.auto.salesman.dao.slick.invariant.GetResultSpec._
import ru.auto.salesman.test.{BaseSpec, DummyResultSet}

import scala.slick.{jdbc => slick}

class GetResultSpec extends BaseSpec {

  "GetResult" should {

    "ignore subtype implicit" in {
      implicit val getResultT: GetResult[T] = GetResult[T] {
        _.nextString() match {
          case "a" => A
          case "b" => B
        }
      }
      implicit val getResultA: GetResult[A.type] = GetResult[A.type] {
        _.nextString() match {
          case "a" => A
        }
      }
      // чтобы компилятор не ругался на неиспользуемый getResultA -- он нужен
      // здесь для проверки, что GetResult работает инвариантно
      implicitly[GetResult[A.type]]
      val rs = new DummyResultSet {
        override def getString(columnIndex: Int): String = "b"
      }
      val pp = new PositionedResult(
        new slick.PositionedResult(rs) {
          def close(): Unit = ()
        }
      )
      implicitly[GetResult[T]].apply(pp) shouldBe B
    }
  }
}

object GetResultSpec {

  sealed trait T
  case object A extends T
  case object B extends T
}
