package ru.yandex.auto.extdata.jobs.feeds.feed.writers.cars.utils.criteo

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers._
import ru.yandex.auto.message.CarAdSchema.CarAdMessage

import scala.collection.JavaConverters._
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class CarAdCriteoTagFieldBuilderSpec extends FlatSpecLike with Matchers {

  private def generateCarAd(tags: Iterable[String] = Iterable.empty): CarAdMessage = {
    CarAdMessage
      .newBuilder()
      .setVersion(Random.nextInt)
      .addAllSearchTags(tags.asJava)
      .build()
  }

  "CriteoTagFieldBuilder" should "build empty tag if carAd has no tags" in {
    val noTagsCarAd = generateCarAd()
    CarAdCriteoTagFieldBuilder.getTagField(noTagsCarAd) shouldEqual None
  }

  it should "generate correct tag even if tags inside carAd are random" in {
    val orderedTagMapping = CarAdCriteoTagFieldBuilder.auto2CriteoTagsOrdered
    val testGenerator = for {
      numOfTags <- Gen.choose(0, orderedTagMapping.size)
      randomIndices = Random.shuffle(orderedTagMapping.indices.toList).take(numOfTags)
      shuffledAutoTags = randomIndices.map(orderedTagMapping(_)._1)
      criteoTag = randomIndices.sorted.map(orderedTagMapping(_)._2).headOption
      carAd = generateCarAd(shuffledAutoTags)
    } yield (carAd, criteoTag)

    check {
      forAll(testGenerator) {
        case (carAd, tag) =>
          CarAdCriteoTagFieldBuilder.getTagField(carAd) == tag
      }
    }
  }
}
