package ru.yandex.vertis.tskv.writer

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.tskv.{KvPrinter, TskvWriter}
import ru.yandex.vertis.tskv.writer.TskvWriterSpec.{A, B}

/**
  * Specs on [[TskvWriter]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class TskvWriterSpec
  extends Matchers
  with WordSpecLike
  with BasicTskvWriters
  with StandardTskvWriters
  with CollectionTskvWriters
  with ProductTskvWriters {

  import ru.yandex.vertis.tskv.writeAny

  implicit def aWriter = productTskvWriter1(A.apply)
  implicit def bWriter = productTskvWriter2(B.apply)

  "TskvWriter" should {
    "convert to tskv" in {
      println(Iterable("1", "2").toTskv)
      println(A("1").toTskv)
      println(B("1", 2).toTskv)
      println(KvPrinter(B("1", 2).toTskv, Some("b")).mkString("\t"))
    }
    /*
    "convert heavy object" in {
      val header = CampaignHeaderGen.next
      val tskv = header.toTskv
      KvPrinter(tskv, Some("header")).foreach(println)
    }
     */
  }

}

object TskvWriterSpec {

  sealed trait TestTrait {
    def field: String
  }
  case class A(field: String) extends TestTrait
  case class B(field: String, i: Int) extends TestTrait
}
