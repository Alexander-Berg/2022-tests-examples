package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.offers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.extdata.service.canonical.router.model.Params._
import ru.yandex.auto.extdata.service.canonical.router.model.{CanonicalUrlRequest, CanonicalUrlRequestType}
import ru.yandex.auto.message.CarAdSchema.CarAdMessage

@RunWith(classOf[JUnitRunner])
class OffersCarsRequestProviderSpec extends OffersRequestProviderSpecBase[CarAdMessage] {

  private type FoldF = (Set[CanonicalUrlRequest], CarAdMessage) => Set[CanonicalUrlRequest]

  private val Cars = "cars"
  private val OfferPrefix = "autoru-"

  private val Audi = "audi"
  private val A7 = "a7"
  private val TT = "tt"

  private val SaleIdA7: Long = 1234
  private val SaleIdTT: Long = 1235
  private val SaleHash: String = "suyt3"
  private val TechParamId: Long = 122
  private val ComplectationId: Long = 123

  private val AudiA7NewMsg =
    CarAdMessage
      .newBuilder()
      .setVersion(1)
      .setModel(A7)
      .setMark(Audi)
      .setSearchState(Search.NEW.name())
      .setId(s"$OfferPrefix$SaleIdA7")
      .setAutoruHashCode(SaleHash)
      .build()

  private def withComplectation(msg: CarAdMessage) =
    msg.toBuilder
      .setTechParamId(TechParamId)
      .setComplectationId(ComplectationId)
      .build()

  private val AudiTtUsed =
    CarAdMessage
      .newBuilder()
      .setVersion(1)
      .setModel(TT)
      .setMark(Audi)
      .setSearchState(Search.USED.name())
      .setId(s"$OfferPrefix$SaleIdTT")
      .setAutoruHashCode(SaleHash)
      .build()

  // ToDo: In-memory stub
  private def getProviderForMessages(msgs: CarAdMessage*): OffersCarsRequestProvider = ???

  // внутри используем foldLeft, можно проверять по одному
  "SitemapOffersCarsRequestProvider" ignore {
    "correctly build requests" when {
      "comes new car" in {
        val provider = getProviderForMessages(AudiA7NewMsg)

        val expected = Set(
          CanonicalUrlRequest(
            CanonicalUrlRequestType.CardOld,
            Set(
              new CategoryParam(Cars),
              new SectionParam("new"),
              new SaleHashParam(SaleHash),
              new SaleIdParam(SaleIdA7)
            )
          ),
          CanonicalUrlRequest(
            CanonicalUrlRequestType.Card,
            Set(
              new ModelParam(A7),
              new MarkParam(Audi),
              new CategoryParam(Cars),
              new SectionParam("new"),
              new SaleHashParam(SaleHash),
              new SaleIdParam(SaleIdA7)
            )
          )
        )

        provider.get().toSeq should contain theSameElementsAs expected
      }

      "comes new car with techId" in {
        val withTech = withComplectation(AudiA7NewMsg)
        val provider = getProviderForMessages(withTech)

        val expected = Set(
          CanonicalUrlRequest(
            CanonicalUrlRequestType.CardOld,
            Set(
              new CategoryParam(Cars),
              new SectionParam("new"),
              new SaleHashParam(SaleHash),
              new SaleIdParam(SaleIdA7)
            )
          ),
          CanonicalUrlRequest(
            CanonicalUrlRequestType.CardNew,
            Set(
              new ModelParam(A7),
              new MarkParam(Audi),
              new CategoryParam(Cars),
              new SectionParam("new"),
              new SaleHashParam(SaleHash),
              new SaleIdParam(SaleIdA7),
              new ComplectationIdParam(ComplectationId),
              new TechIdParam(TechParamId)
            )
          )
        )

        provider.get().toSeq should contain theSameElementsAs expected
      }

      "comes used car" in {
        val provider = getProviderForMessages(AudiTtUsed)

        val old = CanonicalUrlRequest(
          CanonicalUrlRequestType.CardOld,
          Set(
            new CategoryParam(Cars),
            new SectionParam("used"),
            new SaleHashParam(SaleHash),
            new SaleIdParam(SaleIdTT)
          )
        )

        val expected = Seq(
          old,
          old
            .withType(CanonicalUrlRequestType.Card)
            .withParam(new MarkParam(Audi))
            .withParam(new ModelParam(TT))
        )

        provider.get().toSeq should contain theSameElementsAs expected
      }

      "comes used car with techId" in {
        val withTech = withComplectation(AudiTtUsed)

        val provider = getProviderForMessages(withTech)

        val old = CanonicalUrlRequest(
          CanonicalUrlRequestType.CardOld,
          Set(
            new CategoryParam(Cars),
            new SectionParam("used"),
            new SaleHashParam(SaleHash),
            new SaleIdParam(SaleIdTT)
          )
        )

        val expected = Seq(
          old,
          old
            .withType(CanonicalUrlRequestType.Card)
            .withParam(new MarkParam(Audi))
            .withParam(new ModelParam(TT))
        )

        provider.get().toSeq should contain theSameElementsAs expected
      }
    }

    "do nothing for beaten" in {
      val msg = AudiA7NewMsg.toBuilder.setSearchState(Search.BEATEN.name()).build()

      val provider = getProviderForMessages(msg)

      provider.get().toSeq shouldBe empty
    }
  }
}
