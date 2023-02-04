package ru.yandex.vertis.moderation.flink

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.flink.FlinkCleanupServiceImplSpec.{JobRecoveryPoint, TestCase, _}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.generators._
import ru.yandex.vertis.moderation.service.S3Service
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class FlinkCleanupServiceImplSpec extends SpecBase {

  private val archiveDir = "archive"

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "remove older recovery points",
        source =
          Set(
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "1"),
              recoveryPoint = Some(RecoveryPoint("offers-1", DateTimeUtil.fromMillis(1), isDiscarded = false)),
              recoveryPointExistOnS3 = true
            ),
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "2"),
              recoveryPoint = Some(RecoveryPoint("offers-2", DateTimeUtil.fromMillis(2), isDiscarded = false)),
              recoveryPointExistOnS3 = true
            ),
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "3"),
              recoveryPoint = Some(RecoveryPoint("offers-3", DateTimeUtil.fromMillis(3), isDiscarded = false)),
              recoveryPointExistOnS3 = true
            )
          ),
        retain = 1,
        toRemovePathsExpected = Set("offers-1", "offers-2", s"$archiveDir/1", s"$archiveDir/2")
      ),
      TestCase(
        description = "not remove anything if there are not enough recovery points",
        source =
          Set(
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job"),
              recoveryPoint = Some(RecoveryPoint("offers-1", DateTimeUtil.fromMillis(1), isDiscarded = false)),
              recoveryPointExistOnS3 = true
            )
          ),
        retain = 2,
        toRemovePathsExpected = Set.empty
      ),
      TestCase(
        description = "not retain discarded recovery point",
        source =
          Set(
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "1"),
              recoveryPoint = Some(RecoveryPoint("offers-1", DateTimeUtil.fromMillis(1), isDiscarded = false)),
              recoveryPointExistOnS3 = true
            ),
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "2"),
              recoveryPoint = Some(RecoveryPoint("offers-2", DateTimeUtil.fromMillis(2), isDiscarded = false)),
              recoveryPointExistOnS3 = true
            ),
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "3"),
              recoveryPoint = Some(RecoveryPoint("offers-3", DateTimeUtil.fromMillis(3), isDiscarded = true)),
              recoveryPointExistOnS3 = true
            )
          ),
        retain = 1,
        toRemovePathsExpected = Set("offers-1", "offers-3", s"$archiveDir/1", s"$archiveDir/3")
      ),
      TestCase(
        description = "not retain not existent recovery point",
        source =
          Set(
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "1"),
              recoveryPoint = Some(RecoveryPoint("offers-1", DateTimeUtil.fromMillis(1), isDiscarded = false)),
              recoveryPointExistOnS3 = true
            ),
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "2"),
              recoveryPoint = Some(RecoveryPoint("offers-2", DateTimeUtil.fromMillis(2), isDiscarded = false)),
              recoveryPointExistOnS3 = true
            ),
            JobRecoveryPoint(
              jobInfo = nextJob("offers-job", "3"),
              recoveryPoint = Some(RecoveryPoint("offers-3", DateTimeUtil.fromMillis(3), isDiscarded = false)),
              recoveryPointExistOnS3 = false
            )
          ),
        retain = 1,
        toRemovePathsExpected = Set("offers-1", "offers-3", s"$archiveDir/1", s"$archiveDir/3")
      )
    )

  "FlinkCleanupServiceImpl" should {
    testCases.foreach { case TestCase(description, source, retain, toRemoveExpected) =>
      description in {
        val s3Service = mock[S3Service]
        val flinkHistoryClient = mock[FlinkHistoryClient]
        val cleanupService = new FlinkCleanupServiceImpl(flinkHistoryClient, s3Service, archiveDir, retain)
        s3Service.removeByPrefix(any[String]).returns(Future.successful(()))
        source.foreach { case JobRecoveryPoint(jobInfo, recoveryPoint, existOnS3) =>
          flinkHistoryClient.getLastCheckpoint(jobInfo.id).returns(Future.successful(recoveryPoint))
          recoveryPoint.foreach(point => s3Service.exists(point.path).returns(Future.successful(existOnS3)))
        }
        flinkHistoryClient.getJobs.returns(Future.successful(source.map(_.jobInfo)))
        cleanupService.cleanup().futureValue
        toRemoveExpected.foreach(there was one(s3Service).removeByPrefix(_))
        there.were(anyTimes(s3Service).exists(any[String]))
        there.were(noMoreCallsTo(s3Service))
      }
    }
  }
}

object FlinkCleanupServiceImplSpec {

  private val JobStateGen: Gen[JobState] = Gen.oneOf(JobState.values.toSeq)

  private def nextJob(name: String, id: String = StringGen.next): JobInfo =
    (for {
      state     <- JobStateGen
      startTime <- DateTimeGen.?
      endTime   <- DateTimeGen.?
    } yield JobInfo(
      id = id,
      name = name,
      state = state,
      startTime = startTime,
      endTime = endTime
    )).next

  case class TestCase(description: String,
                      source: Set[JobRecoveryPoint],
                      retain: Int,
                      toRemovePathsExpected: Set[String]
                     )

  case class JobRecoveryPoint(jobInfo: JobInfo, recoveryPoint: Option[RecoveryPoint], recoveryPointExistOnS3: Boolean)

}
