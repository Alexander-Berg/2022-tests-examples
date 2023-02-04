package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.IncludeTagsClauseBuilder
import ru.yandex.realty.searcher.search.clause.IncludeTagsClauseBuilderSpec.offers

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class IncludeTagsClauseBuilderSpec extends SpecBase with NRTIndexFixture {
  private val clauseBuilders = Seq(new IncludeTagsClauseBuilder)
  insertOffers(offers)

  "IncludeTagsClauseBuilder" should {
    "search offers with tags use logical AND" in {
      val searchQuery = new SearchQuery()
      searchQuery.setIncludeTag(1L)
      searchQuery.setIncludeTag(2L)

      val result = search(searchQuery, clauseBuilders)
      result.getItems.asScala.map(_.getId).toSet shouldEqual Set("1", "4")
    }
  }
}

object IncludeTagsClauseBuilderSpec extends NRTIndexOfferGenerator {

  val offerWithTags12 = buildOffer(1, Seq(1L, 2L))
  val offerWithTags1 = buildOffer(2, Seq(1L))
  val offerWithTags2 = buildOffer(3, Seq(2L))
  val offerWithTags123 = buildOffer(4, Seq(1L, 2L, 3L))
  val offerWithTags234 = buildOffer(5, Seq(2L, 3L, 4L))
  val offers = Seq(offerWithTags12, offerWithTags1, offerWithTags2, offerWithTags123, offerWithTags234)

  def buildOffer(offerId: Long, tags: Seq[Long]): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.setTags(tags.map(Long.box).asJava)
    offer
  }
}
