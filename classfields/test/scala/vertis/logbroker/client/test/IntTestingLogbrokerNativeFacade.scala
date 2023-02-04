package vertis.logbroker.client.test

import common.zio.logging.Logging

import java.util.function.Supplier
import ru.yandex.kikimr.persqueue.auth.Credentials
import ru.yandex.kikimr.persqueue.consumer.transport.LogbrokerConsumerTransport
import ru.yandex.kikimr.persqueue.producer.transport.LogbrokerProducerV1Transport
import vertis.logbroker.client.consumer.config.LbConsumerSessionConfig
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession.{ConsumerCb, CreateConsumerListener}
import vertis.logbroker.client.consumer.session.lb_native.{LbNativeConsumerSession, LbNativeConsumerSessionImpl}
import vertis.logbroker.client.producer.LbNativeProducerSession.ProducerCb
import vertis.logbroker.client.producer.config.LbProducerSessionConfig
import vertis.logbroker.client.producer.{LbNativeProducerSession, LbNativeProducerSessionImpl}
import vertis.logbroker.client.{BLbManaged, BLbTask, LbTransportFactory, LogbrokerNativeFacade}
import vertis.logbroker.client.model.LbErrors

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class IntTestingLogbrokerNativeFacade(
    lbFactory: LbTransportFactory,
    credentialsProvider: Supplier[Credentials],
    logger: Logging.Service)
  extends LogbrokerNativeFacade
  with LbErrors {

  override def openConsumerSession(
      config: LbConsumerSessionConfig,
      createListener: Option[CreateConsumerListener] = None
    )(callback: ConsumerCb): BLbTask[LbNativeConsumerSession] =
    for {
      _ <- logger.info(s"Open lb native session for test: $config")
      lbTransport <- lbFactory.buildTransport(credentialsProvider, None)
      transport <- lbTask(new LogbrokerConsumerTransport(lbTransport, credentialsProvider))
    } yield new LbNativeConsumerSessionImpl(config, transport, callback, createListener = createListener)

  override def makeProducerSession(
      config: LbProducerSessionConfig
    )(callback: ProducerCb): BLbTask[LbNativeProducerSession] =
    openProducerSession(config)(callback)

  private def openProducerSession(
      config: LbProducerSessionConfig
    )(callback: ProducerCb): BLbTask[LbNativeProducerSession] =
    for {
      _ <- logger.info(s"Open lb native session for test: $config")
      lbTransport <- lbFactory.buildTransport(credentialsProvider, None) // TODO
      transport <- lbTask(new LogbrokerProducerV1Transport(lbTransport, credentialsProvider))
    } yield new LbNativeProducerSessionImpl(config, transport, callback, logger)
}
