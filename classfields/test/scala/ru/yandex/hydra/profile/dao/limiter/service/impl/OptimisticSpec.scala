package ru.yandex.hydra.profile.dao.limiter.service.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import ru.yandex.hydra.profile.dao.limiter.service.LimiterServiceSpec._
import ru.yandex.hydra.profile.dao.limiter.service.{LimiterService, LimiterServiceSpec}

/** Unit tests for [[Optimistic]]
  *
  * @author incubos
  */
class OptimisticSpec extends TestKit(ActorSystem("OptimistingSpec")) with LimiterServiceSpec {
  def limiterService: LimiterService = new Optimistic(limiterDao, Limit)
}
