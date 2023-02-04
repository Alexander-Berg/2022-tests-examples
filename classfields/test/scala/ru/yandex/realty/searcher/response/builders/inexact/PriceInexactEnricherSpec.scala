package ru.yandex.realty.searcher.response.builders.inexact

import com.google.protobuf.util.JsonFormat
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.model.offer.{AreaUnit, PriceInfo, PricingPeriod}
import ru.yandex.realty.proto.search.inexact.{InexactMatching, PriceInexact}
import ru.yandex.realty.proto.unified.offer.rent.TemporaryPrice
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.util
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._

@RunWith(classOf[JUnitRunner])
class PriceInexactEnricherSpec extends WordSpec with MockFactory with Matchers {

  import ProtoHelper._

  val searchQuery = new SearchQuery()
  private val priceRange: util.Range = util.Range.create(100.0f, 500.0f)
  searchQuery.setPrice(priceRange)

  val priceInexactEnricher = new PriceInexactEnricher(searchQuery)

  val builder = InexactMatching.newBuilder()
  val parser = JsonFormat.parser()

  "PriceInexactEnricher" should {
    "checkAndEnrich with offer price less than search range " in new OfferBuilderContextFixture {
      val inexact =
        """{
          |  "inexact": {
          |    "value": 50.0,
          |    "diff":  50.0,
          |    "trend": "LESS"
          |  }
          |}""".stripMargin
          .toProto[PriceInexact]

      (dummyOfferBuilderContext.offer.getTemporaryPrice _)
        .expects()
        .returning(null)

      priceInexactEnricher.checkAndEnrich(builder, dummyOfferBuilderContext).getPrice shouldBe inexact
    }

    "checkAndEnrich with big value " in new OfferBuilderContextFixture {
      val priceInfo =
        PriceInfo.create(Currency.RUR, 1085788288f, PricingPeriod.PER_MONTH, AreaUnit.WHOLE_OFFER)
      private val context: OfferBuilderContext = dummyOfferBuilderContext.copy(price = priceInfo)
      val inexact =
        """{
          |  "inexact": {
          |    "value": 1.085788288E9,
          |    "diff":  1.085787788E9,
          |    "trend": "MORE"
          |  }
          |}""".stripMargin
          .toProto[PriceInexact]

      (context.offer.getTemporaryPrice _)
        .expects()
        .returning(null)

      priceInexactEnricher.checkAndEnrich(builder, context).getPrice shouldBe inexact
    }

    "checkAndEnrich with temporary price less than searchQuery.price less than price" in new OfferBuilderContextFixture {
      val inexact1 =
        """{
          |  "inexact": {
          |    "value": 50000.0,
          |    "diff":  1000.0,
          |    "trend": "MORE"
          |  }
          |}""".stripMargin
          .toProto[PriceInexact]

      val inexact2 =
        """{
          |  "inexact": {
          |    "value": 40000.0,
          |    "diff":  2000.0,
          |    "trend": "LESS"
          |  }
          |}""".stripMargin
          .toProto[PriceInexact]

      (offerBuilderContextWithTemporaryPrice.offer.getTemporaryPrice _)
        .expects()
        .returning(TemporaryPrice.newBuilder().setValue(temporaryRentValue40000Rub).build())
        .anyNumberOfTimes()

      buildPriceInexactEnricher(util.Range.create(42000.0f, 49000.0f))
        .checkAndEnrich(builder.clearPrice(), offerBuilderContextWithTemporaryPrice)
        .getPrice shouldBe inexact1

      buildPriceInexactEnricher(util.Range.create(42000.0f, 47000.0f))
        .checkAndEnrich(builder.clearPrice(), offerBuilderContextWithTemporaryPrice)
        .getPrice shouldBe inexact2
    }

    "checkAndEnrich with temporary price less than price less than searchQuery.price" in new OfferBuilderContextFixture {
      val inexact =
        """{
          |  "inexact": {
          |    "value": 50000.0,
          |    "diff":  4000.0,
          |    "trend": "LESS"
          |  }
          |}""".stripMargin
          .toProto[PriceInexact]

      (offerBuilderContextWithTemporaryPrice.offer.getTemporaryPrice _)
        .expects()
        .returning(TemporaryPrice.newBuilder().setValue(temporaryRentValue40000Rub).build())
        .anyNumberOfTimes()

      buildPriceInexactEnricher(util.Range.create(54000.0f, 60000.0f))
        .checkAndEnrich(builder.clearPrice(), offerBuilderContextWithTemporaryPrice)
        .getPrice shouldBe inexact
    }

    "checkAndEnrich with searchQuery.price less than temporary price less than price" in new OfferBuilderContextFixture {
      val inexact =
        """{
          |  "inexact": {
          |    "value": 40000.0,
          |    "diff":  2000.0,
          |    "trend": "MORE"
          |  }
          |}""".stripMargin
          .toProto[PriceInexact]

      (offerBuilderContextWithTemporaryPrice.offer.getTemporaryPrice _)
        .expects()
        .returning(TemporaryPrice.newBuilder().setValue(temporaryRentValue40000Rub).build())
        .anyNumberOfTimes()

      buildPriceInexactEnricher(util.Range.create(34000.0f, 38000.0f))
        .checkAndEnrich(builder.clearPrice(), offerBuilderContextWithTemporaryPrice)
        .getPrice shouldBe inexact
    }

    "checkAndEnrich with temporary price inside searchQuery.price which is less than price" in new OfferBuilderContextFixture {
      val inexact = PriceInexact.getDefaultInstance

      (offerBuilderContextWithTemporaryPrice.offer.getTemporaryPrice _)
        .expects()
        .returning(TemporaryPrice.newBuilder().setValue(temporaryRentValue40000Rub).build())
        .anyNumberOfTimes()

      buildPriceInexactEnricher(util.Range.create(35000.0f, 49000.0f))
        .checkAndEnrich(builder.clearPrice(), offerBuilderContextWithTemporaryPrice)
        .getPrice shouldBe inexact
    }

    def buildPriceInexactEnricher(searchPrice: util.Range): PriceInexactEnricher = {
      val searchQuery = new SearchQuery()
      searchQuery.setPrice(searchPrice)
      new PriceInexactEnricher(searchQuery)
    }
  }

}
