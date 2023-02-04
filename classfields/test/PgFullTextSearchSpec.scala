package auto.dealers.calltracking.storage.test

import auto.dealers.calltracking.storage.testkit.TestPostgresql
import zio.test.TestAspect._
import zio.test._
import zio.test.Assertion._
import common.zio.doobie.ConnManager
import auto.dealers.calltracking.storage.postgresql.{PgCallTranscriptionDao, PgCalltrackingDao}
import auto.dealers.calltracking.model.{Call, ClientId}
import ru.auto.calltracking.proto.model.Call.CallResult
import auto.dealers.calltracking.model.ExternalId

import java.time.Instant
import scala.collection.immutable.HashSet
import scala.concurrent.duration._
import ru.auto.calltracking.proto.model.CallTranscription
import auto.dealers.calltracking.storage.CallTranscriptionDao
import auto.dealers.calltracking.storage.CalltrackingDao
import ru.auto.calltracking.proto.filters_model._
import auto.dealers.calltracking.model.Filters
import auto.common.pagination.RequestPagination
import ru.auto.calltracking.proto.filters_model.Sorting
import ru.auto.calltracking.proto.filters_model.FullTextDomain

object PgFullTextSearchSpec extends DefaultRunnableSpec {

  private val clientId = ClientId(1L)

  private val call1 =
    Call(
      0,
      ExternalId.TeleponyId("1234-5678"),
      clientId.id,
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

  private val call2 =
    Call(
      0,
      ExternalId.TeleponyId("1234-abcd"),
      clientId.id,
      Instant.parse("2052-12-10T18:28:28.732Z"),
      Instant.parse("2046-02-20T19:23:12.352Z"),
      CallResult.NO_ANSWER,
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

  private val sourcePhrase1 =
    "Продам Хонда Аккорд, цвет белый, не битая, полный электропакет, климат-контроль."

  private val targetPhrase1 =
    "Какое количество владельцев, и за какую вы цену ее продаете?"

  private val transcription1 =
    CallTranscription(
      Seq(
        CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, sourcePhrase1),
        CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, targetPhrase1)
      )
    )

  private val sourcePhrase2 =
    "вы продаете рыбов?"

  private val targetPhrase2 =
    "нет, показываю"

  private val transcription2 =
    CallTranscription(
      Seq(
        CallTranscription.Phrase(CallTranscription.Speaker.SOURCE, 0, 42, sourcePhrase2),
        CallTranscription.Phrase(CallTranscription.Speaker.TARGET, 40, 78, targetPhrase2)
      )
    )

