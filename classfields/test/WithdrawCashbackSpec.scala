package billing.cashback.logic.test

import auto.common.clients.promocoder.PromocoderClient
import auto.common.clients.promocoder.PromocoderClient.Features
import auto.common.clients.promocoder.model.{FeatureInstanceId, PromocoderService}
import auto.common.clients.promocoder.model.features.{FeatureInstance, FeatureInstanceRequest}
import auto.common.clients.promocoder.model.users.PromocoderUser
import billing.cashback.payment.{GetPaymentByIdRequest, PaymentWithdrawRequest}
import common.zio.logging.Logging
import common.zio.sttp.model.SttpError
import common.zio.ydb.Ydb
import ru.yandex.vertis.billing.cashback.storage.PaymentEventDao
import ru.yandex.vertis.billing.cashback.storage.ydb.{YdbLogQueueDao, YdbPaymentEventDao}
import zio.{IO, ZIO}
import zio.test.{DefaultRunnableSpec, ZSpec}
import common.zio.ydb.testkit.TestYdb
import infra.feature_toggles.client.testkit.TestFeatureToggles
import ru.yandex.vertis.billing.cashback.logic.{
  Payment,
  PaymentExtractorLive,
  PromocoderServiceLive,
  WithdrawCashbackService,
  WithdrawCashbackServiceLive
}
import ru.yandex.vertis.billing.cashback.model.{Conflict, Duplicate, NotEnoughCashback, PromocoderError}
import ru.yandex.vertis.ydb.zio.TxRunner
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._
import common.zio.sttp.model.SttpError.SttpHttpError
import org.joda.time.DateTime
import sttp.model.StatusCode

object WithdrawCashbackSpec extends DefaultRunnableSpec {

