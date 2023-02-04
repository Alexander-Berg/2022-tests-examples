package auto.dealers.dealer_calls_auction.storage.calls.postgres

import auto.dealers.dealer_calls_auction.storage.calls.postgres.PgOffersLastCallsDao.protoTimestampMeta
import auto.scalapb.InstantProtoConversions._
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import ru.auto.dealer_calls_auction.proto.relevant_calls_model.LastRelevantCallInfo
import zio.{Task, ZIO}
import zio.interop.catz._
import zio.stream.ZStream
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment

import java.time.Instant
import java.time.temporal.ChronoUnit

object PgOffersLastCallsDaoSpec extends DefaultRunnableSpec {

  // cause we store data in table with millis precision
  private val currentTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)

  private val timeFrom = currentTime.minus(10, ChronoUnit.DAYS)

  private val firstOffer =
    LastRelevantCallInfo(offerId = "first-offer", lastRelevantCall = Some(currentTime.asProtoTimestamp))

  private val secondOffer = LastRelevantCallInfo(
    offerId = "second-offer",
    lastRelevantCall = Some(currentTime.minus(2, ChronoUnit.DAYS).asProtoTimestamp)
  )

  private val offerForSecondUpdate =
    firstOffer.copy(lastRelevantCall = Some(currentTime.minusSeconds(1234).asProtoTimestamp))

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("PgOffersLastCallsDao")(
      testM("get changed offers should return only changed ones")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgOffersLastCallsDao(xa)
          result <- client.getChangedNewestCalls(timeFrom).runCollect
        } yield assert(result)(hasSameElements(Seq(firstOffer, secondOffer).map(_.copy(lastRelevantCall = None))))
      ),
      testM("update newest calls should change newest_last_call field")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgOffersLastCallsDao(xa)
          firstOffersUpdate <- client.updateNewestCalls(ZStream.apply(firstOffer, secondOffer))
          offersAfterFirstUpdate <- client.getChangedNewestCalls(timeFrom).runCollect

          secondOffersUpdate <- client.updateNewestCalls(ZStream.apply(offerForSecondUpdate))
          offersAfterSecondUpdate <- client.getChangedNewestCalls(timeFrom).runCollect
        } yield assert(firstOffersUpdate)(isUnit) &&
          assert(offersAfterFirstUpdate)(isEmpty) &&
          assert(secondOffersUpdate)(isUnit) &&
          assert(offersAfterSecondUpdate)(hasSameElements(Seq(offerForSecondUpdate)))
      ),
      testM("update newest calls should add new rows for previously not existed offers")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgOffersLastCallsDao(xa)
          firstOffersUpdate <- client.updateNewestCalls(ZStream.apply(firstOffer, secondOffer))
          offersAfterFirstUpdate <- client.getChangedNewestCalls(timeFrom).runCollect

          newOffer = LastRelevantCallInfo(offerId = "new-offer", lastRelevantCall = Some(currentTime.asProtoTimestamp))
          secondOffersUpdate <- client.updateNewestCalls(ZStream.apply(newOffer))
          offersAfterSecondUpdate <- client.getChangedNewestCalls(timeFrom).runCollect
        } yield assert(firstOffersUpdate)(isUnit) &&
          assert(offersAfterFirstUpdate)(isEmpty) &&
          assert(secondOffersUpdate)(isUnit) &&
          assert(offersAfterSecondUpdate)(hasSameElements(Seq(newOffer)))
      ),
      testM("update stored calls should change stored_last_call and newest_last_call fields")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgOffersLastCallsDao(xa)

          firstOffersUpdate <- client.updateStoredCalls(
            ZStream.apply(firstOffer, secondOffer).map(_.copy(lastRelevantCall = None))
          )
          offersAfterFirstUpdate <- client.getChangedNewestCalls(timeFrom).runCollect

          secondOffersUpdate <- client.updateStoredCalls(ZStream.apply(offerForSecondUpdate))
          offersAfterSecondUpdate <- client.getChangedNewestCalls(timeFrom).runCollect
        } yield assert(firstOffersUpdate)(isUnit) &&
          assert(offersAfterFirstUpdate)(isEmpty) &&
          assert(secondOffersUpdate)(isUnit) &&
          assert(offersAfterSecondUpdate)(hasSameElements(Seq(offerForSecondUpdate.copy(lastRelevantCall = None))))
      ),
      testM("update stored calls should use newest_last_call as null if there wasn't calls in period")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgOffersLastCallsDao(xa)

          firstOffersUpdate <- client.updateNewestCalls(ZStream.apply(firstOffer))

          resultSecondOffer = secondOffer.copy(lastRelevantCall = None)
          offersAfterNewestUpdate <- client.getChangedNewestCalls(timeFrom).runCollect

          secondOffersUpdate <- client.updateStoredCalls(ZStream.fromChunk(offersAfterNewestUpdate))
          offersAfterSecondUpdate <- client.getChangedNewestCalls(timeFrom).runCollect
        } yield assert(firstOffersUpdate)(isUnit) &&
          assert(offersAfterNewestUpdate)(hasSameElements(Seq(resultSecondOffer))) &&
          assert(secondOffersUpdate)(isUnit) &&
          assert(offersAfterSecondUpdate)(isEmpty)
      ),
      testM("update newest and stored calls shouldn't change state for requests without offers")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgOffersLastCallsDao(xa)
          offersResultCheck = hasSameElements(Seq(firstOffer, secondOffer).map(_.copy(lastRelevantCall = None)))

          currentOffersState <- client.getChangedNewestCalls(timeFrom).runCollect

          firstOffersChange <- client.updateNewestCalls(ZStream.empty)
          offersStateAfterFirstUpdate <- client.getChangedNewestCalls(timeFrom).runCollect

          secondOffersChange <- client.updateStoredCalls(ZStream.empty)
          offersStateAfterSecondUpdate <- client.getChangedNewestCalls(timeFrom).runCollect
        } yield assert(currentOffersState)(offersResultCheck) &&
          assert(firstOffersChange)(isUnit) &&
          assert(offersStateAfterFirstUpdate)(offersResultCheck) &&
          assert(secondOffersChange)(isUnit) &&
          assert(offersStateAfterSecondUpdate)(offersResultCheck)
      ),
      testM("erase not changed newest calls should set them as NULLs")(
        for {
          xa <- ZIO.service[Transactor[Task]]
          client = new PgOffersLastCallsDao(xa)
          offersWithoutNewestCalls = Seq(firstOffer, secondOffer).map(_.copy(lastRelevantCall = None))

          offersBeforeUpdate <- client.getChangedNewestCalls(timeFrom).runCollect

          firstOffersUpdate <- client.updateNewestCalls(ZStream.apply(firstOffer, secondOffer))
          offersAfterNewestUpdate <- client.getChangedNewestCalls(timeFrom).runCollect

          eraseOffersResult <- client.eraseNotChangedNewestCalls(timeFrom)
          offersAfterErasing <- client.getChangedNewestCalls(timeFrom).runCollect
        } yield assert(offersBeforeUpdate)(hasSameElements(offersWithoutNewestCalls)) &&
          assert(firstOffersUpdate)(isUnit) &&
          assert(offersAfterNewestUpdate)(isEmpty) &&
          assert(eraseOffersResult)(isUnit) &&
          assert(offersAfterErasing)(hasSameElements(offersWithoutNewestCalls))
      )
    ) @@
      beforeAll(
        ZIO.service[Transactor[Task]].flatMap(InitSchema("/schema.sql", _)).orDie
      ) @@
      before(
        ZIO
          .service[Transactor[Task]]
          .flatMap { xa =>
            val firstOfferInsertion = fr"${firstOffer.offerId}, ${firstOffer.lastRelevantCall}, NULL"
            val secondOfferInsertion = fr"${secondOffer.offerId}, ${secondOffer.lastRelevantCall}, NULL"

            for {
              _ <-
                sql"""INSERT INTO last_relevant_calls(offer_id, stored_last_call, newest_last_call) VALUES ($firstOfferInsertion)""".update.run
                  .transact(xa)
              _ <-
                sql"""INSERT INTO last_relevant_calls(offer_id, stored_last_call, newest_last_call) VALUES ($secondOfferInsertion)""".update.run
                  .transact(xa)
            } yield ()
          }
      ) @@
      after(
        ZIO.service[Transactor[Task]].flatMap(xa => sql"DELETE FROM last_relevant_calls".update.run.transact(xa))
      ) @@ sequential).provideCustomLayerShared(TestPostgresql.managedTransactor)
  }
}
