package ru.yandex.vertis.moderation.dao.impl.ydb

import cats.effect.IO._
import cats.effect.{ContextShift, IO}
import org.scalatest.Ignore
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.InstanceDaoSerDe
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.ydb_utils.config.YdbConfig
import ru.yandex.vertis.quality.ydb_utils.factory.YdbFactory
import ru.yandex.vertis.quality.ydb_utils.{DefaultYdbWrapper, WithTransaction}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt

@Ignore
class RunYdbInstanceDaoSpec extends SpecBase {
  type F[A] = IO[A]

  implicit val ydbCs: ContextShift[IO] = IO.contextShift(global)

  val service = Service.AUTORU

  val testConfig =
    YdbConfig(
      "ydb-ru-prestable.yandex.net:2135",
      "/ru-prestable/verticals/testing/common",
      "/ru-prestable/verticals/testing/common/moderation/instances/dealers_autoru",
      10.seconds,
      "token"
    )

  val prodConfig =
    YdbConfig(
      "ydb-ru.yandex.net:2135",
      "/ru/verticals/production/moderation",
      "/ru/verticals/production/moderation/instances/autoru",
      10.seconds,
      "token"
    )

  val ydbWrapper: DefaultYdbWrapper[F] = YdbFactory(prodConfig)
  val serDe = new InstanceDaoSerDe(service)
  val schema = new YdbInstanceDaoSchema(serDe, None, None)

  val ydbInstanceDao = new YdbInstanceDao[WithTransaction[IO, *]](ydbWrapper, schema, serDe, service)

  "" should {
    "" in {}
  }

}
