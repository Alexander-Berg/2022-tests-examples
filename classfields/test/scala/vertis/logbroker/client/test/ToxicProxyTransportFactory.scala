package vertis.logbroker.client.test

import common.zio.logging.Logging
import eu.rekawek.toxiproxy.model.ToxicDirection
import org.testcontainers.containers.ToxiproxyContainer.ContainerProxy
import ru.yandex.kikimr.persqueue.auth.Credentials
import ru.yandex.kikimr.persqueue.cds.CDSWriterSettings
import ru.yandex.kikimr.persqueue.transport.LogbrokerTransport
import ru.yandex.kikimr.persqueue.ydb.{YdbCoreConfig, YdbTransport}
import vertis.core.model.DataCenters
import vertis.logbroker.client.model.LbErrors
import vertis.logbroker.client.{LbTask, LbTransportFactory}
import vertis.zio.BaseEnv
import zio._
import zio.duration.Duration

import java.util.function.Supplier

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class ToxicProxyTransportFactory(actualProxy: ContainerProxy) extends LbTransportFactory with LbErrors {

  override def buildTransport(
      credentialsProvider: Supplier[Credentials],
      cdsWriterSettings: Option[CDSWriterSettings]): LbTask[LogbrokerTransport] =
    lbTaskFromFuture {
      new YdbTransport.Builder(
        new YdbCoreConfig(s"${actualProxy.getContainerIpAddress}:${actualProxy.getProxyPort}", "/Root", false)
      )
        .build(credentialsProvider, cdsWriterSettings.orNull)
    }

  private def networkDown(operationId: String): RIO[BaseEnv, Unit] =
    Logging.info(s"bringing container's network down $operationId") *>
      zio.blocking.effectBlocking {
        actualProxy.toxics().timeout(s"${operationId}_down", ToxicDirection.DOWNSTREAM, 1)
        actualProxy.toxics().timeout(s"${operationId}_up", ToxicDirection.UPSTREAM, 1)
      }.unit

  private def networkUp(operationId: String, delay: Duration): RIO[BaseEnv, Unit] =
    Logging
      .info(s"bringing container's network up after $delay $operationId")
      .delay(delay) *> zio.blocking.effectBlocking {
      actualProxy.toxics().get(s"${operationId}_down").remove()
      actualProxy.toxics().get(s"${operationId}_up").remove()
    }

  def blinkNetwork(duration: Duration): ZIO[BaseEnv, Throwable, Unit] =
    for {
      randomInt <- random.nextInt
      blinkId = s"blink_$randomInt"
      _ <- ZIO.bracket[BaseEnv, Throwable, Unit, Unit](
        networkDown(blinkId),
        _ => networkUp(blinkId, duration).ignore,
        _ => Task.unit
      )
    } yield ()
}
