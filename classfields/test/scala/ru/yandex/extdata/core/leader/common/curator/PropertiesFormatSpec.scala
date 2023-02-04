package ru.yandex.extdata.core.leader.common.curator

import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.extdata.core.gens.Producer._
import scala.collection.JavaConverters._

/**
  * @author evans
  */
class PropertiesFormatSpecs extends WordSpecLike with Matchers {

  val PropertiesGen: Gen[Map[String, String]] = for {
    length <- Gen.chooseNum(1, 1000)
    keys <- Gen.listOfN(length, Gen.alphaStr)
    values <- Gen.listOfN(length, Gen.alphaStr)
  } yield keys.zip(values).toMap

  "Properties" should {
    "serialized and deserialized" in {
      val map = PropertiesGen.next
      val parsed = PropertiesFormat.asProperties(PropertiesFormat.asBytes(map.asJava)).asScala
      parsed.toMap shouldEqual map
    }
  }
}
