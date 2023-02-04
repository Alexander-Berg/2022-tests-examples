package ru.yandex.vertis.telepony.tasks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalatest.Ignore
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.client.mtt.MttClientImplIntSpec
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.{OperatorAccounts, RawCall, TypedDomain, TypedDomains}
import ru.yandex.vertis.telepony.service.{DateTimeStorage, SharedPoolService}
import ru.yandex.vertis.telepony.tasks.shared.MttCallTask

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * It is not a real test. Just a convenient tool for testing MttCallTask using production calls.
  * You can bootstrap mtt client with any account(credentials).
  * @author neron
  */
@Ignore
class MttCallTaskSpec extends SpecBase with MockitoSupport {

  implicit val as: ActorSystem = ActorSystem("MttCallTaskSpec", ConfigFactory.empty())
  implicit val am: ActorMaterializer = ActorMaterializer()

  implicit val pr: PrometheusRegistry = {
    val mocked = mock[PrometheusRegistry]
    when(mocked.register(?)).thenReturn(null)
    mocked
  }

  trait TestEnv {
    val mttClient = MttClientImplIntSpec.createMttClient // real mtt client

    val sharedNumber = SharedOperatorNumberGen.next.copy(
      account = OperatorAccounts.MttShared,
      domain = Some(TypedDomains.autoru_def)
    )

    val sharedPool = mock[SharedPoolService]
    when(sharedPool.find(?)(?)).thenReturn(Future.successful(Some(sharedNumber)))
    val timeStorage = mock[DateTimeStorage] // not used
    val journal = mock[WriteJournal[RawCall]]
    when(journal.send(?)).thenReturn(Future.successful(null))
    val journals: Map[TypedDomain, WriteJournal[RawCall]] = Map.empty.withDefaultValue(journal)
    val task = new MttCallTask(sharedPool, timeStorage, mttClient, journals)
  }

  "MttCallTask" should {
    "parse calls" in new TestEnv {
      val to = DateTime.now()
      val from = to.minusHours(1)
      val config = ConfigFactory.parseString(s"""{ "from": "$from", to: "$to" }""")
      Await.result(task.payload(config), 5.minutes)

      import scala.jdk.CollectionConverters._
      MttCallTask.actionsCounter.collect().asScala.foreach { samples =>
        samples.samples.asScala.foreach { sample =>
          println(sample.toString)
        }
      }
    }
  }

}
