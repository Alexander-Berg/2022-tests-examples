package ru.yandex.vertis.passport.service.identity

import org.joda.time.DateTime
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.model.{ApiPayload, ClientInfo, Platforms, RequestContext}
import ru.yandex.vertis.passport.test.SpecBase
import ru.yandex.vertis.passport.util.DateTimeUtils.DateTimeOrdering

/**
  *
  * @author zvez
  */
class IdentityServiceImpl2Spec extends WordSpec with SpecBase {

  val service = new IdentityServiceImpl2

  implicit val ctx: RequestContext = RequestContext("123")

  "IdentityServiceImpl2" should {
    "generate values" in {
      (1 to 10000).foreach { _ =>
        val uid = service.generate()
        service.validate(uid) shouldBe true
        uid.length shouldBe 65
      }
    }
    "generate unique values" in {
      val generated = (1 to 10000).map(_ => service.generate())
      generated.size shouldBe generated.distinct.size
    }

    "be able to validate uid" in {
      service.validate("fake") shouldBe false
      service.validate("fake.for-real") shouldBe false
      service.validate(service.generate()) shouldBe true
      service.validate(service.generate().drop(1)) shouldBe false
    }

    "validate legacy autoruuid" in {
      val realUid = "93ad226bd64c7d2b7a0e331f4ac991a6.b5c269b6d3123d42f9a28ed0a6fb5554"
      service.validate(realUid) shouldBe true

      service.validate(LegacyIdentityService.generate()) shouldBe true
    }

    "validate legacy suid" in {
      val realUid = "bfed1c1dbde0a7c0085e1351f09c847c.0d789907681470cc5662a21bb04c7646"
      service.validate(realUid) shouldBe true
    }

    "still validate initially generated uid" in {
      val uid = "g59706ee759lpho0c6bucn4g4jj3kd9g.944594d6bd2f651411efbfcef8529f65"
      service.validate(uid) shouldBe true
      val Some(data) = service.validateAndExtract(uid)
      data.version shouldBe 1
      data.platform shouldBe Platforms.Android
      data.ts.toDateTime.toLocalDate.toString shouldBe "2017-07-20"
    }

    "extract payload from deviceUid" in {
      val t1 = DateTime.now().withMillisOfSecond(0)
      val uid = service.generate()
      val t2 = DateTime.now().withMillisOfSecond(0)
      val dataOpt = service.validateAndExtract(uid)
      dataOpt shouldBe defined
      val Some(data) = dataOpt
      data.version shouldBe 1
      data.ts.toDateTime shouldBe >=(t1)
      data.ts.toDateTime shouldBe <=(t2)
      data.platform shouldBe Platforms.Undefined
    }

    "store platform in deviceUid" in {
      val ctx = wrap(ApiPayload("123", ClientInfo(platform = Some("m"))))
      val uid = service.generate()(ctx)
      val Some(data) = service.validateAndExtract(uid)
      data.platform shouldBe Platforms.FrontendMobile
    }
  }

}
