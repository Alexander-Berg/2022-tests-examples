package ru.yandex.vertis.moderation.photo.duplicates

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Instance, RealtyEssentials}
import ru.yandex.vertis.moderation.model.signal.{ManualSource, NoMarker, SignalSet}
import ru.yandex.vertis.moderation.model.signal.SignalInfo.DuplicateByPhoto
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Visibility._
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials.CategoryType._
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials.FlatType._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author akhazhoyan 03/2019
  */
@RunWith(classOf[JUnitRunner])
class RealtyFraudPhotoDuplicateDeciderSpec extends CvHashBasedPhotoDuplicateDeciderSpecBase {

  override val resourceSchemaFileName: String = "/cv-hashes.sql"

  def newDecider()(implicit tc: TestContext): RealtyFraudPhotoDuplicateDecider =
    new RealtyFraudPhotoDuplicateDecider(
      instanceDao = tc.instanceDao,
      opinionCalculator = tc.opinionCalculator,
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
      categoryType = None,
      flatType = None
    )
  }

  def regularInstance(createTime: DateTime, cvHashes: Seq[String])(implicit tc: TestContext): Instance =
    tc.createInstance(createTime, newEssentials(createTime, cvHashes))

  def bannedInstance(
      createTime: DateTime,
      cvHashes: Seq[String],
      reason: Model.Reason = Model.Reason.USER_SELECT
  )(implicit tc: TestContext): Instance = {
    tc.createInstance(
      createTime,
      newEssentials(createTime, cvHashes),
      visibility = BLOCKED,
      signals =
        SignalSet(
          BanSignalGen.next.copy(
            domain = Domain.Realty.default,
            source = ManualSource(UserIdGen.next, NoMarker),
            detailedReason = DetailedReason.fromReason(reason),
            switchOff = None
          )
        )
    )
  }

  "decide" should {
    "return duplicates for banned duplicate" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyFraudPhotoDuplicateDecider = newDecider()

        val banned1: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash2), Model.Reason.PHOTO_STEAL)
        val banned2: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash3), Model.Reason.USER_SELECT)
        val sample: Instance = regularInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] =
          Some(
            Duplicate.fromDuplicates(
              duplicates = Set(banned1, banned2),
              info = decider.name,
              action = Warn,
              auxInfo = Some(DuplicateByPhoto(Set(Model.Reason.USER_SELECT, Model.Reason.PHOTO_STEAL))),
              hashes = Seq(cvHash1, cvHash2, cvHash3)
            )
          )
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if duplicates banned with wrong reason" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyFraudPhotoDuplicateDecider = newDecider()

        val banned1: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash2), Model.Reason.AD_ON_PHOTO)
        val banned2: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash3), Model.Reason.BLOCKED_IP)
        val sample: Instance = regularInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return None if instance has specific flat type or category" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyFraudPhotoDuplicateDecider = newDecider()

        val banned1: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash2))
        val banned2: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash3))
        val essentials: RealtyEssentials = newEssentials(today, Seq(cvHash1, cvHash2, cvHash3))
        val samples: Seq[Instance] =
          Seq(
            createInstance(today, essentials.copy(flatType = Some(NEW_SECONDARY), categoryType = Some(APARTMENT))),
            createInstance(today, essentials.copy(flatType = Some(NEW_FLAT), categoryType = Some(APARTMENT))),
            createInstance(today, essentials.copy(categoryType = Some(LOT))),
            createInstance(today, essentials.copy(categoryType = Some(COMMERCIAL)))
          )

        val expectedResult: Option[Verdict] = None
        for (sample <- samples) {
          val actualResult: Option[Verdict] = decider.decide(sample).futureValue
          actualResult shouldBe expectedResult
        }
      }
    }

    "return None if instance is banned itself" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyFraudPhotoDuplicateDecider = newDecider()

        val banned1: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash2))
        val banned2: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash3))
        val sample: Instance = bannedInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] = None
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if duplicates are not banned" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyFraudPhotoDuplicateDecider = newDecider()

        val banned1: Instance = regularInstance(yesterday, Seq(cvHash1, cvHash2))
        val banned2: Instance = regularInstance(yesterday, Seq(cvHash1, cvHash3))
        val sample: Instance = regularInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if duplicates are banned after this instance was created" in {
      new TestContext(minIntersection = 2) {
        val decider: RealtyFraudPhotoDuplicateDecider = newDecider()

        val banned1: Instance = bannedInstance(today, Seq(cvHash1, cvHash2))
        val banned2: Instance = bannedInstance(today, Seq(cvHash1, cvHash3))
        val sample: Instance = createInstance(yesterday, newEssentials(today, Seq(cvHash1, cvHash2, cvHash3)))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }

    "return NotDuplicate if actual photo intersection is too small" in {
      new TestContext(minIntersection = 4) {
        val decider: RealtyFraudPhotoDuplicateDecider = newDecider()

        val banned1: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash2))
        val banned2: Instance = bannedInstance(yesterday, Seq(cvHash1, cvHash3))
        val sample: Instance = regularInstance(today, Seq(cvHash1, cvHash2, cvHash3))

        val actualResult: Option[Verdict] = decider.decide(sample).futureValue
        val expectedResult: Option[Verdict] = Some(NotDuplicate)
        actualResult shouldBe expectedResult
      }
    }
  }
}
