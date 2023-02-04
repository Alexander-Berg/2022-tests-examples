package ru.yandex.vertis.banker.dao.impl.jdbc

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object JdbcSpecConstants {

  val ActionOnDBTimeout: FiniteDuration = 2.minutes

}
