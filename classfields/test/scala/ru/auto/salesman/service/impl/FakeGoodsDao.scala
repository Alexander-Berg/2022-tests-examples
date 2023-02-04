package ru.auto.salesman.service.impl

import ru.auto.salesman.Task
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.GoodsDao.Condition.WithGoodsId
import ru.auto.salesman.dao.GoodsDao.Filter.{AlreadyBilled, ForOfferProductStatus}
import ru.auto.salesman.dao.GoodsDao.{
  AppliedSource,
  ArchiveFilter,
  Condition,
  Filter,
  Patch,
  Record,
  Source,
  Update
}
import ru.auto.salesman.dao.impl.jdbc.JdbcGoodsQueries
import ru.auto.salesman.model.GoodsId
import ru.auto.salesman.service.impl.FakeGoodsDao._

import scala.collection.mutable
import scala.util.{Success, Try}

class FakeGoodsDao extends GoodsDao {

  private val goods = new mutable.LinkedHashMap[GoodsId, Record]

  def upsert(good: Record): Unit = goods.put(good.primaryKeyId, good)

  def scan(filter: Filter)(handler: Record => Unit): Try[Unit] = ???

  def scanIter(filter: Filter)(f: Iterator[Record] => Unit): Try[Unit] =
    Success(f(goods.values.iterator))

  def get(filter: Filter): Try[Iterable[Record]] = {

    def predicate(good: Record): Boolean = filter match {
      case AlreadyBilled(offer, category, product) =>
        good.offerId == offer &&
          good.category == category &&
          good.product == product &&
          good.offerBilling.isDefined
      case ForOfferProductStatus(offer, product, category) =>
        good.offerId == offer &&
          good.category == category &&
          good.product == product
      case _ => ???
    }

    Success(goods.values.filter(predicate))
  }

  def archive(filter: ArchiveFilter): Try[Unit] = ???

  def update(condition: Condition, patch: Patch): Try[Unit] = {

    def predicate(good: Record): Boolean = condition match {
      case WithGoodsId(goodsId) => good.primaryKeyId == goodsId
      case _ => ???
    }

    def patched(good: Record): Record =
      good.copy(
        status = patch.status.getOrElse(good.status),
        expireDate = patch.expireDate.orElse(good.expireDate),
        firstActivateDate = patch.firstActivateDate.orOld(good.firstActivateDate),
        offerBilling = patch.offerBilling.orOld(good.offerBilling),
        offerBillingDeadline =
          patch.offerBillingDeadline.orOld(good.offerBillingDeadline),
        holdTransactionId = patch.holdId.orOld(good.holdTransactionId)
      )

    Success(goods.values.filter(predicate).map(patched).foreach(upsert))
  }

  def insert(source: Source): Task[Record] = ???

  def insertApplied(source: AppliedSource): Try[Unit] = ???

  /** for outer transactions - implementation-dependant =(
    * ideally need separate trait
    */
  override def getJdbcDaoQueries: JdbcGoodsQueries = ???
}

object FakeGoodsDao {

  implicit class RichOptionUpdate[A](private val patch: Option[Update[A]])
      extends AnyVal {

    def orOld(oldValue: Option[A]): Option[A] = patch match {
      case Some(Update(newValue)) => Some(newValue)
      case None => oldValue
    }

    def orOld(oldValue: A): A = patch match {
      case Some(Update(newValue)) => newValue
      case None => oldValue
    }
  }
}
