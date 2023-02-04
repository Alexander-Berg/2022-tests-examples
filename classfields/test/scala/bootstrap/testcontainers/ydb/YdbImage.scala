package bootstrap.testcontainers.ydb

sealed class YdbImage private[ydb] (val name: String)

object YdbImage {

  case class Version(version: String)
      extends YdbImage(s"registry.yandex.net/yandex-docker-local-ydb:$version")

  case object Stable
      extends YdbImage("registry.yandex.net/yandex-docker-local-ydb:stable")

  case object Latest
      extends YdbImage("registry.yandex.net/yandex-docker-local-ydb:latest")

}
