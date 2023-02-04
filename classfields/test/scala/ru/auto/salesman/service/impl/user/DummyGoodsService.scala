package ru.auto.salesman.service.impl.user

import java.util.concurrent.atomic.AtomicInteger

import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.dao.user.GoodsDao
import ru.auto.salesman.dao.user.GoodsDao.Filter.ChangedSince
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.ProductContext.GoodsContext
import ru.auto.salesman.model.user.product.{ProductProvider, Products}
import ru.auto.salesman.model.{ProductStatus, ProductStatuses, UserId}
import ru.auto.salesman.model.user.{Goods, PaidTransaction, ProductRequest, Prolongable}
import ru.auto.salesman.service.user.{GoodsService, UserProductService}
import ru.auto.salesman.test.dummy.IgnoredInAssertions
import zio.ZIO

class DummyGoodsService extends GoodsService {

  @volatile private var goods = List.empty[Goods]
  private val idCounter = new AtomicInteger()

  def get(filter: GoodsDao.Filter): Task[Iterable[Goods]] =
    ZIO.succeed {
      filter match {
        case ChangedSince(epoch) =>
          goods.filter(_.epoch > epoch.getMillis)
        case _ =>
          throw new NotImplementedError(s"No implementation for $filter yet")
      }
    }

  def increaseActivePlacementDeadline(
      user: UserId,
      offerId: OfferIdentity,
      deadline: DateTime
  ): Task[Unit] = IgnoredInAssertions.unit

  def setProlongable(
      user: UserId,
      offerId: OfferIdentity,
      product: Products.Goods with ProductProvider.AutoProlongable,
      prolongable: Prolongable
  ): Task[UserProductService.SetProlongableResult] =
    ???

  def add(
      transaction: PaidTransaction,
      productRequest: ProductRequest
  ): Task[Goods] =
    Task {
      val id = idCounter.incrementAndGet().toString
      val product = productRequest.product match {
        case product: Products.Goods => product
        case unexpected =>
          throw new IllegalArgumentException(
            s"Expected Products.Goods; got $unexpected"
          )
      }
      val context = productRequest.context match {
        case context: GoodsContext => context
        case unexpected =>
          throw new IllegalArgumentException(
            s"Expected GoodsContext; got $unexpected"
          )
      }
      val activationInterval = UserProductsHelper.getActivationInterval(
        productRequest,
        existentOpt = None,
        now()
      )
      import transaction.{transactionId, user}
      val good = Goods(
        id,
        productRequest.offer.get,
        user,
        product,
        productRequest.amount,
        ProductStatuses.Active,
        transactionId,
        context,
        activationInterval.from,
        activationInterval.to,
        now().getMillis,
        productRequest.prolongable
      )
      goods = good :: goods
      good
    }

  def deactivate(
      transaction: PaidTransaction,
      products: List[Goods],
      reason: ProductStatus
  ): Task[Unit] = ???

  def clean(): Unit = goods = Nil
}
