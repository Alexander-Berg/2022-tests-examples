package ru.yandex.vertis.moderation.photo.duplicates

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  AutoPhotoInfoOptGen,
  AutoruEssentialsGen,
  AutoruUserGen,
  ExternalIdGen,
  ObjectIdGen,
  RealtyPhotoInfoGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, ExternalId, Instance, User}
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Section
import ru.yandex.vertis.moderation.proto.Model.Visibility

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author akhazhoyan 03/2019
  */
@RunWith(classOf[JUnitRunner])
class AutoruBanPhotoDuplicateDeciderSpec extends CvHashBasedPhotoDuplicateDeciderSpecBase {
  override val resourceSchemaFileName: String = "/cv-hashes.sql"

  import AutoruBanPhotoDuplicateDeciderSpec._

  def newDecider(featureValue: Boolean)(implicit tc: TestContext): AutoruBanPhotoDuplicateDecider = {
    val deciderNew =
      new AutoruBanPhotoDuplicateDecider(
        instanceDao = tc.instanceDao,
        minIntersection = tc.minIntersection,
        tc.featureRegistry,
        cvHashDao = tc.cvHashDao
      )
    tc.featureRegistry
      .updateFeature(AutoruBanPhotoDuplicateDecider.PhotoDuplicateNewRuleFeatureEnabled, featureValue)
      .futureValue

    deciderNew
  }

  def newEssentials(createTime: DateTime, cvHashes: Seq[String], isCallCenter: Boolean = false): AutoruEssentials = {
    val photos =
      cvHashes.map { cvHash =>
        AutoPhotoInfoOptGen.next.copy(cvHash = Some(cvHash), deleted = Some(false))
      }
    AutoruEssentialsGen.next.copy(
      photos = photos,
      timestampCreate = Some(createTime),
      section = Some(Section.USED),
      isCallCenter = isCallCenter
    )
  }

  def newInstance(
      createTime: DateTime,
      cvHashes: Seq[String],
      visibility: Visibility = Visibility.VISIBLE,
      signals: SignalSet = SignalSet.Empty,
      isCallCenter: Boolean = false
  )(implicit tc: TestContext): Instance = {

    tc.createInstance(
      createTime,
      newEssentials(createTime, cvHashes, isCallCenter),
      visibility = visibility,
      signals = signals,
      externalId = AutoruUserGen.flatMap(u => ObjectIdGen.map(ExternalId(u, _))).next
    )
  }

  "decider with old logic" should {
    "return original for duplicate" in {
      new TestContext(minIntersection = 2) {
        val decider: AutoruBanPhotoDuplicateDecider = newDecider(featureValue = false)

        val original: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2))
        val duplicate: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(duplicate).futureValue
        val expectedResult: Option[Verdict] =
          Some(
            Duplicate.fromOriginal(
              original,
              Set(duplicate),
              action = Ban,
              Seq(cvHash1, cvHash2)
            )
          )
        actualResult should smartEqual(expectedResult)
      }
    }

    "return NotDuplicate for original because it was created earlier" in {
      new TestContext(minIntersection = 2) {
        val decider: AutoruBanPhotoDuplicateDecider = newDecider(featureValue = false)

        val original: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2))
        val duplicate: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(original).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if duplicate has wrong section" in {
      new TestContext(minIntersection = 2) {
        val decider: AutoruBanPhotoDuplicateDecider = newDecider(featureValue = false)

        val duplicate: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(duplicate).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if original is not VISIBLE" in {
      new TestContext(minIntersection = 2) {
        val decider: AutoruBanPhotoDuplicateDecider = newDecider(featureValue = false)

        val original: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2), visibility = Visibility.BLOCKED)
        val duplicate: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(duplicate).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return None if duplicate has wrong section" in {
      new TestContext(minIntersection = 2) {
        val decider: AutoruBanPhotoDuplicateDecider = newDecider(featureValue = false)

        val duplicate: Instance =
          createInstance(
            today,
            newEssentials(today, Seq(cvHash1, cvHash2, cvHash3)).copy(section = Some(Section.NEW)),
            externalId = AutoruUserGen.flatMap(u => ObjectIdGen.map(ExternalId(u, _))).next
          )

        val actualResult: Option[Verdict] = decider.decide(duplicate).futureValue
        val expectedResult: Option[Verdict] = None
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if not enough photos" in {
      new TestContext(minIntersection = 4) {
        val decider: AutoruBanPhotoDuplicateDecider = newDecider(featureValue = false)

        val instance1: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2))
        val instance2: Instance = newInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(instance2).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if photo intersection is too small" in {
      new TestContext(minIntersection = 2) {
        val instance1: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2))
        val instance2: Instance = newInstance(today, Seq(cvHash1, cvHash3))

        val decider: AutoruBanPhotoDuplicateDecider = newDecider(featureValue = false)

        val actualResult: Option[Verdict] = decider.decide(instance2).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "decider with new logic" should {

      "correctly sorts instances: newCallCenter, oldCallCenter, oldNotCallCenter" in {
        new TestContext() {
          val oldNotCallCenter: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2))
          val oldCallCenter: Instance = newInstance(yesterday, Seq(cvHash1, cvHash2), isCallCenter = true)
          val newCallCenter: Instance = newInstance(today, Seq(cvHash1, cvHash2), isCallCenter = true)
          val newNotCallCenter: Instance = newInstance(today, Seq(cvHash1, cvHash2))

          val decider = newDecider(featureValue = true)

          val toCheckFirst = Seq(newCallCenter, oldNotCallCenter, oldCallCenter)
          val expectedFirst = Seq(newCallCenter, oldCallCenter, oldNotCallCenter)

          toCheckFirst.sorted(decider.PhotoDuplicateOrdering) shouldBe expectedFirst

          val toCheckSecond = Seq(newNotCallCenter, oldNotCallCenter, oldCallCenter)
          val expectedSecond = Seq(oldCallCenter, newNotCallCenter, oldNotCallCenter)

          toCheckSecond.sorted(decider.PhotoDuplicateOrdering) shouldBe expectedSecond

          val toCheckThird = Seq(newNotCallCenter, oldNotCallCenter, newCallCenter, oldCallCenter)
          val expectedThird = Seq(newCallCenter, oldCallCenter, newNotCallCenter, oldNotCallCenter)

          toCheckThird.sorted(decider.PhotoDuplicateOrdering) shouldBe expectedThird

        }
      }
    }
  }
}

object AutoruBanPhotoDuplicateDeciderSpec {

  case class PhotoDuplicateTestCase(description: String,
                                    decider: AutoruBanPhotoDuplicateDecider,
                                    toCheck: Seq[Instance],
                                    expected: Seq[Instance]
                                   )
}
