package auto.dealers.calltracking.logic.test.managers.transcriptions

import auto.dealers.calltracking.logic.managers.transcriptions.TranscriptionsManager
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.interop.catz._
import common.zio.doobie.ConnManager
import ru.auto.calltracking.proto.model.CallTranscription
import ru.yandex.vertis.telepony.model.proto.model.{CallDialog, CallTranscription => TeleponyTranscription}
import ru.auto.calltracking.proto.model.Call.CallResult
import auto.dealers.calltracking.storage.CalltrackingDao
import auto.dealers.calltracking.model.ExternalId
import auto.dealers.calltracking.model.{Call, CallId, ClientId}
import auto.dealers.calltracking.storage.testkit.TestPostgresql
import auto.dealers.calltracking.storage.testkit.TestStopWordsReplacer
import auto.dealers.calltracking.storage.postgresql._
import auto.dealers.calltracking.logic.managers.transcriptions.TranscriptionsManager._

import scala.collection.immutable.HashSet
import scala.concurrent.duration._
import java.time.Instant
import doobie.implicits._
import doobie.util.transactor.Transactor
import ru.auto.calltracking.proto.filters_model.FullTextFilter
import ru.auto.calltracking.proto.filters_model.FullTextDomain

object DefaultTranscriptionsManagerSpec extends DefaultRunnableSpec {

  private val stopWords = Set("белый", "количество")

  private val externalId = ExternalId.TeleponyId("1234-5678")

  private val call =
    Call(
      0,
      externalId,
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
    "Продам Хонда, цвет белый, не битая, полный электропакет, климат-контроль."

  private val targetPhrase =
    "Какая модель хонда, какое количество владельцев, цена?"

  private val censoredSourcePhrase =
    "Продам Хонда, цвет ***, не битая, полный электропакет, климат-контроль."

  private val censoredTargetPhrase =
    "Какая модель хонда, какое *** владельцев, цена?"

  private val transcription =
    CallTranscription(
      Seq(
        CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, censoredSourcePhrase),
        CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, censoredTargetPhrase)
      )
    )

  private val ponyTranscription =
    TeleponyTranscription(
      externalId.callId,
      Some(
        CallDialog(
          Seq(
            CallDialog.Phrase(CallDialog.Speaker.SOURCE, 0, 42, sourcePhrase),
            CallDialog.Phrase(CallDialog.Speaker.TARGET, 40, 78, targetPhrase)
          )
        )
      )
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("DefaultTranscriptionsManagerSpec")(
      insertionTest,
      validationTest,
      doubleInsertionTest,
      highlightedSourceTest,
      highlightedAllTest
    ) @@ beforeAll(TestPostgresql.initSchema.orDie) @@ after(cleanup) @@ sequential

    suit.provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> ConnManager.fromTransactor >+> PgCalltrackingDao.live >+> PgCallTranscriptionDao.live >+> TestStopWordsReplacer
        .make(stopWords) >+> TranscriptionsManager.live
    )
  }

  val insertionTest =
    testM("insertion") {
      for {
        (id, clientId) <- insertCall(call)
        _ <- TranscriptionsManager.insert(ponyTranscription)
        result <- TranscriptionsManager.get(clientId, id, None)
      } yield assertTrue(result == Some(transcription))
    }

  val validationTest =
    testM("validation") {
      for {
        (id, clientId) <- insertCall(call)
        result <- TranscriptionsManager.insert(ponyTranscription.withCallId("")).run
      } yield assert(result)(fails(equalTo(MissingFieldError("call_id"))))
    }

  val doubleInsertionTest =
    testM("double insertion should fail") {
      for {
        (id, clientId) <- insertCall(call)
        _ <- TranscriptionsManager.insert(ponyTranscription)
        result <- TranscriptionsManager.insert(ponyTranscription).run
      } yield assert(result)(fails(isSubtype[RepositoryError](anything)))
    }

  val highlightedSourceTest =
    testM("highlighted transcription (source)") {
      val query = FullTextFilter(
        domain = FullTextDomain.SOURCE,
        query = FullTextFilter.Query.WebsearchQuery("хонда")
      )
      val censoredSourcePhraseHigh =
        "Продам <b>Хонда</b>, цвет ***, не битая, полный электропакет, климат-контроль."
      val expected =
        CallTranscription(
          Seq(
            CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, censoredSourcePhraseHigh),
            CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, censoredTargetPhrase)
          )
        )
      for {
        (id, clientId) <- insertCall(call)
        _ <- TranscriptionsManager.insert(ponyTranscription)
        result <- TranscriptionsManager.get(clientId, id, Some(query))
      } yield assertTrue(result == Some(expected))
    }

  val highlightedAllTest =
    testM("highlighted transcription (all)") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("хонда")
      )
      val sourcePhraseHigh =
        "Продам <b>Хонда</b>, цвет ***, не битая, полный электропакет, климат-контроль."
      val targetPhraseHigh =
        "Какая модель <b>хонда</b>, какое *** владельцев, цена?"
      val expected =
        CallTranscription(
          Seq(
            CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, sourcePhraseHigh),
            CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, targetPhraseHigh)
          )
        )
      for {
        (id, clientId) <- insertCall(call)
        _ <- TranscriptionsManager.insert(ponyTranscription)
        result <- TranscriptionsManager.get(clientId, id, Some(query))
      } yield assertTrue(result == Some(expected))
    }

  private def insertCall(call: Call) = for {
    _ <- CalltrackingDao.insertCall(call)
    c <- CalltrackingDao.getCallByExternalId(call.externalId)
  } yield (CallId(c.id), ClientId(c.clientId))

  private val cleanup = for {
    xa <- ZIO.service[Transactor[Task]]
    _ <- (for {
      _ <- sql"DELETE FROM call_tags".update.run
      _ <- sql"DELETE FROM client_tags".update.run
      _ <- sql"DELETE FROM offers".update.run
      _ <- sql"DELETE FROM calls".update.run
      _ <- sql"DELETE FROM call_transcriptions".update.run
    } yield ()).transact(xa).unit.orDie
  } yield ()

}
