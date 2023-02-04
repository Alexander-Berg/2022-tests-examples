package vertis.logbroker.client.test

import vertis.logbroker.client.producer.LbProducerSession
import vertis.logbroker.client.producer.config.LbProducerSessionConfig
import vertis.logbroker.client.producer.model.Message
import vertis.zio.BaseEnv
import vertis.zio.managed.ManagedUtils._
import vertis.logbroker.client.LogbrokerNativeFacade
import vertis.zio.managed.HalfManaged
import zio.duration.Duration
import zio.{RIO, ZIO}

/** A simple write to use in read tests
  *
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait LbWrite extends LbTest {

  private def writeToGroup(
      facade: LogbrokerNativeFacade
    )(topic: String,
      group: Int): RIO[BaseEnv, HalfManaged[LbProducerSession]] = {
    val sessionConfig = LbProducerSessionConfig(topic, s"test-source-$group", Some(group))
    val makeProducer = facade.makeProducerSession(sessionConfig) _
    LbProducerSession.makeSession(sessionConfig, makeProducer).acquire
  }

  protected def write(
      topic: String,
      messagesByGroups: Map[Int, Iterable[Message]],
      networkFailEvery: Option[Duration] = None): RIO[BaseEnv, Int] = {
    transportFactoryM.use { balancer =>
      lbLocalFacadeM(balancer).use { facade =>
        for {
          groupSessions <- ZIO.foreachPar(messagesByGroups.keys.toSeq) { group =>
            writeToGroup(facade)(topic, group).map(group -> _)
          }
          _ <- scheduleNetworkFailure(networkFailEvery, balancer)
          responses <- ZIO
            .foreachPar(groupSessions) { case (group, session) =>
              ZIO
                .foreach(messagesByGroups(group).toSeq)(session.r.write)
                .flatMap(ZIO.collectAll(_))
                .tap(xs => logger.info(s"Got ${xs.size} responses for group $group"))
            }
            .map(xs => xs.flatten)
          _ <- logger.info(s"Got ${responses.size} responses")
          _ <- ZIO.foreachPar_(groupSessions.map(_._2))(_.close)
        } yield responses.size
      }
    }
  }
}
