package ru.yandex.vertis.telepony.util.tskv

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.Status
import ru.yandex.vertis.telepony.util.tskv.TskvUtilsSpec.{Complex, Middle, Simple}

import scala.concurrent.duration._

/**
  * @author evans
  */
class TskvUtilsSpec extends Matchers with AnyWordSpecLike with SimpleLogging {

  import TskvUtils._

  "TskvUtils" should {
    "convert simple to kv" in {
      val obj = Simple(1, 2.4)
      val actual = toKv("obj", obj).toSet
      val expected = Set("obj.a=1", "obj.b=2.4")
      actual shouldEqual expected
    }
    "convert middle to kv" in {
      val obj = Middle(Simple(1, 2), "1234")
      val actual = toKv("ob", obj).toSet
      val expected = Set("ob.s.a=1", "ob.s.b=2.0", "ob.str=1234")
      actual shouldEqual expected
    }
    "convert complex to kv" in {
      val obj = Complex(Array(Middle(Simple(1, 2), "1234")), Map("1" -> "23"))
      val actual = toKv("ob", obj).toSet
      val expected = Set("ob.map.1=23", "ob.m@0.str=1234", "ob.m@0.s.b=2.0", "ob.m@0.s.a=1")
      actual shouldEqual expected
    }
    "not fail" in {
      val obj = Map(1 -> 2)
      toKv("ob", obj)
    }
    "convert finite duraiton" in {
      val actual = toKv("ob", 10.seconds).toSet
      val expected = Set("ob=10000")
      actual shouldEqual expected
    }
    "not provide None" in {
      toKv("ob", None) shouldEqual Iterable.empty
    }
  }

  "TskvUtilsSpecific" should {
    "handle status" in {
      val time = DateTime.parse("2016-02-25T15:45:07.146+03:00")
      val expected = Set(
        "ob.value=Ready",
        "ob.deadline=2016-02-25T15:45:07.146+03:00",
        "ob.updateTime=2016-02-25T15:45:07.146+03:00"
      )
      val actual = toKv("ob", Status.Ready(Some(time), time)).toSet
      actual shouldEqual expected
    }
  }
}

object TskvUtilsSpec {

  case class Simple(a: Int, b: Double)

  case class Middle(s: Simple, str: String)

  case class Complex(m: Array[Middle], map: Map[String, String])

}
