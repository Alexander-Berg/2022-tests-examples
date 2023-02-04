package ru.yandex.auto.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.actor.cars.{Unifier, UnifyRequest}
import ru.yandex.auto.app.OperationalSupport
import ru.yandex.auto.clone.unifier.InfoProcessor
import ru.yandex.auto.core.model.UnifiedCarInfo
import ru.yandex.auto.index.PartitionKey
import ru.yandex.auto.offers.InvalidOffersLogBroker
import ru.yandex.auto.persistence.AutoPersistenceMetadata
import ru.yandex.auto.unifier.UnunifiedFieldManager
import ru.yandex.common.monitoring.error.ExpiringWarningErrorCounterReservoir
import ru.yandex.common.sharding.persistence.cassandra.grained.Reader
import ru.yandex.util.cassandra.RoutedSessionFactory

import scala.util.Success

/**
  * Specification for [[ru.yandex.auto.shard.Unifier]] actor
  *
  * @author incubos
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class CassandraReaderSpec(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with MockitoSugar {

  def this() =
    this(ActorSystem("testsystem", ConfigFactory.parseString("""
akka.event-handlers = ["akka.testkit.TestEventListener"]
                                 """)))

  val cassandraSessionFactory = new RoutedSessionFactory(
    nodes = "index.cassandra01ht.vs.yandex.net,index.cassandra02ht.vs.yandex.net",
    routes = "sas->SAS,fol->SAS,iva->SAS,myt->SAS,ugr->SAS",
    localDataCenter = "sas",
    keyspace = Some("auto")
  )(new ExpiringWarningErrorCounterReservoir())

  val cassandraSession = cassandraSessionFactory.createSession()

  val cassandraPartitionReader =
    new Reader(AutoPersistenceMetadata, cassandraSession)

  val infoProcessor = new InfoProcessor {

    def processInfo(carInfo: UnifiedCarInfo, ununifiedFieldManager: UnunifiedFieldManager) {
      // Do nothing
    }
  }

  implicit val operationalSupport: OperationalSupport = new OperationalSupport()
  val offersLogBroker = new InvalidOffersLogBroker()
  val imageUnifierProbe = TestProbe()
  val unifier = system.actorOf(Props(new Unifier(imageUnifierProbe.ref, infoProcessor, offersLogBroker)))

  import concurrent.duration._

  "cassandraPartitionReader" should {
    "read partition" in {

      cancel("Only manual run")

      val partition = PartitionKey(886)

      cassandraPartitionReader.getPartitionEntitiesWithDetails(partition) match {
        case Success(offers) =>
          info(s"Get ${offers.size}")
          offers.map(offer => {
            val deserialized = AutoPersistenceMetadata.deserialize(offer)
            if (deserialized.exists(_.getId == "1398787571609662060")) {
              println(deserialized)

              imageUnifierProbe.within(1000.second) {
                unifier ! UnifyRequest(partition, List(deserialized.get))
                imageUnifierProbe.expectMsgClass(classOf[ModifyCarInfoRequest]) match {
                  case ModifyCarInfoRequest(partition, offers) => {
                    val o = offers
                    info(s"Get ${o.size}")
                  }
                  case msg => fail("Wrong clusterization request: %s".format(msg))
                }
              }
            }
          })

        case other =>
          fail(s"Unexpected $other")
      }
    }
  }
}
