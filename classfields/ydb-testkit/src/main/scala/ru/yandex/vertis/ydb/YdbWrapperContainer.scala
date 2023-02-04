package ru.yandex.vertis.ydb

import ru.yandex.vertis.ydb.zio.YdbZioWrapper

import scala.concurrent.duration._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-07-05
  */
class YdbWrapperContainer(imageName: String,
                          defaultQueryOptions: QueryOptions = QueryOptions.Default) extends YdbContainer(imageName) {

  lazy val ydb: YdbZioWrapper =
    YdbZioWrapper.make(tableClient, "/local", sessionAcquireTimeout = 3.seconds, defaultQueryOptions)
}

object YdbWrapperContainer {
  import YdbContainer._

  def stable: YdbWrapperContainer =
    new YdbWrapperContainer(StableImage)

  def latest: YdbWrapperContainer =
    new YdbWrapperContainer(LatestImage)

  def apply: YdbWrapperContainer = stable
}
