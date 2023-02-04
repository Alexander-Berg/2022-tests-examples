package ru.auto.salesman.service.impl.user

import java.util.concurrent.atomic.AtomicInteger
import ru.auto.salesman.Task
import ru.auto.salesman.dao.user.BundleDao
import ru.auto.salesman.dao.user.BundleDao.Filter.{
  ChangedSince,
  ForActiveProductUserOffer
}
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.Bundle
import ru.auto.salesman.model.user.product.Products.GoodsBundle
import ru.auto.salesman.test.dummy.IgnoredInAssertions

class DummyBundleDao extends BundleDao {

  @volatile private var bundles = List.empty[Bundle]
  private val idCounter = new AtomicInteger(0)

  def insertIfNotExists(request: BundleDao.Request): Task[Bundle] =
    Task {
      bundles.find(_.id == request.bundleId).getOrElse {
        val id = idCounter.incrementAndGet().toString
        val offerId = request.offer match {
          case Some(id: AutoruOfferId) => id
          case unexpected =>
            throw new IllegalArgumentException(
              s"Expected AutoruOfferId; got $unexpected"
            )
        }
        val product = request.product match {
          case bundle: GoodsBundle => bundle
          case unexpected =>
            throw new IllegalArgumentException(
              s"Expected GoodsBundle; got $unexpected"
            )
        }
        val bundle = Bundle(
          id,
          offerId,
          request.user,
          product,
          request.amount,
          request.status,
          request.transactionId,
          request.context,
          request.activated,
          request.deadline,
          now().getMillis,
          request.prolongable
        )
        bundles = bundle :: bundles
        bundle
      }
    }

  def get(filter: BundleDao.Filter): Task[Iterable[Bundle]] = Task {
    filter match {
      case ForActiveProductUserOffer(product, user, offer) =>
        bundles.filter(bundle =>
          bundle.product == product && bundle.user == user && offer.forall(
            _ == bundle.offer
          )
        )
      case ChangedSince(epoch) => bundles.filter(_.epoch > epoch.getMillis)
      case _ =>
        throw new NotImplementedError(s"No implementation for $filter yet")
    }
  }

  def replace(bundle: Bundle, request: BundleDao.Request): Task[Bundle] = ???

  def update(
      condition: BundleDao.Condition,
      patch: BundleDao.Patch
  ): Task[Int] = IgnoredInAssertions.int

  def clean(): Unit = bundles = Nil
}
