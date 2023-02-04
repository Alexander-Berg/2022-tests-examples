package ru.auto.api.util

import org.joda.time.{DateTime, DateTimeUtils}
import ru.auto.api.BaseSpec
import ru.auto.api.exceptions.JWTVerificationFailureException
import ru.auto.api.model.SignedData

class JwtUtilsSpec extends BaseSpec {

  "JwtUtils" should {
    "data sign success" in {
      val testNamespaceValue = "namespace"
      val testGroupIdValue = "123456"
      val testHashValue = "hash"

      val signed = JwtUtils.sign(SignedData(testNamespaceValue, testGroupIdValue, testHashValue))

      signed should not be null
      signed should not be empty
      signed.split('.').length shouldBe 3
    }

    "signed data verify success" in {
      val testNamespaceValue = "namespace"
      val testGroupIdValue = "123456"
      val testHashValue = "hash"
      //Default ttl 24 hours
      val signed = JwtUtils.sign(SignedData(testNamespaceValue, testGroupIdValue, testHashValue))

      val data = JwtUtils.verify(signed)

      data should not be null
      data.namespace shouldBe testNamespaceValue
      data.groupId shouldBe testGroupIdValue
      data.hash shouldBe testHashValue
    }

    "ttl expired" in {
      val now = DateTime.now()
      DateTimeUtils.setCurrentMillisFixed(now.getMillis)
      val testNamespaceValue = "namespace"
      val testGroupIdValue = "123456"
      val testHashValue = "hash"
      val signed = JwtUtils.sign(SignedData(testNamespaceValue, testGroupIdValue, testHashValue))

      DateTimeUtils.setCurrentMillisFixed(now.plusHours(64).getMillis)

      an[JWTVerificationFailureException] should be thrownBy JwtUtils.verify(signed)

      DateTimeUtils.setCurrentMillisSystem()
    }

    "token was expected to have 3 parts but got 1" in {
      an[JWTVerificationFailureException] should be thrownBy JwtUtils.verify("YWFh")
    }

    "decoded claim doesn't have a valid JSON format" in {
      an[JWTVerificationFailureException] should be thrownBy JwtUtils.verify("YWFh.YmJi.Y2Nj")
    }
  }
}
