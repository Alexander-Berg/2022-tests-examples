package ru.yandex.hydra.profile.dao.limiter.service.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import ru.yandex.hydra.profile.dao.limiter.service.LimiterServiceSpec._
import ru.yandex.hydra.profile.dao.limiter.service.{LimiterService, LimiterServiceSpec}

/** Unit tests for [[Pessimistic]]
  *
  * @author incubos
  */
class PessimisticSpec extends TestKit(ActorSystem("PessimisticSpec")) with LimiterServiceSpec {
  def limiterService: LimiterService = new Pessimistic(limiterDao, Limit)
}
