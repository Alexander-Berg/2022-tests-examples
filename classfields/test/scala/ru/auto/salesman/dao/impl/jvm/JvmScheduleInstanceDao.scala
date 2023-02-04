package ru.auto.salesman.dao.impl.jvm

import java.util.concurrent.atomic.AtomicLong

import ru.auto.salesman.dao.ScheduleInstanceDao
import ru.auto.salesman.dao.ScheduleInstanceDao.InstanceFilter.{
  FireTimeIn,
  ForIds,
  ForScheduleIds,
  ForStatuses
}
import ru.auto.salesman.dao.ScheduleInstanceDao.InstanceLimit.{Count, NoLimit}
import ru.auto.salesman.dao.ScheduleInstanceDao.InstanceOrder.{ByFireTime, NoOrder}
import ru.auto.salesman.dao.ScheduleInstanceDao.Patch.{FireTimePatch, StatusPatch}
import ru.auto.salesman.dao.ScheduleInstanceDao._
import ru.auto.salesman.dao.exceptions.ProcessScheduleInstanceException
import ru.auto.salesman.model.ScheduleInstance
import ru.auto.salesman.model.ScheduleInstance.Statuses
import ru.auto.salesman.model.ScheduleInstance.Statuses.{Acquired, Cancelled, Pending}
import ru.auto.salesman.model.schedule.RescheduleStrategy
import ru.auto.salesman.service.ScheduleInstanceService.Source
import ru.auto.salesman.util.DateTimeInterval
import ru.yandex.vertis.util.collection.TryUtil
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class JvmScheduleInstanceDao(
    initial: Iterable[ScheduleInstance] = Iterable.empty
) extends ScheduleInstanceDao {

  if (initial.nonEmpty)
    require(
      initial.map(_.id).toSet.size == initial.size,
      "Found duplicate ids in given initial instances"
    )

  private val idGen = {
    val startId =
      if (initial.nonEmpty)
        initial.map(_.id).max
      else
        0L
    new AtomicLong(startId)
  }

  private val storage = mutable.ArrayBuffer.empty[ScheduleInstance] ++ initial

  def insert(sources: Seq[Source]): Try[Unit] = Try {
    val now = DateTimeUtil.now()
    val instances = sources.map(s =>
      ScheduleInstance(
        idGen.incrementAndGet(),
        s.scheduleId,
        s.fireTime,
        now,
        s.scheduleUpdateTime,
        Statuses.Pending,
        now
      )
    )
    storage ++= instances
  }

  def get(filters: InstanceFilter*)(
      order: InstanceOrder,
      limit: InstanceLimit
  ): Try[Iterable[ScheduleInstance]] = Try {
    val filtered = storage.filter(toPredicate(filters))
    val ordered = order match {
      case ByFireTime(asc) =>
        val preFiltered = filtered.sortBy(_.fireTime)
        if (asc) preFiltered else preFiltered.reverse
      case NoOrder => filtered
    }
    limit match {
      case NoLimit =>
        ordered
      case Count(n) =>
        ordered.take(n)
    }
  }

  private def toPredicate(
      filters: Seq[InstanceFilter]
  ): ScheduleInstance => Boolean =
    filters.foldLeft((_: ScheduleInstance) => true) {
      case (prev, ForIds(ids)) =>
        r => prev(r) && ids(r.id)
      case (prev, ForStatuses(statuses)) =>
        r => prev(r) && statuses(r.status)
      case (prev, ForScheduleIds(scheduleIds)) =>
        r => prev(r) && scheduleIds(r.scheduleId)
      case (prev, FireTimeIn(interval)) =>
        r => prev(r) && interval.contains(r.fireTime)
    }

  def update(patch: Patch, filters: InstanceUpdateFilter*): Try[Int] =
    Try {
      if (filters.isEmpty) throw new IllegalArgumentException("Empty filters")
      val predicate = filters.foldLeft((_: ScheduleInstance) => true) {
        case (prev, ForIds(ids)) =>
          r => prev(r) && ids(r.id)
        case (prev, ForScheduleIds(scheduleIds)) =>
          r => prev(r) && scheduleIds(r.scheduleId)
        case (prev, ForStatuses(statuses)) =>
          r => prev(r) && statuses(r.status)
      }
      val indices = storage.zipWithIndex.collect {
        case (r, idx) if predicate(r) => idx
      }
      if (indices.nonEmpty) {
        val now = DateTimeUtil.now()
        patch match {
          case StatusPatch(status) =>
            indices.foreach { idx =>
              storage.update(
                idx,
                storage(idx).copy(status = status, epoch = now)
              )
            }
          case FireTimePatch(fireTime) =>
            indices.foreach { idx =>
              storage.update(
                idx,
                storage(idx).copy(fireTime = fireTime, epoch = now)
              )
            }
        }
        indices.size
      } else
        0
    }

  def cancelPendingAndInsert(sources: Iterable[Source]): Try[Unit] =
    for {
      _ <- update(
        StatusPatch(Cancelled),
        ForScheduleIds(sources.map(_.scheduleId).toSet),
        ForStatuses(Pending)
      ).map(_ => ())
      _ <- insert(sources.toSeq)
    } yield ()

  def updateAndReschedule(
      instance: ScheduleInstance,
      patch: Patch,
      rescheduleStrategy: RescheduleStrategy
  ): Try[Int] =
    for {
      processedCount <- update(
        patch,
        ForIds(instance.id),
        ForStatuses(Acquired)
      )
      _ <-
        if (processedCount == 1)
          Success(())
        else
          Failure(
            new ProcessScheduleInstanceException(
              s"Unable to cancel one acquired instance with id = ${instance.id}. " +
              s"Cancelled $processedCount instances actually, will rollback it."
            )
          )
      instancesForUpdate <- get(
        ForScheduleIds(instance.scheduleId),
        ForStatuses(Pending),
        FireTimeIn(DateTimeInterval.wholeDay(instance.fireTime))
      )()
      // + 1 to generate date for new instance too
      rescheduledCount <- rescheduleStrategy(
        instancesForUpdate.size + 1
      ) match {
        case Nil => Success(0)
        case createFireTime :: updateFireTimes =>
          insert(
            List(
              Source(
                instance.scheduleId,
                createFireTime,
                instance.scheduleUpdateTime
              )
            )
          )
          val insertCount = 1
          val updateCounts = instancesForUpdate.zip(updateFireTimes).map {
            case (instanceForUpdate, fireTime) =>
              update(
                FireTimePatch(fireTime),
                ForIds(instanceForUpdate.id)
              )
          }
          TryUtil.sequence(updateCounts).map(_.sum + insertCount)
      }
    } yield rescheduledCount

  def clear(): Unit =
    storage.clear()
}
