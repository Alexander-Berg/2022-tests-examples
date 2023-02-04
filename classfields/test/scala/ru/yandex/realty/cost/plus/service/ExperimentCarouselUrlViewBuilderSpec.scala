package ru.yandex.realty.cost.plus.service

import org.joda.time.Instant
import org.junit.runner.RunWith
import ru.yandex.realty.cost.plus.model.yml.YmlOfferData.OfferSearchData
import ru.yandex.realty.cost.plus.model.yml.{RawYmlOffer, YmlPrice}
import ru.yandex.realty.cost.plus.service.builder.UrlViewBuilder
import ru.yandex.realty.cost.plus.service.builder.UrlViewBuilder.BuilderContext
import ru.yandex.realty.cost.plus.service.builder.impl.ExperimentCarouselUrlViewBuilder
import ru.yandex.realty.cost.plus.utils.CustomAssertions
import ru.yandex.realty.traffic.model.ad.GrouppedByUrlAds
import ru.yandex.realty.traffic.model.offer.OfferType
import ru.yandex.realty.traffic.model.urls.ExtractedSourceUrl
import ru.yandex.vertis.mockito.MockitoSupport
import zio.test._
import zio.test.junit._

import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random

@RunWith(classOf[ZTestJUnitRunner])
class ExperimentCarouselUrlViewBuilderSpec extends JUnitRunnableSpec with MockitoSupport {

  import ExperimentCarouselUrlViewBuilderSpec._

  private def joinSpec(
    a: Seq[Int],
    b: Seq[Int],
    expected: Seq[Int]
  ) =
    test(s"should correctly join $a and $b") {
      val actual = ExperimentCarouselUrlViewBuilder.join[Int, Int](a, b, identity)

      assert(actual)(CustomAssertions.hasSameElementsAndOrder(expected))
    }

  private def rawYmlOffer(id: Int): RawYmlOffer = {
    RawYmlOffer(
      name = "",
      url = RawYmlOffer.Url(path = s"/url/", pinnedOfferId = Some(id.toString)),
      price = YmlPrice(1000, isFrom = false),
      categoryId = 1,
      imageUrl = "",
      additionalData = OfferSearchData(
        id.toString,
        None,
        None,
        OfferType.Sell,
        None,
        Instant.ofEpochMilli(0),
        offerForSiteTable = false
      ),
      setRequired = true
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ExperimentCarouselUrlViewBuilder")(
      joinSpec(
        Seq(1, 2, 3, 4),
        Seq(2, 1, 5, 6),
        Seq(1, 2, 3, 5, 4, 6)
      ),
      joinSpec(
        Seq(2, 1, 5, 6),
        Seq(1, 2, 3, 4),
        Seq(2, 1, 5, 3, 6, 4)
      ),
      joinSpec(
        Seq(),
        Seq(1, 2, 3, 4),
        Seq(1, 2, 3, 4)
      ),
      joinSpec(
        Seq(1, 2, 3, 4),
        Seq(),
        Seq(1, 2, 3, 4)
      ),
      test("should call carousel view builder once when current ordering is empty") {
        val builder =
          getBuilder {
            new CarouselMock(Seq(Seq.empty))
          }

        assertTrue(builder.buildYmlOffers(GrouppedAds, BuilderContext.Empty).isEmpty)
      },
      test("should call carousel view builder twice when current ordering is not empty") {
        val offers = (1 to 6).map(rawYmlOffer)

        val builder =
          getBuilder {
            new CarouselMock(
              Seq(
                Seq(1, 2, 3, 4).map(i => offers(i - 1)),
                Seq(2, 1, 5, 6).map(i => offers(i - 1))
              )
            )
          }

        val expected = {
          Seq(1, 2, 3, 5, 4, 6)
            .map(i => offers(i - 1))
            .zipWithIndex
            .map {
              case (o, i) =>
                val c = if (i % 2 == 0) "a" else "b"
                o.copy(
                  url = o.url.copy(
                    path = o.url.path,
                    from = Set(s"${c}_$TimeTag", "experiment_badcarousel")
                  )
                )
            }
        }

        assert(builder.buildYmlOffers(GrouppedAds, BuilderContext.Empty))(
          CustomAssertions.hasSameElementsAndOrder(expected)
        )
      },
      test("should return no more than 20 offers") {
        val offers = (1 to 40).map(rawYmlOffer)

        val builder =
          getBuilder {
            new CarouselMock(
              Seq(
                offers.take(20),
                offers.drop(20)
              )
            )
          }

        assertTrue(builder.buildYmlOffers(GrouppedAds, BuilderContext.Empty).size == 20)
      }
    )
}

object ExperimentCarouselUrlViewBuilderSpec extends MockitoSupport {

  final class CarouselMock(
    returns: Seq[Seq[RawYmlOffer]]
  ) extends UrlViewBuilder.Service[BuilderContext.CarouselContext] {

    private val calls = new AtomicInteger(0)

    override def buildYmlOffers(ads: GrouppedByUrlAds, ctx: BuilderContext.CarouselContext): Seq[RawYmlOffer] = {

      if (calls.incrementAndGet() > returns.size) {
        throw new RuntimeException(s"Broken spec. Expected only ${returns.size} calls")
      }

      returns(calls.get() - 1)
    }
  }

  val RandomMock: Random = {
    val res = mock[Random]

    when(res.nextBoolean()).thenReturn(true)
    res
  }

  private val TimeTag: String = "time_tag"

  private def getBuilder(
    carousel: UrlViewBuilder.Service[BuilderContext.CarouselContext]
  ): UrlViewBuilder.Service[BuilderContext.Empty] =
    new ExperimentCarouselUrlViewBuilder(carousel, RandomMock, TimeTag)

  private val GrouppedAds =
    GrouppedByUrlAds(
      ExtractedSourceUrl("", "", "", null),
      Seq.empty,
      0,
      Seq.empty
    )
}
