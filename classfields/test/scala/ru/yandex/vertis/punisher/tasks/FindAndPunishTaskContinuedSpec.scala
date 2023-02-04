package ru.yandex.vertis.punisher.tasks

import cats.syntax.applicative._
import org.junit.runner.RunWith
import org.scalatest.PrivateMethodTester
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.clustering.proto.Model.ClusteringFormula
import ru.yandex.vertis.feature.impl.InMemoryFeatureRegistry
import ru.yandex.vertis.punisher.feature.UserPunisherFeatureTypes
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.stages.impl.EmptyPunisher
import ru.yandex.vertis.punisher.stages.{Clusterizer, FindAndPunish, Finder, PunishPolicy}
import ru.yandex.vertis.punisher.tasks.FindAndPunishTaskContinuedSpec._
import ru.yandex.vertis.punisher.tasks.settings.TaskSettings
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.punisher.{AutoruStagesBuilder, BaseSpec}
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.feature_registry_utils.FeatureRegistryF

import java.time.ZonedDateTime
import scala.concurrent.duration._

/**
  * Specs for [[FindAndPunishTaskContinued]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class FindAndPunishTaskContinuedSpec extends BaseSpec with PrivateMethodTester {

  import FindAndPunishTaskContinued._

  val StepMin = 1.hour
  val StepMax = 2.hours
  val StepBack = 24.hours
  val MaxGap = 7.days
  val Now = DateTimeUtils.now

  val finder =
    new Finder[F] {
      override def find(implicit context: TaskContext.Batch): F[Set[UserId]] = Set("1").pure[F]
    }

  val clusterizer =
    new Clusterizer[F] {
      override def clusterize(userId: UserId, kafkaOffset: Option[KafkaOffset])(
          implicit context: TaskContext
      ): F[UserIdCluster] = UserIdCluster(Set("1"), "1").pure[F]

      override def formula: ClusteringFormula = ClusteringFormula.L1_STRICT
    }
  val enricher = AutoruStagesBuilder.baseEnricher
  val policy = PunishPolicy[AutoruUser, TaskContext.Batch](Seq.empty)
  val punisher = new EmptyPunisher[F, AutoruUser]
  val taskSettings: TaskSettings = TaskSettings(1.hours, 1.hours, 24.hours)

  val featureRegistry: FeatureRegistryF[F] =
    new FeatureRegistryF[F](new InMemoryFeatureRegistry(UserPunisherFeatureTypes))
  val epochRegForIntervalTest = MemoryEpochRegistry(TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers))
  val epochReg = MemoryEpochRegistry(TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers))

  val findAndPunish = new FindAndPunish(clusterizer, enricher, policy, punisher)

  "FindAndPunishContinued.getStep" should {

    val testCases: Seq[StepTestCase] =
      Seq(
        StepTestCase(
          description = "return lag cause lag is less than stepMax",
          now = Now,
          stepMax = StepMax,
          stepBack = StepBack,
          epoch = Now.minusHours(25),
          expectedStep = 1.hour
        ),
        StepTestCase(
          description = "return stepMax cause lag is greater than stepMax",
          now = Now,
          stepMax = StepMax,
          stepBack = StepBack,
          epoch = Now.minusHours(28),
          expectedStep = StepMax
        )
      )

    testCases.foreach { case StepTestCase(description, now, stepMax, stepBack, epoch, expectedStep) =>
      description in {
        getStep(now, stepMax, stepBack, epoch) shouldBe expectedStep
      }
    }
  }

  "FindAndPunishContinued.isTooOldEpoch" should {

    val testCases: Seq[OldEpochTestCase] =
      Seq(
        OldEpochTestCase(
          descrption = "be true if stepMin = 1h, stepBack = 24h, epoch = now - 26h",
          now = Now,
          stepMin = StepMin,
          stepBack = StepBack,
          epoch = Now.minusHours(26),
          expectedResult = true
        ),
        OldEpochTestCase(
          descrption = "be false if stepMin = 1h, stepBack = 24h, epoch = now - 25h",
          now = Now,
          stepMin = StepMin,
          stepBack = StepBack,
          epoch = Now.minusHours(25),
          expectedResult = false
        ),
        OldEpochTestCase(
          descrption = "be true if stepMin = 1h, stepBack = 24h, epoch = now - 25h - 1nano",
          now = Now,
          stepMin = StepMin,
          stepBack = StepBack,
          epoch = Now.minusHours(25).minusNanos(1),
          expectedResult = true
        ),
        OldEpochTestCase(
          descrption = "be false if stepMin = 1h, stepBack = 24h, epoch = now - 25h + 1nano",
          now = Now,
          stepMin = StepMin,
          stepBack = StepBack,
          epoch = Now.minusHours(25).plusNanos(1),
          expectedResult = false
        )
      )

    testCases.foreach { case OldEpochTestCase(description, now, stepMin, stepBack, epoch, expectedResult) =>
      description in {
        isTooOldEpoch(now, stepMin, stepBack)(epoch) shouldBe expectedResult
      }
    }
  }

  "FindAndPunishContinued private method getInterval" should {
    val findAndPunishTask =
      new FindAndPunishTask(
        TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
        finder,
        taskSettings,
        findAndPunish,
        featureRegistry
      ) with FindAndPunishTaskContinued[F, AutoruUser] {

        override protected def epochRegistry: EpochRegistry = epochRegForIntervalTest
      }

    val testCases: Seq[IntervalTestCase] =
      Seq(
        IntervalTestCase(
          description = "return timeinterval if epoch is None",
          now = Now,
          stepMin = StepMin,
          stepMax = StepMax,
          stepBack = StepBack,
          maxGap = MaxGap,
          epoch = None,
          expectedResult = Some(TimeInterval(from = Now.minusHours(25), to = Now.minusHours(24)))
        ),
        IntervalTestCase(
          description = "return timeinterval cause epoch is too old",
          now = Now,
          stepMin = StepMin,
          stepMax = StepMax,
          stepBack = StepBack,
          maxGap = MaxGap,
          epoch = Some(Now.minusHours(27)),
          expectedResult = Some(TimeInterval(from = Now.minusHours(27), to = Now.minusHours(25)))
        ),
        IntervalTestCase(
          description = "return None cause epoch is NOT too old",
          now = Now,
          stepMin = StepMin,
          stepMax = StepMax,
          stepBack = StepBack,
          maxGap = MaxGap,
          epoch = Some(Now.minusHours(23)),
          expectedResult = None
        )
      )

    testCases.foreach {
      case IntervalTestCase(description, now, stepMin, stepMax, stepBack, maxGap, epoch, expectedResult) =>
        description in {
          epoch.foreach { ep =>
            epochRegForIntervalTest.put(ep)
          }
          findAndPunishTask.getInterval(now, stepMin, stepMax, stepBack, maxGap) shouldBe expectedResult
        }
    }

    "throw an IllegalArgumentException if epoch more than maxGap" in {
      epochRegForIntervalTest.put(Now.minusDays(8))
      assertThrows[IllegalArgumentException] {
        findAndPunishTask.getInterval(Now, StepMin, StepMax, StepBack, MaxGap)
      }
    }
  }

  "FindAndPunishContinuedSpec" should {
    val stepMin = 1.hour
    val offsetBack = 24.hours

    import DateTimeUtils._
    val Now = DateTimeUtils.now

    val ToTest =
      Seq(
        (Now, Now.plusHours(1), false),
        (Now.minusHours(1), Now, false),
        (TimeInterval(Now, stepMin, Some(offsetBack)).to, Now, false),
        (TimeInterval(Now, stepMin, Some(offsetBack)).to.minusMinutes(59), Now, false),
        (TimeInterval(Now, stepMin, Some(offsetBack)).to.minusMinutes(60), Now, false),
        (TimeInterval(Now, stepMin, Some(offsetBack)).to.minusMinutes(61), Now, true),
        (TimeInterval(Now, stepMin, Some(offsetBack)).to.minusMinutes(60), Now.plus(1.millis), true),
        (TimeInterval(Now, stepMin, Some(offsetBack)).to.minusMinutes(60), Now.minus(1.millis), false),
        (
          ZonedDateTime.parse("2017-10-01T16:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T16:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-10-01T16:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T17:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-10-01T16:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T17:34:18.120+03:00[Europe/Moscow]"),
          true
        ),
        (
          ZonedDateTime.parse("2017-10-01T16:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T19:34:18.121+03:00[Europe/Moscow]"),
          true
        ),
        (
          ZonedDateTime.parse("2017-10-01T16:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T19:34:18.120+03:00[Europe/Moscow]"),
          true
        ),
        (
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-10-02T16:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-10-02T01:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-10-01T16:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-10-01T15:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-10-01T15:34:17.119+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-10-01T15:34:16.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          false
        ),
        (
          ZonedDateTime.parse("2017-09-02T16:34:17.120+03:00[Europe/Moscow]"),
          ZonedDateTime.parse("2017-10-02T15:34:17.120+03:00[Europe/Moscow]"),
          true
        )
      )

    ToTest.foreach { case (epoch, now, isTooOld) =>
      s"be correct on ($epoch, $now, $isTooOld)" in {
        isTooOldEpoch(now, stepMin, offsetBack)(epoch) should be(isTooOld)
      }
    }

    "run and update epoch" in {

      epochReg.get() should be(None)

      val findAndPunishTask: FindAndPunishTask[F, AutoruUser] =
        new FindAndPunishTask(
          TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
          finder,
          taskSettings,
          findAndPunish,
          featureRegistry
        ) with FindAndPunishTaskContinued[F, AutoruUser] {

          override protected def epochRegistry: EpochRegistry = epochReg
        }

      findAndPunishTask.payload.await

      val now = DateTimeUtils.now
      var currentEpoch: ZonedDateTime = None.orNull

      epochReg.get() match {
        case Some(epoch) =>
          epoch.plusHours(24).isBefore(now) should be(true)
          epoch.isAfter(now.minusHours(24).minusMinutes(1)) should be(true)
          currentEpoch = epoch
        case other => fail(s"Unexpected(1) $other")
      }

      // Nothing to do
      findAndPunishTask.payload.await
      epochReg.get() match {
        case Some(epoch) if epoch == currentEpoch => ()
        case other                                => fail(s"Unexpected(2) $other")
      }

      // Skip if shift epoch to 30 min
      epochReg.put(currentEpoch.minusMinutes(30))
      findAndPunishTask.payload.await
      epochReg.get() match {
        case Some(epoch) =>
          epoch should be(currentEpoch.minusMinutes(30))
        case other => fail(s"Unexpected(3) $other")
      }

      // Has smth to do and set epoch to e1 after the find and punish
      epochReg.put(currentEpoch.minus(stepMin))
      findAndPunishTask.payload.await
      epochReg.get() match {
        case Some(epoch) =>
          epoch should be(currentEpoch)
        case other => fail(s"Unexpected(4) $other")
      }

      // Has smth to do in several steps
      epochReg.put(currentEpoch.minusHours(3).minusMinutes(7))
      findAndPunishTask.payload.await
      epochReg.get() match {
        case Some(epoch) =>
          epoch should be(currentEpoch.minusHours(2).minusMinutes(7))
        case other => fail(s"Unexpected(5) $other")
      }
      findAndPunishTask.payload.await
      epochReg.get() match {
        case Some(epoch) =>
          epoch should be(currentEpoch.minusHours(1).minusMinutes(7))
        case other => fail(s"Unexpected(6) $other")
      }
      findAndPunishTask.payload.await
      epochReg.get() match {
        case Some(epoch) =>
          epoch should be(currentEpoch.minusMinutes(7))
        case other => fail(s"Unexpected(7) $other")
      }
      findAndPunishTask.payload.await
      epochReg.get() match {
        case Some(epoch) =>
          epoch should be(currentEpoch.minusMinutes(7))
        case other => fail(s"Unexpected(6) $other")
      }
    }
  }
}

object FindAndPunishTaskContinuedSpec {

  case class StepTestCase(description: String,
                          now: ZonedDateTime,
                          stepMax: FiniteDuration,
                          stepBack: FiniteDuration,
                          epoch: ZonedDateTime,
                          expectedStep: FiniteDuration
                         )

  case class OldEpochTestCase(descrption: String,
                              now: ZonedDateTime,
                              stepMin: FiniteDuration,
                              stepBack: FiniteDuration,
                              epoch: ZonedDateTime,
                              expectedResult: Boolean
                             )

  case class IntervalTestCase(description: String,
                              now: ZonedDateTime,
                              stepMin: FiniteDuration,
                              stepMax: FiniteDuration,
                              stepBack: FiniteDuration,
                              maxGap: FiniteDuration,
                              epoch: Option[ZonedDateTime],
                              expectedResult: Option[TimeInterval]
                             )
}
