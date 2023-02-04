package vertis.yt.zio

import common.yt.Yt.Attribute.ExpirationTime
import common.yt.{YtError, YtNoSchedulerConfigured}
import org.scalatest.BeforeAndAfterAll
import ru.yandex.inside.yt.kosher.cypress.YPath
import vertis.zio.BaseEnv
import vertis.yt.RealYtTest
import vertis.yt.model.YPaths
import vertis.yt.zio.YtZioTest.{YtTestResources, testTtl}
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.test.ZioSpecBase
import zio.duration._
import zio.{Schedule, ZManaged}
import java.time.Instant

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait YtZioTest extends ZioSpecBase with RealYtTest with BeforeAndAfterAll {

  override protected val ioTestTimeout: Duration = 2.minutes

  val noScheduler: Schedule[Any, YtError, YtError] =
    Schedule.recurUntil[YtError](_.isInstanceOf[YtNoSchedulerConfigured])

  def ytResources: ZManaged[BaseEnv, Throwable, YtTestResources] =
    ytZio.map(YtTestResources)

  def testBasePath: YPath = YPaths.path(ytConfig.basePath, s"${getClass.getName}")

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    initContainer()

    runSync {
      ytZio.use { yt =>
        // create folder for the test and set expiration for the base path
        yt.cypressNoTx.touchDir(testBasePath) *>
          yt.cypressNoTx.setAttribute(ytConfig.basePath, ExpirationTime.withValue(Instant.now.plus(testTtl)))
      }
    }
    ()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    runSync {
      ytZio.use(_.cypressNoTx.drop(testBasePath))
    }
    ()
  }
}

object YtZioTest {
  case class YtTestResources(yt: YtZio)
  private val testTtl = 15.minutes
}
