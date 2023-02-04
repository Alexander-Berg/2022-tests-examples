package ru.yandex.realty.model.user

import java.util.UUID

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.auth.AuthInfo
import ru.yandex.realty.model.user.UserRef.{app, passport, web}

/**
  * Specs on [[UserRef]] to plain representation conversion.
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class UserRefSpec extends SpecBase with PropertyChecks {

  "UserSpec" should {
    "round trip across plain string representation" in {
      forAll(UserRefGenerators.userRefGen) { userRef =>
        UserRef.parse(userRef.toPlain) should be(userRef)
      }
    }

    "parse PassportUser user from valid AuthInfo#uidOpt" in {
      val auth = AuthInfo(uidOpt = Some("93747209134"))
      UserRef(auth) should be(passport(93747209134L))
    }

    "parse NoUser from invalid AuthInfo#uidOpt" in {
      val auth = AuthInfo(uidOpt = Some("invalid"))
      UserRef(auth) should be(NoUser)
    }

    "parse WebUser from valid AuthInfo#yandexUid" in {
      val auth = AuthInfo(yandexUid = Some("93747209134"))
      UserRef(auth) should be(web("93747209134"))
    }

    "parse NoUser from invalid AuthInfo#yandexUid" in {
      val auth = AuthInfo(yandexUid = Some("invalid"))
      UserRef(auth) should be(NoUser)
    }

    "parse AppUser from valid device UUID" in {
      val deviceUuid = UUID.randomUUID().toString
      val auth = AuthInfo(uuid = Some(deviceUuid))
      UserRef(auth) should be(app(deviceUuid))
    }

  }

}
