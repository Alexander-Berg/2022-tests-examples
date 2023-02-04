package ru.yandex.vertis.moderation.scheduler.task.vin

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.auto.api.vin.VinApiModel
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.converters.Protobuf
import ru.yandex.vertis.moderation.httpclient.clustering.ClusteringClient
import ru.yandex.vertis.moderation.httpclient.vin._
import ru.yandex.vertis.moderation.model.autoru.{PriceInfo => AutoruPriceInfo}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.PriceInfo.{Currency => AutoruCurrency}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.scheduler.task.vin.VinHistoryDecider._
import ru.yandex.vertis.moderation.util.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class VinHistoryDeciderSpec extends SpecBase {

  val decider: VinHistoryDecider = VinHistoryDecider.forService(Service.AUTORU)

  private val origin = DateTimeUtil.now().minusHours(1)

  private case class TestCase(description: String, source: Source, expected: Verdict)

  private case class Item(price: Option[Money], miliage: Option[Miliage])

  private case class OfferHistoryRecord(userId: UserId,
                                        offerId: OfferId,
                                        mark: Option[Mark],
                                        model: Option[Model],
                                        kmAge: Option[Miliage],
                                        price: Option[Money],
                                        dateOfRemoval: Option[DateTime],
                                        dateOfPlacement: Option[DateTime]
                                       )

  implicit private class RichOfferHistoryRecord(val offer: OfferHistoryRecord) {

    def toOfferItem: VinApiModel.OfferItem = {
      val builder = VinApiModel.OfferItem.newBuilder
      offer.mark.foreach(builder.setMark)
      offer.model.foreach(builder.setModel)
      offer.kmAge.foreach(builder.setKmage)
      offer.price.map(_.toInt).foreach(builder.setPrice)
      offer.dateOfPlacement.foreach { dt =>
        builder.setDateOfPlacement(Protobuf.toMessage(dt))
      }
      offer.dateOfRemoval.foreach { dt =>
        builder.setDateOfRemoval(Protobuf.toMessage(dt))
      }
      builder
        .setOfferId(offer.offerId)
        .setUser(offer.userId)
        .build
    }
  }

  private val priceMinGrowthRubDefault: Int = 11000
  private val priceMinGrowthRatDefault: Double = 0.03

  private def source(internal: Item, vinHistory: Item, minusMonths: Int): Source = {
    val priceInfo = internal.price.map(AutoruPriceInfo(_, AutoruCurrency.RUB, None, None))
    val autoru =
      AutoruEssentialsGen.next.copy(priceInfo = priceInfo, miliage = internal.miliage, timestampCreate = Some(origin))
    val instance = InstanceGen.next.copy(essentials = autoru)
    val history =
      Seq(
        OfferHistoryRecord(
          userId = "user:123456",
          offerId = "history_offer_id",
          mark = autoru.mark,
          model = autoru.model,
          kmAge = vinHistory.miliage,
          price = vinHistory.price,
          dateOfRemoval = autoru.timestampCreate.map(_.minusMonths(minusMonths)),
          dateOfPlacement = autoru.timestampCreate.map(_.minusMonths(minusMonths + 1))
        )
      )
    Source(instance, None, history.map(_.toOfferItem), Seq.empty, priceMinGrowthRubDefault, priceMinGrowthRatDefault)
  }

  val EmptyVerdict = Verdict(Set.empty)
  private val tests =
    Seq(
      TestCase(
        description = "Ok on empty history fields",
        source = source(Item(Some(500000), Some(100000)), Item(None, None), 1),
        expected = EmptyVerdict
      ),
      TestCase(
        description = "Ok on empty moderation fields",
        source = source(Item(None, None), Item(Some(500000), Some(100000)), 1),
        expected = EmptyVerdict
      ),
      TestCase(
        description = "Ok on equal fields",
        source = source(Item(Some(500000), Some(100000)), Item(Some(500000), Some(100000)), 1),
        expected = EmptyVerdict
      ),
      TestCase(
        description = "Ok on less price",
        source = source(Item(Some(500000), Some(100000)), Item(Some(600000), Some(100000)), 1),
        expected = EmptyVerdict
      ),
      TestCase(
        description = "Ok on little growing price",
        source = source(Item(Some(500050), Some(100000)), Item(Some(500000), Some(100000)), 1),
        expected = EmptyVerdict
      ),
      TestCase(
        description = "Ok on above mileage",
        source = source(Item(Some(500000), Some(95000)), Item(Some(500000), Some(100000)), 1),
        expected = EmptyVerdict
      ),
      TestCase(
        description = "Suspected on less miliage",
        source = source(Item(Some(500000), Some(94000)), Item(Some(500000), Some(100000)), 1),
        expected = Verdict(Set(MiliageSuspectType))
      ),
      TestCase(
        description = "Suspected on above price",
        source = source(Item(Some(600000), Some(100000)), Item(Some(500000), Some(100000)), 1),
        expected = Verdict(Set(PriceSuspectType))
      ),
      TestCase(
        description = "Suspected on above price & less miliage",
        source = source(Item(Some(600000), Some(100000)), Item(Some(500000), Some(110000)), 1),
        expected = Verdict(Set(MiliageSuspectType, PriceSuspectType))
      )
    )

  "VinHistoryDecider" should {
    tests.foreach { testCase =>
      s"${testCase.description}" in {
        val actual = decider(testCase.source)
        actual shouldBe testCase.expected
      }
    }

    "Rule.previousOffer expired offer" in {
      val actual = decider(source(Item(Some(600000), Some(100000)), Item(Some(500000), Some(110000)), 5))
      val expected = Verdict(Set.empty)
      actual shouldBe expected
    }

    "Rule.previousOffer" in {
      val expectedOfferId = "matched_offer"
      val autoru =
        AutoruEssentialsGen.next.copy(
          mark = Some("KIA"),
          model = Some("RIO"),
          timestampCreate = Some(origin.minusMinutes(1))
        )
      val instance = InstanceGen.next.copy(essentials = autoru)
      val history =
        Seq(
          // Exclude newer than target offer
          OfferHistoryRecord(
            userId = "user:654321",
            offerId = "newer_offer",
            mark = Some("KIA"),
            model = Some("RIO"),
            kmAge = None,
            price = None,
            dateOfRemoval = autoru.timestampCreate.map(_.plusMinutes(1)),
            dateOfPlacement = autoru.timestampCreate.map(_.plusMinutes(1))
          ),
          // Exclude newer than target user
          OfferHistoryRecord(
            userId = "user:123456",
            offerId = "newer_offer",
            mark = Some("KIA"),
            model = Some("RIO"),
            kmAge = None,
            price = None,
            dateOfRemoval = autoru.timestampCreate.map(_.plusMinutes(1)),
            dateOfPlacement = autoru.timestampCreate.map(_.plusMinutes(1))
          ),
          // Exclude offer with mark&model mismatch
          OfferHistoryRecord(
            userId = "user:654321",
            offerId = "mismatch_mark_model",
            mark = None,
            model = None,
            kmAge = None,
            price = None,
            dateOfRemoval = autoru.timestampCreate.map(_.minusMinutes(2)),
            dateOfPlacement = autoru.timestampCreate.map(_.minusMinutes(3))
          ),
          // Exclude offer with equal to target ID
          OfferHistoryRecord(
            userId = "user:654321",
            offerId = instance.externalId.objectId,
            mark = Some("KIA"),
            model = Some("RIO"),
            kmAge = None,
            price = None,
            dateOfRemoval = autoru.timestampCreate.map(_.minusMinutes(3)),
            dateOfPlacement = autoru.timestampCreate.map(_.minusMinutes(4))
          ),
          // Matched offer
          OfferHistoryRecord(
            userId = "user:654321",
            offerId = "matched_offer",
            mark = Some("KIA"),
            model = Some("RIO"),
            kmAge = None,
            price = None,
            dateOfRemoval = autoru.timestampCreate.map(_.minusMinutes(4)),
            dateOfPlacement = autoru.timestampCreate.map(_.minusMinutes(5))
          ),
          // Mismatched second offer
          OfferHistoryRecord(
            userId = "user:654321",
            offerId = "mismatched_offer",
            mark = Some("KIA"),
            model = Some("RIO"),
            kmAge = None,
            price = None,
            dateOfRemoval = autoru.timestampCreate.map(_.minusMinutes(5)),
            dateOfPlacement = autoru.timestampCreate.map(_.minusMinutes(6))
          ),
          OfferHistoryRecord(
            userId = "user:654322",
            offerId = "mismatched_offer2",
            mark = Some("KIA"),
            model = Some("RIO"),
            kmAge = None,
            price = None,
            dateOfRemoval = autoru.timestampCreate.map(_.minusMinutes(3)),
            dateOfPlacement = autoru.timestampCreate.map(_.minusMinutes(4))
          )
        )

      import ClusteringClient.Domains._

      val actualOfferId =
        AutoruVinHistoryDecider.Rule
          .previousOffer(
            Source(
              instance,
              None,
              history.map(_.toOfferItem),
              Seq(ClusteringClient.User("654322", AutoRu), ClusteringClient.User("654321", Realty)),
              priceMinGrowthRubDefault,
              priceMinGrowthRatDefault
            )
          )
          .get
          .getOfferId
      actualOfferId shouldBe expectedOfferId
    }
  }
}
