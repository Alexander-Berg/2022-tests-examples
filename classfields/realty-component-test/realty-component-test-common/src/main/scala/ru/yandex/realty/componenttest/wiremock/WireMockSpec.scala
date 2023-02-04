package ru.yandex.realty.componenttest.wiremock

import java.util

import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.verification.{LoggedRequest, NearMiss}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait WireMockSpec extends BeforeAndAfterAll {
  self: WireMockProvider with Suite =>

  protected def shouldCheckForUnmatchedRequests: Boolean = true

  abstract override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.start()
  }

  abstract override def afterAll(): Unit = {
    try {
      if (shouldCheckForUnmatchedRequests) {
        checkForUnmatchedRequests()
      }
    } finally {
      wireMock.stop()
    }
    super.afterAll()
  }

  /**
    * @see [[com.github.tomakehurst.wiremock.junit.WireMockRule]]
    */
  private def checkForUnmatchedRequests(): Unit = {
    val unmatchedRequests: util.List[LoggedRequest] = wireMock.findAllUnmatchedRequests
    if (!unmatchedRequests.isEmpty) {
      val nearMisses: util.List[NearMiss] = wireMock.findNearMissesForAllUnmatchedRequests
      if (nearMisses.isEmpty) throw VerificationException.forUnmatchedRequests(unmatchedRequests)
      else throw VerificationException.forUnmatchedNearMisses(nearMisses)
    }
  }

}