  override def spec = {
    val suit = suite("PgFullTextSearchSpec")(
      searchTest,
      searchMultTest,
      searchRankingTest,
      searchDomainTest,
      searchWithFiltersTest,
      searchUnsuccessfulTest,
      countTest,
      countMultTest,
      countDomainTest,
      countWithFiltersTest,
      countUnsuccessfulTest,
      annotationsTest,
      annotationsSpeakerFilterTest,
      annotationsMultipleMatchesTest
    ) @@ beforeAll(init) @@ sequential

    suit.provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> ConnManager.fromTransactor >+> PgCalltrackingDao.live >+> PgCallTranscriptionDao.live
    )
  }

  val searchTest =
    testM("search text") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("хонда")
      )
      for {
        result <- CalltrackingDao.getCalls(
          clientId,
          Filters.empty,
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
      } yield assertTrue(result.map(_.externalId) == Seq(call1.externalId))
    }

  val searchMultTest =
    testM("search text (multiple results)") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("продаете")
      )
      for {
        result <- CalltrackingDao.getCalls(
          clientId,
          Filters.empty,
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
      } yield assertTrue(result.map(_.externalId) == Seq(call1.externalId, call2.externalId))
    }

  val searchRankingTest =
    testM("search text (check ranking)") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("продаете рыбов OR хонда электропакет")
      )
      for {
        result <- CalltrackingDao.getCalls(
          clientId,
          Filters.empty,
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
      } yield assertTrue(result.map(_.externalId) == Seq(call2.externalId, call1.externalId))
    }

  val searchDomainTest =
    testM("search text (target domain only)") {
      val query = FullTextFilter(
        domain = FullTextDomain.TARGET,
        query = FullTextFilter.Query.WebsearchQuery("продаете")
      )
      for {
        result <- CalltrackingDao.getCalls(
          clientId,
          Filters.empty,
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
      } yield assertTrue(result.map(_.externalId) == Seq(call1.externalId))
    }

  val searchWithFiltersTest =
    testM("search text with listing filters applied") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("продаете")
      )
      for {
        result <- CalltrackingDao.getCalls(
          clientId,
          Filters(callResultGroup = CallResultGroup.ANSWERED_GROUP),
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
      } yield assertTrue(result.map(_.externalId) == Seq(call1.externalId))
    }

  val searchUnsuccessfulTest =
    testM("search text (no matches)") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("Акура")
      )
      for {
        result <- CalltrackingDao.getCalls(
          clientId,
          Filters.empty,
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
      } yield assertTrue(result.isEmpty)
    }

  val countTest =
    testM("count search results") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("хонда")
      )
      for {
        result <- CalltrackingDao.countCalls(
          clientId,
          Filters.empty,
          Some(query)
        )
      } yield assertTrue(result == 1)
    }

  val countMultTest =
    testM("count search results (multiple results)") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("продаете")
      )
      for {
        result <- CalltrackingDao.countCalls(
          clientId,
          Filters.empty,
          Some(query)
        )
      } yield assertTrue(result == 2)
    }

  val countDomainTest =
    testM("count search results (target domain only)") {
      val query = FullTextFilter(
        domain = FullTextDomain.TARGET,
        query = FullTextFilter.Query.WebsearchQuery("продаете")
      )
      for {
        result <- CalltrackingDao.countCalls(
          clientId,
          Filters.empty,
          Some(query)
        )
      } yield assertTrue(result == 1)
    }

  val countWithFiltersTest =
    testM("count search results with listing filters applied") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("продаете")
      )
      for {
        result <- CalltrackingDao.countCalls(
          clientId,
          Filters(callResultGroup = CallResultGroup.ANSWERED_GROUP),
          Some(query)
        )
      } yield assertTrue(result == 1)
    }

  val countUnsuccessfulTest =
    testM("count search results (no matches)") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("Акура")
      )
      for {
        result <- CalltrackingDao.countCalls(
          clientId,
          Filters.empty,
          Some(query)
        )
      } yield assertTrue(result == 0)
    }

  val annotationsTest =
    testM("annotation field is filled properly") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("продаете")
      )
      for {
        calls <- CalltrackingDao.getCalls(
          clientId,
          Filters.empty,
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
        annotations = calls.flatMap(_.transcriptionAnnotation).map(ann => ann.text -> ann.matches)
      } yield assert(annotations)(
        hasSameElements(
          Seq(
            "вы <b>продаете</b> рыбов?" -> 1,
            "Какое количество владельцев, и за какую вы цену ее <b>продаете</b>?" -> 1
          )
        )
      )
    }

  val annotationsSpeakerFilterTest =
    testM("annotations are filtered by speaker properly") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("продаете"),
        domain = FullTextDomain.SOURCE
      )
      for {
        calls <- CalltrackingDao.getCalls(
          clientId,
          Filters.empty,
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
        annotations = calls.flatMap(_.transcriptionAnnotation).map(ann => ann.text -> ann.matches)
      } yield assert(annotations)(
        hasSameElements(
          Seq(
            "вы <b>продаете</b> рыбов?" -> 1
          )
        )
      )
    }

  val annotationsMultipleMatchesTest =
    testM("annotations count matched phrases properly") {
      val query = FullTextFilter(
        query = FullTextFilter.Query.WebsearchQuery("продаете OR цвет белый")
      )
      for {
        calls <- CalltrackingDao.getCalls(
          clientId,
          Filters.empty,
          RequestPagination(1, 100),
          Sorting.defaultInstance,
          Some(query)
        )
        annotations = calls.flatMap(_.transcriptionAnnotation).map(ann => ann.text -> ann.matches)
      } yield assert(annotations)(
        hasSameElements(
          Seq(
            "вы <b>продаете</b> рыбов?" -> 1,
            "Продам Хонда Аккорд, <b>цвет</b> <b>белый</b>, не битая, полный электропакет, климат-контроль." -> 2
          )
        )
      )
    }

  private lazy val init = for {
    _ <- PgCalltrackingDao.initSchema.orDie
    _ <- CalltrackingDao.insertCall(call1)
    _ <- CalltrackingDao.insertCall(call2)
    _ <- CallTranscriptionDao.insert(call1.externalId, sourcePhrase1, targetPhrase1, transcription1)
    _ <- CallTranscriptionDao.insert(call2.externalId, sourcePhrase2, targetPhrase2, transcription2)
  } yield ()

}
