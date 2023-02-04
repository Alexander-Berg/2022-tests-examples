package ru.auto.salesman.dao.impl.jvm.user

import cats.data.NonEmptyList
import cats.implicits._
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.dao.user.ProductScheduleDao.ScheduleFilter._
import ru.auto.salesman.dao.user.ProductScheduleDao.{
  Patch,
  ScheduleFilter,
  ScheduleUpdateFilter
}
import ru.auto.salesman.model.user.schedule.AllowMultipleRescheduleUpsert.{
  False,
  SameOrTrue
}
import ru.auto.salesman.model.user.schedule.{ProductSchedule, ScheduleSource}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.mutable
import scala.util.Try

class JvmProductScheduleDao(initial: Iterable[ProductSchedule] = Iterable.empty)
    extends ProductScheduleDao {

  if (initial.nonEmpty)
    require(
      initial.map(_.id).size == initial.size,
      "Found duplicate ids in given initial schedules"
    )

  private[user] val storage = mutable.ArrayBuffer
    .empty[ProductSchedule] ++ initial

  def get(filters: ScheduleFilter*): Try[Iterable[ProductSchedule]] =
    Try {
      storage.filter(toPredicate(filters))
    }

  def scan(
      filters: ScheduleFilter*
  )(callback: ProductSchedule => Try[Unit]): Try[Unit] =
    Try {
      storage.filter(toPredicate(filters)).map(callback).toStream.sequence_
    }.flatten

  def update(patch: Patch, filters: ScheduleUpdateFilter*): Try[Unit] =
    Try {
      if (filters.isEmpty)
        throw new IllegalArgumentException("Empty filters are not allowed")
      val predicate = filters.foldLeft((_: ProductSchedule) => true) {
        case (prev, ForId(id)) =>
          r => prev(r) && r.id == id
        case (prev, ForIds(ids)) =>
          r => prev(r) && ids.exists(_ == r.id)
        case (prev, ForOfferId(offerId)) =>
          r => prev(r) && r.offerId == offerId
        case (prev, ForProduct(product)) =>
          r => prev(r) && r.product == product
        case (prev, ForProducts(products)) =>
          r => prev(r) && products.exists(_ == r.product)
        case (prev, ForUserRef(userRef)) =>
          r => prev(r) && r.user == userRef
        case (prev, ForOfferIds(offerIds)) =>
          r => prev(r) && offerIds.exists(_ == r.offerId)
        case (prev, Visible) =>
          r => prev(r) && r.isVisible.value
        case (prev, Expired) =>
          r => prev(r) && r.expireDate.exists(_.isBeforeNow())
        case (prev, NonExpired) =>
          r => prev(r) && r.expireDate.forall(_.isAfterNow())
      }
      val patchF: ProductSchedule => ProductSchedule = patch match {
        case Patch.Delete =>
          _.copy(isDeleted = true)
      }
      val indices = storage.zipWithIndex.collect {
        case (r, idx) if predicate(r) => idx
      }
      indices.foreach { idx =>
        storage.update(
          idx,
          patchF(storage(idx)).copy(epoch = DateTimeUtil.now())
        )
      }
    }

  private def toPredicate(
      filters: Seq[ScheduleFilter]
  ): (ProductSchedule => Boolean) =
    filters.foldLeft((_: ProductSchedule) => true) {
      case (prev, ForId(id)) =>
        r => prev(r) && r.id == id
      case (prev, ForUserRef(user)) =>
        r => prev(r) && r.user == user
      case (prev, UpdatedSince(since)) =>
        r => prev(r) && r.epoch.getMillis > since
      case (prev, IsDeleted(isDeleted)) =>
        r => prev(r) && r.isDeleted == isDeleted
      case (prev, ForOfferId(offerId)) =>
        r => prev(r) && r.offerId.toString == offerId.toString
      case (prev, ForProduct(product)) =>
        r => prev(r) && r.product == product
      case (prev, ForIds(ids)) =>
        r => prev(r) && ids.contains_(r.id)
      case (prev, ForOfferIds(offerIds)) =>
        r => prev(r) && offerIds.contains_(r.offerId)
      case (prev, ForProducts(products)) =>
        r => prev(r) && products.contains_(r.product)
      case (prev, Visible) =>
        r => prev(r) && r.isVisible.value
      case (prev, Expired) =>
        r => prev(r) && r.expireDate.exists(_.isBeforeNow())
      case (prev, NonExpired) =>
        r => prev(r) && r.expireDate.forall(_.isAfterNow())
    }

  def clear(): Unit =
    storage.clear()

  def insert(requests: Iterable[ProductSchedule]): Try[Unit] = Try {
    for (r <- requests)
      storage.append(r)
  }

  def replace(requests: Iterable[ScheduleSource]): Try[Unit] =
    Try {
      for (s <- requests) {
        val filters = Seq(
          ForUserRef(s.user),
          ForOfferId(s.offerId),
          ForProduct(s.product),
          IsDeleted(false),
          Visible
        )

        val existingSchedules = storage.filter(toPredicate(filters))

        if (
          !existingSchedules.exists(
            _.scheduleParameters == s.scheduleParameters
          )
        ) {
          NonEmptyList.fromList(existingSchedules.map(_.id).toList).foreach { ids =>
            update(Patch.Delete, ForIds(ids))
          }

          storage.append(toProductSchedule(s, existingSchedules.headOption))
        }
      }
    }

  def insertIfAbsent(schedule: ScheduleSource): Try[Unit] = Try {
    val existingSchedules = storage.filter(
      toPredicate(
        List(
          ForUserRef(schedule.user),
          ForOfferId(schedule.offerId),
          ForProduct(schedule.product),
          IsDeleted(false)
        )
      )
    )
    if (
      !existingSchedules.exists(
        _.scheduleParameters == schedule.scheduleParameters
      )
    )
      storage.append(toProductSchedule(schedule, existingSchedules.headOption))
  }

  private def toProductSchedule(
      s: ScheduleSource,
      previousSchedule: Option[ProductSchedule]
  ): ProductSchedule = {
    val allowMultipleReschedule = s.allowMultipleReschedule match {
      case SameOrTrue => previousSchedule.forall(_.allowMultipleReschedule)
      case False => false
    }
    ProductSchedule(
      -1,
      s.offerId,
      s.user,
      s.product,
      DateTimeUtil.now,
      s.scheduleParameters,
      false,
      DateTimeUtil.now,
      s.isVisible,
      s.expireDate,
      s.customPrice,
      allowMultipleReschedule,
      s.prevScheduleId
    )
  }

  def delete(filters: ScheduleUpdateFilter*): Try[Unit] =
    update(Patch.Delete, filters: _*)
}
