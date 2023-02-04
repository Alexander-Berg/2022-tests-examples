package ru.yandex.realty.application

import java.util
import java.util.concurrent.atomic.AtomicInteger
import _root_.akka.actor.{ActorSystem, Cancellable, Scheduler}
import com.typesafe.config.ConfigFactory
import io.prometheus.client.SimpleCollector
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.tokenization.TokensFilter
import ru.yandex.realty.clients.feedprocessor.FeedProcessorClient
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.feedprocessor.FeedIdsResponse
import ru.yandex.realty.persistence.cassandra.CassandraAPI
import ru.yandex.realty.persistence.{FeedId, GroupId, IndexingBytes, PartnerId}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.tracing.NoopTracingSupport
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class ReindexerSpec extends WordSpec with Matchers with BasicGenerators with PropertyChecks with MockitoSupport {

  implicit val trace: Traced = Traced.empty

  def indexingBytesGen(groupId: GroupId): Gen[IndexingBytes] =
    for {
      offerId <- readableString
    } yield IndexingBytes(None, Some(groupId), offerId, None, None, None, None, None, Map.empty)

  def rawOfferGen(min: Int, max: Int): Gen[(GroupId, Seq[IndexingBytes])] =
    for {
      groupId <- Gen.choose(0, 100)
      size <- Gen.choose(min, max)
      seq <- Gen.listOfN(size, indexingBytesGen(groupId))
    } yield (groupId, seq)

  def dataGen(min: Int, max: Int, minOffers: Int, maxOffers: Int): Gen[Seq[(GroupId, Seq[IndexingBytes])]] =
    for {
      size <- Gen.choose(min, max)
      rawOffers <- Gen.listOfN(size, rawOfferGen(minOffers, maxOffers))
    } yield rawOffers

  val tokensFilter = mock[TokensFilter]
  stub(tokensFilter.isAcceptable _) { case _ => true }

  // prevent reindexing task from being rescheduled
  val scheduler = mock[Scheduler]
  stub(scheduler.scheduleOnce(_: FiniteDuration, _: Runnable)(_: ExecutionContext)) {
    case _ =>
      Cancellable.alreadyCancelled
  }
  val actorSystem = Mockito.spy(ActorSystem("test", ConfigFactory.empty))
  Mockito.when(actorSystem.scheduler).thenReturn(scheduler)

  val features = new SimpleFeatures()

  import actorSystem.dispatcher

  "Reindexer" should {
    "process all available data, respecting batch size" in {
      val BatchSize = 25
      val MaxOffers = BatchSize * 2 / 3

      forAll(dataGen(0, 1000, 0, MaxOffers)) { data =>
        val batchSizes = util.Collections.synchronizedList(new util.LinkedList[Int]())

        val components = mock[ReindexerComponents]
        val cassandraAPI = mock[CassandraAPI]
        val feedProcessorClient = mock[FeedProcessorClient]
        val batchReindexer = mock[BatchReindexer]

        stub(components.register(_: SimpleCollector[_])) { case c => c }
        stub(components.ec _)(actorSystem.dispatcher)
        stub(components.ReindexMaxInMemoryBatches _)(8)
        stub(components.ReindexOneBatchOffersSize _)(BatchSize)
        stub(components.indexerDatabaseAPI _)(cassandraAPI)
        stub(components.feedProcessorClient _)(feedProcessorClient)
        stub(cassandraAPI.getAllRawOffers(_: ExecutionContext)) { case _ => Future.successful(data.iterator) }
        stub(feedProcessorClient.getAllFeedIds(_: Traced)) {
          case _ => Future.successful(FeedIdsResponse.getDefaultInstance)
        }
        stub(components.features _)(features)
        stub(
          (entities: Seq[(GroupId, Seq[IndexingBytes])], _: Traced) => batchReindexer.processBatch(entities)
        ) {
          case (entities, _) =>
            batchSizes.add(entities.map(_._2.size).sum)
            Future(())
        }

        val rdx = new Reindexer(components, actorSystem, tokensFilter, batchReindexer, new NoopTracingSupport)
        Await.result(rdx.reindex(), 5.seconds)

        val chunks = batchSizes.synchronized(batchSizes.asScala)
        chunks.sum shouldBe data.map(_._2.size).sum
        chunks.count(_ < BatchSize) shouldBe <=(1)
        chunks.forall(_ > 0) shouldBe true
      }
    }

    "ignore batch processing errors" in {
      forAll(dataGen(4, 50, 1, 1)) { data =>
        val batchCount = new AtomicInteger(0)

        val components = mock[ReindexerComponents]
        val cassandraAPI = mock[CassandraAPI]
        val feedProcessorClient = mock[FeedProcessorClient]
        val batchReindexer = mock[BatchReindexer]

        stub(components.register(_: SimpleCollector[_])) { case c => c }
        stub(components.ec _)(actorSystem.dispatcher)
        stub(components.ReindexMaxInMemoryBatches _)(4)
        stub(components.ReindexOneBatchOffersSize _)(1)
        stub(components.indexerDatabaseAPI _)(cassandraAPI)
        stub(components.feedProcessorClient _)(feedProcessorClient)
        stub(cassandraAPI.getAllRawOffers(_: ExecutionContext)) { case _ => Future.successful(data.iterator) }
        stub(feedProcessorClient.getAllFeedIds(_: Traced)) {
          case _ => Future.successful(FeedIdsResponse.getDefaultInstance)
        }
        stub(components.features _)(features)
        stub(
          (entities: Seq[(GroupId, Seq[IndexingBytes])], _: Traced) => batchReindexer.processBatch(entities)
        ) {
          case _ =>
            // fail every 3rd time
            val n = batchCount.incrementAndGet()
            if (n % 3 == 0) Future.failed(new RuntimeException)
            else Future(())
        }

        val rdx = new Reindexer(components, actorSystem, tokensFilter, batchReindexer, new NoopTracingSupport)
        Await.result(rdx.reindex(), 5.seconds)

        batchCount.get shouldBe data.size
      }
    }
  }
}
