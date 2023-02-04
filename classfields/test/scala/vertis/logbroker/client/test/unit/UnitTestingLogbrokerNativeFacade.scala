package vertis.logbroker.client.test.unit

import vertis.logbroker.client.consumer.config.LbConsumerSessionConfig
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession.{ConsumerCb, CreateConsumerListener}
import vertis.logbroker.client.model.LogbrokerError.translateLbException
import vertis.logbroker.client.producer.LbNativeProducerSession
import vertis.logbroker.client.producer.LbNativeProducerSession.ProducerCb
import vertis.logbroker.client.producer.config.LbProducerSessionConfig
import vertis.logbroker.client.test.unit.UnitTestingLogbrokerNativeFacade._
import vertis.logbroker.client._
import zio.blocking.Blocking
import zio.{UIO, URIO, ZIO, ZManaged}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class UnitTestingLogbrokerNativeFacade(
    makeConsumer: (LbConsumerSessionConfig, ConsumerCb) => BLbTask[LbNativeConsumerSession] = NoConsumer,
    makeProducer: (LbProducerSessionConfig, ProducerCb) => BLbTask[LbNativeProducerSession] = NoProducer)
  extends LogbrokerNativeFacade {

  override def makeProducerSession(
      config: LbProducerSessionConfig
    )(callback: ProducerCb): BLbTask[LbNativeProducerSession] =
    makeProducer(config, callback)

  override def openConsumerSession(
      config: LbConsumerSessionConfig,
      createListener: Option[CreateConsumerListener] = None
    )(callback: ConsumerCb): BLbTask[LbNativeConsumerSession] =
    makeConsumer(config, callback)
}

object UnitTestingLogbrokerNativeFacade {

  val NoConsumer: (LbConsumerSessionConfig, ConsumerCb) => LbTask[LbNativeConsumerSession] =
    (_, _) => ZIO.fail(translateLbException(new UnsupportedOperationException("No consumer in this test")))

  val NoProducer: (LbProducerSessionConfig, ProducerCb) => LbTask[LbNativeProducerSession] =
    (_, _) => ZIO.fail(translateLbException(new UnsupportedOperationException("No producer in this test")))
}
