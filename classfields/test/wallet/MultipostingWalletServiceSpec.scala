package auto.dealers.multiposting.logic.test.wallet

import java.time.OffsetDateTime
import cats.syntax.option._
import common.scalapb.ScalaProtobuf
import common.zio.clock.MoscowClock
import auto.dealers.multiposting.logic.auth.CredentialsService
import auto.dealers.multiposting.logic.wallet.MultipostingWalletService
import auto.dealers.multiposting.logic.wallet.MultipostingWalletService.RichAvitoDayExpenses
import auto.dealers.multiposting.logic.avito.AvitoUserInfoService
import auto.dealers.multiposting.clients.avito.AvitoClient
import auto.dealers.multiposting.clients.avito.model._
import ru.auto.multiposting.filter_model.{AvitoWalletOperationFilter, PeriodFilter}
import auto.dealers.multiposting.model
import auto.dealers.multiposting.model.{AvitoDayExpenses, AvitoWalletOperationMeta, ClientId, OperationId, PaymentType}
import auto.dealers.multiposting.storage.AvitoWalletOperationDao.AvitoWalletOperationDao
import auto.dealers.multiposting.storage.AvitoWalletOperationMetaDao.AvitoWalletOperationMetaDao
import common.zio.logging.Logging
import common.zio.sttp.model._
import auto.dealers.multiposting.storage.testkit.{AvitoWalletOperationDaoMock, AvitoWalletOperationMetaDaoMock}
import common.zio.features.Features
import ru.auto.multiposting.wallet_model.AvitoWalletDailyOperation
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import zio._
import zio.magic._
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object MultipostingWalletServiceSpec extends DefaultRunnableSpec {

  private val SplitPlacementPaymentsPerDayFeatureName = "split_placement_payments_per_day_feature"

  override def spec: ZSpec[environment.TestEnvironment, Any] = {
    suite("MultipostingWalletService.live")(
      testM(
        """should fetch list of AvitoDayExpenses from underlying DAO and return same-size list of AvitoWalletDailyOperation"""
      ) {

        checkM(Gen.listOf(Gen.fromRandomSample(avitoDayExpensesRandomSample))) { givenAvitoDayExpensesList =>
          val givenClientId = 100L

          // на базе обрезается по дату и группируется по этой дате
          val distinctGivenExpensesList = givenAvitoDayExpensesList.distinctBy(_.day.toLocalDate)

          val givenFrom = OffsetDateTime.now(MoscowClock.timeZone)
          val givenTo = OffsetDateTime.now(MoscowClock.timeZone)
          val givenFilter = createAvitoWalletOperationFilter(givenFrom, givenTo)
          val mock = AvitoWalletOperationDaoMock
            .GetPerDayExpenses(
              equalTo((givenClientId, givenFrom, givenTo, List.empty[String])),
              value(distinctGivenExpensesList)
            )
            .toLayer ++ AvitoWalletOperationMetaDaoMock.empty

          assertM(
            MultipostingWalletService.getAvitoWalletDailyOperationList(givenClientId, givenFilter)
          )(hasSize(equalTo(distinctGivenExpensesList.size)))
            .provideSomeLayer(mock >>> createEnvironment)
        }
      },
      test("should unzip meta operation on few daily expenses") {
        val givenClientId = ClientId(100L)

        val avitoWalletOperationMeta = AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("123"),
          activeFrom = OffsetDateTime.parse("2021-11-16T12:07:48.000000+00:00"),
          activeTo = OffsetDateTime.parse("2021-11-20T12:07:48.000000+00:00"),
          clientId = givenClientId,
          factAmountSumKopecks = 4003,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        )

        val unzippedOperations = List(
          getDayExpense(day = OffsetDateTime.parse("2021-11-16T00:00:00.000000+00:00"), placementSumKopecks = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-17T00:00:00.000000+00:00"), placementSumKopecks = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-18T00:00:00.000000+00:00"), placementSumKopecks = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-19T00:00:00.000000+00:00"), placementSumKopecks = 1003)
        )

        assert(MultipostingWalletService.unzipMapToDayExpense(List(avitoWalletOperationMeta)))(
          hasSameElements(unzippedOperations)
        )
      },
      testM("should return list of expenses with toggled feature on true") {
        val givenClientId = ClientId(100L)

        val givenFrom = OffsetDateTime.parse("2021-11-14T12:07:48.000000+03:00")
        val givenTo = OffsetDateTime.parse("2021-11-18T12:07:48.000000+03:00")
        val givenFilter = createAvitoWalletOperationFilter(givenFrom, givenTo)

        val avitoWalletOperationMeta = AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("123"),
          activeFrom = OffsetDateTime.parse("2021-11-16T12:07:48.000000+00:00"),
          activeTo = OffsetDateTime.parse("2021-11-20T12:07:48.000000+00:00"),
          clientId = givenClientId,
          factAmountSumKopecks = 4003,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        )

        val givenAvitoDayExpensesList = List(
          getDayExpense(day = OffsetDateTime.parse("2021-11-16T00:00:00.000000+00:00"), placementSumKopecks = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-17T00:00:00.000000+00:00"), placementSumKopecks = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-17T00:00:00.000000+00:00"), placementSumKopecks = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-18T00:00:00.000000+00:00"), placementSumKopecks = 1200)
        )

        val expectedResult = List(
          AvitoWalletDailyOperation(
            date = "2021-11-16",
            placement = 20
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-17",
            placement = 30
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-18",
            placement = 22
          )
        )

        val mock = {
          AvitoWalletOperationMetaDaoMock
            .ListInPeriodByClient(
              equalTo((givenFrom, givenTo, givenClientId)),
              value(List(avitoWalletOperationMeta))
            ) ++
            AvitoWalletOperationDaoMock
              .GetPerDayExpenses(
                equalTo(
                  (
                    givenClientId.value,
                    givenFrom,
                    givenTo,
                    List(avitoWalletOperationMeta.avitoWalletOperationId.value)
                  )
                ),
                value(givenAvitoDayExpensesList)
              )
        }.toLayer

        assertM(
          Features.updateFeature(SplitPlacementPaymentsPerDayFeatureName, value = true) *>
            MultipostingWalletService.getAvitoWalletDailyOperationList(givenClientId.value, givenFilter)
        )(hasSameElements(expectedResult))
          .provideLayer(mock >>> createEnvironment)

      },
      testM("should return list of expenses with toggled feature on true with merging") {
        val givenClientId = ClientId(100L)

        val givenFrom = OffsetDateTime.parse("2021-11-14T12:07:48.000000+03:00")
        val givenTo = OffsetDateTime.parse("2021-11-18T12:07:48.000000+03:00")
        val givenFilter = createAvitoWalletOperationFilter(givenFrom, givenTo)

        val avitoWalletOperationMeta = AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("123"),
          activeFrom = OffsetDateTime.parse("2021-11-16T12:07:48.000000+00:00"),
          activeTo = OffsetDateTime.parse("2021-11-20T12:07:48.000000+00:00"),
          clientId = givenClientId,
          factAmountSumKopecks = 4003,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        )

        val givenAvitoDayExpensesList = List(
          getDayExpense(day = OffsetDateTime.parse("2021-11-16T00:00:00.000000+00:00"), placementSumKopecks = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-17T00:00:00.000000+00:00"), placementSumKopecks = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-17T00:00:00.000000+00:00"), otherExpensesSum = 1000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-18T00:00:00.000000+00:00"), vasSum = 1000)
        )

        val expectedResult = List(
          AvitoWalletDailyOperation(
            date = "2021-11-16",
            placement = 20
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-17",
            placement = 20,
            other = 10
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-18",
            placement = 10,
            vas = 10
          )
        )

        val mock = {
          AvitoWalletOperationMetaDaoMock
            .ListInPeriodByClient(
              equalTo((givenFrom, givenTo, givenClientId)),
              value(List(avitoWalletOperationMeta))
            ) ++
            AvitoWalletOperationDaoMock
              .GetPerDayExpenses(
                equalTo(
                  (
                    givenClientId.value,
                    givenFrom,
                    givenTo,
                    List(avitoWalletOperationMeta.avitoWalletOperationId.value)
                  )
                ),
                value(givenAvitoDayExpensesList)
              )
        }.toLayer

        assertM(
          Features.updateFeature(SplitPlacementPaymentsPerDayFeatureName, value = true) *>
            MultipostingWalletService.getAvitoWalletDailyOperationList(givenClientId.value, givenFilter)
        )(hasSameElements(expectedResult))
          .provideLayer(mock >>> createEnvironment)

      },
      testM("should return list of expenses with toggled feature on true with merging actual") {
        val givenClientId = ClientId(100L)

        val givenFrom = OffsetDateTime.parse("2021-11-14T12:07:48.000000+03:00")
        val givenTo = OffsetDateTime.parse("2021-11-18T12:07:48.000000+03:00")
        val givenFilter = createAvitoWalletOperationFilter(givenFrom, givenTo)

        val avitoWalletOperationMeta = AvitoWalletOperationMeta(
          avitoWalletOperationId = OperationId("003ec7ae674d13d762e52983a1af4da6cf7b213b"),
          activeFrom = OffsetDateTime.parse("2021-11-16T12:07:48.000000+00:00"),
          activeTo = OffsetDateTime.parse("2021-12-16T12:07:48.000000+00:00"),
          clientId = givenClientId,
          factAmountSumKopecks = 300000000,
          paymentType = PaymentType.Tariff,
          refundAvitoWalletOperationId = None
        )

        val givenAvitoDayExpensesList = List(
          getDayExpense(
            day = OffsetDateTime.parse("2021-11-15T00:00:00.000000+00:00"),
            placementSumKopecks = 300000000
          ),
          getDayExpense(
            day = OffsetDateTime.parse("2021-11-16T21:00:00.000000+00:00"),
            vasSum = 15900
          )
        )

        val expectedResult = List(
          AvitoWalletDailyOperation(
            date = "2021-11-15",
            placement = 3000000
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-16",
            placement = 100000
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-17",
            placement = 100000,
            vas = 159
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-18",
            placement = 100000
          )
        )

        val mock = {
          AvitoWalletOperationMetaDaoMock
            .ListInPeriodByClient(
              equalTo((givenFrom, givenTo, givenClientId)),
              value(List(avitoWalletOperationMeta))
            ) ++
            AvitoWalletOperationDaoMock
              .GetPerDayExpenses(
                equalTo(
                  (
                    givenClientId.value,
                    givenFrom,
                    givenTo,
                    List(avitoWalletOperationMeta.avitoWalletOperationId.value)
                  )
                ),
                value(givenAvitoDayExpensesList)
              )
        }.toLayer

        assertM(
          Features.updateFeature(SplitPlacementPaymentsPerDayFeatureName, value = true) *>
            MultipostingWalletService.getAvitoWalletDailyOperationList(givenClientId.value, givenFilter)
        )(hasSameElements(expectedResult))
          .provideLayer(mock >>> createEnvironment)
      },
      test("should group operations by day") {

        val givenAvitoDayExpensesList = List(
          getDayExpense(
            day = OffsetDateTime.parse("2021-11-17T00:00:00.000000+00:00"),
            vasSum = 15900
          )
        )

        val unzippedOperations = List(
          getDayExpense(day = OffsetDateTime.parse("2021-11-15T00:00:00.000000+00:00"), placementSumKopecks = 100000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-16T00:00:00.000000+00:00"), placementSumKopecks = 100000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-17T00:00:00.000000+00:00"), placementSumKopecks = 100000),
          getDayExpense(day = OffsetDateTime.parse("2021-11-18T00:00:00.000000+00:00"), placementSumKopecks = 100003)
        )

        val expectedResult = List(
          AvitoWalletDailyOperation(
            date = "2021-11-15",
            placement = 1000
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-16",
            placement = 1000
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-17",
            placement = 1000,
            vas = 159
          ),
          AvitoWalletDailyOperation(
            date = "2021-11-18",
            placement = 1001
          )
        )

        val dailyOperations = (givenAvitoDayExpensesList ::: unzippedOperations).map(_.toAvitoWallerDailyOperation)
        val res = MultipostingWalletService.groupedAndSortByDay(dailyOperations)

        assert(res)(
          hasSameElements(expectedResult)
        )

      }
    ) @@
      sequential
  }

  private def createEnvironment =
    ZLayer.fromSomeMagic[
      AvitoWalletOperationMetaDao with AvitoWalletOperationDao,
      Features.Features with MultipostingWalletService.MultipostingWalletService
    ](
      Features.liveInMemory,
      Logging.live,
      avitoClientEmpty,
      credentialsEmpty,
      Clock.live,
      AvitoUserInfoService.live,
      MultipostingWalletService.live
    )

  private val credentialsEmpty = ZLayer.succeed(
    new CredentialsService.Service {

      override def getAvitoCredentials(
          client: model.ClientId): IO[CredentialsService.CredentialsException, (model.AvitoUserId, Token)] =
        ???
    }
  )

  private val avitoClientEmpty = ZLayer.succeed(new AvitoClient.Service {

    override def authorize(
        avitoClientId: model.AvitoClientId,
        secret: model.AvitoSecret): IO[SttpError, Either[AuthorizationError, Token]] = ???

    override def getBalance(
        avitoUserId: model.AvitoUserId,
        token: Token): IO[SttpError, Balance] = ???

    override def buyVas(
        avitoUserId: model.AvitoUserId,
        avitoOfferId: model.AvitoOfferId,
        token: Token,
        vasId: Vas): IO[SttpError, VasApplyResult] = ???

    override def buyVasPackage(
        avitoUserId: model.AvitoUserId,
        avitoOfferId: model.AvitoOfferId,
        token: Token,
        packageId: VasPackage): IO[SttpError, VasPackageApplyResult] = ???

    override def getTariffInfo(token: Token): IO[SttpError, TariffInfo] = ???
  })

  private def getDayExpense(
      day: OffsetDateTime,
      placementSumKopecks: Long = 0,
      vasSum: Long = 0,
      otherExpensesSum: Long = 0): AvitoDayExpenses =
    AvitoDayExpenses(
      day = day,
      placementSum = placementSumKopecks,
      vasSum = vasSum,
      otherExpensesSum = otherExpensesSum
    )

  private def createAvitoWalletOperationFilter(givenFrom: OffsetDateTime, givenTo: OffsetDateTime) =
    AvitoWalletOperationFilter(period =
      PeriodFilter(ScalaProtobuf.toTimestamp(givenFrom).some, ScalaProtobuf.toTimestamp(givenTo).some).some
    )

  private def avitoDayExpensesRandomSample(rnd: Random.Service) =
    for {
      placementSum <- rnd.nextLong
      vasSum <- rnd.nextLong
      otherExpensesSum <- rnd.nextLong
    } yield Sample.noShrink(
      AvitoDayExpenses(
        OffsetDateTime.now(MoscowClock.timeZone),
        BigDecimal(placementSum * 100),
        BigDecimal(vasSum * 100),
        BigDecimal(otherExpensesSum * 100)
      )
    )
}
