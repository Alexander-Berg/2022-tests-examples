package ru.yandex.vertis.billing.wm

import java.io.IOException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Specs on [[RemoteWebmasterClient]]
  */
class RemoteWebmasterClientSpec extends AnyWordSpec with Matchers {

  "RemoteWebmasterClient" should {
    "parse checkVerification valid response" in {
      val response =
        """{
          |  "application" : "webmaster-notifier",
          |  "version" : "1.0-219@2014-06-27T12:53:26",
          |  "startDate" : "2014-06-27T09:18:36.136Z",
          |  "action" : "/internal/api/checkVerification",
          |  "status" : "SUCCESS",
          |  "duration" : 12,
          |  "request" : {
          |    "source" : "test",
          |    "usersHosts" : [ {
          |      "userId" : 139551309,
          |      "hostName" : "lenta"
          |    } ],
          |    "convertToMainMirror" : false
          |  },
          |  "data" : {
          |    "usersHosts" : [ {
          |      "userId" : 139551309,
          |      "hostName" : "lenta",
          |      "verified" : false
          |    } ]
          |  },
          |  "errors" : null
          |}""".stripMargin
      RemoteWebmasterClient.parseCheckVerification(response) should be(false)
    }

    "parse checkVerification erroneous response" in {
      val response =
        """{
          |  "application" : "webmaster-notifier",
          |  "version" : "1.0-219@2014-06-27T12:53:26",
          |  "startDate" : "2014-06-27T09:18:36.136Z",
          |  "action" : "/internal/api/checkVerification",
          |  "status" : "SUCCESS",
          |  "duration" : 12,
          |  "request" : {
          |    "source" : "test",
          |    "usersHosts" : [ {
          |      "userId" : 139551309,
          |      "hostName" : "lenta"
          |    } ],
          |    "convertToMainMirror" : false
          |  },
          |  "errors" : [{"error": "Database error"}]
          |}""".stripMargin
      intercept[IOException] {
        RemoteWebmasterClient.parseCheckVerification(response)
      }
    }
  }
}
