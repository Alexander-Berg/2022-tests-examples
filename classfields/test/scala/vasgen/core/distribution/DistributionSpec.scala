package vasgen.core.distribution

object DistributionSpec extends ZIOSpecDefault with Logging {

  override def spec =
    suite("Distributor test")(
      test("Distribute partitions over three workers") {
        ZIO.bracket(ZIO.succeed(new TestingServer()))(server =>
          ZIO.succeed(server.close()),
        ) { server =>
          val l1 = DistributionContext
            .instanceLayer(server.getConnectString, "one")
          val l2 = DistributionContext
            .instanceLayer(server.getConnectString, "two")
          val l3 = DistributionContext
            .instanceLayer(server.getConnectString, "three")

          for {
            q1  <- Queue.unbounded[Seq[Int]]
            q2  <- Queue.unbounded[Seq[Int]]
            q3  <- Queue.unbounded[Seq[Int]]
            e1  <- ZIO.service[Distribution.Service].provideLayer(l1)
            e2  <- ZIO.service[Distribution.Service].provideLayer(l2)
            e3  <- ZIO.service[Distribution.Service].provideLayer(l3)
            _   <- e1.subscribe(seq => q1.offer(seq).unit)
            _   <- e2.subscribe(seq => q2.offer(seq).unit)
            s1  <- e3.subscribe(seq => q3.offer(seq).unit)
            f1  <- e1.asInstanceOf[DistributionWorker].doDistribution.fork
            p11 <- q1.take
            f2  <- e2.asInstanceOf[DistributionWorker].doDistribution.fork
            p12 <- q1.take
            p22 <- q2.take
            f3  <- e3.asInstanceOf[DistributionWorker].doDistribution.fork
            p13 <- q1.take
            p23 <- q2.take
            p33 <- q3.take
            _   <- f1.interrupt
            p24 <- q2.take
            p34 <- q3.take
            _   <- s1.unsubscribe()
            s2 <- e3
              .subscribe(_ => ZIO.fail(VasgenErrorContainer("Just test case")))
            _   <- f2.interrupt
            f4  <- e2.asInstanceOf[DistributionWorker].doDistribution.fork
            _   <- s2.unsubscribe()
            _   <- q2.take
            _   <- e3.subscribe(seq => q3.offer(seq).unit)
            _   <- q3.take
            _   <- f4.interrupt
            p35 <- q3.take
            _   <- f3.interrupt
          } yield assert(p11)(equalTo(Seq(0, 1, 2, 3, 4, 5, 6, 7))) &&
            assert(p12)(equalTo(Seq(0, 2, 4, 6))) &&
            assert(p13)(equalTo(Seq(0, 3, 6))) &&
            assert(p22)(equalTo(Seq(1, 3, 5, 7))) &&
            assert(p23)(equalTo(Seq(1, 4, 7))) &&
            assert(p33)(equalTo(Seq(2, 5))) &&
            assert(p24)(equalTo(Seq(0, 2, 4, 6))) &&
            assert(p34)(equalTo(Seq(1, 3, 5, 7))) &&
            assert(p35)(equalTo(Seq(0, 1, 2, 3, 4, 5, 6, 7)))
        }
      } @@ TestAspect.ignore, // todo corney, fix it
    )

}
