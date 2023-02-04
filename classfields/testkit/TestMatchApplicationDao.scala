package auto.dealers.match_maker.logic

import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

import auto.dealers.match_maker.logic.dao.MatchApplicationDao
import auto.dealers.match_maker.model.MatchApplicationState
import ru.auto.match_maker.model.api.ApiModel._
import auto.dealers.match_maker.util.ApiExceptions.MatchApplicationDuplicateException
import auto.dealers.match_maker.util.RichTypes._
import auto.dealers.match_maker.util.TimestampUtils._
import zio.{Task, UIO, UManaged, ZIO}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

class TestMatchApplicationDao extends MatchApplicationDao.Service {
  import TestMatchApplicationDao._

  private val matchApplications = new ConcurrentHashMap[String, MatchApplicationInfo]()
  private val processedUsersApplications = new ConcurrentHashMap[Long, Timestamp]()

  override def nextBatchForProcessing(count: Int): Task[List[MatchApplication]] =
    ZIO.effectTotal {
      matchApplications.asScala
        .filter { case (k, v) =>
          v.state == MatchApplicationState.NeedsProcessing
        }
        .take(count)
        .map { case (k, v) =>
          matchApplications.put(k, v.copy(state = MatchApplicationState.Busy, lastModified = now()))
          v.application
        }
        .toList
    }

  override def update(matchApplication: MatchApplication, state: Option[MatchApplicationState.Value]): Task[Unit] =
    ZIO.effectTotal {
      if (matchApplications.containsKey(matchApplication.getId)) {
        matchApplications.put(
          matchApplication.getId, {
            val old = matchApplications.get(matchApplication.getId)
            old.copy(application = matchApplication, state = state.getOrElse(old.state), lastModified = now())
          }
        )
        ()
      }
    }

  override def create(
      id: String,
      matchReq: MatchApplication,
      state: MatchApplicationState.Value,
      ttl: FiniteDuration): Task[Unit] =
    ZIO
      .effectTotal(matchApplications.put(id, MatchApplicationInfo(matchReq, state, now(), now().add(ttl), now())))
      .unit

  override def list(matchReqIds: List[String]): Task[List[MatchApplication]] =
    ZIO.effectTotal(matchApplications.asScala.collect {
      case (k, v) if matchReqIds.contains(k) => v.application
    }.toList)

  override def getRecentUserApplications(
      userId: Long,
      dealersLowPriorityDays: FiniteDuration): Task[List[MatchApplication]] =
    for {
      from <- ZIO.effectTotal(now().sub(dealersLowPriorityDays))
    } yield matchApplications.asScala.collect {
      case (k, v) if v.application.getUserInfo.getUserId == userId && v.created.compareTo(from) > 0 => v.application
    }.toList

  override def fixIdle(): Task[Int] =
    for {
      from <- ZIO.effectTotal(now().sub(10.minutes))
    } yield {
      processedUsersApplications.asScala.foreach {
        case (k, v) if v.compareTo(from) < 0 => processedUsersApplications.remove(k)
      }
      matchApplications.asScala.collect {
        case (k, v) if v.state == MatchApplicationState.Busy && v.lastModified.compareTo(from) < 0 =>
          matchApplications.remove(k)
      }.size
    }

  override def tryAcquireUserLock(userId: Long): Task[Boolean] =
    if (processedUsersApplications.containsKey(userId))
      ZIO.succeed(false)
    else {
      processedUsersApplications.put(userId, now())
      ZIO.succeed(true)
    }

  override def releaseUserLock(userId: Long): Task[Unit] =
    ZIO.effectTotal(processedUsersApplications.remove(userId)).unit

  override def queueStats(): Task[Map[MatchApplicationState.Value, Int]] =
    ZIO.effect {
      matchApplications
        .values()
        .asScala
        .filterNot(_.state == MatchApplicationState.Processed)
        .groupBy(_.state)
        .view
        .mapValues(_.size)
        .toMap
    }
}

object TestMatchApplicationDao {

  case class MatchApplicationInfo(
      application: MatchApplication,
      state: MatchApplicationState.Value,
      created: Timestamp,
      expiring: Timestamp,
      lastModified: Timestamp)

  def make: UIO[MatchApplicationDao.Service] =
    ZIO.effectTotal(new TestMatchApplicationDao)

}
