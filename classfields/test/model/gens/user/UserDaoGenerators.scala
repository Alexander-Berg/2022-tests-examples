package ru.auto.salesman.test.model.gens.user

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.dao.user.{
  BundleDao,
  GoodsDao,
  NotificationsDao,
  SubscriptionDao,
  TransactionDao
}
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.{
  AutoruUser,
  DomainAware,
  Funds,
  ProductStatus,
  ProductStatuses,
  TransactionId,
  TransactionStatus,
  TransactionStatuses,
  UserId
}
import ru.auto.salesman.model.user.{Prolongable, Transaction}
import ru.auto.salesman.model.user.product.ProductProvider.AutoProlongable
import ru.auto.salesman.model.user.product.Products
import ru.auto.salesman.model.user.product.Products.{Goods, GoodsBundle}
import ru.yandex.vertis.generators.DateTimeGenerators

import scala.concurrent.duration.DurationInt

trait UserDaoGenerators
    extends UserModelGenerators
    with DateTimeGenerators
    with DomainAware {

  def transactionRecordGen(
      statusGen: Gen[TransactionStatus] = enumGen(TransactionStatuses)
  ): Gen[TransactionDao.Record] =
    for {
      transaction <- finalTransactionGen(statusGen)
    } yield
      transaction match {
        case Transaction(
              id,
              transactionId,
              user,
              amount,
              status,
              payload,
              createdAt,
              bankerTransactionId,
              paidAt,
              fields,
              epoch
            ) =>
          TransactionDao.Record(
            id,
            transactionId,
            user,
            amount,
            status,
            payload,
            createdAt,
            bankerTransactionId,
            paidAt,
            fields,
            epoch
          )
      }

  val notificationsRequestGen: Gen[NotificationsDao.Request] = for {
    id <- Gen.posNum[Long]
    dateTime <- dateTime()
  } yield NotificationsDao.Request(AutoruUser(id).toString, dateTime)

  val notificationsRequestGenInFuture: Gen[NotificationsDao.Request] = for {
    user <- AutoruUserGen
    dateTime <- dateTimeInFuture(3.minutes, 7.days)
  } yield NotificationsDao.Request(user.toString, dateTime)

  val notificationsRequestInPast: Gen[NotificationsDao.Request] = for {
    user <- AutoruUserGen
    dateTime <- dateTimeInPast(3.minutes, 7.days)
  } yield NotificationsDao.Request(user.toString, dateTime)

  val requestsNotificationsDaoGen: Gen[List[NotificationsDao.Request]] = for {
    numElems <- Gen.choose(1, 50)
    elems <- Gen.listOfN(numElems, notificationsRequestGen)
  } yield elems

  val requestsNotificationDaoInFutureGen: Gen[List[NotificationsDao.Request]] =
    for {
      elems <- Gen.listOfN(1, notificationsRequestGenInFuture).next
    } yield elems

  val requestsNotificationsDaoInPastGen: Gen[List[NotificationsDao.Request]] =
    for {
      numElems <- Gen.choose(1, 5)
      elems <- Gen.listOfN(numElems, notificationsRequestInPast).next
    } yield elems

  // ProductType, а не Product, чтобы избежать конфликта со scala.Product
  private def createRequestGen[OfferId, ProductType, Request, Context](
      createRequest: (
          OfferId,
          UserId,
          ProductType,
          Funds,
          ProductStatus,
          TransactionId,
          Option[String],
          DateTime,
          DateTime,
          Context,
          Prolongable
      ) => Request,
      product: Gen[ProductType],
      context: Gen[Context],
      offerId: Gen[OfferId],
      userId: Gen[UserId],
      status: Gen[ProductStatus],
      activated: Gen[DateTime],
      deadline: Gen[DateTime],
      transactionId: Gen[String]
  ) =
    for {
      offerId <- offerId
      userId <- userId
      product <- product
      amount <- Gen.posNum[Funds]
      status <- status
      transactionId <- transactionId
      baseGoodsId <- Gen.option(readableString)
      activated <- activated
      deadline <- deadline
      context <- context
      prolongable <- bool.map { prolong =>
        product match {
          case _: AutoProlongable => Prolongable(prolong)
          case _ => Prolongable(false)
        }
      }
    } yield
      createRequest(
        offerId,
        userId,
        product,
        amount,
        status,
        transactionId,
        baseGoodsId,
        activated,
        deadline,
        context,
        prolongable
      )

  def goodsCreateRequestGen(
      offerId: Gen[OfferIdentity] = OfferIdentityGen,
      userId: Gen[UserId] = AutoruUserIdGen,
      product: Gen[Goods] = productGen[Goods],
      status: Gen[ProductStatus] = productStatusGen,
      activated: Gen[DateTime] = dateTimeInPast,
      deadline: Gen[DateTime] = dateTimeInFuture(),
      transactionId: Gen[String] = readableString
  ): Gen[GoodsDao.Request] =
    createRequestGen(
      GoodsDao.Request.apply,
      product,
      GoodsContextGen,
      offerId,
      userId,
      status,
      activated,
      deadline,
      transactionId
    )

  def bundleCreateRequestGen(
      offerId: Gen[OfferIdentity] = OfferIdentityGen,
      userId: Gen[UserId] = AutoruUserIdGen,
      product: Gen[GoodsBundle] = productGen[GoodsBundle],
      status: Gen[ProductStatus] = productStatusGen,
      activated: Gen[DateTime] = dateTimeInPast,
      deadline: Gen[DateTime] = dateTimeInFuture(),
      transactionId: Gen[String] = readableString
  ): Gen[BundleDao.Request] =
    createRequestGen(
      BundleDao.Request.apply,
      product,
      BundleContextGen,
      Gen.some(offerId),
      userId,
      status,
      activated,
      deadline,
      transactionId
    )

  def subscriptionCreateRequestGen(
      userIdGen: Gen[UserId] = AutoruUserIdGen,
      productGen: Gen[Products.Subscription] = SubscriptionProductGen
  ): Gen[SubscriptionDao.Request] =
    subscriptionGen(userIdGen = userIdGen, productGen = productGen).map { subscription =>
      SubscriptionDao.Request(
        subscription.user,
        subscription.product,
        subscription.counter,
        subscription.amount,
        ProductStatuses.Active,
        subscription.transactionId,
        subscription.activated,
        subscription.deadline,
        subscription.context,
        subscription.prolongable
      )
    }
}
