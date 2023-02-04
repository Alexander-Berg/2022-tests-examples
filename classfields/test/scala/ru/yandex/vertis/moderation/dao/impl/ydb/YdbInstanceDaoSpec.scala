package ru.yandex.vertis.moderation.dao.impl.ydb

import cats.effect.IO
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.InstanceDaoSerDe
import ru.yandex.vertis.moderation.dao.{FuturedInstanceDao, InstanceDao, InstanceDaoSpecBase}
import ru.yandex.vertis.moderation.model.ObjectId
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.UserGen
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{EssentialsPatch, Instance}
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class YdbInstanceDaoSpec extends InstanceDaoSpecBase with YdbSpecBase {

  override val resourceSchemaFileName: String = "/ydb-instance-dao.sql"

  val serDe = new InstanceDaoSerDe(Service.REALTY)

  lazy val instanceDaoF =
    new YdbInstanceDao[WithTransaction[IO, *]](
      ydbWrapper,
      new YdbInstanceDaoSchema(serDe, None, None),
      serDe,
      Service.REALTY
    )

  override lazy val instanceDao: InstanceDao[Future] = new FuturedInstanceDao(instanceDaoF)

  before {
    val query =
      """DELETE FROM instances;
        |DELETE FROM user_object_relations;
        |DELETE FROM feed_object_relations;""".stripMargin
    Try(ydbWrapper.runTx(ydbWrapper.execute(query)).await)
  }

  "getObjectIds" should {
    "return huge list of object ids" in {
      val user = UserGen.next
      val objectIds = (1 to 3001).toList.map(_.toString)
      val prefix =
        """
          |--!syntax_v1
          |
          |PRAGMA TablePathPrefix = "/local";
          |
          |INSERT INTO user_object_relations (hash, user_id, object_id) VALUES
          |""".stripMargin

      def entry(objectId: ObjectId): String =
        s"""
          |   (
          |     ${java.lang.Long.toUnsignedString(YdbInstanceDao.calculateStringHash(user.key))},
          |     '${user.key}',
          |     '$objectId'
          |   )
          |""".stripMargin

      val query = prefix + objectIds.map(entry).mkString(start = "", sep = ",", end = ";")
      ydbWrapper.runTx(ydbWrapper.execute(query)).await

      val actualResult = instanceDao.getObjectIds(user).futureValue
      actualResult shouldBe objectIds.toSet
    }
  }

  "upsertAll" should {
    "upsert all the fields" in {

      val expiredInstance = CoreGenerators.ExpiredInstanceGen.next
      val essentials =
        for {
          essentials <- expiredInstance.essentials
          createTime <- expiredInstance.createTime
          updateTime <- expiredInstance.essentialsUpdateTime
        } yield EssentialsPatch(expiredInstance.id, essentials, createTime, updateTime, Seq.empty)

      instanceDao
        .upsertAll(
          id = expiredInstance.id,
          essentials = essentials,
          maybeContext = expiredInstance.context,
          signals = expiredInstance.signals,
          metadata = expiredInstance.metadata
        )
        .futureValue

      val expectedResult =
        essentials match {
          case Some(payload) =>
            Left(
              Instance(
                id = expiredInstance.id,
                essentials = payload.essentials,
                signals = expiredInstance.signals,
                createTime = payload.createTime,
                essentialsUpdateTime = payload.essentialsUpdateTime,
                context = expiredInstance.context.getOrElse(Context.Default),
                metadata = expiredInstance.metadata
              )
            )
          case None =>
            Right(expiredInstance.copy(essentials = None, createTime = None, essentialsUpdateTime = None))
        }

      val actualResult = instanceDao.getMaybeExpiredInstance(expiredInstance.id).futureValue
      actualResult shouldBe expectedResult
    }
  }
}
