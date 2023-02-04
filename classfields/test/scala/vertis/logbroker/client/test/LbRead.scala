package vertis.logbroker.client.test

import vertis.logbroker.client.consumer.config.LbConsumerSessionConfig
import vertis.logbroker.client.consumer.model.out.BatchRequestMessage
import vertis.logbroker.client.consumer.session.LbConsumerSession
import vertis.zio.{BTask, BaseEnv}
import vertis.core.model.{DataCenter, DataCenters}
import vertis.logbroker.client.LogbrokerNativeFacade
import vertis.logbroker.client.metrics.NopConsumerSessionMetrics
import vertis.logbroker.client.consumer.model.in.ReadResult
import vertis.logbroker.client.consumer.model.offsets.OffsetSource
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession.CreateConsumerListener
import zio._
import zio.duration._
import zio.stream.ZStream

/** A simple read to use in write tests
  *
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait LbRead extends LbTest {

  protected def readFromDc(
      facade: LogbrokerNativeFacade,
      offsetSource: Option[OffsetSource],
      createListener: Option[CreateConsumerListener] = None
    )(dc: DataCenter,
      topic: String,
      groups: Set[Int],
      readQSize: Int = 1): BTask[LbConsumerSession] = {
    val sessionConfig = LbConsumerSessionConfig(
      "/vertis/broker/test/consumer",
      dc,
      topic,
      groups,
      readQSize,
      commitOffsets = true
    )
    val makeConsumer = facade.openConsumerSession(sessionConfig, createListener) _
    LbConsumerSession.openSession(
      sessionConfig,
      makeConsumer,
      NopConsumerSessionMetrics,
      offsetSource = offsetSource
    )
  }

  private def resources(
      qSize: Int): RManaged[BaseEnv, (ToxicProxyTransportFactory, LogbrokerNativeFacade, Queue[ReadResult])] =
    for {
      balancer <- transportFactoryM
      facade <- lbLocalFacadeM(balancer)
      q <- Queue.bounded[ReadResult](qSize).toManaged(_.shutdown)
    } yield (balancer, facade, q)

  protected def read(
      topic: String,
      groups: Seq[Int],
      awaitFor: Duration,
      networkFailEvery: Option[Duration] = None,
      readQSize: Int = 1,
      readBatchSize: Int = 1,
      offsetSource: Option[OffsetSource] = None,
      createListener: Option[CreateConsumerListener] = None): ZIO[BaseEnv, Throwable, Long] =
    resources(readBatchSize * groups.size).use { case (balancer, facade, q) =>
      val batchRequest = BatchRequestMessage(readBatchSize, Int.MaxValue)
      readFromDc(facade, offsetSource, createListener)(DataCenters.Sas, topic, groups.toSet, readQSize)
        .toManaged(_.close)
        .tap { session =>
          LbConsumerSession
            .readIntoQueue(session, batchRequest, q)
            .map(fiber => fiber.await.forkDaemon)
            .toManaged_
        }
        .use_ {
          for {
            read <- ZStream.fromQueueWithShutdown(q).map(_.count).runSum.fork
            _ <- scheduleNetworkFailure(networkFailEvery, balancer)
            _ <- (ZIO.sleep(awaitFor) *> q.shutdown).fork
            readCount <- read.join
            _ <- logger.info(s"Read $readCount messages")
          } yield readCount.toLong
        }
    }

}
