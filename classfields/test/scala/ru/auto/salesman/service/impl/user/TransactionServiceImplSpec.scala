package ru.auto.salesman.service.impl.user

import ru.auto.salesman.dao.user.TransactionDao
import ru.auto.salesman.dao.user.TransactionDao.{Filter, TransactionAlreadyExists}
import ru.auto.salesman.dao.user.TransactionDao.Filter.ForTransactionId
import ru.auto.salesman.model.DeprecatedDomain
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.user.CreateTransactionResult
import ru.auto.salesman.service.banker.BankerService
import ru.auto.salesman.service.user.{BundleService, GoodsService, SubscriptionService}
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig, TestException}
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators

import scala.language.existentials

class TransactionServiceImplSpec
    extends BaseSpec
    with UserDaoGenerators
    with IntegrationPropertyCheckConfig {

  private val transactionDao = mock[TransactionDao]

  private val transactionService = new TransactionServiceImpl(
    transactionDao,
    mock[GoodsService],
    mock[BundleService],
    mock[SubscriptionService],
    mock[BankerService]
  )

  implicit override def domain: DeprecatedDomain = AutoRu

  private def expectGetTransaction(
      filter: TransactionDao.Filter,
      operateOnMaster: Boolean
  ) =
    (transactionDao.get(_: Filter, _: Boolean)).expects(filter, operateOnMaster)

  private def expectSucceedGetTransaction(
      filter: TransactionDao.Filter,
      operateOnMaster: Boolean,
      result: List[TransactionDao.Record]
  ) =
    expectGetTransaction(filter, operateOnMaster).returningZ(result)

  private def expectFailedGetTransaction(
      filter: TransactionDao.Filter,
      operateOnMaster: Boolean,
      e: Throwable
  ) =
    expectGetTransaction(filter, operateOnMaster).throwingZ(e)

  "Transaction service.getTransaction()" should {

    "get transaction by id from master" in {
      forAll(transactionRecordGen()) { expected =>
        val filter = ForTransactionId(expected.transactionId)
        expectSucceedGetTransaction(
          filter,
          operateOnMaster = true,
          List(expected)
        )
        val actual = transactionService.getTransaction(filter).success.value
        actual shouldBe TransactionServiceImpl.asTransaction(expected)
      }
    }

    "get transaction by id from master as fallback" in {
      pending
      forAll(transactionRecordGen()) { expected =>
        val filter = ForTransactionId(expected.transactionId)
        expectSucceedGetTransaction(filter, operateOnMaster = false, Nil)
        expectSucceedGetTransaction(
          filter,
          operateOnMaster = true,
          List(expected)
        )
        val actual = transactionService.getTransaction(filter).success.value
        actual shouldBe TransactionServiceImpl.asTransaction(expected)
      }
    }

    "fail immediately (no fallback to master) on unexpected error" in {
      pending
      forAll(readableString) { transactionId =>
        val filter = ForTransactionId(transactionId)
        val exception = new NoSuchElementException
        expectFailedGetTransaction(filter, operateOnMaster = false, exception)
        transactionService
          .getTransaction(filter)
          .failure
          .exception shouldBe exception
      }
    }
  }
  "removeRedundantInformation" should {
    "remove productPriceInfo" in {
      forAll(ProductRequestGen) { productRequest =>
        val pr =
          TransactionServiceImpl.removeRedundantInformation(productRequest)
        pr.context.productPrice.productPriceInfo shouldBe None
      }
    }
  }

  "create transaction" should {
    "create new transaction if not exists" in {
      forAll(TransactionRequestGen) { request =>
        (transactionDao.createWithIdentity _)
          .expects(*, *)
          .returningZ(Right(unit))

        transactionService
          .create(request)
          .success
          .value shouldBe an[CreateTransactionResult]
      }
    }

    "fail if dao can't create transaction" in {
      forAll(TransactionRequestGen) { request =>
        val exception = new TestException()
        (transactionDao.createWithIdentity _)
          .expects(*, *)
          .throwingZ(exception)

        transactionService
          .create(request)
          .failure
          .exception shouldBe exception
      }
    }

    "reuse transactionId if dao returns TransactionAlreadyExists from createWithIdentity" in {
      forAll(TransactionRequestGen) { request =>
        val existedId = "existed"
        (transactionDao.createWithIdentity _)
          .expects(*, *)
          .returningZ(Left(TransactionAlreadyExists(existedId)))

        transactionService
          .create(request)
          .success
          .value
          .transactionId shouldBe existedId
      }
    }
  }
}
