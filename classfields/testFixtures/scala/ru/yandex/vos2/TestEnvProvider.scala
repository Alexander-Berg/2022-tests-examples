package ru.yandex.vos2

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vos2.util.environment.EnvProvider

/**
  * Created by roose on 28.12.15.
  */
class TestEnvProvider(component: String = "") extends EnvProvider {

  override val environmentType: String = "unit-tests"

  override val props: Config = {
    val pl = ConfigFactory.parseResources("properties.unit-tests.local.conf")
    val p = ConfigFactory.parseResources("properties.unit-tests.conf")
    val a = ConfigFactory.parseResources("application.unit-tests.conf")
    val c = ConfigFactory.parseResources(s"$component.unit-tests.conf")
    ConfigFactory.load(c.withFallback(pl).withFallback(p).withFallback(a))
  }
}
