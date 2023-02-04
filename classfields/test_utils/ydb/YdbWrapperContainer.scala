package ru.yandex.vertis.vsquality.utils.test_utils.ydb

import cats.effect.{Async, Concurrent, ContextShift}
import ru.yandex.vertis.vsquality.utils.cats_utils.Executable
import ru.yandex.vertis.vsquality.utils.ydb_utils.{DefaultYdbWrapper, YdbWrapper}
import ru.yandex.vertis.ydb.YdbContainer

import scala.concurrent.duration._
import scala.language.higherKinds

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-07-05
  */
class YdbWrapperContainer[F[_]: Concurrent: Executable](imageName: String)(implicit cs: ContextShift[F])
  extends YdbContainer(imageName) {

  lazy val ydb: DefaultYdbWrapper[F] = YdbWrapper.make[F](tableClient, "/local", sessionAcquireTimeout = 3.seconds)
}

object YdbWrapperContainer {
  import YdbContainer._

  def stable[F[_]: Concurrent: Executable](implicit cs: ContextShift[F]): YdbWrapperContainer[F] =
    new YdbWrapperContainer[F](StableImage)

  def latest[F[_]: Concurrent: Executable](implicit cs: ContextShift[F]): YdbWrapperContainer[F] =
    new YdbWrapperContainer[F](LatestImage)

  def apply[F[_]: Concurrent: Executable](implicit cs: ContextShift[F]): YdbWrapperContainer[F] = stable[F]
}
