package ru.yandex.vertis.feedprocessor.autoru.app

import java.time.LocalDateTime
import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.BeforeAndAfter
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.dao.{OfferDao, TasksDao}
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.dao.BlockedKVClient
import ru.yandex.vertis.feedprocessor.services.mds.MdsClient
import ru.yandex.vertis.feedprocessor.util.DummyTokensDistribution
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => argEq}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * @author pnaydenov
  */
class UtilizatorServiceSpec extends WordSpecBase with MockitoSupport with BeforeAndAfter {

  val rt = zio.Runtime.default
  val distribution = new DummyTokensDistribution(1)
  val taskDao = mock[TasksDao]
  val offerDao = mock[OfferDao]
  val blockedKVClient = mock[BlockedKVClient]
  val mdsClient = mock[MdsClient]
  val everyDay = ConfigFactory.parseString("""
      |{
      |  schedule {
      |    hour = 5
      |  }
      |  mds-rate-limit = 2
      |  mds-retry = 3
      |  tasks-limit = 1000
      |}
    """.stripMargin)

  def createUtilizatorService(conf: Config = everyDay, td: TasksDao = taskDao): UtilizatorService =
    new UtilizatorService(distribution, td, offerDao, blockedKVClient, mdsClient, conf)

  private def plus(dateTime: LocalDateTime, duration: FiniteDuration) = dateTime.plusSeconds(duration.toSeconds)

  when(taskDao.findEarliestTaskIdByTime(?)).thenReturn(None)

  after {
    Mockito.clearInvocations(mdsClient, blockedKVClient)
  }

  "UtilizatorService" should {
    "correctly evaluate nearby run" in {
      val now = LocalDateTime.parse("2018-05-31T04:23:17")
      plus(now, createUtilizatorService(everyDay).nextRunDelay(now)) shouldEqual
        LocalDateTime.parse("2018-05-31T05:00:00")
    }

    "correctly evaluate next day run" in {
      val now = LocalDateTime.parse("2018-05-31T05:23:17")
      plus(now, createUtilizatorService(everyDay).nextRunDelay(now)) shouldEqual
        LocalDateTime.parse("2018-06-01T05:00:00")
    }

    "correctly evaluate current moment run" in {
      val now = LocalDateTime.parse("2018-05-31T05:00:00")
      plus(now, createUtilizatorService(everyDay).nextRunDelay(now)) shouldEqual
        LocalDateTime.parse("2018-06-01T05:00:00")
    }

    "delete task urls" in {
      val now = LocalDateTime.now()
      val task1 = newTasksGen.next.copy(
        createdAt = now.minusDays(10),
        url = "http://storage.mds.yandex.net/get-autoru/904914/1fb537c04a403b713285e5301179f90d.xml"
      )
      when(taskDao.getOldHandTasks(?, ?, ?)).thenReturn(List(task1))
      when(blockedKVClient.get(?)).thenReturn(None)
      Mockito.doNothing().when(blockedKVClient).set(?, ?)
      when(mdsClient.delete(?, ?)).thenReturn(Future.successful(true))

      val utilizator = createUtilizatorService()
      rt.unsafeRunTask(utilizator.run())

      verify(mdsClient).delete("904914", "1fb537c04a403b713285e5301179f90d.xml")
      verify(blockedKVClient).set(?, argEq(task1.id.toString))
    }

    "don't fail on incorrect url" in {
      val now = LocalDateTime.now()
      val task1 = newTasksGen.next.copy(createdAt = now.minusDays(10), url = "foo[1]%1%2 #@(*$:")
      when(taskDao.getOldHandTasks(?, ?, ?)).thenReturn(List(task1))

      val utilizator = createUtilizatorService()
      rt.unsafeRunTask(utilizator.run())
    }

    "fail utilizeHandFeeds if MDS HTTP exceptions repeated more then N times" in {
      val now = LocalDateTime.now()
      val task1 = newTasksGen.next.copy(
        createdAt = now.minusDays(10),
        url = "http://storage.mds.yandex.net/get-autoru/904914/1fb537c04a403b713285e5301179f90d.xml"
      )
      val taskDaoLocal = mock[TasksDao]
      when(taskDaoLocal.findEarliestTaskIdByTime(?)).thenReturn(None)
      when(taskDaoLocal.getOldHandTasks(?, ?, ?)).thenReturn(List(task1))

      when(blockedKVClient.get(?)).thenReturn(None)
      Mockito.doNothing().when(blockedKVClient).set(?, ?)
      when(mdsClient.delete(?, ?))
        .thenAnswer((invocation: InvocationOnMock) => Future.failed(new RuntimeException("ERROR 1")))

      intercept[RuntimeException] {
        val utilizator = createUtilizatorService(td = taskDaoLocal)
        rt.unsafeRunTask(utilizator.run())
      }.getMessage shouldEqual "ERROR 1"

      verify(mdsClient, times(3)).delete("904914", "1fb537c04a403b713285e5301179f90d.xml")
      verify(taskDaoLocal, atLeastOnce()).findEarliestTaskIdByTime(?)
      verify(blockedKVClient, never()).set(?, ?)
    }

    "retry MDS request N times in case of failure" in {
      val now = LocalDateTime.now()
      val task1 = newTasksGen.next.copy(
        createdAt = now.minusDays(10),
        url = "http://storage.mds.yandex.net/get-autoru/904914/1fb537c04a403b713285e5301179f90d.xml"
      )
      when(taskDao.getOldHandTasks(?, ?, ?)).thenReturn(List(task1))
      when(blockedKVClient.get(?)).thenReturn(None)
      Mockito.doNothing().when(blockedKVClient).set(?, ?)
      when(mdsClient.delete(?, ?))
        .thenAnswer((invocation: InvocationOnMock) => Future.failed(new RuntimeException("ERROR 1")))
        .thenAnswer((invocation: InvocationOnMock) => Future.failed(new RuntimeException("ERROR 2")))
        .thenReturn(Future.successful(true))

      val utilizator = createUtilizatorService()
      rt.unsafeRunTask(utilizator.run())

      verify(mdsClient, times(3)).delete("904914", "1fb537c04a403b713285e5301179f90d.xml")
      verify(blockedKVClient).set(?, argEq(task1.id.toString))
    }
  }
}
