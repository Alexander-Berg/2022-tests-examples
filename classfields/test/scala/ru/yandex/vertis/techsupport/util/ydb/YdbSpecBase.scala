package ru.yandex.vertis.vsquality.techsupport.util.ydb

import com.dimafeng.testcontainers.ForAllTestContainer
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.vsquality.utils.ydb_utils.DefaultYdbWrapper
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao
import ru.yandex.vertis.vsquality.techsupport.util.{Clearable, ClearableAppealDaoProvider, SpecBase}

import scala.io.{Codec, Source}

/**
  * Provides [[ru.yandex.vertis.vsquality.utils.ydb_utils.YdbWrapper]] for YDB running in docker container
  *
  * @author potseluev
  */
trait YdbSpecBase extends SpecBase with ForAllTestContainer with ClearableAppealDaoProvider {

  override lazy val container: YdbWrapperContainer[F] = YdbWrapperContainer.stable

  lazy val ydb: DefaultYdbWrapper[F] = {
    val database = container.ydb
    val stream = getClass.getResourceAsStream("/schema.sql")
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    database.executeSchema(schema).await
    database
  }

  implicit override protected def clearableAppealDao[C[_]]: Clearable[AppealDao[C]] =
    () =>
      ydb
        .runTx(
          ydb.execute(
            s"""
               |DELETE FROM appeals;
               |DELETE FROM conversations;
               |DELETE FROM messages;
               |""".stripMargin
          )
        )
        .void
        .await
}
