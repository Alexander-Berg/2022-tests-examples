package ru.yandex.hydra.profile.dao.limiter.service.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import ru.yandex.hydra.profile.dao.CachedGetDao
import ru.yandex.hydra.profile.dao.limiter.service.LimiterServiceSpec._
import ru.yandex.hydra.profile.dao.limiter.service.{LimiterService, LimiterServiceSpec}

/** Unit tests for [[Optimistic]] with underlying
  * [[CachedGetDao]]
  *
  * @author incubos
  */
class CachingOptimisticSpec extends TestKit(ActorSystem("CachinigOptimistingSpec")) with LimiterServiceSpec {

  def limiterService: LimiterService = new Optimistic(cachingDao, Limit)

}
