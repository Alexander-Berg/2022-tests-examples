package ru.yandex.vertis.chat.components.time

import java.util.concurrent.atomic.AtomicReference

import org.joda.time.DateTime

trait TestTimeSupport extends TimeAware {
  val time = new AtomicReference(DateTime.now())

  override val timeService: TimeService = new FuncTimeServiceImpl(time.get())

  def tickTime: Unit = resetTime(time.get().plusMinutes(1))

  def resetTime(newTime: DateTime): Unit = time.set(newTime)

}
