package ru.yandex.realty.rent.clients.spectrumdata

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner

/**
  * @author azakharov
  */
@RunWith(value = classOf[JUnitRunner])
class SpectrumDataTokenMakerSpec extends WordSpec with Matchers {
  "SpectrumDataTokenMaker" should {
    "calculate token for documentation example successfully" in {
      val tokenMaker = new SpectrumDataTokenMaker("test@test", "123")
      val expectedToken = "dGVzdEB0ZXN0OjE0ODM2MzQ3MjM6NTpaVGZBTzQramFDdmhWMCs2elk1dWFnPT0="
      val actualToken = tokenMaker.makeToken(tokenTtlSeconds = 5, timestampSeconds = Some(1483634723))
      actualToken.shouldEqual(expectedToken)
    }
  }
}
