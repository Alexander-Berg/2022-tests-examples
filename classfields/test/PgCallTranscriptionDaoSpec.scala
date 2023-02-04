package auto.dealers.calltracking.storage.test

import doobie.implicits._
import doobie.util.transactor.Transactor

import java.time.Instant
import zio._
import zio.interop.catz._
import zio.test.TestAspect._
import zio.test._
import common.zio.doobie.ConnManager
import auto.dealers.calltracking.storage.CalltrackingDao
import auto.dealers.calltracking.storage.postgresql.PgCalltrackingDao
import auto.dealers.calltracking.storage.testkit.TestPostgresql
import auto.dealers.calltracking.storage.postgresql.PgCallTranscriptionDao
import auto.dealers.calltracking.model.{CallId, ClientId, ExternalId}
import ru.auto.calltracking.proto.model.CallTranscription
import ru.auto.calltracking.proto.model.Call.CallResult
import auto.dealers.calltracking.storage.CallTranscriptionDao
import auto.dealers.calltracking.model.Call

import scala.collection.immutable.HashSet
import scala.concurrent.duration._
import ru.auto.calltracking.proto.filters_model.FullTextFilter

object PgCallTranscriptionDaoSpec extends DefaultRunnableSpec {

  private val call =
    Call(
      0,
      ExternalId.TeleponyId("1234-5678"),
      1,
      Instant.parse("2052-12-10T18:28:28.732Z"),
      Instant.parse("2046-02-20T19:23:12.352Z"),
      CallResult.SUCCESS,
      false,
      None,
      true,
      true,
      HashSet(),
      None,
      1470947849.seconds,
      545611468.seconds,
      None,
      "+74445811642",
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )

  private val sourcePhrase =
    "Продам Хонда Аккорд, цвет белый, не битая, полный электропакет, климат-контроль."

  private val targetPhrase =
    "Какое количество владельцев, цена?"

  private val transcription =
    CallTranscription(
      Seq(
        CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, sourcePhrase),
        CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, targetPhrase)
      )
    )

  def spec = {
    val suit = suite("PgCallTranscriptionDao")(
      insertTranscriptionTest,
      wrongClientTest,
      wrongIdTest,
      insertCallAfterTranscriptTest,
      highlightsENTest,
      highlightsRUTest
    ) @@
      after(cleanup) @@
      beforeAll(PgCalltrackingDao.initSchema.orDie) @@
      sequential

    suit.provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> ConnManager.fromTransactor >+> PgCalltrackingDao.live >+> PgCallTranscriptionDao.live
    )
  }

  val insertTranscriptionTest =
    testM("insert single transcription")(
      for {
        (id, clientId) <- insertCall(call)
        _ <- CallTranscriptionDao.insert(call.externalId, sourcePhrase, targetPhrase, transcription)
        result <- CallTranscriptionDao.get(clientId, id)
      } yield assertTrue(result.contains(transcription))
    )

  val wrongClientTest =
    testM("no transcription for wrong client id")(
      for {
        (id, _) <- insertCall(call)
        _ <- CallTranscriptionDao.insert(call.externalId, sourcePhrase, targetPhrase, transcription)
        result <- CallTranscriptionDao.get(ClientId(99999L), id)
      } yield assertTrue(result.isEmpty)
    )

  val wrongIdTest =
    testM("no transcription for wrong call id")(
      for {
        (_, clientId) <- insertCall(call)
        _ <- CallTranscriptionDao.insert(call.externalId, sourcePhrase, targetPhrase, transcription)
        result <- CallTranscriptionDao.get(clientId, CallId(99999L))
      } yield assertTrue(result.isEmpty)
    )

  val insertCallAfterTranscriptTest =
    testM("call can be inserted after transcription")(
      for {
        _ <- CallTranscriptionDao.insert(call.externalId, sourcePhrase, targetPhrase, transcription)
        (id, clientId) <- insertCall(call)
        result <- CallTranscriptionDao.get(clientId, id)
      } yield assertTrue(result.contains(transcription))
    )

  val highlightsENTest =
    testM("highlights (EN)") {
      val query = FullTextFilter.Query.WebsearchQuery("your deserving OR to have wounds")

      val source =
        """You shall not be The grave of your deserving;
           Rome must know The value of her own: 'twere a concealment
           Worse than a theft, no less than a traducement,
           To hide your doings"""

      val target =
        """I have some wounds upon me, and they smart
           To hear themselves remember'd"""
      val transcription =
        CallTranscription(
          Seq(
            CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, source),
            CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, target)
          )
        )

      val expectedSource =
        """You shall not be The grave of your <b>deserving</b>;
           Rome must know The value of her own: 'twere a concealment
           Worse than a theft, no less than a traducement,
           To hide your doings"""
      val expectedTarget =
        """I have some <b>wounds</b> upon me, and they smart
           To hear themselves remember'd"""
      val expected =
        CallTranscription(
          Seq(
            CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, expectedSource),
            CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, expectedTarget)
          )
        )
      for {
        (id, clientId) <- insertCall(call)
        _ <- CallTranscriptionDao.insert(call.externalId, source, target, transcription)
        result <- CallTranscriptionDao.getWithHighlights(clientId, id, FullTextFilter(query))
        (_, highlighted) = result.get
      } yield assertTrue(highlighted == expected)
    }

  val highlightsRUTest =
    testM("highlights (RU)") {
      val query = FullTextFilter.Query.WebsearchQuery("белые цены")
      val expectedSource =
        "Продам Хонда Аккорд, цвет <b>белый</b>, не битая, полный электропакет, климат-контроль."
      val expectedTarget =
        "Какое количество владельцев, <b>цена</b>?"
      val expected =
        CallTranscription(
          Seq(
            CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, expectedSource),
            CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, expectedTarget)
          )
        )
      for {
        (id, clientId) <- insertCall(call)
        _ <- CallTranscriptionDao.insert(call.externalId, sourcePhrase, targetPhrase, transcription)
        result <- CallTranscriptionDao.getWithHighlights(clientId, id, FullTextFilter(query))
        (_, highlighted) = result.get
      } yield assertTrue(highlighted == expected)
    }

  private def insertCall(call: Call) = for {
    _ <- CalltrackingDao.insertCall(call)
    c <- CalltrackingDao.getCallByExternalId(call.externalId)
  } yield (CallId(c.id), ClientId(c.clientId))

  private val cleanup = for {
    xa <- ZIO.service[Transactor[Task]]
    _ <- PgCalltrackingDao.clean
    _ <- sql"DELETE FROM call_transcriptions".update.run.transact(xa).unit.orDie
  } yield ()

}
