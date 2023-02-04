package ru.yandex.hydra.profile.dao.limiter.service.impl

import ru.yandex.hydra.profile.dao.limiter.service.LimiterService

import scala.concurrent.{ExecutionContext, Future}

/** Passes everything
  *
  * @author incubos
  */
object Forgetting extends LimiterService {

  def budgetOf(user: String, limit: Option[Int])(implicit executionContext: ExecutionContext): Future[Int] =
    Future.successful(1)

  override protected def defaultLimit: Int = 10
}
