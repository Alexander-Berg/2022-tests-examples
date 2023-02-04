package ru.yandex.vertis.billing.util.clean

import scala.util.Try

trait CleanableDao {

  def clean(): Try[Unit]

}
