package ru.yandex.hydra.profile.dao.clicker.impl

import org.jetbrains.annotations.NotNull
import ru.yandex.hydra.profile.dao.clicker.service.ClickerService

import scala.concurrent.{ExecutionContext, Future}

/** Remembers nothing
  *
  * @author incubos
  */
object Forgetting extends ClickerService {

  @NotNull
  override def increment(
      @NotNull
      objectId: String
    )(implicit executionContext: ExecutionContext): Future[Unit] =
    Future.successful(())

  @NotNull
  override def count(
      @NotNull
      objectId: String
    )(implicit executionContext: ExecutionContext): Future[Int] =
    Future.successful(0)

  override def count(objectIds: Set[String])(implicit executionContext: ExecutionContext): Future[Map[String, Int]] =
    Future.successful(objectIds.map(_ -> 0).toMap)
}
