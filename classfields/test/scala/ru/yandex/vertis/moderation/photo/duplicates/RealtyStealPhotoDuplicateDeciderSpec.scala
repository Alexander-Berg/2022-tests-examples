package ru.yandex.vertis.moderation.photo.duplicates

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{OfferTypeGen, _}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Instance, RealtyEssentials}
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Model.Visibility
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials.{CategoryType, OfferType}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration.Inf

/**
  * @author akhazhoyan 03/2019
  */
@RunWith(classOf[JUnitRunner])
class RealtyStealPhotoDuplicateDeciderSpec extends CvHashBasedPhotoDuplicateDeciderSpecBase {
  override val resourceSchemaFileName: String = "/cv-hashes.sql"

  def newDecider()(implicit tc: TestContext): RealtyStealPhotoDuplicateDecider =
    new RealtyStealPhotoDuplicateDecider(
      instanceDao = tc.instanceDao,
      featureRegistry = tc.featureRegistry,
      minIntersection = tc.minIntersection,
      cvHashDao = tc.cvHashDao
    )

  def newEssentials(createTime: DateTime, cvHashes: Seq[String]): RealtyEssentials = {
    val photos =
      cvHashes.map { cvHash =>
        RealtyPhotoInfoGen.next.copy(cvHash = Some(cvHash), deleted = Some(false))
      }
    RealtyEssentialsGen.next.copy(
      photos = photos,
      timestampCreate = Some(createTime),
      offerType = Some(OfferType.RENT),
      categoryType = Some(CategoryType.APARTMENT)
    )
  }

  def newInstance(
      createTime: DateTime,
      cvHashes: Seq[String],
      clusterId: Option[Long],
      visibility: Visibility = Visibility.VISIBLE,
      signals: SignalSet = SignalSet.Empty
  )(implicit tc: TestContext): Instance = {

    tc.createInstance(
      createTime,
      newEssentials(createTime, cvHashes).copy(clusterId = clusterId),
      visibility = visibility,
      signals = signals
    )

  }

  "decide" should {
    "return duplicates for duplicates from different cluster" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyStealPhotoDuplicateDecider = newDecider()

        val instance1: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2), Some(1))
        val instance2: Instance = newInstance(yesterday, Seq(cvHash1, cvHash3), Some(1))
        val sample: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3), Some(0))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] =
          Some(
            Duplicate.fromDuplicates(
              duplicates = Set(instance1, instance2),
              info = decider.name,
              action = Warn,
              auxInfo = None,
              hashes = Seq(cvHash1, cvHash2, cvHash3)
            )
          )
        actualResult shouldBe expectedResult
      }
    }

    "return duplicates if sample has no cluster" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyStealPhotoDuplicateDecider = newDecider()

        val instance1: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2), Some(1))
        val instance2: Instance = newInstance(yesterday, Seq(cvHash1, cvHash3), Some(1))
        val sample: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3), None)

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] =
          Some(
            Duplicate.fromDuplicates(
              duplicates = Set(instance1, instance2),
              info = decider.name,
              action = Warn,
              auxInfo = None,
              hashes = Seq(cvHash1, cvHash2, cvHash3)
            )
          )
        actualResult shouldBe expectedResult
      }
    }

    "return duplicates if duplicates have no cluster" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyStealPhotoDuplicateDecider = newDecider()

        val instance1: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2), None)
        val instance2: Instance = newInstance(yesterday, Seq(cvHash1, cvHash3), None)
        val sample: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3), Some(0))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] =
          Some(
            Duplicate.fromDuplicates(
              duplicates = Set(instance1, instance2),
              info = decider.name,
              action = Warn,
              auxInfo = None,
              hashes = Seq(cvHash1, cvHash2, cvHash3)
            )
          )
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if duplicates are from the same cluster" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyStealPhotoDuplicateDecider = newDecider()

        val instance1: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2), Some(0))
        val instance2: Instance = newInstance(yesterday, Seq(cvHash1, cvHash3), Some(0))
        val sample: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3), Some(0))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return None if needCheck does not hold" in {
      val wrongOfferTypeGen: Gen[Option[OfferType]] = Gen.option(OfferTypeGen.suchThat(_ != OfferType.RENT))
      val wrongCategoryTypeGen: Gen[Option[CategoryType]] =
        Gen.option(CategoryTypeGen.suchThat(_ != CategoryType.APARTMENT))

      new TestContext(minIntersection = 2) {
        val decider: RealtyStealPhotoDuplicateDecider = newDecider()

        val instance1: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2), Some(1))
        val instance2: Instance = newInstance(yesterday, Seq(cvHash1, cvHash3), Some(1))
        val essentials: RealtyEssentials =
          newEssentials(today, Seq(cvHash1, cvHash2, cvHash3)).copy(clusterId = Some(0))
        val samples: Seq[Instance] =
          Seq(
            createInstance(today, essentials.copy(offerType = wrongOfferTypeGen.next)),
            createInstance(today, essentials.copy(categoryType = wrongCategoryTypeGen.next)),
            createInstance(
              today,
              essentials.copy(offerType = wrongOfferTypeGen.next, categoryType = wrongCategoryTypeGen.next)
            )
          )

        val expectedResult: Option[Verdict] = None
        for (sample <- samples) {
          val actualResult: Option[Verdict] = decider.decide(sample).futureValue
          actualResult shouldBe expectedResult
        }
      }
    }

    "return NotDuplicate if a duplicate is not VISIBLE" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyStealPhotoDuplicateDecider = newDecider()

        newInstance(yesterday, Seq(cvHash1, cvHash2), Some(1), visibility = Visibility.BLOCKED)
        newInstance(yesterday, Seq(cvHash1, cvHash3), Some(1), visibility = Visibility.INACTIVE)

        val sample: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3), Some(0))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if actual photo intersection is too small" in {
      new TestContext(minIntersection = 4) {
        val decider: RealtyStealPhotoDuplicateDecider = newDecider()

        val instance1: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2), Some(1))
        val instance2: Instance = newInstance(yesterday, Seq(cvHash1, cvHash3), Some(1))
        val sample: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3), Some(0))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }
  }
}
