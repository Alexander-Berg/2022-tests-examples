package auto.dealers.calltracking.storage.test

import java.time.Instant
import common.collections.syntax._
import common.zio.doobie.ConnManager
import auto.common.pagination.RequestPagination
import ru.auto.api.cars_model.CarInfo
import ru.auto.api.api_offer_model.{Category, Section}
import auto.dealers.calltracking.model._
import auto.dealers.calltracking.model.testkit.{CallGen, TranscriptionGen}
import auto.dealers.calltracking.storage.{CallTranscriptionDao, CalltrackingDao}
import auto.dealers.calltracking.storage.postgresql.{PgCallTranscriptionDao, PgCalltrackingDao}
import auto.dealers.calltracking.storage.testkit.{TestCalltrackingDao, TestPostgresql}
import ru.auto.calltracking.proto.model.Call.CallResult
import ru.auto.calltracking.proto.filters_model.Sorting
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import cats.data.NonEmptySet
import cats.implicits._
import java.time.temporal.TemporalUnit
import java.time.temporal.ChronoUnit
import cats.data.NonEmptyList

object PgCalltrackingDaoSpec extends DefaultRunnableSpec {

  def spec = {
    suite("PgCalltrackingDao")(
      testM("insert single call") {
        val instant = Instant.EPOCH
        checkNM(5)(CallGen.anyCall) { call =>
          val exec = for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call)
            inserted <- CalltrackingDao.getCallByExternalId(call.externalId)
          } yield inserted
          if (call.callResult.isBlocked || call.callResult.isCallbackFailed) {
            // this line checks only empty fetching - insertion is still not fixed
            assertM(exec.run)(fails(isSubtype[CalltrackingDao.CallNotFound](anything)))
          } else {
            exec.map { inserted =>
              assert(inserted.id)(isGreaterThan(0L)) &&
              assert(inserted.copy(id = 0, created = instant, transcriptionAvailable = call.transcriptionAvailable))(
                equalTo(call.copy(id = 0, created = instant))
              ) &&
              assert(inserted.platform)(equalTo(call.platform))
            }
          }
        }
      },
      testM("insert many calls") {
        checkNM(5)(Gen.listOf1(CallGen.anyCall(callResult = Gen.const(CallResult.SUCCESS)))) { calls =>
          val exec = for {
            _ <- PgCalltrackingDao.clean
            map = calls.toMapWithKey(_.externalId)
            _ <- CalltrackingDao.upsertCalls(calls)
            inserted <- ZIO.foreach(calls: List[Call])(c => CalltrackingDao.getCallByExternalId(c.externalId))
          } yield (inserted, map)

          exec.map { case (inserted, map) =>
            assert(inserted)(forall(hasField("id", _.id, isGreaterThan(0L)))) &&
            assert(inserted.head)(hasField("tags", _.tags, equalTo(map(inserted.head.externalId).tags)))
          }
        }
      },
      testM("get calls with transcriptions") {
        checkNM(5)(
          CallGen.anyCall(
            callResult = Gen.const(CallResult.SUCCESS),
            clientId = Gen.const(20101L),
            transcriptionAvailable = Gen.const(true)
          ),
          CallGen.anyCall(
            callResult = Gen.const(CallResult.SUCCESS),
            clientId = Gen.const(20101L),
            transcriptionAvailable = Gen.const(false)
          ),
          TranscriptionGen.oneTranscription
        ) { (callWithTranscription, callWithoutTranscription, transcription) =>
          val exec = for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCall(callWithTranscription)
            _ <- CalltrackingDao.insertCall(callWithoutTranscription)
            _ <- CallTranscriptionDao.insert(
              callWithTranscription.externalId,
              transcription.phrases.head.text,
              transcription.phrases.tail.head.text,
              transcription
            )
            calls <- CalltrackingDao.getCalls(
              ClientId(20101L),
              Filters(),
              RequestPagination.of(1, 2),
              Sorting.defaultInstance
            )
          } yield calls

          exec.map { calls =>
            assert(calls)(hasSize(equalTo(2))) &&
            assert(calls.filter(!_.transcriptionAvailable).head)(
              hasField("transcriptionAvailable", _.transcriptionAvailable, equalTo(false))
            ) &&
            assert(calls.filter(_.transcriptionAvailable).head)(
              hasField("transcriptionAvailable", _.transcriptionAvailable, equalTo(true))
            )
          }
        }
      },
      testM("get stats") {
        checkNM(5)(CallGen.anyCall(callResult = Gen.const(CallResult.SUCCESS))) { call =>
          for {
            _ <- PgCalltrackingDao.clean
            inMemory <- TestCalltrackingDao.make
            _ <- CalltrackingDao.insertCall(call)
            _ <- inMemory.insertCalls(Seq(call))
            actualDaily <- CalltrackingDao.getDailyCallStats(ClientId(call.clientId), Filters())
            expectedDaily <- inMemory.getDailyCallStats(ClientId(call.clientId), Filters())
          } yield assert(actualDaily)(equalTo(expectedDaily))
        }
      },
      testM("don't get blocked calls") {
        checkNM(5)(CallGen.anyCall(callResult = Gen.const(CallResult.BLOCKED))) { call =>
          val exec = for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call)
            _ <- CalltrackingDao.getCall(ClientId(call.clientId), CallId(call.id))
          } yield ()

          assertM(exec.run)(fails(isSubtype[CalltrackingDao.CallNotFound](anything)))
        }
      },
      testM("don't get failed calls") {
        checkNM(5)(CallGen.anyCall(callResult = Gen.const(CallResult.CALLBACK_FAILED))) { call =>
          val exec = for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.upsertCall(call)
            _ <- CalltrackingDao.getCall(ClientId(call.clientId), CallId(call.id))
          } yield ()

          assertM(exec.run)(fails(isSubtype[CalltrackingDao.CallNotFound](anything)))
        }
      },
      testM("filter blocked or failed calls") {
        val clientId = 100
        checkNM(5)(Gen.listOfN(3)(CallGen.anyCall(clientId = Gen.const(clientId)))) { calls =>
          val fixedCalls = calls
            .updated(0, calls.head.copy(callResult = CallResult.SUCCESS))
            .updated(1, calls(1).copy(callResult = CallResult.CALLBACK_FAILED))
            .updated(2, calls(2).copy(callResult = CallResult.BLOCKED))
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCalls(fixedCalls)
            pagination = RequestPagination.of(1, 3)
            sorting = Sorting.defaultInstance
            inserted <- CalltrackingDao.getCalls(ClientId(clientId), Filters.empty, pagination, sorting)
            expected = fixedCalls.head.copy(id = inserted.head.id)
          } yield assert(inserted)(hasSize(equalTo(1))) && assert(inserted.head)(equalTo(expected))
        }
      },
      testM("don't count blocked or failed calls") {
        val clientId = 100
        checkNM(5)(Gen.listOfN(50)(CallGen.anyCall(clientId = Gen.const(clientId)))) { calls =>
          val goodCalls = calls.filterNot(c => c.callResult.isBlocked || c.callResult.isCallbackFailed)
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCalls(calls)
            counted <- CalltrackingDao.countCalls(ClientId(clientId), Filters.empty)
          } yield assert(counted)(equalTo(goodCalls.length))
        }
      },
      testM("batch counters") {
        val clientId = 100
        val offer =
          Call.OfferInfo("test-offer", Section.NEW, Call.CarInfo("BMW", "X5", 0, "SEDAN", "MANUAL"), 2020, 300000, None)
        checkNM(5)(Gen.listOfN(50)(CallGen.anyCall(clientId = Gen.const(clientId)))) { calls =>
          val allCalls =
            calls.map(_.copy(offer = Some(offer), category = Some(Category.CARS), section = Some(Section.NEW)))
          val goodCalls = allCalls.filterNot { c =>
            c.callResult.isBlocked || c.callResult.isCallbackFailed
          }
          val expected = Seq(
            OfferId("test-offer") -> goodCalls.length,
            OfferId("no-calls") -> 0L
          )
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCalls(allCalls)
            counted <- CalltrackingDao.countCallsBatch(
              ClientId(clientId),
              NonEmptySet.of(OfferId("test-offer"), OfferId("no-calls"))
            )
          } yield assert(counted)(hasSameElements(expected))
        }
      },
      testM("multiple client calls filtering") {
        val clientId1 = 100L
        val clientId2 = 101L
        checkNM(5)(Gen.listOfN(50)(CallGen.anyCall(clientId = Gen.elements(clientId1, clientId2)))) { calls =>
          val goodCalls = calls.filterNot(c => c.callResult.isBlocked || c.callResult.isCallbackFailed)
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCalls(calls)
            pagination = RequestPagination.of(1, Int.MaxValue)
            sorting = Sorting.defaultInstance
            filtered <- CalltrackingDao.getCalls(
              NonEmptySet.of(ClientId(clientId1), ClientId(clientId2)),
              Filters.empty,
              pagination,
              sorting,
              None
            )
            res = filtered.map(_.externalId)
          } yield assert(res)(hasSameElements(goodCalls.map(_.externalId)))
        }
      },
      testM("multiple client calls filtering count") {
        val clientId1 = 100L
        val clientId2 = 101L
        checkNM(5)(Gen.listOfN(50)(CallGen.anyCall(clientId = Gen.elements(clientId1, clientId2)))) { calls =>
          val goodCalls = calls.filterNot(c => c.callResult.isBlocked || c.callResult.isCallbackFailed)
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCalls(calls)
            counted <- CalltrackingDao.countCalls(
              NonEmptySet.of(ClientId(clientId1), ClientId(clientId2)),
              Filters.empty,
              None
            )
          } yield assert(counted)(equalTo(goodCalls.length))
        }
      },
      testM("per-offer daily call count") {
        val offerId1 = OfferId("offer-1")
        val offerId2 = OfferId("offer-2")
        val offer1 = CallGen.anyOfferInfo.map(_.copy(id = offerId1.toString()).some)
        val offer2 = CallGen.anyOfferInfo.map(_.copy(id = offerId2.toString()).some)
        val from = Instant.parse("2021-01-02T00:00:00Z")
        val to = Instant.parse("2021-01-03T00:00:00Z")
        val minCallTime = Instant.parse("2021-01-01T00:00:00Z")
        val maxCallTime = Instant.parse("2021-01-04T00:00:00Z")

        def callPred(call: Call, id: OfferId): Boolean = {
          val relevant = call.isRelevant.getOrElse(false)
          val properId = call.offer.map(_.id == id.toString()).getOrElse(false)
          val isBeforeOfEqFrom = from.isBefore(call.created) || from == call.created

          relevant && properId && isBeforeOfEqFrom && to.isAfter(call.created)
        }

        checkNM(5)(
          Gen.listOfN(50)(
            CallGen.anyCall(offerInfo = Gen.oneOf(offer1, offer2), created = Gen.instant(minCallTime, maxCallTime))
          )
        ) { calls =>
          val calls1 = calls.filter(c => callPred(c, offerId1))
          val calls2 = calls.filter(c => callPred(c, offerId2))
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCalls(calls)
            counted <- CalltrackingDao.countRelevantCallsPerOffer(
              NonEmptySet.of(offerId1, offerId2),
              from,
              to
            )
          } yield assertTrue(counted(offerId1) == calls1.length && counted(offerId2) == calls2.length)
        }
      },
      testM("per-client last calls") {
        val from = Instant.parse("2021-01-02T00:00:00Z")
        val to = Instant.parse("2021-01-03T00:00:00Z")
        val minCallTime = Instant.parse("2021-01-01T00:00:00Z")
        val maxCallTime = Instant.parse("2021-01-04T00:00:00Z")

        def callPred(call: Call): Boolean = {
          lazy val relevant = call.isRelevant.getOrElse(false)
          lazy val hasOfferId = call.offer.isDefined
          lazy val isBeforeOfEqFrom = from.isBefore(call.created) || from == call.created

          relevant && hasOfferId && isBeforeOfEqFrom && to.isAfter(call.created)
        }

        checkNM(5)(
          Gen.listOfN(50)(
            CallGen.anyCall(
              created = Gen.instant(minCallTime, maxCallTime).map(_.truncatedTo(ChronoUnit.MICROS))
            )
          )
        ) { calls =>
          val expected = calls
            .filter(callPred)
            .map { call =>
              call.offer.get.id -> call.created
            }
            .groupBy(_._1)
            .map { case (offerId, ts) =>
              OfferId(offerId) -> ts.map(_._2).max
            }
            .toList
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCalls(calls)
            lastCalls <- CalltrackingDao.lastRelevantCallPerOffer(from, to).runCollect
          } yield assert(lastCalls)(hasSameElements(expected))
        }
      },
      testM("per-client last calls (with category and section)") {
        val from = Instant.parse("2021-01-02T00:00:00Z")
        val to = Instant.parse("2021-01-03T00:00:00Z")
        val minCallTime = Instant.parse("2021-01-01T00:00:00Z")
        val maxCallTime = Instant.parse("2021-01-04T00:00:00Z")

        def callPred(call: Call): Boolean = {
          lazy val relevant = call.isRelevant.getOrElse(false)
          lazy val hasOfferId = call.offer.isDefined
          lazy val isBeforeOfEqFrom = from.isBefore(call.created) || from == call.created
          lazy val properCategory = call.category.exists(_ == Category.CARS)
          lazy val properSection = call.section.exists(_ == Section.NEW)

          relevant && hasOfferId && properCategory && properSection && isBeforeOfEqFrom && to.isAfter(call.created)
        }

        checkNM(5)(
          Gen.listOfN(50)(
            CallGen.anyCall(
              created = Gen.instant(minCallTime, maxCallTime).map(_.truncatedTo(ChronoUnit.MICROS))
            )
          )
        ) { calls =>
          val expected = calls
            .filter(callPred)
            .map { call =>
              call.offer.get.id -> call.created
            }
            .groupBy(_._1)
            .map { case (offerId, ts) =>
              OfferId(offerId) -> ts.map(_._2).max
            }
            .toList
          for {
            _ <- PgCalltrackingDao.clean
            _ <- CalltrackingDao.insertCalls(calls)
            lastCalls <- CalltrackingDao
              .lastRelevantCallPerOffer(
                from,
                to,
                Filters.AnyOf(NonEmptyList.one(Category.CARS)),
                Filters.AnyOf(NonEmptyList.one(Section.NEW))
              )
              .runCollect
          } yield assert(lastCalls)(hasSameElements(expected))
        }
      }
    ) @@ after(PgCalltrackingDao.clean) @@ beforeAll(PgCalltrackingDao.initSchema.orDie) @@ sequential
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor >+> ConnManager.fromTransactor >+> PgCalltrackingDao.live >+> PgCallTranscriptionDao.live
  )
}
