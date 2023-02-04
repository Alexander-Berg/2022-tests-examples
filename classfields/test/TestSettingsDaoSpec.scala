package auto.dealers.calltracking.storage.test

import auto.dealers.calltracking.model.{ClientId, ClientSettings}
import auto.dealers.calltracking.model.testkit.ClientSettingsGen
import auto.dealers.calltracking.storage.SettingsDao
import auto.dealers.calltracking.storage.testkit.TestSettingsDao
import zio.ZIO
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test._
import zio.test.Assertion._
import scala.concurrent.duration._

object TestSettingsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("TestSettingsDao")(
      testM("Should return default settings") {
        checkM(Gen.anyLong) { clientId =>
          for {
            settings <- SettingsDao.getClientSettings(ClientId(clientId))
          } yield assert(settings)(equalTo(ClientSettings()))
        }
      },
      testM("Should update settings") {
        checkM(Gen.anyLong, ClientSettingsGen.anyClientSettings) { (clientId, clientSettings) =>
          for {
            newSettings <- ZIO.succeed(clientSettings)
            _ <- SettingsDao.updateClientSettings(ClientId(clientId), newSettings)
            updated <- SettingsDao.getClientSettings(ClientId(clientId))
          } yield assert(updated)(equalTo(newSettings))
        }
      }
    ).provideCustomLayer(TestSettingsDao.live)
  }
}
