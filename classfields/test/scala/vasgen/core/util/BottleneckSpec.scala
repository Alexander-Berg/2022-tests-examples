package vasgen.core.util

object BottleneckSpec extends ZIOSpecDefault with Logging {

  val expected1: List[Int] =
    Range.Int.inclusive(2, 96, 2).toList ++ List(0, 0, 0) ++
      Range.Int.inclusive(104, 200, 2).toList
  val expected2: List[Int] = Range.Int.inclusive(-200, 2, 2).toList

  override def spec =
    suite("Bottleneck")(
      test("Processing two independent sequences")(
        for {
          assertion <-
            (
              for {
                worker <- ZIO.service[Bottleneck.Service[Throwable, Int, Int]]
                f1 <-
                  ZIO
                    .foreach((1 to 100).grouped(3).toList)(items =>
                      worker
                        .process(items.toList)
                        .catchAll(_ => ZIO.succeed(List(0, 0, 0))),
                    )
                    .map(_.flatten)
                    .fork
                f2 <-
                  ZIO
                    .foreach((-100 to 1).grouped(7).toList)(items =>
                      worker
                        .process(items.toList)
                        .catchAll(_ =>
                          ZIO.succeed(List(-1, -1, -1, -1, -1, -1, -1)),
                        ),
                    )
                    .map(_.flatten)
                    .fork
                (actual1, actual2) <- f1.zip(f2).join
                // Мы точно знаем содержимое первого обработанного списка, так как ошибка происходит всегда в одном и том
                // же месте. При этом мы ничего не можем сказать, будет ли ошибка во втором списке, и, если будет,
                // то в каком месте точно. Мы ожидаем, что первые 60% второго списка будут гарантированно
                // обработаны правильно. Если тест будет флапать, нужно уменьшить это значение, например, до 50.
              } yield assert(actual1)(equalTo(expected1)) &&
                assert(actual2.size)(equalTo(expected2.size)) &&
                assert(actual2.take(60))(equalTo(expected2.take(60)))
            ).provideLayer(layer)
        } yield assertion,
      ) @@ TestAspect.ignore,
    )

  def layer
    : ZLayer[TestClock, Nothing, Bottleneck.Service[Throwable, Int, Int]] =
    (Tracing.noop ++
      (
        for {
          testClock <- ZIO.service[TestClock]
        } yield Bottleneck.Config[Throwable, Int, Int](
          (list: List[Int]) =>
            testClock.adjust(100.milliseconds) *>
              ZIO.foreach(list)(i =>
                if (i == 49)
                  ZIO
                    .fail(new IllegalArgumentException(s"We don't like the $i"))
                else
                  ZIO.succeed(i * 2),
              ),
          5,
        ),
      ).toLayer) >>> Bottleneck.live[Throwable, Int, Int]

}
