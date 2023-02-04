package ru.yandex.vertis.phonoteka.util

import com.dimafeng.testcontainers.ForAllTestContainer
import ru.yandex.vertis.phonoteka.dao.MetadataDao
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.quality.ydb_utils.DefaultYdbWrapper

import scala.io.{Codec, Source}

/**
  * Provides [[ru.yandex.vertis.quality.ydb_utils.YdbWrapper]] for YDB running in docker container
  */
trait YdbSpecBase extends SpecBase with ForAllTestContainer with ClearableMetadataDaoProvider {

  override lazy val container: YdbWrapperContainer[F] = YdbWrapperContainer.stable

  lazy val ydb: DefaultYdbWrapper[F] = {
    val database = container.ydb
    val stream = getClass.getResourceAsStream("/schema.sql")
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    database.executeSchema(schema).await
    database
  }

  implicit override protected def clearableMetadataDao[C[_]]: Clearable[MetadataDao[C]] =
    () => ydb.runTx(ydb.execute("DELETE FROM metadata;")).await
}
