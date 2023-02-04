package vasgen.entrypoint

object WorkDistributorSpec extends ZIOSpecDefault {

  private lazy val zkTestServer: TestingServer = new TestingServer(false)

  private lazy val zkClient = CuratorFrameworkFactory
    .builder()
    .namespace("vasgen")
    .connectString(zkTestServer.getConnectString)
    .retryPolicy(new RetryOneTime(500))
    .build()

  private lazy val workDistributor =
    new WorkDistributor(
      WorkDistributorConfig("", "", 1.second),
      zkClient,
      _ => (),
    )

  override def spec = {
    suite("WorkDistributor")(
      test("divideAndRoundUp") {
        assert(divideAndRoundUp(0, 3))(equalTo(0L)) &&
        assert(divideAndRoundUp(1, 3))(equalTo(1L)) &&
        assert(divideAndRoundUp(2, 3))(equalTo(1L)) &&
        assert(divideAndRoundUp(3, 3))(equalTo(1L)) &&
        assert(divideAndRoundUp(4, 3))(equalTo(2L)) &&
        assert(divideAndRoundUp(5, 3))(equalTo(2L)) &&
        assert(divideAndRoundUp(6, 3))(equalTo(2L))
      },
      test("nodeName") {
        assert(nodeName(""))(equalTo("")) &&
        assert(nodeName("/name"))(equalTo("name")) &&
        assert(nodeName("/name/"))(equalTo("name")) &&
        assert(nodeName("/a/b/c"))(equalTo("c")) &&
        assert(nodeName("/a/b/c/"))(equalTo("c"))
      },
      test("createEphemeralNode && getChildrenNodes") {
        for {
          _             <- ZIO.from(zkClient.create().forPath("/workers"))
          workersBefore <- workDistributor.getChildrenNodes("/workers")
          _ <- workDistributor.createEphemeralNode("/workers", "node_1")
          _ <- workDistributor.createEphemeralNode("/workers", "node_2")
          _ <- workDistributor.createEphemeralNode("/workers", "node_3")
          workersAfter <- workDistributor.getChildrenNodes("/workers")
        } yield {
          assert(workersBefore)(hasSameElements(Set.empty)) &&
          assert(workersAfter)(
            hasSameElements(Set("node_1", "node_2", "node_3")),
          )
        }
      },
      test("lockNodes") {
        val all = Set("/node_1", "/node_2", "/node_3", "/node_4", "/node_5")
        for {
          _  <- ZIO.from(zkClient.create().forPath("/node_1"))
          _  <- ZIO.from(zkClient.create().forPath("/node_2"))
          _  <- ZIO.from(zkClient.create().forPath("/node_3"))
          _  <- ZIO.from(zkClient.create().forPath("/node_4"))
          _  <- ZIO.from(zkClient.create().forPath("/node_5"))
          r1 <- workDistributor.lockNodes(1, Set.empty)
          r2 <- workDistributor.lockNodes(0, Set("/node_1"))
          r3 <- workDistributor.lockNodes(1, all)
          r4 <- workDistributor.lockNodes(2, all)
          r5 <- workDistributor.lockNodes(5, all)
          r6 <- workDistributor.lockNodes(1, all)
        } yield {
          assert(r1)(hasSameElements(Seq.empty)) &&
          assert(r2)(hasSameElements(Seq.empty)) &&
          assert(r3.size)(equalTo(1)) && assert(r4.size)(equalTo(2)) &&
          assert(r5.size)(equalTo(2)) &&
          assert(r5)(hasSameElements(all -- r3 -- r4)) &&
          assert(r6.size)(equalTo(0)) && assert(2 + 2)(equalTo(4))
        }
      },
      test("workCounter") {
        for {
          r1 <- workDistributor.currentWorkCounter
          r2 <- workDistributor.modifyWorkCounter(_ + 1)
          r3 <- workDistributor.modifyWorkCounter(_ + 4)
          r4 <- workDistributor.modifyWorkCounter(_ - 5)
        } yield {
          assert(r1)(equalTo(0L)) && assert(r2)(equalTo(1L)) &&
          assert(r3)(equalTo(5L)) && assert(r4)(equalTo(0L))
        }
      },
    )
  } @@
    before(
      UIO {
        zkTestServer.start()
        zkClient.start()
      },
    ) @@
    after(
      ZIO {
        zkClient.close()
        zkTestServer.close()
      },
    ) @@ ignore

}
