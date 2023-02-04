package ru.yandex.vertis.feedprocessor.autoru.scheduler.importer

import akka.stream.QueueOfferResult
import akka.stream.scaladsl.SourceQueue
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.slf4j.helpers.NOPLogger
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.app.{DefaultEnvironment, Environment}
import ru.yandex.vertis.feedprocessor.autoru.dao.MainOffice7Dao.{Client, XmlHost}
import ru.yandex.vertis.feedprocessor.autoru.dao.{MainOffice7Dao, TasksDao}
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.{SaleCategories, TaskError}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman.SalesmanClient
import ru.yandex.vertis.feedprocessor.dao.KVClient
import ru.yandex.vertis.feedprocessor.services.feedloader.FeedloaderClient
import ru.yandex.vertis.feedprocessor.services.feedloader.FeedloaderClient.{Feed, FeedStatus}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => argEq}

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.Random

/**
  * @author pnaydenov
  */
class Office7ImporterSpec extends WordSpecBase with MockitoSupport with ScalaFutures {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(1, Seconds))

  implicit private val logger = NOPLogger.NOP_LOGGER

  abstract class Fixture {
    val office7Dao = mock[MainOffice7Dao]
    val feedloader = mock[FeedloaderClient]
    val tasksDao = mock[TasksDao]
    val failedTasks = mock[SourceQueue[TaskError]]
    val salesmanClient = mock[SalesmanClient]
    val kvClient = mock[KVClient]

    val environment: Environment = new DefaultEnvironment {
      override lazy val environmentType: String = "test"
    }

    val importer = new Office7Importer(
      office7Dao,
      tasksDao,
      feedloader,
      salesmanClient,
      kvClient,
      5,
      1000000,
      1000,
      100,
      environment
    ) {
      override val TestClients = Set(1, 2, 3, 4, 5)
    }

    val prevFeedloaderClients = collection.mutable.ListBuffer.empty[Int]
  }

  private val HoursToNextProcessing = 6

  private def host(clientId: Int): XmlHost =
    XmlHost(Random.nextInt(), clientId, 3, 2, s"http://$clientId", None, None, false, false, false, false, false)

  private def feed(
      clientId: Int,
      lastChanged: LocalDateTime = LocalDateTime.now().minusHours(HoursToNextProcessing)): Feed = {
    val partnerId = 1000000 + clientId + 3 * 100 + 2 * 1000
    Feed(
      Random.nextLong(),
      Random.nextLong(),
      partnerId,
      1,
      1,
      FeedStatus.OK,
      LocalDateTime.now(),
      s"http://$clientId",
      Some(s"http://mds/$clientId"),
      Some(lastChanged)
    )
  }

  implicit private def anyToFuture[A](any: A): Future[A] = Future.successful(any)

  "Office7Importer" should {
    "skip when don't have bucket" in new Fixture {
      importer.createActiveTasks(Nil, 5, failedTasks)
    }

    "create tasks" in new Fixture {
      val hosts = host(1) :: host(3) :: host(4) :: host(5) :: Nil
      val taskMock = newTasksGen.next

      when(office7Dao.allActiveXmlHosts(?, ?, ?, ?)).thenReturn(hosts)
      stub[Seq[Int], Seq[Client]](t1 => office7Dao.getClientsByIds(t1)) {
        case ids =>
          ids.map(id => Client(id, id + 1000, "active", None, 0))
      }

      val feeds =
        Seq(feed(1), feed(2), feed(3), feed(4), feed(5, LocalDateTime.now().minusHours(HoursToNextProcessing - 2)))

      when(feedloader.getLastFeeds(?)).thenReturn(feeds.init)
      when(feedloader.getLastValidFeeds(?)).thenReturn(feeds)
      stub(feedloader.createTask _) {
        case (task, _) =>
          task.url shouldEqual "http://5"
          111L
      }
      when(failedTasks.offer(?)).thenReturn(Future.successful(QueueOfferResult.enqueued))
      when(tasksDao.tryCreateTaskAndRemovePrevious(?, ?, ?, ?, ?, ?, ?, ?)).thenReturn(None, Some(taskMock))
      when(salesmanClient.getCampaigns(?, ?, ?))
        .thenReturn(
          Future.successful(
            hosts.map(host =>
              SalesmanClient.Campaign(None, host.getCategory, None, Some(Seq(host.getSection)), Some(1000), None)
            )
          )
        )

      val processTimes = Map(
        "ATV/NEW/1" -> (System.currentTimeMillis() - HoursToNextProcessing.hours.toMillis).toString,
        "ATV/NEW/2" -> (System.currentTimeMillis() - HoursToNextProcessing.hours.toMillis).toString,
        "ATV/NEW/3" -> (System.currentTimeMillis() - HoursToNextProcessing.hours.toMillis).toString,
        "ATV/NEW/4" -> (System.currentTimeMillis() - (HoursToNextProcessing - 1).hours.toMillis).toString,
        "ATV/NEW/5" -> (System.currentTimeMillis() - (HoursToNextProcessing - 1).hours.toMillis).toString
      )
      stub(kvClient.bulkGet(_: Seq[String])) {
        case keys if keys.toSet.equals(processTimes.keys.toSet) => processTimes
        case _ => Map.empty[String, String]
      }
      when(kvClient.bulkSetWithTTL(?)).thenReturn(Future.unit)

      importer.createActiveTasks(List(1), 2, failedTasks).futureValue

      verify(office7Dao).allActiveXmlHosts(argEq(List(1)), argEq(2), ?, ?)
      verify(feedloader).getLastFeeds(argEq(5))
      verify(feedloader).getLastValidFeeds(argEq(5))
      verify(feedloader).createTask(?, ?)
      verify(kvClient, times(2)).bulkGet(?)
      verify(kvClient).bulkSetWithTTL(?)
      verify(tasksDao).tryCreateTaskAndRemovePrevious(argEq(1), ?, ?, ?, ?, ?, ?, ?)
      verify(tasksDao).tryCreateTaskAndRemovePrevious(argEq(3), ?, ?, ?, ?, ?, ?, ?)
      verify(tasksDao).tryCreateTaskAndRemovePrevious(argEq(5), ?, ?, ?, ?, ?, ?, ?)
      verify(salesmanClient).getCampaigns(argEq(1), ?, ?)
      verify(salesmanClient).getCampaigns(argEq(3), ?, ?)
      verify(salesmanClient).getCampaigns(argEq(4), ?, ?)
      verify(salesmanClient).getCampaigns(argEq(5), ?, ?)
    }

    "update task" in new Fixture {
      val hosts = host(1) :: host(3) :: Nil
      val taskMock = newTasksGen.next

      val feeds = Seq(feed(1), feed(2), feed(3))

      when(office7Dao.allActiveXmlHosts(?, ?, ?, ?)).thenReturn(hosts)
      stub[Seq[Int], Seq[Client]](t1 => office7Dao.getClientsByIds(t1)) {
        case ids =>
          ids.map(id => Client(id, id + 1000, "active", Some(1), 0))
      }
      when(feedloader.getLastFeeds(?))
        .thenReturn(feeds(0) :: feeds(1) :: feeds(2).copy(url = "http://wrong-value") :: Nil)
      when(feedloader.getLastValidFeeds(?)).thenReturn(feeds)
      stub(feedloader.updateTask _) {
        case (task, _) =>
          task.url shouldEqual "http://3"
          true
      }
      when(failedTasks.offer(?)).thenReturn(Future.successful(QueueOfferResult.enqueued))
      when(tasksDao.tryCreateTaskAndRemovePrevious(?, ?, ?, ?, ?, ?, ?, ?)).thenReturn(None, Some(taskMock))
      when(salesmanClient.getCampaigns(?, ?, ?))
        .thenReturn(
          Future.successful(
            hosts.map(host =>
              SalesmanClient.Campaign(
                None,
                Some(SaleCategories.Commercial),
                Some(host.getCategory.toSeq),
                Some(Seq(host.getSection)),
                Some(1000),
                None
              )
            )
          )
        )

      val processTimes = Map(
        "ATV/NEW/1" -> (System.currentTimeMillis() - HoursToNextProcessing.hours.toMillis).toString,
        "ATV/NEW/3" -> (System.currentTimeMillis() - HoursToNextProcessing.hours.toMillis).toString
      )
      stub(kvClient.bulkGet(_: Seq[String])) {
        case keys if keys.toSet.equals(processTimes.keys.toSet) => processTimes
        case _ => Map.empty[String, String]
      }
      when(kvClient.bulkSetWithTTL(?)).thenReturn(Future.unit)

      importer.createActiveTasks(List(1), 2, failedTasks).futureValue

      verify(office7Dao).allActiveXmlHosts(argEq(List(1)), argEq(2), ?, ?)
      verify(feedloader).getLastFeeds(argEq(5))
      verify(feedloader).getLastValidFeeds(argEq(5))
      verify(feedloader).updateTask(?, ?)
      verify(kvClient, times(2)).bulkGet(?)
      verify(kvClient).bulkSetWithTTL(?)
      verify(tasksDao).tryCreateTaskAndRemovePrevious(argEq(1), ?, ?, ?, ?, ?, ?, ?)
      verify(tasksDao).tryCreateTaskAndRemovePrevious(argEq(3), ?, ?, ?, ?, ?, ?, ?)
      verify(salesmanClient).getCampaigns(argEq(1), ?, ?)
      verify(salesmanClient).getCampaigns(argEq(3), ?, ?)
    }
  }

  "Office7Importer.hostEquals" should {
    "detect nonequal paths" in {
      Office7Importer.equalUrls("http://ya.ru/hello", "http://ya.ru/world") shouldBe false
      Office7Importer.equalUrls("http://ya.ru/hello", "http://ya.ru/") shouldBe false
    }

    "detect nonequal params" in {
      Office7Importer.equalUrls("http://ya.ru/hello", "http://ya.ru/hello?foo=bar") shouldBe false
      Office7Importer.equalUrls("http://ya.ru/hello?foo=baz", "http://ya.ru/hello?foo=bar") shouldBe false
    }

    "detect nonequal hosts" in {
      Office7Importer.equalUrls("http://ya.ru/hello", "http://auto.ru/hello") shouldBe false
    }

    "detect nonequal ports" in {
      Office7Importer.equalUrls("http://ya.ru:1/hello", "http://auto.ru:2/hello") shouldBe false
      Office7Importer.equalUrls("http://ya.ru/hello", "http://auto.ru:2/hello") shouldBe false
    }

    "ignore protocol" in {
      Office7Importer.equalUrls("http://ya.ru/hello", "https://ya.ru/hello") shouldBe true
    }

    "ignore www" in {
      Office7Importer.equalUrls("http://ya.ru/hello", "http://www.ya.ru/hello") shouldBe true
    }

    "ignore hash" in {
      Office7Importer.equalUrls("http://ya.ru/hello", "https://ya.ru/hello#foo") shouldBe true
    }

    "match equal uri" in {
      Office7Importer.equalUrls("http://ya.ru", "http://ya.ru/") shouldBe true
      Office7Importer.equalUrls("http://ya.ru/?", "http://ya.ru/") shouldBe true
      Office7Importer.equalUrls("http://ya.ru:80/?", "http://ya.ru") shouldBe true
    }

    "ignore explisit port" in {
      Office7Importer.equalUrls(
        "http://carsload.ru/xml/xml_autoru_ramilz.xml",
        "https://carsload.ru:443/xml/xml_autoru_ramilz.xml"
      ) shouldBe true
      Office7Importer.equalUrls(
        "http://carsload.ru:80/xml/xml_autoru_ramilz.xml",
        "https://carsload.ru:443/xml/xml_autoru_ramilz.xml"
      ) shouldBe true
    }

    "autocloud" in {
      Office7Importer.equalUrls(
        "http://autocloud.ru/files/export/autoru2/suzukiyaravtogermes_new.xml",
        "http://aaa24.ru/files/export/autoru2/suzukiyaravtogermes_new.xml"
      ) shouldBe true
    }

    "don't fall on malformed urls" in {
      Office7Importer.equalUrls("malformed-url", "hD:lghвп438)*&2") shouldBe false
    }
  }
}
