package vasgen.core.distribution

object DistributionContext extends Logging {
  val config = DistributionWorker.Config(
    inputNode = "/work/distribution",
    childPrefix = "distribution",
    outputNode = "/work/partitions",
    partitions = 8,
  )
  private val clock = Clock.live

  def instanceLayer(
    connectionString: String,
    id: String,
  ): ZLayer[Any, Nothing, DistributionServiceLayer with CuratorFrameworkLayer] =
    curatorFrameworkLayer(connectionString) >+> distributionServiceLayer(id)

  private def curatorFrameworkLayer(
    connectionString: String,
  ): ZLayer[Any, Nothing, CuratorFrameworkLayer] =
    ZIO
      .succeed {
        val curator = CuratorFrameworkFactory
          .builder()
          .namespace("test")
          .connectString(connectionString)
          .retryPolicy(new BoundedExponentialBackoffRetry(10000, 60000, 29))
          .build()
        curator.start()
        curator
      }
      .toLayer

  private def distributionServiceLayer(id: String) =
    (ZLayer.requires[CuratorFrameworkLayer] ++ clock) >>>
      (
        for {
          curator <- ZIO.service[CuratorFramework]
          rts     <- ZIO.runtime[Any]
          queue   <- Queue.unbounded[Map[InstanceId, Seq[Int]]]
          ref <- Ref.make(Map.empty[Int, Seq[Int] => IO[VasgenStatus, Unit]])
          instances <- Ref.make(Map.empty[InstanceId, Seq[Int]])
        } yield new DistributionWorker(
          config = config,
          rts = rts,
          queue = queue,
          mapRef = instances,
          instanceId = InstanceId(id),
        )(curator = curator, subscriptions = ref): Distribution.Service
      ).toLayer

}
