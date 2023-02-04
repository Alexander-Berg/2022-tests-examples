package ru.yandex.vertis.telepony.tasks

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalatest.Ignore
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.client.vox.VoxClientImplIntSpec
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.RawCall
import ru.yandex.vertis.telepony.service.impl.vox.VoxDomainClient
import ru.yandex.vertis.telepony.service.DateTimeStorage
import ru.yandex.vertis.telepony.tasks.vox.VoxCallTask

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * It is not a real test. Just a convenient tool for testing VoxCallTask using production calls.
  * You can bootstrap mtt client with any account(credentials).
  *
  * @author neron
  */
@Ignore
class VoxCallTaskSpec extends SpecBase with MockitoSupport {

  implicit val as: ActorSystem = ActorSystem("VoxCallTaskSpec", ConfigFactory.empty())
  implicit val am: Materializer = Materializer(as)

  trait TestEnv {
    val voxClient = VoxClientImplIntSpec.createClient()
    val domainClient = new VoxDomainClient(voxClient, "autoru_def_redirect")
    val timeStorage = mock[DateTimeStorage] // not used
    when(timeStorage.set(?)).thenReturn(Future.unit)
    when(timeStorage.get()).thenReturn(Future.successful(Some(DateTime.parse("2019-02-17"))))
    val journal = mock[WriteJournal[RawCall]]
    when(journal.send(?)).thenReturn(Future.successful(null))
    val task = new VoxCallTask(domainClient, timeStorage, journal)
  }

  "VoxCallTask" should {
    "parse calls" in new TestEnv {
      val to = DateTime.parse("2019-02-22")
      val from = to.minusHours(1)
      val config = ConfigFactory.parseString(s"""{ "reset-from": "$from", to: "$to" }""")
      Await.result(task.payload(config), 5.minutes)
    }
  }

}
