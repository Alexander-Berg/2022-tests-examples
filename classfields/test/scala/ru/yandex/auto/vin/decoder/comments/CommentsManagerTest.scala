package ru.yandex.auto.vin.decoder.comments

import auto.carfax.common.utils.avatars.AvatarsExternalUrlsBuilder
import auto.carfax.common.utils.tracing.Traced
import auto.carfax.pro_auto.core.src.testkit.YdbContainerKit
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.comments.CommentApiModel
import ru.auto.api.vin.comments.CommentApiModel.VinReportComment
import ru.yandex.auto.vin.decoder.api.exceptions.NotFoundException
import ru.yandex.auto.vin.decoder.manager.CommentsManager
import ru.yandex.auto.vin.decoder.model.CommonVinCode
import ru.yandex.auto.vin.decoder.utils.comments.CommentsPrepareUtils
import ru.yandex.auto.vin.decoder.ydb.comments.{VinCommentPair, YdbCommentDao}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import ru.yandex.vertis.ydb.QueryOptions
import zio.Runtime
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.ListHasAsScala

class CommentsManagerTest
  extends AnyWordSpecLike
  with YdbContainerKit
  with MockitoSupport
  with ForAllTestContainer
  with Matchers {

  implicit val t: Traced = Traced.empty

  private val zioRuntime: zio.Runtime[Blocking with Clock with Random] = Runtime.default

  private val prefix = "/local"

  private lazy val ydb = YdbZioWrapper.make(container.tableClient, prefix, 3.seconds, QueryOptions.Default.withV1Syntax)

  val commentsUtils: CommentsPrepareUtils = {
    val avatarsExternalUrlsBuilder = new AvatarsExternalUrlsBuilder("avatars.mdst.yandex.net")
    CommentsPrepareUtils(avatarsExternalUrlsBuilder)
  }

  lazy val dao = new YdbCommentDao(ydb, zioRuntime, None)
  lazy val commentsManager = new CommentsManager(zioRuntime, ydb, dao, commentsUtils)

  def daoTest(action: YdbCommentDao => Any): Unit = {
    action(dao): Unit
  }

  def managerTest(action: CommentsManager => Any): Unit = {
    action(commentsManager): Unit
  }

  "YdbCommentDao" should {
    "init" in daoTest { dao =>
      dao.init()
    }

    "upsert & get comments" in managerTest { commentsManager =>
      val vin = CommonVinCode("vin1")
      val user = CommentApiModel.User.newBuilder().setId("userId")

      val comment1 = VinReportComment
        .newBuilder()
        .setBlockId("block1")
        .setUser(user)
        .setText("text1 and a few more")
        .build()

      val comment2 = VinReportComment
        .newBuilder()
        .setBlockId("block2")
        .setUser(user)
        .setText("text2 and a few more")
        .build()

      Await.result(commentsManager.addComment(vin, comment1), 10.seconds)
      Await.result(commentsManager.addComment(vin, comment2), 10.seconds)

      val loadedComments = Await.result(commentsManager.getComments(vin), 10.seconds).getCommentsList.asScala

      assert(loadedComments.nonEmpty)
      assert(
        loadedComments.exists(lc => lc.toBuilder.clearId().clearCreateTime().clearUpdateTime().build() == comment1)
      )
      assert(
        loadedComments.exists(lc => lc.toBuilder.clearId().clearCreateTime().clearUpdateTime().build() == comment2)
      )
    }

    "upsert & delete" in managerTest { commentsManager =>
      val vin = CommonVinCode("vin2")
      val user = CommentApiModel.User.newBuilder().setId("userId")

      val comment1 = VinReportComment
        .newBuilder()
        .setBlockId("block1")
        .setText("text1 and a few more")
        .setUser(user)
        .build()

      Await.result(commentsManager.addComment(vin, comment1), 10.seconds)

      val savedComment =
        Await
          .result(commentsManager.getActiveComment(vin, comment1.getBlockId, comment1.getUser.getId), 10.seconds)
          .getComment

      assert(savedComment.getBlockId == comment1.getBlockId)

      Await.result(commentsManager.deleteComment(vin, comment1.getBlockId, comment1.getUser.getId), 10.seconds)

      intercept[NotFoundException] {
        Await
          .result(commentsManager.getActiveComment(vin, comment1.getBlockId, comment1.getUser.getId), 10.seconds)
          .getComment

      }
    }

    "update" in managerTest { commentsManager =>
      val vin = CommonVinCode("vin3")
      val user = CommentApiModel.User.newBuilder().setId("userId")

      val comment1 = VinReportComment
        .newBuilder()
        .setBlockId("block1")
        .setText("text1 and a few more")
        .setUser(user)
        .build()

      val comment2 = VinReportComment
        .newBuilder()
        .setBlockId("block1")
        .setText("text2 and a few more")
        .setUser(user)
        .build()

      Await.result(commentsManager.addComment(vin, comment1), 10.seconds)

      val savedComment =
        Await
          .result(commentsManager.getActiveComment(vin, comment1.getBlockId, comment1.getUser.getId), 10.seconds)
          .getComment

      assert(savedComment.getText == comment1.getText)
      assert(savedComment.getCreateTime != 0)
      assert(savedComment.getUpdateTime != 0)

      Await.result(commentsManager.addComment(vin, comment2), 10.seconds)
      val savedComment2 =
        Await
          .result(commentsManager.getActiveComment(vin, comment1.getBlockId, comment1.getUser.getId), 10.seconds)
          .getComment

      assert(savedComment2.getCreateTime == savedComment.getCreateTime)
      assert(savedComment2.getUpdateTime != savedComment.getUpdateTime)
      assert(savedComment2.getText == comment2.getText)
    }

    "not found" in managerTest { commentsManager =>
      val vin = CommonVinCode("vin3")
      val futureComment = commentsManager.getActiveComment(vin, "block", "user")

      assertThrows[NotFoundException](Await.result(futureComment, 10.seconds))
    }

    "batch upsert" in managerTest { commentsManager =>
      val vin1 = CommonVinCode("vin_batch_1")
      val vin2 = CommonVinCode("vin_batch_2")

      val user = CommentApiModel.User.newBuilder().setId("userId")

      val comment1 = VinReportComment
        .newBuilder()
        .setBlockId("LEGAL")
        .setText("text1 and a few more")
        .setUser(user)
        .build()

      val pair1 = VinCommentPair(vin1, comment1)
      val pair2 = VinCommentPair(vin2, comment1)

      Await.result(commentsManager.batchUpsertComments(Seq(pair1, pair2)), 10.seconds)

      val vin1Comments =
        Await
          .result(commentsManager.getComments(vin1), 10.seconds)

      assert(vin1Comments.getCommentsList.asScala.head.getText == comment1.getText)

      val vin2Comments =
        Await
          .result(commentsManager.getComments(vin2), 10.seconds)

      assert(vin2Comments.getCommentsList.asScala.head.getText == comment1.getText)

    }

    "drop" in daoTest { dao =>
      dao.drop()
    }
  }

}
