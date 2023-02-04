package ru.yandex.vertis.telepony.journal

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorRef, Status}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.testkit.TestKit
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.telepony.component.{DomainComponent, DomainKafkaComponent, KafkaSupportComponent}
import ru.yandex.vertis.telepony.journal.Journal.{CommittableMetadata, EventEnvelope, KafkaMetadata}
import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals.{ConsumerGroup, GenericKafkaJournal, Topic}
import ru.yandex.vertis.telepony.util.serializer.StringSerializer

import scala.collection.immutable.Seq
import scala.concurrent.{Await, Future}
import scala.util.{Random, Success, Try}

/**
  * @author @logab
  */
trait JournalSpec extends Matchers with ScalaFutures with Eventually with AnyWordSpecLike {
  self: TestKit with DomainKafkaComponent with KafkaSupportComponent with DomainComponent =>
  implicit val am = ActorMaterializer()

  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  private val DefaultPatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Milliseconds))

  def testTopic: Topic

  trait Test {

    private lazy val consumerGroup = ConsumerGroup("test-" + Random.nextInt())

    private lazy val testKafkaJournal =
      new GenericKafkaJournal[String]("test", StringSerializer, testTopic) {}

    def elements: Iterator[String] = {
      val i = new AtomicInteger(0)
      Iterator.continually(i.getAndIncrement().toString)
    }

    val journalWriter: WriteJournal[String] = writeJournal(testKafkaJournal)
    val journalReader: ReadJournal[String] = readJournal(testKafkaJournal, consumerGroup)
    val topicOffsetStorage: OffsetStorage = offsetStorage(testTopic, consumerGroup)

    val src: RunnableGraph[(ActorRef, Future[Seq[EventEnvelope[KafkaMetadata, String]]])] =
      Source
        .actorRef(10000, OverflowStrategy.dropNew)
        .mapAsync(5)(journalWriter.send)
        .toMat(Sink.seq)(Keep.both)

    import scala.concurrent.duration._

    def store(element: String): EventEnvelope[KafkaMetadata, String] = {
      val (source, sink) = src.run()
      source ! element
      source ! Status.Success(())
      val head = Await.result(sink, 1.minute).head
      head
    }

    @volatile
    var list = List.empty[EventEnvelope[CommittableMetadata, Try[String]]]

    def read(): Consumer.Control = {
      journalReader
        .allEvents()
        .toMat(Sink.foreach { elem =>
          list = list :+ elem
        })(Keep.left)
        .run()
    }

    def commit(metadata: CommittableMetadata): Unit = metadata.offset.commitScaladsl().futureValue
  }

  "journal" should {
    "write an element" in new Test {
      val element = elements.next()
      val graph = read()
      Thread.sleep(3000)
      store(element).metadata
      eventually {
        list.head.event shouldEqual Success(element)
      }
      commit(list.head.metadata)
      graph.shutdown().futureValue
    }
    "read the same element" in new Test {
      val graph = read()
      Thread.sleep(3000)
      val first = store(elements.next()).metadata.seqNr
      val second = store(elements.next()).metadata.seqNr
      val third = store(elements.next()).metadata.seqNr
      eventually {
        (list.map(_.metadata.offset.partitionOffset.offset) should contain).allOf(first, second, third)
      }
      val secondMeta = list.find(_.metadata.offset.partitionOffset.offset == second).get.metadata
      commit(secondMeta)
      graph.shutdown().futureValue
      val control = read()
      Thread.sleep(3000)
      eventually {
        list.map(_.metadata.offset.partitionOffset.offset).count(_ == third) should be >= 2
      }
      control.shutdown().futureValue
    }
    "determine valid next offset" in new Test {
      val control = read()
      Thread.sleep(3000)
      val first = store(elements.next()).metadata.seqNr
      val second = store(elements.next()).metadata.seqNr
      eventually {
        (list.map(_.metadata.offset.partitionOffset.offset) should contain).allOf(first, second)
      }
      val firstMeta = list.find(_.metadata.offset.partitionOffset.offset == first).get.metadata
      commit(firstMeta)

      topicOffsetStorage.current().futureValue.head.seqNr should be > firstMeta.seqNr
      control.shutdown().futureValue
    }
  }
}
