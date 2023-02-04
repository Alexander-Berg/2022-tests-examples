package ru.yandex.auto.carfax.tasks

import auto.carfax.common.utils.avatars.AvatarsExternalUrlsBuilder
import auto.carfax.common.utils.tracing.Traced
import auto.carfax.pro_auto.core.src.testkit.YdbContainerKit
import com.dimafeng.testcontainers.ForAllTestContainer
import io.opentracing.noop.{NoopTracer, NoopTracerFactory}
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.VinReportModel.{LegalBlock, PtsBlock, RawVinEssentialsReport, WantedBlock}
import ru.auto.api.vin.VinResolutionEnums.Status
import ru.auto.api.vin.comments.CommentApiModel
import ru.auto.api.vin.comments.CommentApiModel.VinReportComment
import ru.yandex.auto.carfax.scheduler.tasks.LegalCommentsExpirationTask
import ru.yandex.auto.vin.decoder.manager.CommentsManager
import ru.yandex.auto.vin.decoder.model.CommonVinCode
import ru.yandex.auto.vin.decoder.report.processors.report.ReportManager
import ru.yandex.auto.vin.decoder.report.processors.report.ReportManager.VinReport
import ru.yandex.auto.vin.decoder.utils.comments.{CommentsPrepareUtils, WantedCommentableId}
import ru.yandex.auto.vin.decoder.ydb.comments.{VinCommentPair, YdbCommentDao}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.ydb.QueryOptions
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import zio.Runtime
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.ListHasAsScala

class LegalCommentsExpirationTaskTest
  extends AnyWordSpecLike
  with YdbContainerKit
  with MockitoSupport
  with ForAllTestContainer
  with Matchers {

  implicit private val t: Traced = Traced.empty
  implicit private val tracer: NoopTracer = NoopTracerFactory.create()

  private val zioRuntime: zio.Runtime[Blocking with Clock with Random] = Runtime.default

  private val prefix = "/local"

  lazy val commentsUtils: CommentsPrepareUtils = {
    val avatarsExternalUrlsBuilder = new AvatarsExternalUrlsBuilder("avatars.mdst.yandex.net")
    CommentsPrepareUtils(avatarsExternalUrlsBuilder)
  }

  lazy val ydbWrapper: YdbZioWrapper =
    YdbZioWrapper.make(container.tableClient, prefix, 3.seconds, QueryOptions.Default.withV1Syntax)

  lazy val dao = new YdbCommentDao(ydbWrapper, zioRuntime, None)
  lazy val commentsManager = new CommentsManager(zioRuntime, ydbWrapper, dao, commentsUtils)
  val reportManager: ReportManager = mock[ReportManager]
  val taskFeature: Feature[Boolean] = mock[Feature[Boolean]]
  val dryRunFeature: Feature[Boolean] = mock[Feature[Boolean]]
  implicit val m = TestOperationalSupport

  lazy val task: LegalCommentsExpirationTask = new LegalCommentsExpirationTask(
    zioRuntime = zioRuntime,
    commentsManager = commentsManager,
    reportManager = reportManager,
    taskFeature = taskFeature,
    dryRunFeature = dryRunFeature
  )

  def daoTest(action: YdbCommentDao => Any): Unit = {
    action(dao): Unit
  }

  def taskTest(action: LegalCommentsExpirationTask => Any): Unit = {
    action(task): Unit
  }

  "LegalCommentsExpirationTask" should {
    "init" in daoTest { dao =>
      dao.init()
    }

    "mark as expired" in taskTest { task =>
      when(dryRunFeature.value).thenReturn(false)
      val vin1 = CommonVinCode("XWEPH81BDH0007111")
      val vin2 = CommonVinCode("XWEPH81BDH0007112")
      val vin3 = CommonVinCode("XWEPH81BDH0007113")

      val user = CommentApiModel.User.newBuilder().setId("userId")

      val comment1 = VinReportComment
        .newBuilder()
        .setBlockId("legal")
        .setText("text1 and a few more")
        .setUser(user)
        .build()

      val comment2 = VinReportComment
        .newBuilder()
        .setBlockId("legal")
        .setText("text2 and a few more")
        .setUser(user)
        .build()

      val comment3 = VinReportComment
        .newBuilder()
        .setBlockId("legal")
        .setText("text3 and a few more")
        .setIsDeleted(true)
        .setUser(user)
        .build()

      val comment4 = VinReportComment
        .newBuilder()
        .setBlockId("wanted")
        .setText("text4 and a few more")
        .setUser(user)
        .build()

      val pair1 = VinCommentPair(vin1, comment1)
      val pair2 = VinCommentPair(vin2, comment2)
      val pair3 = VinCommentPair(vin3, comment3)
      val pair4 = VinCommentPair(vin2, comment4)

      val essentialReport1 = {
        val pts =
          PtsBlock
            .newBuilder()
            .setVin(vin1.toString)

        val legal =
          LegalBlock
            .newBuilder()
            .setWantedStatus(Status.OK)
            .setConstraintsStatus(Status.OK)
            .setPledgeStatus(Status.OK)

        val report =
          RawVinEssentialsReport
            .newBuilder()
            .setPtsInfo(pts)
            .setLegal(legal)

        VinReport(vin1, report.build())
      }

      val essentialReport2 = {
        val pts =
          PtsBlock
            .newBuilder()
            .setVin(vin2.toString)

        val legal =
          LegalBlock
            .newBuilder()
            .setWantedStatus(Status.ERROR)
            .setConstraintsStatus(Status.OK)
            .setPledgeStatus(Status.OK)

        val wantedBlock = WantedBlock
          .newBuilder()
          .setStatus(Status.OK)

        val report =
          RawVinEssentialsReport
            .newBuilder()
            .setPtsInfo(pts)
            .setLegal(legal)
            .setWanted(wantedBlock)

        VinReport(vin2, report.build())
      }

      when(reportManager.getBatchEssentialsReports(eq(List(vin1, vin2, vin2)), ?)(?))
        .thenReturn(Future.successful(Seq(essentialReport1, essentialReport2)))

      Await.result(commentsManager.batchUpsertComments(Seq(pair1, pair2, pair3, pair4)), 10.seconds)

      Await.result(task.markCommentsExpired, 10.seconds)
      verify(reportManager).getBatchEssentialsReports(eq(List(vin1, vin2, vin2)), ?)(?)

      val updatedComment1 = Await.result(commentsManager.getComments(vin1), 10.seconds).getCommentsList.asScala.head
      val updatedComment2 = Await.result(commentsManager.getComments(vin2), 10.seconds).getCommentsList.asScala
      val updatedComment3 = Await.result(commentsManager.getComments(vin3), 10.seconds).getCommentsList.asScala.head
      val updatedComment4 = Await.result(commentsManager.getComments(vin2), 10.seconds).getCommentsList.asScala

      updatedComment1.getIsExpired shouldBe true
      updatedComment2 should contain(comment2)
      updatedComment3 shouldBe comment3
      updatedComment4.exists { c =>
        c.getBlockId == WantedCommentableId.id && c.getIsExpired
      } shouldBe true
    }

    "drop" in daoTest { dao =>
      dao.drop()
    }
  }

}
