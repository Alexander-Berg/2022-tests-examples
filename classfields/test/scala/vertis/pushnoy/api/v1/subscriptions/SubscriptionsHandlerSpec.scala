package vertis.pushnoy.api.v1.subscriptions

import akka.http.scaladsl.model.StatusCodes
import vertis.pushnoy.dao.{Dao, TestDao}
import vertis.pushnoy.model.request.PushMessageV1
import vertis.pushnoy.model.template.Template
import vertis.pushnoy.{ApiSuiteBase, PushnoySpecBase, TestClients}
import vertis.pushnoy.services.{PushBanChecker, SubscriptionsManager}
import vertis.pushnoy.services.xiva.XivaManagerSupport

class SubscriptionsHandlerSpec extends PushnoySpecBase with ApiSuiteBase with TestClients with XivaManagerSupport {
  lazy val subscriptionsManager: SubscriptionsManager = new SubscriptionsManager(dao)

  lazy val pushBanChecker: PushBanChecker = new PushBanChecker {
    override def isPushBanned(push: PushMessageV1): Boolean = false

    override def isPushBanned(template: Template): Boolean = false
  }

  override def dao: Dao = new TestDao

  "SubscriptionsHandler" should {

    "give list of disabled subscriptions" in {
      Get("/v1/auto/device/777/subscriptions/disabled") ~> root.route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "enable subscription notifications" in {
      Post("/v1/auto/device/777/subscriptions/TestSubscription") ~> root.route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "disable subscription notifications" in {
      Delete("/v1/auto/device/777/subscriptions/TestSubscription") ~> root.route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}
