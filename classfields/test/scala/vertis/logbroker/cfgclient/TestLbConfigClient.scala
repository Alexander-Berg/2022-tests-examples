package vertis.logbroker.cfgclient

import vertis.logbroker.cfgclient.model.{ExecuteModifyCommandsResult, LbPath, SingleModifyRequest}
import vertis.logbroker.cfgclient.model.topic.TopicDescription
import zio.{RIO, ZIO}

/** @author kusaeva
  */
class TestLbConfigClient(partitions: Map[LbPath, Long]) extends LbConfigClient {

  override def describeTopic(topicPath: LbPath): RIO[zio.ZEnv, TopicDescription] =
    ZIO
      .fromOption(partitions.get(topicPath))
      .map(TopicDescription(_, Seq(), Seq()))
      .mapError(_ => new IllegalArgumentException("No topic found"))

  override def exists(path: LbPath): RIO[zio.ZEnv, Boolean] = ???

  override def executeModifyCommands(requests: Seq[SingleModifyRequest]): RIO[zio.ZEnv, ExecuteModifyCommandsResult] =
    ???
}