  // TODO: вынести cleanup в @@ beforeAll. И там же чистить данные в ydb.
  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = (
    suite("WithdrawCashbackSpec")(
      testM("Withdraw success path. Withdraw promocoder feature, write payment event into database.") {
        for {
          // prepare test data
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ <- ZIO.succeed(featureTogglesClient.set("withdraw_real_cashback", true))
          timeNow <- zio.clock.instant.map(inst => new DateTime(inst.toEpochMilli))
          promocoderStub <- ZIO.service[PromocoderClientStub]
          _ <- ZIO.succeed(
            promocoderStub.setUnderlyingPromocoderClient(
              new PromocoderClient.Service {
                override def createFeatures(
                    service: PromocoderService,
                    batchId: String,
                    user: PromocoderUser,
                    featureInstances: Iterable[FeatureInstanceRequest]): IO[SttpError, Features] = ???

                override def getFeatures(service: PromocoderService, user: PromocoderUser): IO[SttpError, Features] = {
                  IO.succeed(
                    List(
                      PromocoderClientStub.feature(
                        user = "autoru_client_1",
                        tag = "loyalty",
                        featureId = "feature_id",
                        timeNow.plusMonths(1),
                        500
                      )
                    )
                  )
                }

                override def decrementFeatureWithKey(
                    service: PromocoderService,
                    featureId: FeatureInstanceId,
                    count: Long,
                    key: String): IO[SttpError, FeatureInstance] = {
                  featureId.value match {
                    case "feature_id" =>
                      IO.succeed(
                        PromocoderClientStub.feature(
                          user = "autoru_client_1",
                          tag = "loyalty",
                          featureId = "feature_id",
                          timeNow.plusMonths(1),
                          300
                        )
                      )
                  }
                }
              }
            )
          )

          // execute logic
          withdrawCashbackService <- ZIO.service[WithdrawCashbackService]
          _ <- withdrawCashbackService.withdraw(
            PaymentWithdrawRequest(
              workspace = "autoru_loyalty",
              clientId = "dealer:1",
              amountKopecks = 200,
              paymentId = "payment_1"
            )
          )

          // check data
          paymentEventDao <- ZIO.service[PaymentEventDao]
          txRunner <- ZIO.service[TxRunner]
          paymentEvents <- txRunner.runTx(paymentEventDao.getPaymentEvents("autoru_loyalty", "dealer:1", "payment_1"))
          paymentEvent = paymentEvents.head
          payment <- withdrawCashbackService.getPaymentById(
            GetPaymentByIdRequest(workspace = "autoru_loyalty", clientId = "dealer:1", paymentId = "payment_1")
          )

          // cleanup
          _ <- ZIO.succeed(promocoderStub.clear())
        } yield assertTrue(paymentEvent.amountKopecks == 200) &&
          assertTrue(paymentEvent.paymentId == "payment_1") &&
          assertTrue(paymentEvent.clientId == "dealer:1") &&
          assertTrue(paymentEvent.paymentEventType == "WITHDRAW") &&
          assertTrue(payment == Payment(amount = 200, isRefunded = false))
      },
      testM("Failed withdraw. Not enough money on promocoder feature.") {
        for {
          // prepare test data
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ <- ZIO.succeed(featureTogglesClient.set("withdraw_real_cashback", true))
          timeNow <- zio.clock.instant.map(inst => new DateTime(inst.toEpochMilli))
          promocoderStub <- ZIO.service[PromocoderClientStub]
          _ <- ZIO.succeed(promocoderStub.setUnderlyingPromocoderClient(new PromocoderClient.Service {
            override def createFeatures(
                service: PromocoderService,
                batchId: String,
                user: PromocoderUser,
                featureInstances: Iterable[FeatureInstanceRequest]): IO[SttpError, Features] = ???

            override def getFeatures(service: PromocoderService, user: PromocoderUser): IO[SttpError, Features] = {
              IO.succeed(
                List(
                  PromocoderClientStub.feature(
                    user = "autoru_client_3",
                    tag = "loyalty",
                    featureId = "feature_id",
                    timeNow.plusMonths(1),
                    100
                  )
                )
              )
            }

            override def decrementFeatureWithKey(
                service: PromocoderService,
                featureId: FeatureInstanceId,
                count: Long,
                key: String): IO[SttpError, FeatureInstance] = ???
          }))

          // execute logic
          withdrawCashbackService <- ZIO.service[WithdrawCashbackService]
          result <- withdrawCashbackService
            .withdraw(
              PaymentWithdrawRequest(
                workspace = "autoru_loyalty",
                clientId = "dealer:3",
                amountKopecks = 200,
                paymentId = "payment_3"
              )
            )
            .run

          // check data
          paymentEventDao <- ZIO.service[PaymentEventDao]
          txRunner <- ZIO.service[TxRunner]
          paymentEvents <- txRunner.runTx(paymentEventDao.getPaymentEvents("autoru_loyalty", "dealer:3", "payment_3"))
          // cleanup
          _ <- ZIO.succeed(promocoderStub.clear())
        } yield assert(result)(fails(equalTo(NotEnoughCashback()))) && assertTrue(paymentEvents.isEmpty)
      },
      testM("Failed withdraw. No money on promocoder feature, when we trying to withdraw.") {
        for {
          // prepare test data
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ <- ZIO.succeed(featureTogglesClient.set("withdraw_real_cashback", true))
          timeNow <- zio.clock.instant.map(inst => new DateTime(inst.toEpochMilli))
          promocoderStub <- ZIO.service[PromocoderClientStub]
          _ <- ZIO.succeed(promocoderStub.setUnderlyingPromocoderClient(new PromocoderClient.Service {
            override def createFeatures(
                service: PromocoderService,
                batchId: String,
                user: PromocoderUser,
                featureInstances: Iterable[FeatureInstanceRequest]): IO[SttpError, Features] = ???

            override def getFeatures(service: PromocoderService, user: PromocoderUser): IO[SttpError, Features] = {
              IO.succeed(
                List(
                  PromocoderClientStub.feature(
                    user = "autoru_client_2",
                    tag = "loyalty",
                    featureId = "feature_id",
                    timeNow.plusMonths(1),
                    500
                  )
                )
              )
            }

            override def decrementFeatureWithKey(
                service: PromocoderService,
                featureId: FeatureInstanceId,
                count: Long,
                key: String): IO[SttpError, FeatureInstance] = {
              notEnoughCountOnFeature()
            }

            private def notEnoughCountOnFeature() = {
              IO.fail(
                SttpHttpError(service = "test", name = "test", uri = "test", code = StatusCode(402), body = "test")
              )
            }
          }))

          withdrawCashbackService <- ZIO.service[WithdrawCashbackService]
          result <- withdrawCashbackService
            .withdraw(
              PaymentWithdrawRequest(
                workspace = "autoru_loyalty",
                clientId = "dealer:2",
                amountKopecks = 200,
                paymentId = "payment_2"
              )
            )
            .run

          // check data
          paymentEventDao <- ZIO.service[PaymentEventDao]
          txRunner <- ZIO.service[TxRunner]
          paymentEvents <- txRunner.runTx(paymentEventDao.getPaymentEvents("autoru_loyalty", "dealer:2", "payment_2"))

          // cleanup
          _ <- ZIO.succeed(promocoderStub.clear())
        } yield assert(result)(fails(equalTo(NotEnoughCashback()))) && assertTrue(paymentEvents.isEmpty)
      },
      testM("Failed withdraw. Promocoder feature not exists") {
        for {
          // prepare test data
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ <- ZIO.succeed(featureTogglesClient.set("withdraw_real_cashback", true))
          promocoderStub <- ZIO.service[PromocoderClientStub]
          _ <- ZIO.succeed(promocoderStub.setUnderlyingPromocoderClient(new PromocoderClient.Service {
            override def createFeatures(
                service: PromocoderService,
                batchId: String,
                user: PromocoderUser,
                featureInstances: Iterable[FeatureInstanceRequest]): IO[SttpError, Features] = ???

            override def getFeatures(service: PromocoderService, user: PromocoderUser): IO[SttpError, Features] =
              IO.succeed(List())

            override def decrementFeatureWithKey(
                service: PromocoderService,
                featureId: FeatureInstanceId,
                count: Long,
                key: String): IO[SttpError, FeatureInstance] = ???
          }))

          // execute logic
          withdrawCashbackService <- ZIO.service[WithdrawCashbackService]
          result <- withdrawCashbackService
            .withdraw(
              PaymentWithdrawRequest(
                workspace = "autoru_loyalty",
                clientId = "dealer:4",
                amountKopecks = 200,
                paymentId = "payment_4"
              )
            )
            .run

          // check data
          paymentEventDao <- ZIO.service[PaymentEventDao]
          txRunner <- ZIO.service[TxRunner]
          paymentEvents <- txRunner.runTx(paymentEventDao.getPaymentEvents("autoru_loyalty", "dealer:4", "payment_4"))

          // cleanup
          _ <- ZIO.succeed(promocoderStub.clear())
        } yield assert(result)(fails(equalTo(NotEnoughCashback()))) &&
          assertTrue(paymentEvents.isEmpty)
      },
      testM("Failed withdraw. Wrong promocoder features.") {
        for {
          // prepare test data
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ <- ZIO.succeed(featureTogglesClient.set("withdraw_real_cashback", true))
          timeNow <- zio.clock.instant.map(inst => new DateTime(inst.toEpochMilli))
          promocoderStub <- ZIO.service[PromocoderClientStub]
          _ <- ZIO.succeed(promocoderStub.setUnderlyingPromocoderClient(new PromocoderClient.Service {
            override def createFeatures(
                service: PromocoderService,
                batchId: String,
                user: PromocoderUser,
                featureInstances: Iterable[FeatureInstanceRequest]): IO[SttpError, Features] = ???

            override def getFeatures(service: PromocoderService, user: PromocoderUser): IO[SttpError, Features] = {
              val featureWithWrongTag = PromocoderClientStub.feature(
                user = "autoru_client_5",
                tag = "wrong_tag",
                featureId = "feature_id_5_1",
                timeNow.plusMonths(1),
                500
              )
              val featureWithWrongTagAndWordLoyalityInIt = PromocoderClientStub.feature(
                user = "autoru_client_5",
                tag = "wrong_tag_with_loyalty_in_it",
                featureId = "feature_id_5_2",
                timeNow.plusMonths(1),
                500
              )
              val expiredFeature = PromocoderClientStub.feature(
                user = "autoru_client_5",
                tag = "loyalty",
                featureId = "feature_id_5_3",
                timeNow.minusMonths(1),
                500
              )
              IO.succeed(
                List(
                  featureWithWrongTag,
                  featureWithWrongTagAndWordLoyalityInIt,
                  expiredFeature
                )
              )
            }

            override def decrementFeatureWithKey(
                service: PromocoderService,
                featureId: FeatureInstanceId,
                count: Long,
                key: String): IO[SttpError, FeatureInstance] = ???
          }))

          withdrawCashbackService <- ZIO.service[WithdrawCashbackService]
          result <- withdrawCashbackService
            .withdraw(
              PaymentWithdrawRequest(
                workspace = "autoru_loyalty",
                clientId = "dealer:5",
                amountKopecks = 200,
                paymentId = "payment_5"
              )
            )
            .run

          // check there is no transaction in database
          paymentEventDao <- ZIO.service[PaymentEventDao]
          txRunner <- ZIO.service[TxRunner]
          paymentEvents <- txRunner.runTx(paymentEventDao.getPaymentEvents("autoru_loyalty", "dealer:5", "payment_5"))

          // cleanup
          _ <- ZIO.succeed(promocoderStub.clear())
        } yield assert(result)(fails(equalTo(NotEnoughCashback()))) && assertTrue(paymentEvents.isEmpty)
      },
      testM("Failed withdraw. Try to withdraw with same id and amount twice.") {
        for {
          // prepare test data
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ <- ZIO.succeed(featureTogglesClient.set("withdraw_real_cashback", true))
          timeNow <- zio.clock.instant.map(inst => new DateTime(inst.toEpochMilli))
          promocoderStub <- ZIO.service[PromocoderClientStub]
          _ <- ZIO.succeed(promocoderStub.setUnderlyingPromocoderClient(new PromocoderClient.Service {
            override def createFeatures(
                service: PromocoderService,
                batchId: String,
                user: PromocoderUser,
                featureInstances: Iterable[FeatureInstanceRequest]): IO[SttpError, Features] = ???

            override def getFeatures(service: PromocoderService, user: PromocoderUser): IO[SttpError, Features] = {
              IO.succeed(
                List(
                  PromocoderClientStub.feature(
                    user = "autoru_client_6",
                    tag = "loyalty",
                    featureId = "feature_id",
                    timeNow.plusMonths(1),
                    500
                  )
                )
              )
            }

            override def decrementFeatureWithKey(
                service: PromocoderService,
                featureId: FeatureInstanceId,
                count: Long,
                key: String): IO[SttpError, FeatureInstance] = {
              featureId.value match {
                case "feature_id" =>
                  IO.succeed(
                    PromocoderClientStub.feature(
                      user = "autoru_client_6",
                      tag = "loyalty",
                      featureId = "feature_id",
                      timeNow.plusMonths(1),
                      300
                    )
                  )
              }
            }
          }))

          // execute logic
          paymentRequest = PaymentWithdrawRequest(
            workspace = "autoru_loyalty",
            clientId = "dealer:6",
            amountKopecks = 200,
            paymentId = "payment_6"
          )
          withdrawCashbackService <- ZIO.service[WithdrawCashbackService]
          _ <- withdrawCashbackService.withdraw(paymentRequest)
          result <- withdrawCashbackService.withdraw(paymentRequest).run

          // check data
          paymentEventDao <- ZIO.service[PaymentEventDao]
          txRunner <- ZIO.service[TxRunner]
          paymentEvents <- txRunner.runTx(paymentEventDao.getPaymentEvents("autoru_loyalty", "dealer:6", "payment_6"))

          // cleanup
          _ <- ZIO.succeed(promocoderStub.clear())
        } yield assert(result)(fails(equalTo(Duplicate()))) && assertTrue(paymentEvents.size == 1)
      },
      testM("Failed withdraw. Try to withdraw with same id and with different amount.") {
        for {
          // prepare test data
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ <- ZIO.succeed(featureTogglesClient.set("withdraw_real_cashback", true))
          timeNow <- zio.clock.instant.map(inst => new DateTime(inst.toEpochMilli))
          promocoderStub <- ZIO.service[PromocoderClientStub]
          _ <- ZIO.succeed(promocoderStub.setUnderlyingPromocoderClient(new PromocoderClient.Service {
            override def createFeatures(
                service: PromocoderService,
                batchId: String,
                user: PromocoderUser,
                featureInstances: Iterable[FeatureInstanceRequest]): IO[SttpError, Features] = ???

            override def getFeatures(service: PromocoderService, user: PromocoderUser): IO[SttpError, Features] = {
              IO.succeed(
                List(
                  PromocoderClientStub.feature(
                    user = "autoru_client_7",
                    tag = "loyalty",
                    featureId = "feature_id",
                    timeNow.plusMonths(1),
                    500
                  )
                )
              )
            }

            override def decrementFeatureWithKey(
                service: PromocoderService,
                featureId: FeatureInstanceId,
                count: Long,
                key: String): IO[SttpError, FeatureInstance] = {
              featureId.value match {
                case "feature_id" =>
                  IO.succeed(
                    PromocoderClientStub.feature(
                      user = "autoru_client_7",
                      tag = "loyalty",
                      featureId = "feature_id",
                      timeNow.plusMonths(1),
                      300
                    )
                  )
              }
            }
          }))

          // execute logic
          withdrawCashbackService <- ZIO.service[WithdrawCashbackService]
          _ <- withdrawCashbackService.withdraw(
            PaymentWithdrawRequest(
              workspace = "autoru_loyalty",
              clientId = "dealer:7",
              amountKopecks = 200,
              paymentId = "payment_7"
            )
          )
          result <- withdrawCashbackService
            .withdraw(
              PaymentWithdrawRequest(
                workspace = "autoru_loyalty",
                clientId = "dealer:7",
                amountKopecks = 300,
                paymentId = "payment_7"
              )
            )
            .run

          // check data
          paymentEventDao <- ZIO.service[PaymentEventDao]
          txRunner <- ZIO.service[TxRunner]
          paymentEvents <- txRunner.runTx(paymentEventDao.getPaymentEvents("autoru_loyalty", "dealer:7", "payment_7"))

          // cleanup
          _ <- ZIO.succeed(promocoderStub.clear())
        } yield assert(result)(fails(equalTo(Conflict()))) && assertTrue(paymentEvents.size == 1)
      },
      testM("Don't withdraw real cashback if feature turned off") {
        for {
          featureTogglesClient <- ZIO.service[TestFeatureToggles.Service]
          _ <- ZIO.succeed(featureTogglesClient.set("withdraw_real_cashback", false))

          // execute logic
          withdrawCashbackService <- ZIO.service[WithdrawCashbackService]
          result <- withdrawCashbackService.withdraw(
            PaymentWithdrawRequest(
              workspace = "autoru_loyalty",
              clientId = "dealer:8",
              amountKopecks = 200,
              paymentId = "payment_8"
            )
          )

        } yield assertTrue(result == Payment(amount = 200, isRefunded = false))
      }
    ) @@ sequential
  ).provideCustomLayerShared(
    TestYdb.ydb >+>
      Ydb.txRunner >+>
      YdbPaymentEventDao.live >+>
      YdbLogQueueDao.live >+>
      PromocoderClientStub.Test >+>
      PaymentExtractorLive.live >+>
      Clock.live >+>
      PromocoderServiceLive.live >+>
      Logging.live >+>
      TestFeatureToggles.live >+>
      WithdrawCashbackServiceLive.live
  )
}
