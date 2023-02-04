package vertis.pushnoy.services.xiva

import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.pushnoy.MockedCtx
import vertis.pushnoy.conf.XivaConfig
import vertis.pushnoy.dao.Dao
import vertis.pushnoy.services.PushBanChecker
import vertis.pushnoy.services.apple.AppleDeviceCheckClient
import vertis.pushnoy.util.TestEventWriter
import vertis.pushnoy.util.event.EventsManager

import scala.concurrent.ExecutionContext.Implicits.global

/** @author kusaeva
  */
trait XivaManagerSupport extends MockedCtx {

  def dao: Dao
  def xivaClient: XivaClient
  def appleDeviceCheckClient: AppleDeviceCheckClient
  def deviceChecker: DeviceChecker
  def xivaConfig: XivaConfig
  def pushBanChecker: PushBanChecker
  val eventsManager: EventsManager = new EventsManager(new TestEventWriter)

  lazy val xivaManager: XivaManager = {
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
  }

}
