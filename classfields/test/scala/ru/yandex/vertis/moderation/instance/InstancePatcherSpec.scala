package ru.yandex.vertis.moderation.instance

import cats.effect.IO
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.dao.{FuturedInstanceDao, InstanceDao}
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.InstanceDaoSerDe
import ru.yandex.vertis.moderation.dao.impl.ydb.{YdbInstanceDao, YdbInstanceDaoSchema}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class InstancePatcherSpec extends InstancePatcherSpecBase[IO] with YdbSpecBase {
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
}
