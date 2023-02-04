package ru.yandex.realty.managers.karma

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.api.handlers.JsonError
import ru.yandex.realty.auth.AuthInfo
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.vertis.ops.test.TestOperationalSupport

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class KarmaManagerSpec extends SpecBase with BeforeAndAfter {

  import KarmaManagerSpec._

  val features = new SimpleFeatures
  val karmaManager = new KarmaManager(TestOperationalSupport, features)

  before {
    features.CheckPassportKarmaStatusValue.setNewState(true)
  }

  "karmaManager" should {
    "throw an exception" when {
      "user has bad karma" in {
        a[JsonError] should be thrownBy karmaManager.checkKarma(BadKarmaUser)
      }

      "user has bad karmaStatus" in {
        a[JsonError] should be thrownBy karmaManager.checkKarma(BadKarmaStatusUser)
      }

      "user has bad karma and karmaStatus" in {
        a[JsonError] should be thrownBy karmaManager.checkKarma(BadKarmaAndKarmaStatusUser)
      }

      "feature flag is disabled and user has bad karma" in {
        features.CheckPassportKarmaStatusValue.setNewState(false)
        a[JsonError] should be thrownBy karmaManager.checkKarma(BadKarmaUser)
        a[JsonError] should be thrownBy karmaManager.checkKarma(BadKarmaAndKarmaStatusUser)
      }
    }

    "ignore" when {
      "user has good karma" in {
        noException should be thrownBy karmaManager.checkKarma(GoodKarmaUser)
      }

      "user is anonymous" in {
        noException should be thrownBy karmaManager.checkKarma(AnonUser)
      }

      "feature flag is disabled and user has bad karmaStatus" in {
        features.CheckPassportKarmaStatusValue.setNewState(false)
        noException should be thrownBy karmaManager.checkKarma(BadKarmaStatusUser)
      }
    }
  }

}

object KarmaManagerSpec {
  val Uid = "uid_12345"
  val GoodKarmaUser = AuthInfo(uidOpt = Some(Uid), karma = 10)
  val BadKarmaUser = AuthInfo(uidOpt = Some(Uid), karma = 81)
  val BadKarmaStatusUser = AuthInfo(uidOpt = Some(Uid), karmaStatus = 6100)
  val BadKarmaAndKarmaStatusUser = AuthInfo(uidOpt = Some(Uid), karma = 81, karmaStatus = 6100)
  val AnonUser = AuthInfo(uidOpt = None, karma = 100, karmaStatus = 6100)
}
