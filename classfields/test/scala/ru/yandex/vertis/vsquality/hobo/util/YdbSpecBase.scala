package ru.yandex.vertis.vsquality.hobo.util

import cats.effect.{ContextShift, IO}
import ru.yandex.vertis.vsquality.hobo.util.YdbSpecBase.container
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.vsquality.utils.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.vsquality.utils.ydb_utils.DefaultYdbWrapper

import scala.concurrent.ExecutionContext.global
import scala.io.{Codec, Source}

trait YdbSpecBase {
  type F[X] = IO[X]

  lazy val ydb: DefaultYdbWrapper[IO] = {
    container.start()
    val database = container.ydb
    val maybeInputStream = Option(getClass.getResourceAsStream(resourceSchemaFileName))
    val stream = maybeInputStream.getOrElse(throw new RuntimeException(s"resource $resourceSchemaFileName not found"))
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    database.executeSchema(schema).await
    database
  }
  val resourceSchemaFileName: String = "/vs-quality/hobo/scripts/ydb.sql"
}

object YdbSpecBase {
  implicit val ydbCs: ContextShift[IO] = IO.contextShift(global)
  lazy val container: YdbWrapperContainer[IO] = YdbWrapperContainer.stable

}
