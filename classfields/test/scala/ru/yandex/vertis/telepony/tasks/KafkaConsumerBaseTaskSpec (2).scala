package ru.yandex.vertis.telepony.tasks

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.testkit.ConsumerResultFactory
import akka.kafka.testkit.scaladsl.ConsumerControlFactory
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Source}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import ru.yandex.vertis.kafka.instrumented.OffsetMetrics
import ru.yandex.vertis.kafka.model.CommittableEvent
import ru.yandex.vertis.ops.prometheus.SimpleCompositeCollector
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.journal.Journal.{CommittableMetadata, EventEnvelope}
import ru.yandex.vertis.telepony.journal.ReadJournal

import scala.jdk.CollectionConverters._
import scala.util.{Success, Try}

class KafkaConsumerBaseTaskSpec extends SpecBase with MockFactory {

  // Тк в коммите стоит тротл на 30 сек, то при 1+ элементах тест будет падать.
  // https://st.yandex-team.ru/TELEPONY-2372
  val ElementsCount = 1

  val domain = "domain"

  "KafkaConsumerBaseTask" should {
    "commit filtered out messages" in {
      (journal.allEvents _)
        .expects()
        .returning(mockedKafkaConsumerSource)

      task.payload().futureValue
      result.toSeq shouldBe (1 to ElementsCount).filter(elem => elem % 2 == 0 && elem % 4 == 0)

      collector.collect().asScala.flatMap(_.samples.asScala.map(_.value)).foreach(_ shouldBe ElementsCount)
    }
  }

  implicit val as = ActorSystem("test", ConfigFactory.empty())
  implicit val am = Materializer(as)
  implicit val sh = as.scheduler

  val journal = mock[ReadJournal[String]]
  val collector = new SimpleCompositeCollector()
  val metrics = new OffsetMetrics(collector)

  val result = scala.collection.mutable.ArrayBuffer[Int]()

  val task = new KafkaConsumerBaseTask[String, Int](journal, metrics, "domain")(am, sh) {

    override def flowProcessor: Flow[CommittableEvent[Int], CommittableEvent[Unit], NotUsed] =
      Flow[CommittableEvent[Int]].map { i =>
        result.addOne(i.data)
        i.copy(data = ())
      }

    override def eventCollector: PartialFunction[Try[String], Int] = {
      case Success(v) if Try(v.toInt).isSuccess => v.toInt
    }

    override def name: String = "test"
  }

  val mockedKafkaConsumerSource: Source[EventEnvelope[CommittableMetadata, Try[String]], Consumer.Control] =
    Source(
      (1 to ElementsCount)
        .map(i => event(offset = i, validData = i % 4 == 0, validDomain = i % 2 == 0))
    ).viaMat(ConsumerControlFactory.controlFlow())(Keep.right)

  private def event(offset: Long, validData: Boolean, validDomain: Boolean) = {

    val topic = "test"
    val partition = 1
    val groupId = "groupId"

    EventEnvelope(
      CommittableMetadata(
        "type",
        if (validDomain) domain else "invalid-domain",
        offset,
        DateTime.now(),
        ConsumerResultFactory.committableOffset(groupId, topic, partition, offset, offset.toString)
      ),
      if (validData) {
        Try(offset.toString)
      } else {
        Try("not-integer")
      }
    )
  }
}