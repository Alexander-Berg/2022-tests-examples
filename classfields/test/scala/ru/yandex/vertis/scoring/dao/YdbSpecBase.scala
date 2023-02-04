package ru.yandex.vertis.scoring.dao

import com.dimafeng.testcontainers.ForAllTestContainer
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.quality.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.quality.ydb_utils.DefaultYdbWrapper
import ru.yandex.vertis.scoring.dao.scores.impl.YdbScoresDao

import scala.io.{Codec, Source}
import com.dimafeng.testcontainers.ForAllTestContainer
import eu.timepit.refined.auto._
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.quality.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.quality.ydb_utils.DefaultYdbWrapper
import ru.yandex.vertis.scoring.model.PassportUid
import vertis.scoring.model.Badge

import scala.io.{Codec, Source}

trait YdbSpecBase extends SpecBase with ForAllTestContainer {
  override lazy val container: YdbWrapperContainer[F] = YdbWrapperContainer.stable

  lazy val ydbWrapper: DefaultYdbWrapper[F] = {
    val database = container.ydb
    val stream = getClass.getResourceAsStream("/schema.sql")
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    database.executeSchema(schema).await
    database
  }

  before {
    ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM scores;\nDELETE FROM summary;")).await
  }
}
