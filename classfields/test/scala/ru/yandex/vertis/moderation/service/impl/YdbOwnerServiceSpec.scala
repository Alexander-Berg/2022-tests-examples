package ru.yandex.vertis.moderation.service.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.dao.impl.ydb.YdbOwnerDao
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.InstanceDaoSerDe
import ru.yandex.vertis.moderation.dao.{FuturedOwnerDao, OwnerDao}
import ru.yandex.vertis.moderation.model.instance.User
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.{OwnerService, OwnerServiceSpecBase}
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Specs on [[OwnerService]] implemented with ydb
  *
  * @author sunlight
  */
@RunWith(classOf[JUnitRunner])
class YdbOwnerServiceSpec extends OwnerServiceSpecBase with YdbSpecBase {

  val serDe = new InstanceDaoSerDe(Service.AUTORU)

  override val resourceSchemaFileName: String = "/owners.sql"

  lazy val ownerDao: OwnerDao[Future, User] =
    new FuturedOwnerDao[F, User](new YdbOwnerDao[F, WithTransaction[F, *]](ydbWrapper, serDe, None))

  override def getOwnerService: OwnerService = new OwnerServiceImpl(ownerDao)
}
