package vertis.logbroker.client.test

import org.scalatest.BeforeAndAfterAll
import ru.yandex.kikimr.persqueue.auth.Credentials
import vertis.zio.BaseEnv
import vertis.logbroker.client.{LbClient, LbTransportFactory, LogbrokerNativeFacade}
import vertis.zio.test.ZioSpecBase
import zio._
import zio.duration._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait LbTest extends ZioSpecBase with BeforeAndAfterAll {

  private var _lbServer: LbServer = _

  protected def lbServer: LbServer = _lbServer

  override protected val ioTestTimeout: Duration = 3.minutes

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    _lbServer = LbServer.create(testTopics)
  }

  override protected def afterAll(): Unit = {
    lbServer.close()
  }

  protected def topicNameFromClassName: String =
    "/" + this.getClass.getSimpleName
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .toLowerCase()

  protected def testTopics: Set[String] = Set(
    topicNameFromClassName
  )

  protected def transportFactory: Task[ToxicProxyTransportFactory] = lbServer.newTransportFactory()

  protected def transportFactoryM: RManaged[BaseEnv, ToxicProxyTransportFactory] =
    lbServer.newTransportFactory().toManaged_

  protected def lbLocalFacade(transportFactory: LbTransportFactory): Task[LogbrokerNativeFacade] = Task {
    new IntTestingLogbrokerNativeFacade(transportFactory, Credentials.NONE_PROVIDER, logger)
  }

  protected def lbLocalFacadeM(transportFactory: LbTransportFactory): RManaged[BaseEnv, LogbrokerNativeFacade] =
    lbLocalFacade(transportFactory).toManaged_

  /** Optionally fail network
    * this failure closes connection sending reset and opens it back in 50ms
    */
  def scheduleNetworkFailure(
      every: Option[Duration],
      balancer: ToxicProxyTransportFactory): URIO[BaseEnv, Fiber[Throwable, Unit]] =
    every match {
      case None => Task.unit.as(Fiber.never)
      case Some(duration) =>
        val blink = balancer.blinkNetwork(50.millis).delay(duration)
        blink.forkDaemon // TODO: make sure that producer/consumer react on failures
    }

  protected def makeLbClient =
    for {
      _transportFactory <- transportFactory
      facade <- lbLocalFacade(_transportFactory)
    } yield new LbClient(facade)

  protected def makeLbClientM =
    for {
      transportFactory <- transportFactoryM
      facade <- lbLocalFacadeM(transportFactory)
    } yield new LbClient(facade)
}
