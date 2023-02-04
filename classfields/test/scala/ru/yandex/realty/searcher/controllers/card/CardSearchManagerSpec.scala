package ru.yandex.realty.searcher.controllers.card

import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.RAMDirectory
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.complaints.ComplaintsInfo.ComplaintsInfoMessage
import ru.yandex.realty.entry.EntryDocumentsBuilder
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.locale.RealtyLocale
import ru.yandex.realty.model.offer.{ErrorSource, IndexingError, Offer, OfferState, PricingPeriod}
import ru.yandex.realty.model.request.PriceType
import ru.yandex.realty.persistence.{EternalOfferDao, OfferId, RealtimeDatabaseAPI}
import ru.yandex.realty.request.{Request, RequestImpl}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.search.exception.EntityNotFoundException
import ru.yandex.realty.searcher.action.card.CardSearchResponse
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextImpl, SearchContextProvider}
import ru.yandex.realty.searcher.personalization.PersonalService
import ru.yandex.realty.searcher.request.CardRequest
import ru.yandex.realty.searcher.response.OfferResponse
import ru.yandex.realty.searcher.response.builders.{CardResponseBuilder, UnifiedOfferResponseBuilder}
import ru.yandex.realty.searcher.search.LuceneByIdSearcher

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class CardSearchManagerSpec extends AsyncSpecBase {

  "CardSearchManager" should {
    "not return banned offers from cassandra" in new CardSearchManagerFixture {
      val offer = new Offer()
      offer.setId(1L)
      val state = new OfferState()
      state.addError(IndexingError.USER_INFO, ErrorSource.MODERATION, "test")
      offer.setOfferState(state)
      val history = OfferHistory.justArrived()
      (cassandraApi
        .readAsync(_: OfferId)(_: ExecutionContext))
        .expects(String.valueOf(1L), *)
        .returns(Future.successful(Some(offer, history)))

      val request = new CardRequest(
        1L,
        "uri",
        "ip",
        "yuid",
        Currency.RUR,
        PriceType.PER_OFFER,
        RealtyLocale.RU,
        PricingPeriod.WHOLE_LIFE,
        "login",
        new SearchQuery()
      )

      val exception: Throwable = manager.cardSearch(request)(new RequestImpl()).failed.futureValue
      exception shouldBe a[EntityNotFoundException]
    }
    "return not banned offers from cassandra" in new CardSearchManagerFixture {
      val offer = new Offer()
      offer.setId(2L)
      val state = new OfferState()
      state.addError(IndexingError.INVALID_GARAGE_TYPE, ErrorSource.MODERATION, "test")
      offer.setOfferState(state)
      val history = OfferHistory.justArrived()
      (cassandraApi
        .readAsync(_: OfferId)(_: ExecutionContext))
        .expects(String.valueOf(2L), *)
        .returns(Future.successful(Some(offer, history)))

      private val offerResponse: OfferResponse = mock[OfferResponse]

      (responseBuilder
        .build(_: Offer, _: CardRequest, _: ComplaintsInfoMessage))
        .expects(*, *, *)
        .anyNumberOfTimes()
        .returns(Seq(offerResponse).asJava)

      val request = new CardRequest(
        2L,
        "uri",
        "ip",
        "yuid",
        Currency.RUR,
        PriceType.PER_OFFER,
        RealtyLocale.RU,
        PricingPeriod.WHOLE_LIFE,
        "login",
        new SearchQuery()
      )

      val response: CardSearchResponse = manager.cardSearch(request)(new RequestImpl()).futureValue
      response.data.size shouldBe 1
      response.data.head.offers.size shouldBe 1
      response.data.head.offers.head shouldBe offerResponse
    }
  }

  trait CardSearchManagerFixture {
    val cassandraApi = mock[RealtimeDatabaseAPI]
    private val eternalOfferDao = mock[EternalOfferDao]
    private val personalService = mock[PersonalService]
    val responseBuilder = mock[CardResponseBuilder]
    private val unifiedOfferBuilder = mock[UnifiedOfferResponseBuilder]

    (personalService
      .getDeletedClusters(_: Option[OfferId])(_: Request))
      .expects(*, *)
      .returns(Future.successful(Seq.empty))
    (personalService
      .getNotes(_: Option[OfferId], _: Long)(_: Request))
      .expects(*, *, *)
      .returns(Future.successful(Map.empty))

    val manager = new CardSearchManager(
      initSearchContext(),
      cassandraApi,
      eternalOfferDao,
      new LuceneByIdSearcher(),
      personalService,
      () => ComplaintsInfoMessage.getDefaultInstance,
      responseBuilder,
      unifiedOfferBuilder
    )

    private def initSearchContext(): SearchContextProvider[SearchContext] = {
      val memoryIndex = new RAMDirectory()
      val indexWriter = new IndexWriter(memoryIndex, new IndexWriterConfig(EntryDocumentsBuilder.analyzer))
      indexWriter.addDocument(Seq.empty.asJava)
      indexWriter.close()
      val reader = DirectoryReader.open(memoryIndex)
      val indexSearcher = new IndexSearcher(reader)
      new SearchContextProvider[SearchContext] {
        override def doWithContext[U](doWith: SearchContext => U): U = doWith(new SearchContextImpl(indexSearcher))

        override def doWithContextAsync[U](doWith: SearchContext => Future[U]): Future[U] =
          doWith(new SearchContextImpl(indexSearcher))
      }
    }
  }
}
