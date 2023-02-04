package vasgen.core.events

object StatePublisherSpec extends ZIOSpecDefault with Logging {

  override def spec =
    suite("StatePublisher")(
      test("Don't call subscription on empty state") {
        assertZIO(
          (
            for {
              deliverer <- ZIO.service[StringStatePublisher]
              queue     <- Queue.unbounded[String]
              _         <- deliverer.subscribe(s => queue.offer(s).unit)
              tail      <- queue.poll
            } yield tail
          ).exit,
        )(succeeds(isNone))
      },
      test("Call subscription on non-empty initial state") {
        assertZIO(
          (
            for {
              deliverer <- ZIO.service[StringStatePublisher]
              queue     <- Queue.unbounded[String]
              _         <- deliverer.currentState.set(Some("one"))
              _         <- deliverer.subscribe(s => queue.offer(s).unit)
              all       <- queue.takeUpTo(2)
            } yield all
          ).exit,
        )(succeeds(equalTo(Seq("one"))))
      },
      test("Call subscription every  time") {
        assertZIO(
          (
            for {
              deliverer <- ZIO.service[StringStatePublisher]
              queue     <- Queue.unbounded[String]
              _         <- deliverer.currentState.set(Some("one"))
              _         <- deliverer.subscribe(s => queue.offer(s).unit)
              _         <- deliverer.notifyStateChanged("two")
              _         <- deliverer.notifyStateChanged("three")
              all       <- queue.takeUpTo(3)
            } yield all
          ).exit,
        )(succeeds(equalTo(Seq("one", "two", "three"))))
      },
      test("Don't call after unsubscribe") {
        assertZIO(
          (
            for {
              deliverer    <- ZIO.service[StringStatePublisher]
              queue        <- Queue.unbounded[String]
              _            <- deliverer.currentState.set(Some("one"))
              subscription <- deliverer.subscribe(s => queue.offer(s).unit)
              _            <- deliverer.notifyStateChanged("two")
              _            <- subscription.unsubscribe()
              _            <- deliverer.notifyStateChanged("three")
              all          <- queue.takeUpTo(4)
            } yield all
          ).exit,
        )(succeeds(equalTo(Seq("one", "two"))))
      },
    ).provideLayerShared(layer) @@ TestAspect.sequential

  def layer: ZLayer[Any, Nothing, StringStatePublisher] =
    (
      for {
        initialState <- Ref.make[Option[String]](None)
        subscriptions <- Ref
          .make(Map.empty[Int, String => IO[VasgenStatus, Unit]])
      } yield StringStatePublisher(
        currentState = initialState,
        subscriptions = subscriptions,
      )
    ).toLayer

  case class StringStatePublisher(
      currentState: Ref[Option[String]],
      subscriptions: Ref[Map[Int, String => IO[VasgenStatus, Unit]]],
  ) extends StatePublisher[String] {

    override def notifyStateChanged(state: String): UIO[Unit] =
      super.notifyStateChanged(state)

    override protected def getCurrentState(): UIO[Option[String]] =
      currentState.get

  }

}
