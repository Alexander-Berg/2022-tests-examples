package ru.yandex.vertis.moderation.dao.impl.ydb

import cats.effect.IO
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.dao.{InstanceArchiveDao, InstanceArchiveDaoSpecBase}
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.cats_utils.Executable.IoExecutable
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

@RunWith(classOf[JUnitRunner])
class YdbInstanceArchiveDaoSpec extends InstanceArchiveDaoSpecBase with YdbSpecBase {
  override val resourceSchemaFileName: String = "/ydb-archive.sql"

  before {
    ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM instance_archive;")).await
  }

  override lazy val instanceArchiveDao: InstanceArchiveDao =
    new YdbInstanceArchiveDao[IO, WithTransaction[IO, *]](ydbWrapper)
}
