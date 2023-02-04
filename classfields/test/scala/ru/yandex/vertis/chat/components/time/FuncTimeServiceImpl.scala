package ru.yandex.vertis.chat.components.time

import org.joda.time.DateTime

/**
  * TODO
  *
  * @author aborunov
  */
class FuncTimeServiceImpl(func: => DateTime) extends TimeService {
  override def getNow: DateTime = func
}
