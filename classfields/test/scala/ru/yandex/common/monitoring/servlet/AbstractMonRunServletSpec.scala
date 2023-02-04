package ru.yandex.common.monitoring.servlet

import com.codahale.metrics.health.HealthCheck
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/**
 * Specs on [[AbstractMonRunServlet]]
 */
@RunWith(classOf[JUnitRunner])
class AbstractMonRunServletSpec
  extends WordSpec
  with Matchers {

  import AbstractMonRunServlet._

  "AbstractMonRunServlet" should {
    "plain healthcheck messages" in {
      plain(Map(
        "0" -> HealthCheck.Result.unhealthy(""),
        "1" -> HealthCheck.Result.unhealthy("1"),
        "2" -> HealthCheck.Result.unhealthy("2\nnext string"),
        "3" -> HealthCheck.Result.unhealthy("3\nnext strings," +
          "fdsjkfndsjkf,jsdnfs,nfjsdjkfjhsdjfkhskfhsdkjhfksdjfhskjfhsdsfjskfdsh" +
          "fkshkfhdskjhfkjdshfkhsdfhsjkfhdjshjkhfshfkjsdhjkfhjdssdfdsfsdfsddfsd" +
          "sdsffdsddsfdsdffsfdssdfdssfdffsddsfdssd"),
        "4" -> HealthCheck.Result.unhealthy("4\r\nnext string"),
        "5" -> HealthCheck.Result.unhealthy("5"),
        "6" -> HealthCheck.Result.unhealthy("6 a\nb\n\rc\nd"),
        "7" -> HealthCheck.Result.unhealthy(new NullPointerException())
      )) should be ("0: , 1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6 a, 7: null")

      plain(Map(
        "100" -> HealthCheck.Result.unhealthy(
          "Task metrika-calls-store failed on dev04i.vs.os.yandex.net at 2015-11-17T18:52:20.455+03:00: Connection refused, Connection refused")
      )) should be ("100: Task metrika-calls-store failed on dev04i.vs.os.yandex.net at 2015-11-17T18:52:20.455+03:00: Connection refused, Connect")

    }
  }

}
