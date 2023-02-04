package vasgen.indexer.saas.integration.consumer

object KafkaIntegrationSpec extends ZIOSpecDefault with Logging {
  private val batch = RawDocumentGenerator.instances(1024)
  private val topic = "general-vasgen-indexer"

  override def spec =
    suite("Kafka integration test")(
      test("Launch kafka, produce, stop, start and produce again") {

        val z = Managed
          .make(
            ZIO
              .service[EmbeddedKafkaLauncher.Service]
              .tap(_.start() *> log.warn(s"Embedded kafka started")),
          )(_.stop() *> log.info(s"stopped"))
          .use(launcher =>
            for {
              aListen <- launcher.isListeningKafka.map(assert(_)(equalTo(true)))
              worker  <- ZIO.service[LogBrokerWorker.Service]
              workerFiber <- worker.start(Subscription.topics(topic)).fork
              _           <- log.warn(s"Worker started")
              aProduce1   <- produceAndAssert(batch.take(128))
              _           <- workerFiber.interrupt
              _           <- log.info(s"Worker stopped")
              workerFiber <- worker.start(Subscription.topics(topic)).fork
              _           <- log.warn(s"Worker started again")
              aProduce2   <- produceAndAssert(batch.take(128))
              _           <- launcher.stop()
              _           <- log.info(s"Embedded kafka stopped")
              _           <- waitKafkaStoppedWithin(30.seconds)
              _           <- launcher.start()
              aListenAgain <- launcher
                .isListeningKafka
                .map(assert(_)(equalTo(true)))
              _           <- log.info(s"Embedded kafka started again")
              aProduce3   <- produceAndAssert(batch.slice(256, 512))
              _           <- workerFiber.interrupt
              _           <- log.info(s"Worker stopped again")
              workerFiber <- worker.start(Subscription.topics(topic)).fork
              _           <- log.warn(s"Worker started at last")
              aProduce4   <- produceAndAssert(batch.slice(512, 1024))
              _           <- workerFiber.interrupt
              _           <- log.info(s"Worker stopped at last")
            } yield aListen && aProduce1 && aListenAgain && aProduce2 &&
              aProduce3 && aProduce4,
          )

        z.provideLayer(ConsumerContext.live)
      },
    ) @@ ignore

  private def waitKafkaStoppedWithin(
    maxDuration: Duration,
  ): ZIO[Clock with Has[EmbeddedKafkaLauncher.Service], VasgenStatus, Unit] =
    for {
      launcher <- ZIO.service[EmbeddedKafkaLauncher.Service]
      clock    <- ZIO.service[Clock.Service]
      started  <- clock.instant
      _ <-
        (
          for {
            _         <- ZIO.sleep(200.milliseconds)
            kafka     <- launcher.isListeningKafka
            zookeeper <- launcher.isListeningZk
            current   <- clock.instant
            duration  <- ZIO.succeed(Duration.fromInterval(started, current))
            _ <-
              if (duration.compareTo(maxDuration) > 0) {
                log.error(s"Timeout $duration exceeded, $kafka $zookeeper") *>
                  ZIO.fail(
                    VasgenFailure(
                      new IllegalStateException(s"Timeout exceeded"),
                    ),
                  )
              } else
                ZIO.succeed(())

          } yield kafka | zookeeper
        ).repeatUntil(!_)
    } yield ()

  private def produceAndAssert(batch: Seq[RawDocument]) =
    for {
      producer <- ZIO.service[Producer]
      mock     <- ZIO.service[LogBrokerServiceMock.Service]
      _        <- mock.clear
      ser <-
        Serializer
          .fromKafkaSerializer(
            new RawDocumentSerializer,
            Map.empty,
            isKey = false,
          )
          .orDie
      _ <-
        producer.produceChunk(
          Chunk.fromIterable(
            batch.map(d =>
              new ProducerRecord[String, RawDocument](
                topic,
                RawDocumentUtil.getPkAsString(d).getOrElse("UNSET"),
                d,
              ),
            ),
          ),
          Serde.string,
          ser,
        ) *> log.info(s"Sent ${batch.size} documents")
      urls     <- ZIO.succeed(distinctUrl(batch))
      _        <- waitUntilNWithin(urls.size, 30.seconds)
      received <- mock.received
      _        <- log.info(s"Received: ${received.size}")
    } yield assert(received.flatMap(_.document.map(_.url)))(
      hasSameElements(urls),
    )

  private def distinctUrl(documents: Seq[RawDocument]): Seq[String] =
    documents
      .map(d =>
        "vasgen/test/" + RawDocumentUtil.getPkAsString(d).getOrElse("UNSET"),
      )
      .distinct

  private def waitUntilNWithin(
    n: Int,
    maxDuration: Duration,
  ): ZIO[Clock with Has[LogBrokerServiceMock.Service], VasgenStatus, Unit] =
    for {
      mock    <- ZIO.service[LogBrokerServiceMock.Service]
      clock   <- ZIO.service[Clock.Service]
      started <- clock.instant
      _ <-
        (
          for {
            _        <- ZIO.sleep(200.milliseconds)
            received <- mock.received.map(_.size)
            current  <- clock.instant
            duration <- ZIO.succeed(Duration.fromInterval(started, current))
            _ <-
              if (duration.compareTo(maxDuration) > 0) {
                log.error(s"Timeout $duration exceeded, $received $n") *>
                  ZIO.fail(
                    VasgenFailure(
                      new IllegalStateException(s"Timeout exceeded"),
                    ),
                  )
              } else
                ZIO.succeed(())

          } yield received
        ).repeatUntil(_ >= n)
    } yield ()

}
