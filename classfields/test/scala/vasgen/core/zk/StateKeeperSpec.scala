package vasgen.core.zk

import bootstrap.logging.Logging
import org.apache.curator.test.TestingServer
import vasgen.core.saas.mock.TestSetup
import zio.test.Assertion.*
import zio.test.*
import zio.*
import zio.test.ZIOSpecDefault

object StateKeeperSpec extends ZIOSpecDefault with Logging {

  override def spec =
    suite("StateKeeper")(
      test("ChangeStateAndListenToIt") {

        Managed
          .makeEffect {
            new TestingServer()
          } {
            _.close()
          }
          .use { server =>
          (
            for {
              keeper   <- ZIO.service[StateKeeper.Service[TestSetup, Int]]
              _        <- keeper.set(1)
              one      <- keeper.current
              queue    <- Queue.sliding[Int](1)
              listener <- ZIO.service[StateKeeper.Listener[TestSetup, Int]]
              _        <- listener.listenTo(i => queue.offer(i).unit).fork
              oneToo   <- queue.take
              _        <- keeper.set(2)
              two      <- keeper.current
              twoToo   <- queue.take
              _        <- keeper.set(3)
              three    <- keeper.current
              threeToo <- queue.take
            } yield assert(one)(equalTo(1)) && assert(oneToo)(equalTo(1)) &&
              assert(two)(equalTo(2)) && assert(twoToo)(equalTo(2)) &&
              assert(three)(equalTo(3)) && assert(threeToo)(equalTo(3)),
          ).provideLayer(Context.instanceLayer(server.getConnectString))
        }

      } @@ diagnose(20.seconds) @@ flaky,
    )

}
