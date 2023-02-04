package ru.yandex.vertis.chat.components.time

import org.joda.time.DateTime

/**
  * TODO
  *
  * @author aborunov
  */
class SetTimeServiceImpl extends TimeService {
  private var inner = new DateTime(0)

  def setNow(newNow: DateTime): Unit = {
    inner = newNow
  }

  override def getNow: DateTime = inner
}
