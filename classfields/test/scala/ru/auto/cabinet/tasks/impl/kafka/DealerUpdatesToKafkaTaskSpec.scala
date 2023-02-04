//package ru.auto.cabinet.tasks.impl.kafka
//
//import java.lang
//
//import akka.actor.ActorSystem
//import akka.stream.Materializer
//import org.apache.kafka.clients.consumer.{KafkaConsumer, OffsetResetStrategy}
//import org.apache.kafka.common.serialization.{ByteArrayDeserializer, LongDeserializer}
//import org.scalatest.time.{Seconds, Span}
//import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}
//import org.testcontainers.containers.KafkaContainer
//import ru.auto.cabinet.DealerAutoru._
//import ru.auto.cabinet.dao.jdbc.{JdbcClientDao, JdbcKeyValueDao}
//import ru.auto.cabinet.test.JdbcSpecTemplate
//
//import scala.jdk.CollectionConverters._
//
// todo migrate to KafkaSpecBase's server
//class DealerUpdatesToKafkaTaskSpec
//  extends AsyncFlatSpec
//    with JdbcSpecTemplate
//    with Matchers
//    with BeforeAndAfterAll {
//
//  implicit val system = ActorSystem()
//  implicit val ec = scala.concurrent.ExecutionContext.global
//
//  val kafka = new KafkaContainer("4.1.0").withEmbeddedZookeeper
//
//  val clientDao = new JdbcClientDao(database, database)
//  val keyValueDao = new JdbcKeyValueDao(database, database)
//
//  override def beforeAll {
//    super.beforeAll
//    kafka.start()
//  }
//
//  override def afterAll {
//    super.afterAll
//    kafka.stop()
//  }
//
//  import cakesolutions.kafka.KafkaConsumer
//
//  it should "work" in {
//
//    val consumer = createConsumer
//    consumer.subscribe(List(DealerUpdatesToKafkaTask.topic).asJava)
//
//    val task = new DealerUpdatesToKafkaTask(clientDao, keyValueDao, kafka.getBootstrapServers)
//    // new dealer
//    executeAndPollRecords(consumer, task) shouldBe List(16453)
//    // no dealer changed
//    executeAndPollRecords(consumer, task) shouldBe List.empty
//    // changing dealer
//    clientDao.updateLoyalty(16453L, loyal = false)
//    executeAndPollRecords(consumer, task) shouldBe List(16453)
//  }
//
//  private def createConsumer = KafkaConsumer(KafkaConsumer.Conf(
//    keyDeserializer = new LongDeserializer(),
//    valueDeserializer = new ByteArrayDeserializer(),
//    bootstrapServers = kafka.getBootstrapServers,
//    autoOffsetReset = OffsetResetStrategy.EARLIEST,
//    groupId = "1"))
//
//  private def executeAndPollRecords(consumer: KafkaConsumer[lang.Long, Array[Byte]],
//                                    task: DealerUpdatesToKafkaTask) = {
//    task.execute().futureValue(timeout(Span(5, Seconds)))
//    val records = (1 to 10).flatMap(_ => consumer.poll(100).asScala).toList
//    records.map(r => Dealer.parseFrom(r.value())).map(_.getId)
//  }
//}
