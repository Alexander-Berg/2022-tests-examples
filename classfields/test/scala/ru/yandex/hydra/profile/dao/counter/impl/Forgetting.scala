package ru.yandex.hydra.profile.dao.counter.impl

import org.jetbrains.annotations.NotNull
import ru.yandex.hydra.profile.dao.counter.TokenCounterDAO
import zio._

/** Remembers only current token
  *
  * @author incubos
  */
object Forgetting extends TokenCounterDAO {

  override def service: String = "forgetting-service"

  override def component: String = "forgetting-component"

  override def locale: String = "forgetting-locale"

  @NotNull
  override def add(
      @NotNull
      objectId: String,
      @NotNull
      token: String): Task[Unit] = ZIO.unit

  @NotNull
  override def get(
      @NotNull
      objectId: String): Task[Int] = UIO(0)

  @NotNull
  override def multiGet(objectIds: Set[String]): Task[Map[String, Int]] = UIO(objectIds.map(_ -> 0).toMap)
}
