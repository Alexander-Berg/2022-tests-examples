package ru.yandex.vertis.curator.recipes.discovery

import org.apache.curator.x.discovery.{ServiceInstance, ServiceType}

/** Collected test data and utilities */
object TestData {

  object DataCenters {
    val Myt = "myt"
    val Fol = "fol"
    val Ugr = "ugr"
  }

  object Hosts {
    val csbo1ft = "csbo1ft.yandex.ru"
    val csbo2ft = "csbo2ft.yandex.ru"
    val csbo1gt = "csbo1gt.yandex.ru"
  }

  object Deploys {
    val csbo1ft = Deploy(DataCenters.Ugr, Hosts.csbo1ft)
    val csbo2ft = Deploy(DataCenters.Ugr, Hosts.csbo2ft)
    val csbo1gt = Deploy(DataCenters.Fol, Hosts.csbo1gt)
  }

  /** Convenient method for create [[ServiceInstance]]s
    */
  def serviceInstance(id: String,
                      payload: String,
                      serviceName: String,
                      serviceType: ServiceType = ServiceType.DYNAMIC,
                      address: String = "example.org",
                      port: Option[Int] = Some(1234),
                      registrationTime: Option[Long] = Some(System.currentTimeMillis()),
                      sslPort: Option[Int] = Some(3456)) = {
    val builder = ServiceInstance.builder().
      id(id).
      name(serviceName).
      serviceType(serviceType).
      address(address).
      payload(payload).
      registrationTimeUTC(System.currentTimeMillis())

    for (p <- port) builder.port(p)
    for (p <- sslPort) builder.sslPort(p)

    builder.build()
  }

}
