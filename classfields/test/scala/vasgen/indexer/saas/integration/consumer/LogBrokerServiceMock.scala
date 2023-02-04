package vasgen.indexer.saas.integration.consumer

import scala.collection.mutable.ArrayBuffer

object LogBrokerServiceMock {

  def mock: ZLayer[Any, Nothing, Has[Service]] =
    ZIO.succeed(LogBrokerServiceImpl).toLayer

  def live: ZLayer[Any, VasgenStatus, Has[LogBrokerService.Service]] =
    ZIO.succeed(LogBrokerServiceImpl).toLayer

  trait Service {
    def clear: IO[VasgenStatus, Unit]
    def received: IO[VasgenStatus, Seq[TMessage]]
  }

  object LogBrokerServiceImpl
      extends LogBrokerService.Service
         with Service
         with Logging {
    private val buffer = ArrayBuffer.empty[TMessage]

    override def send(messages: Iterable[TMessage]): IO[VasgenStatus, Unit] =
      log.info(s"Receive ${messages.size} messages").as(buffer.addAll(messages))

    override def received: IO[VasgenStatus, Seq[TMessage]] =
      ZIO.succeed(buffer.toSeq)

    override def clear: IO[VasgenStatus, Unit] = ZIO.succeed(buffer.clear())
  }

}
