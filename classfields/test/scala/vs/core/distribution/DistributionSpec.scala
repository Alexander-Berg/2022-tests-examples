//package vs.core.distribution
//
//import bootstrap.test.BootstrapSpec
//import org.apache.curator.test.TestingServer
//import zio.*
//import zio.test.*
//
//object DistributionSpec extends BootstrapSpec[Any] {
//
//  override def spec =
//    suite("Distributor test")(
//      test("Distribute partitions over three workers") {
//        ZIO.acquireReleaseWith(ZIO.succeed(new TestingServer()))(server =>
//          ZIO.succeed(server.close()),
//        ) { server =>
//          val l1 = DistributionContext
//            .instanceLayer(server.getConnectString, "one")
//          val l2 = DistributionContext
//            .instanceLayer(server.getConnectString, "two")
//          val l3 = DistributionContext
//            .instanceLayer(server.getConnectString, "three")
//
//          for {
//            q1  <- Queue.unbounded[Seq[Int]]
//            q2  <- Queue.unbounded[Seq[Int]]
//            q3  <- Queue.unbounded[Seq[Int]]
//            e1  <- ZIO.service[ShardDistributionNotifier].provideLayer(l1)
//            e2  <- ZIO.service[ShardDistributionNotifier].provideLayer(l2)
//            e3  <- ZIO.service[ShardDistributionNotifier].provideLayer(l3)
//            _   <- e1.subscribe(seq => q1.offer(seq).unit)
//            _   <- e2.subscribe(seq => q2.offer(seq).unit)
//            s1  <- e3.subscribe(seq => q3.offer(seq).unit)
//            f1  <- e1.asInstanceOf[DistributionWorker].doDistribution.fork
//            p11 <- q1.take
//            f2  <- e2.asInstanceOf[DistributionWorker].doDistribution.fork
//            p12 <- q1.take
//            p22 <- q2.take
//            f3  <- e3.asInstanceOf[DistributionWorker].doDistribution.fork
//            p13 <- q1.take
//            p23 <- q2.take
//            p33 <- q3.take
//            _   <- f1.interrupt
//            p24 <- q2.take
//            p34 <- q3.take
//            _   <- s1.unsubscribe()
//            s2 <- e3.subscribe(_ =>
//              ZIO.fail(new IllegalStateException("Just test case")),
//            )
//            _   <- f2.interrupt
//            f4  <- e2.asInstanceOf[DistributionWorker].doDistribution.fork
//            _   <- s2.unsubscribe()
//            _   <- q2.take
//            _   <- e3.subscribe(seq => q3.offer(seq).unit)
//            _   <- q3.take
//            _   <- f4.interrupt
//            p35 <- q3.take
//            _   <- f3.interrupt
//          } yield assertTrue(p11 == Seq(0, 1, 2, 3, 4, 5, 6, 7)) &&
//            assertTrue(p12 == Seq(0, 2, 4, 6)) &&
//            assertTrue(p13 == Seq(0, 3, 6)) &&
//            assertTrue(p22 == Seq(1, 3, 5, 7)) &&
//            assertTrue(p23 == Seq(1, 4, 7)) && assertTrue(p33 == Seq(2, 5)) &&
//            assertTrue(p24 == Seq(0, 2, 4, 6)) &&
//            assertTrue(p34 == Seq(1, 3, 5, 7)) &&
//            assertTrue(p35 == Seq(0, 1, 2, 3, 4, 5, 6, 7))
//        }
//      } @@ TestAspect.ignore, // todo corney, fix it
//    )
//
//  override def RLive: ZLayer[DistributionSpec.BaseEnv, Nothing, Any] =
//    ZLayer.empty
//
//}
