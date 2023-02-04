package ru.yandex.vertis.telepony.service.impl

import akka.actor.{ActorSystem, Scheduler}
import com.typesafe.config.ConfigFactory
import ru.yandex.vertis.telepony.service.PhoneStatisticsLoaderSpec
import ru.yandex.vertis.telepony.util.{TestComponent, TestPrometheusComponent}

/**
  * @author neron
  */
class PhoneStatisticsBackgroundLoaderSpec
  extends TestPrometheusComponent
  with TestComponent
  with PhoneStatisticsLoaderSpec {

  private lazy val config = ConfigFactory.parseResources("service.conf").resolve()

  lazy val actorSystem = ActorSystem(component.name, config)

  implicit override val scheduler: Scheduler = actorSystem.scheduler

}
