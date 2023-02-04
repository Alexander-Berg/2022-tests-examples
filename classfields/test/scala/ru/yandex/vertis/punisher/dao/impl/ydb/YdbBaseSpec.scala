package ru.yandex.vertis.punisher.dao.impl.ydb

import com.dimafeng.testcontainers.ForAllTestContainer
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.quality.ydb_utils._

import scala.io.{Codec, Source}

/**
  * @author mpoplavkov
  */
trait YdbBaseSpec extends BaseSpec with ForAllTestContainer {
  override val container: YdbWrapperContainer[F] = YdbWrapperContainer.stable[F]

  implicit lazy val ydb: DefaultYdbWrapper[F] = {
    val database = container.ydb
    val stream = getClass.getResourceAsStream("/schema.sql")
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    database.executeSchema(schema).await
    database
  }

  type Tx[T] = WithTransaction[F, T]
}
