package ru.yandex.vertis.moderation.dao.impl.ydb

import cats.effect.{ContextShift, IO}
import org.scalatest.Ignore
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.FuturedOwnerDao
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.InstanceDaoSerDe
import ru.yandex.vertis.moderation.model.instance.User
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.ydb_utils.{DefaultYdbWrapper, WithTransaction}
import ru.yandex.vertis.quality.ydb_utils.config.YdbConfig
import ru.yandex.vertis.quality.ydb_utils.factory.YdbFactory

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt

@Ignore
class RunYdbOwnerDaoSpec extends SpecBase {
  type F[X] = IO[X]

  implicit val ydbCs: ContextShift[IO] = IO.contextShift(global)

  val serDe = new InstanceDaoSerDe(Service.DEALERS_AUTORU)

  val testConfig =
    YdbConfig(
      "ydb-ru-prestable.yandex.net:2135",
      "/ru-prestable/verticals/testing/common",
      "/ru-prestable/verticals/testing/common/moderation/owners/autoru",
      10.seconds,
      "token"
    )

  val prodConfig =
    YdbConfig(
      "ydb-ru.yandex.net:2135",
      "/ru/verticals/production/moderation",
      "/ru/verticals/production/moderation/owners/autoru",
      10.seconds,
      "token"
    )

  lazy val ydb: DefaultYdbWrapper[F] = YdbFactory[F](prodConfig)

  lazy val ownerDao = new FuturedOwnerDao[F, User](new YdbOwnerDao[F, WithTransaction[F, *]](ydb, serDe, None))

  "" should {
    "" in {
      println(ownerDao.getSignals(User.Dealer("24199")).futureValue)
    }
  }

}
