package ru.yandex.vertis.shark.controller

import baker.shark.core.dao.testkit.Mock._
import cats.syntax.option._
import common.zio.features.testkit.FeaturesTest
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoSyntax._
import ru.yandex.vertis.shark.Mock._
import ru.yandex.vertis.shark.sender.testkit.RealtimeCreditApplicationBankSenderMock
import ru.yandex.vertis.shark.controller.updater.testkit.CreditApplicationUpdaterMock
import ru.yandex.vertis.shark.controller.credit_application.testkit.CreditApplicationSchedulerMock
import ru.yandex.vertis.shark.controller.CreditApplicationController.CreditApplicationController
import ru.yandex.vertis.shark.controller.credit_product.testkit.CreditProductControllerMock
import ru.yandex.vertis.shark.controller.credit_product_calculator.testkit.CreditProductCalculatorMock
import ru.yandex.vertis.shark.controller.person_profile.testkit.PersonProfileControllerMock
import ru.yandex.vertis.shark.converter.protobuf.Implicits._
import ru.yandex.vertis.shark.dao.CreditApplicationDao
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.AutoruCreditApplication
import ru.yandex.vertis.shark.model.CreditApplicationId
import ru.yandex.vertis.shark.model.PersonProfileId
import ru.yandex.vertis.shark.model.PersonProfileImpl
import ru.yandex.vertis.shark.model.PersonProfileStub
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.ydb.zio.TxError
import ru.yandex.vertis.zio_baker.zio.token_distributor.config.TokenDistributorConfig
import zio.RIO
import zio.ULayer
import zio.ZIO
import zio.ZLayer
import zio.clock.Clock
import zio.test.{assertM, Assertion, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion.anything
import zio.test.Assertion.equalTo
import zio.test.Assertion.fails
import zio.test.Assertion.hasField
import zio.test.Assertion.isSome
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation
import zio.test.mock.Expectation.failure
import zio.test.mock.Expectation.value
import zio.test.mock.Expectation.valueF

object CreditApplicationControllerSpec extends DefaultRunnableSpec {

  private val domain = Domain.DOMAIN_AUTO
  private val tokenDistributorConfig = TokenDistributorConfig(16, EmptyString)

  private val emptyMocks =
    CreditApplicationDaoMock.empty ++ CreditApplicationSchedulerQueueDaoMock.empty ++
      CreditApplicationBankClaimDaoMock.empty ++ CreditApplicationClaimDaoMock.empty ++
      PersonProfileControllerMock.empty ++ CreditProductCalculatorMock.empty ++
      CreditProductControllerMock.empty ++ CreditApplicationUpdaterMock.empty ++
      CreditApplicationSchedulerMock.empty ++ RealtimeCreditApplicationBankSenderMock.empty

  private val anyQueueChanges = (
    CreditApplicationSchedulerQueueDaoMock.Upsert(anything) ||
      CreditApplicationSchedulerQueueDaoMock.Delete(anything)
  ).optional

  private val stubs =
    Clock.live ++ fakeTransactionSupportLayer ++ FeaturesTest.test ++ ZLayer.succeed(tokenDistributorConfig)

  private def creditApplicationControllerLayer[R <: zio.Has[_]](
      expectations: Expectation[R]
    )(implicit tag: zio.Tag[R]): ULayer[CreditApplicationController] =
    stubs ++ emptyMocks ++ expectations.toLayer >>> CreditApplicationController.live(domain)

  private def assertFirst[A, B](
      assertion: Assertion[A]): Assertion[(A, B)] =
    hasField("_1", _._1, assertion)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("CreditApplicationController")(
      suite("update")(
        testM("should insert new person profiles") {
          val creditApplicationId = generate[CreditApplicationId].sample.get
          val personProfileStub = generate[PersonProfileStub].sample.get

          val before = generate[AutoruCreditApplication].sample.get.copy(
            id = creditApplicationId,
            borrowerPersonProfile = None
          )
          val after = before.copy(borrowerPersonProfile = personProfileStub.some)

          val expectations =
            anyQueueChanges && (
              CreditApplicationDaoMock
                .Get(equalTo(CreditApplicationDao.GetById(creditApplicationId)), value(before.toProtoMessage.some)) ++
                CreditApplicationUpdaterMock.ApplyUpdateRequest(anything, valueF(_._1)) ++
                PersonProfileControllerMock
                  .Upsert(
                    anything,
                    value(PersonProfileController.UpsertResult.InsertedNewSnapshot(personProfileStub))
                  ) ++
                CreditApplicationDaoMock.Upsert(assertFirst(equalTo(after.forStore.toProtoMessage)))
            )

          val res: RIO[CreditApplicationController, CreditApplicationController.UpsertData] = ZIO
            .service[CreditApplicationController.Service]
            .flatMap(_.update(before.id)(_ => ZIO.succeed(after)))

          assertM(res)(anything)
            .provideLayer(creditApplicationControllerLayer(expectations))
        },
        testM("should replace existing person profiles") {
          val creditApplicationId = generate[CreditApplicationId].sample.get
          val oldPersonProfileId = generate[PersonProfileId].sample.get
          val oldPersonProfile = generate[PersonProfileImpl].sample.get.copy(id = oldPersonProfileId.some)
          val oldPersonProfileStub = generate[PersonProfileStub].sample.get.copy(id = oldPersonProfileId.some)
          val personProfileStub = generate[PersonProfileStub].sample.get

          val before = generate[AutoruCreditApplication].sample.get.copy(
            id = creditApplicationId,
            borrowerPersonProfile = oldPersonProfileStub.some
          )
          val after = before.copy(borrowerPersonProfile = personProfileStub.some)

          val expectations =
            anyQueueChanges && (
              CreditApplicationDaoMock
                .Get(equalTo(CreditApplicationDao.GetById(creditApplicationId)), value(before.toProtoMessage.some)) ++
                PersonProfileControllerMock
                  .Enrich(hasField("id", _.id, isSome(equalTo(oldPersonProfileId))), value(oldPersonProfile)) ++
                CreditApplicationUpdaterMock.ApplyUpdateRequest(anything, valueF(_._1)) ++
                PersonProfileControllerMock
                  .Upsert(
                    anything,
                    value(PersonProfileController.UpsertResult.InsertedNewSnapshot(personProfileStub))
                  ) ++
                CreditApplicationDaoMock.Upsert(assertFirst(equalTo(after.forStore.toProtoMessage))) ++
                PersonProfileControllerMock.Delete(equalTo(oldPersonProfileId))
            )

          val res: RIO[CreditApplicationController, CreditApplicationController.UpsertData] = ZIO
            .service[CreditApplicationController.Service]
            .flatMap(_.update(before.id)(_ => ZIO.succeed(after)))

          assertM(res)(anything)
            .provideLayer(creditApplicationControllerLayer(expectations))
        },
        testM("should not replace existing person profiles that haven't changed") {
          val creditApplicationId = generate[CreditApplicationId].sample.get
          val oldPersonProfileId = generate[PersonProfileId].sample.get
          val oldPersonProfile = generate[PersonProfileImpl].sample.get.copy(id = oldPersonProfileId.some)
          val oldPersonProfileStub = generate[PersonProfileStub].sample.get.copy(id = oldPersonProfileId.some)

          val before = generate[AutoruCreditApplication].sample.get.copy(
            id = creditApplicationId,
            borrowerPersonProfile = oldPersonProfileStub.some
          )
          val after = before

          val expectations =
            anyQueueChanges && (
              CreditApplicationDaoMock
                .Get(equalTo(CreditApplicationDao.GetById(creditApplicationId)), value(before.toProtoMessage.some)) ++
                PersonProfileControllerMock
                  .Enrich(hasField("id", _.id, isSome(equalTo(oldPersonProfileId))), value(oldPersonProfile)) ++
                CreditApplicationUpdaterMock.ApplyUpdateRequest(anything, valueF(_._1)) ++
                PersonProfileControllerMock
                  .Upsert(anything, value(PersonProfileController.UpsertResult.HasNotChanged(oldPersonProfileStub))) ++
                CreditApplicationDaoMock.Upsert(assertFirst(equalTo(after.forStore.toProtoMessage)))
            )

          val res: RIO[CreditApplicationController, CreditApplicationController.UpsertData] = ZIO
            .service[CreditApplicationController.Service]
            .flatMap(_.update(before.id)(_ => ZIO.succeed(after)))

          assertM(res)(anything)
            .provideLayer(creditApplicationControllerLayer(expectations))
        },
        testM("should clean up inserted person profiles on transaction failure") {
          val creditApplicationId = generate[CreditApplicationId].sample.get
          val personProfileId = generate[PersonProfileId].sample.get
          val personProfileStub = generate[PersonProfileStub].sample.get.copy(id = personProfileId.some)

          val before = generate[AutoruCreditApplication].sample.get.copy(
            id = creditApplicationId,
            borrowerPersonProfile = None
          )
          val after = before.copy(borrowerPersonProfile = personProfileStub.some)

          val exception = new RuntimeException("Boom")

          val expectations =
            anyQueueChanges && (
              CreditApplicationDaoMock
                .Get(equalTo(CreditApplicationDao.GetById(creditApplicationId)), value(before.toProtoMessage.some)) ++
                CreditApplicationUpdaterMock.ApplyUpdateRequest(anything, valueF(_._1)) ++
                PersonProfileControllerMock
                  .Upsert(
                    anything,
                    value(PersonProfileController.UpsertResult.InsertedNewSnapshot(personProfileStub))
                  ) ++
                CreditApplicationDaoMock.Upsert(anything, failure(TxError.abort(exception))) ++
                PersonProfileControllerMock.Delete(equalTo(personProfileId))
            )

          val res: RIO[CreditApplicationController, CreditApplicationController.UpsertData] = ZIO
            .service[CreditApplicationController.Service]
            .flatMap(_.update(before.id)(_ => ZIO.succeed(after)))

          assertM(res.run)(fails(equalTo(exception)))
            .provideLayer(creditApplicationControllerLayer(expectations))
        },
        testM("should not clean up untouched person profiles on transaction failure") {
          val creditApplicationId = generate[CreditApplicationId].sample.get
          val personProfileId = generate[PersonProfileId].sample.get
          val personProfile = generate[PersonProfileImpl].sample.get.copy(id = personProfileId.some)
          val personProfileStub = generate[PersonProfileStub].sample.get

          val before = generate[AutoruCreditApplication].sample.get.copy(
            id = creditApplicationId,
            borrowerPersonProfile = personProfileStub.some
          )
          val after = before.copy(borrowerPersonProfile = personProfileStub.some)

          val exception = new RuntimeException("Boom")

          val expectations =
            anyQueueChanges && (
              CreditApplicationDaoMock
                .Get(equalTo(CreditApplicationDao.GetById(creditApplicationId)), value(before.toProtoMessage.some)) ++
                PersonProfileControllerMock.Enrich(anything, value(personProfile)) ++
                CreditApplicationUpdaterMock.ApplyUpdateRequest(anything, valueF(_._1)) ++
                PersonProfileControllerMock
                  .Upsert(anything, value(PersonProfileController.UpsertResult.HasNotChanged(personProfileStub))) ++
                CreditApplicationDaoMock.Upsert(anything, failure(TxError.abort(exception)))
            )

          val res: RIO[CreditApplicationController, CreditApplicationController.UpsertData] = ZIO
            .service[CreditApplicationController.Service]
            .flatMap(_.update(before.id)(_ => ZIO.succeed(after)))

          assertM(res.run)(fails(equalTo(exception)))
            .provideLayer(creditApplicationControllerLayer(expectations))
        }
      )
    )
}
