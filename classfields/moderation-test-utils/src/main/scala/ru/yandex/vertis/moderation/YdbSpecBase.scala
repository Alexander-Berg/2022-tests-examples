package ru.yandex.vertis.moderation

import cats.effect.{ContextShift, IO}
import com.dimafeng.testcontainers.ForAllTestContainer
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.quality.ydb_utils.DefaultYdbWrapper

import scala.io.{Codec, Source}
import scala.concurrent.ExecutionContext.global

trait YdbSpecBase extends SpecBase with ForAllTestContainer {
  type F[X] = IO[X]

  implicit val ydbCs: ContextShift[IO] = IO.contextShift(global)

  override lazy val container: YdbWrapperContainer[IO] = YdbWrapperContainer.stable[IO]

  val resourceSchemaFileName: String

  lazy val ydbWrapper: DefaultYdbWrapper[IO] = {
    val database = container.ydb
    val maybeInputStream = Option(getClass.getResourceAsStream(resourceSchemaFileName))
    val stream = maybeInputStream.getOrElse(throw new RuntimeException(s"resource $resourceSchemaFileName not found"))
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    database.executeSchema(schema).await
    database
  }
}
