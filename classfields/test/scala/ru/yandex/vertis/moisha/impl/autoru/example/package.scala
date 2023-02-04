package ru.yandex.vertis.moisha.impl.autoru

/**
  * Common stuff for client examples.
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
package object example {

  /**
    * Usage of moisha-api-01-sas on Mac OS X may produce
    *  Exception in thread "main" java.net.NoRouteToHostException: No route to host
    *    at java.net.PlainSocketImpl.socketConnect(Native Method)
    *
    * because of bug in jdk implementation
    * which fails when communicate with IPv6-only hosts with [[java.net.Socket]]
    */
  //  val MoishaTestingUrl = "http://moisha-api-01-sas.test.vertis.yandex.net:34410/api/1.x/service/autoru/price"

  val MoishaTestingUrl = "http://dev55h.vs.os.yandex.net:34410/api/1.x/service/autoru/price"

}
