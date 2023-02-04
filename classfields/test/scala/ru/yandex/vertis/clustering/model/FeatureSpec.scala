package ru.yandex.vertis.clustering.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.utils.features.FeatureHelpers

@RunWith(classOf[JUnitRunner])
class FeatureSpec extends BaseSpec {

  private val testsEquality = Seq(
    (Suid("430e24fcd0bfb6f0518f32fe9cb2f9ac.40e59cacbb4b2d23bdaff6226f8e6afa"),
     Suid("430e24fcd0bfb6f0518f32fe9cb2f9ac.40e59cacbb4b2d23bdaff6226f8e6afa")),
    (FeatureHelpers.parsePhoneNet("+7 (987) 4567891"), FeatureHelpers.parsePhoneNet("+7(987)4561111")),
    (FeatureHelpers.parsePhone("+7 (987) 456-78-91"), FeatureHelpers.parsePhone("79874567891")),
    (FeatureHelpers.parseIpNet("192.168.181.1"), FeatureHelpers.parseIpNet("192.168.181.99"))
  )

  private val testsNotEquality = Seq(
    (Suid("430e24fcd0bfb6f0518f32fe9cb2f9ac.40e59cacbb4b2d23bdaff6226f8e6afa"),
     Suid("28ac42eb09cd729ccacc758eaabd7fa3.5525a1e8f97065a584b598c062773563")),
    (FeatureHelpers.parsePhoneNet("+7(123)4567891"), FeatureHelpers.parsePhoneNet("+7(123)4521111")),
    (FeatureHelpers.parseIpNet("192.168.181.1"), FeatureHelpers.parseIpNet("192.168.182.1"))
  )

  "Feature" should {

    intercept[IllegalArgumentException] {
      Ip("FOR THE JOB AT HAND")
      FeatureHelpers.parseIpNet("THE RIGHT TECHNOLOGY STACK")
      FeatureHelpers.parseIpNet("2a02:6b8::2:242")
      FeatureHelpers.parsePhoneNet("12345")
    }

    "Ip" in {
      Ip("192.168.0.11") shouldBe Ip("192.168.0.11")
    }

    "IpNet" in {
      FeatureHelpers.parseIpNet("192.168.0.255") shouldBe IpNet("192.168.0.255")
      FeatureHelpers.parseIpNet("192.168.0.11").value shouldBe "192.168.0.255"
    }

    "PhoneNet" in {
      FeatureHelpers.parsePhoneNet("+7 965 286 10 88").value shouldBe "7965286"
      FeatureHelpers.parsePhone("+7 965 286 10 88") shouldBe Phone("79652861088")
    }

    "Phone" in {
      Phone.validate("+7 965 286 10 88") should be(true)
      Phone.validate("+7-965-286-10-88") should be(true)
      Phone.validate("+7 (965) 286-1088") should be(true)
      Phone.validate("+7 (965) 286-10-88") should be(true)
      Phone.validate("+7(965)286-10-88") should be(true)
      Phone.validate("+7(965)2861088") should be(true)
      Phone.validate("+79652861088") should be(true)
      Phone.validate("89652861088") should be(true)
      Phone.validate("79652861088") should be(true)
      Phone.validate("9652861088") should be(true)
      Phone.validate("375333638580") should be(true)

      Phone.validate("+7 123 286 10 88") should be(false)
      Phone.validate("+9 965 286 10 88") should be(false)
      Phone.validate("+1 (123) 456–7890") should be(false)
      Phone.validate("65286108") should be(false)
    }

    testsEquality.foreach { item =>
      s"$item equality" in {
        item._1 == item._2 shouldBe true
      }
    }

    testsNotEquality.foreach { item =>
      s"$item not equality" in {
        item._1 != item._2 shouldBe true
      }
    }
  }
}
