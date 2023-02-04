package vertis.pushnoy

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{Suite, SuiteMixin}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.core.utils.NoWarnFilters
import vertis.pushnoy.dao.{Dao, TestDao}
import vertis.pushnoy.model.request.PushMessageV1
import vertis.pushnoy.model.template.Template
import vertis.pushnoy.services.xiva.XivaManager
import vertis.pushnoy.services.{PushBanChecker, SubscriptionsManager}
import vertis.pushnoy.util.{Logging, TestEventWriter}
import vertis.pushnoy.util.event.EventsManager

import scala.annotation.nowarn
import scala.concurrent.duration.DurationInt

trait MockedApiSuiteBase extends SuiteMixin with ApiSuiteBase with TestClients with MockedCtx with Logging {
  suite: Suite =>

  @nowarn(NoWarnFilters.UnusedParams)
  implicit def default(implicit system: ActorSystem): RouteTestTimeout = RouteTestTimeout(5.seconds)

  lazy val eventsManager: EventsManager = new EventsManager(new TestEventWriter)
  lazy val dao: Dao = new TestDao

  lazy val pushBanChecker: PushBanChecker = new PushBanChecker {
    override def isPushBanned(push: PushMessageV1): Boolean = false

    override def isPushBanned(template: Template): Boolean = false
  }

  lazy val xivaManager: XivaManager =
    new XivaManager(
      xivaConfig,
      dao,
      xivaClient,
      eventsManager,
      appleDeviceCheckClient,
      deviceChecker,
      pushBanChecker,
      TestOperationalSupport.prometheusRegistry
    )

  lazy val subscriptionsManager: SubscriptionsManager =
    new SubscriptionsManager(dao)

}

trait MockedApiSuite extends AnyFunSuite with MockedApiSuiteBase
