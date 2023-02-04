package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.model.ShortCarAd
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.sort.AdsFixture

import java.util
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ShortCaradSpec extends WordSpec with Matchers with AdsFixture with CommonFixtures {

  private val bucketSize = 1000

  def genList[T](g: Gen[T]): util.List[T] = {
    Gen.listOfN(bucketSize, g).map(_.toBuffer.asJava).sample.get
  }

  val privatesUsed = genList(genShortCarAdMessage())

  "should work" in {
    println(ShortCarAd.fromMessage(genShortCarAdMessage().sample.get))
  }
}
