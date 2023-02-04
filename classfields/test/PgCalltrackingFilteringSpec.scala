package auto.dealers.calltracking.storage.test

import cats.data.NonEmptyList
import common.zio.doobie.ConnManager
import auto.common.pagination.RequestPagination
import auto.dealers.calltracking.model.{ClientId, Filters}
import auto.dealers.calltracking.model.testkit.CallGen
import ru.auto.calltracking.proto.filters_model.{CallResultGroup, Sorting}
import ru.auto.calltracking.proto.model.Call.CallResult
import auto.dealers.calltracking.storage.CalltrackingDao
import auto.dealers.calltracking.storage.postgresql.PgCalltrackingDao
import auto.dealers.calltracking.storage.testkit.TestPostgresql
import zio.test.Assertion.{equalTo, hasSize}
import zio.test.TestAspect._
import zio.test._

object PgCalltrackingFilteringSpec extends DefaultRunnableSpec {

  def spec = {
    suite("PgCalltrackingDao filtering")(
      testM("filter by tags") {
        checkNM(5)(CallGen.anyCall(callResult = Gen.const(CallResult.SUCCESS))) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call.withTags(Seq("a", "b", "c")).withoutTag("d"))
            pagination = RequestPagination(1, 10)
            sorting = Sorting(Sorting.SortingField.CALL_TIME, Sorting.SortingType.ASCENDING)
            filters1 = Filters(tags = Set("a"))
            found1 <- CalltrackingDao.getCalls(ClientId(call.clientId), filters1, pagination, sorting)
            filters2 = Filters(tags = Set("a", "c"))
            found2 <- CalltrackingDao.getCalls(ClientId(call.clientId), filters2, pagination, sorting)
            filters3 = Filters(tags = Set("a", "d"))
            found3 <- CalltrackingDao.getCalls(ClientId(call.clientId), filters3, pagination, sorting)
          } yield assert(found1)(hasSize(equalTo(1))) &&
            assert(found2)(hasSize(equalTo(1))) &&
            assert(found3)(hasSize(equalTo(0)))
        }
      },
      testM("count by tags") {
        checkNM(5)(CallGen.anyCall(callResult = Gen.const(CallResult.SUCCESS))) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call.withTags(Seq("a", "b", "c")).withoutTag("d"))
            filters1 = Filters(tags = Set("a"))
            found1 <- CalltrackingDao.countCalls(ClientId(call.clientId), filters1)
            filters2 = Filters(tags = Set("a", "c"))
            found2 <- CalltrackingDao.countCalls(ClientId(call.clientId), filters2)
            filters3 = Filters(tags = Set("a", "d"))
            found3 <- CalltrackingDao.countCalls(ClientId(call.clientId), filters3)
          } yield assert(found1)(equalTo(1)) &&
            assert(found2)(equalTo(1)) &&
            assert(found3)(equalTo(0))
        }
      },
      testM("filter by call result") {
        checkNM(5)(CallGen.anyCall) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            answeredFilter = Filters(callResultGroup = CallResultGroup.ANSWERED_GROUP)
            allResultFilter = Filters(callResultGroup = CallResultGroup.ALL_RESULT_GROUP)
            missedFilter = Filters(callResultGroup = CallResultGroup.MISSED_GROUP)

            _ <- CalltrackingDao.upsertCall(call.copy(callResult = CallResult.SUCCESS))

            onSuccessAnswered <- CalltrackingDao.countCalls(ClientId(call.clientId), answeredFilter)
            onSuccessAllResults <- CalltrackingDao.countCalls(ClientId(call.clientId), allResultFilter)
            onSuccessMissed <- CalltrackingDao.countCalls(ClientId(call.clientId), missedFilter)

            _ <- CalltrackingDao.upsertCall(call.copy(callResult = CallResult.BUSY_CALLEE))

            onBusyAnswered <- CalltrackingDao.countCalls(ClientId(call.clientId), answeredFilter)
            onBusyAllResults <- CalltrackingDao.countCalls(ClientId(call.clientId), allResultFilter)
            onBusyMissed <- CalltrackingDao.countCalls(ClientId(call.clientId), missedFilter)

            _ <- CalltrackingDao.upsertCall(call.copy(callResult = CallResult.BLOCKED))

            onBlockedAnswered <- CalltrackingDao.countCalls(ClientId(call.clientId), answeredFilter)
            onBlockedAllResults <- CalltrackingDao.countCalls(ClientId(call.clientId), allResultFilter)
            onBlockedMissed <- CalltrackingDao.countCalls(ClientId(call.clientId), missedFilter)

          } yield {
            val onSuccess = assert(onSuccessAnswered)(equalTo(1)) &&
              assert(onSuccessAllResults)(equalTo(1)) &&
              assert(onSuccessMissed)(equalTo(0))

            val onBusy = assert(onBusyAnswered)(equalTo(0)) &&
              assert(onBusyAllResults)(equalTo(1)) &&
              assert(onBusyMissed)(equalTo(1))

            val onBlocked = assert(onBlockedAnswered)(equalTo(0)) &&
              assert(onBlockedAllResults)(equalTo(0)) &&
              assert(onBlockedMissed)(equalTo(0))

            onSuccess && onBusy && onBlocked
          }
        }
      },
      testM("filter by platforms") {
        checkNM(5)(CallGen.anyCall(callResult = Gen.const(CallResult.SUCCESS))) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call.copy(platform = Some("autoru")))
            pagination = RequestPagination(1, 10)
            sorting = Sorting(Sorting.SortingField.CALL_TIME, Sorting.SortingType.ASCENDING)
            filtersAutoru = Filters(platforms = Filters.AnyOf(NonEmptyList.of("autoru")))
            found1 <- CalltrackingDao.getCalls(ClientId(call.clientId), filtersAutoru, pagination, sorting)
            filtersRandom = Filters(platforms = Filters.AnyOf(NonEmptyList.of("random data")))
            found2 <- CalltrackingDao.getCalls(ClientId(call.clientId), filtersRandom, pagination, sorting)
          } yield assert(found1)(hasSize(equalTo(1))) &&
            assert(found2)(hasSize(equalTo(0)))
        }
      }
    ) @@ after(PgCalltrackingDao.clean) @@ beforeAll(PgCalltrackingDao.initSchema.orDie) @@ sequential
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor >+> ConnManager.fromTransactor >>> PgCalltrackingDao.live
  )
}
