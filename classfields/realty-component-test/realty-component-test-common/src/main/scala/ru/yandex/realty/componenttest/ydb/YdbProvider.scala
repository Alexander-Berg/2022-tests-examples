package ru.yandex.realty.componenttest.ydb

import org.testcontainers.containers.Network
import ru.yandex.realty.application.ng.ydb.{YdbConfig, YdbHostPort}
import ru.yandex.vertis.ydb.YdbContainer

import scala.util.Random

trait YdbProvider {

  val YdbPort = 2135
  val ChOverYdbPort = 8123
  val TablePrefix = "/local"

  // TODO: REMOVE ME AFTER https://st.yandex-team.ru/KIKIMR-9675
  private val network = Network.builder.enableIpv6(false).build()

  private val ydbContainer = YdbContainer.stable
  ydbContainer.container.addEnv("YDB_YQL_SYNTAX_VERSION", "1")
  ydbContainer.container.addEnv("YDB_USE_IN_MEMORY_PDISKS", "true")
  ydbContainer.container.withNetwork(network)
  ydbContainer.container.withExposedPorts(YdbPort, ChOverYdbPort)
  ydbContainer.container.start()

  sys.addShutdownHook {
    ydbContainer.stop()
    network.close()
  }

  def buildYdbConfig(): YdbConfig = {
    YdbConfig(
      address = YdbHostPort(
        ydbContainer.container.getHost,
        ydbContainer.container.getMappedPort(YdbPort)
      ),
      token = "",
      tablePrefix = s"$TablePrefix/${System.currentTimeMillis()}_${Random.nextLong()}"
    )
  }

}
