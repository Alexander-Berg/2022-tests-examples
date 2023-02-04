package ru.vertistraf.cost_plus.builder.auto.service

import ru.vertistraf.common.testkit.CustomAssertions
import ru.vertistraf.cost_plus.builder.auto.service.Collapsing.Collapsing
import ru.vertistraf.cost_plus.builder.auto.testkit.StubIdSupplier
import ru.vertistraf.cost_plus.builder.model.distribution.DistributionTarget
import ru.vertistraf.cost_plus.builder.model.thumb.{CostPlusThumb, ThumbUrlPath}
import ru.vertistraf.cost_plus.builder.reducer.CostPlusReducer.UrlAsKey
import ru.vertistraf.cost_plus.model.ServiceSetInfo.AutoSetInfo.Collapse
import ru.vertistraf.cost_plus.model.auto.Section
import ru.vertistraf.cost_plus.model.{CarouselImage, Review, ServiceOffer}
import zio._
import zio.random.Random
import zio.stream.ZStream
import zio.test._

object CollapsingSpec extends DefaultRunnableSpec {

  final case class TestCase(
      cf: Collapse,
      url: String,
      offers: Seq[ServiceOffer.Auto.Car],
      expected: Seq[CostPlusThumb.Auto])

  private def baseTestM[R: Tag](
      name: String
    )(testCase: URIO[R with Random, TestCase]
    )(implicit ev: Has.Union[Collapsing with Random, R]) =
    testM(name) {

      testCase
        .flatMap { tc =>
          ZIO
            .accessM[Collapsing](
              _.get.collapseCars(
                UrlAsKey(
                  tc.url,
                  "test tile",
                  tc.cf
                ),
                ZStream.fromIterable(tc.offers)
              )
            )
            .map(assert(_)(CustomAssertions.seqEquals(tc.expected, loggedPrefix = Some(name))))
        }
        .provideLayer(
          (UrlModifier.live ++ StubIdSupplier.layer >>> Collapsing.live) ++ Random.live ++ ZLayer.requires[R]
        )
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Collapsing")(
      baseTestM[Sized]("should not collapse by model if there are not enough models") {
        val setUrlPath = "/moskva/cars/audi/all/"

        def expectedThumbs(id: Long, price: Long, model: String) =
          CostPlusThumb.Auto.CarOfferThumb(
            urlPath = ThumbUrlPath.Default.copy(
              pinnedOfferId = Some(id.toString),
              utmSource = Set("landings"),
              utmMedium = Set(DistributionTarget.Auto.Marks.prefix),
              utmTerm = Some(s"utm-term-audi")
            ),
            price = price,
            vendor = "audi vendor",
            imageWithName = CarouselImage(s"img.host/offer/$id", s"offer $id title"),
            review = Review(100, s"review.host/audi/$model"),
            categoryId = 1,
            target = DistributionTarget.Auto.Marks
          )

        for {
          a3 <- generateN(3) {
            offerGen("AUDI", "A3", id = Some(3), relevance = Some(3), price = Some(3))
          }
          a5 <- generateN(1) {
            offerGen("AUDI", "A5", id = Some(5), relevance = Some(5), price = Some(5))
          }
          a7 <- generateN(2) {
            offerGen("AUDI", "A7", id = Some(7), relevance = Some(7), price = Some(7))
          }
          offers <- zio.random.shuffle((a3 ++ a5 ++ a7).toList).map(_.toSeq)

          expected = Seq(
            expectedThumbs(7, 7, "a7"),
            expectedThumbs(7, 7, "a7"),
            expectedThumbs(5, 5, "a5"),
            expectedThumbs(3, 3, "a3"),
            expectedThumbs(3, 3, "a3"),
            expectedThumbs(3, 3, "a3")
          )
        } yield TestCase(Collapse.ByModel(Some("AUDI")), setUrlPath, offers, expected)
      },
      baseTestM[Sized]("should correctly collapse by model if there are enough models") {
        val mark = "audi"
        val setUrlPath = "/moskva/cars/audi/all/"

        def expectedCollapsed(model: String, count: Int, minPrice: Long) =
          CostPlusThumb.Auto.CollapsedCarThumb(
            urlPath = ThumbUrlPath.Default.copy(
              overrideSetPath = Some(s"/moskva/cars/$mark/$model/all/"),
              utmSource = Set("landings"),
              utmMedium = Set(DistributionTarget.Auto.Marks.prefix),
              utmTerm = Some(s"utm-term-$mark"),
              from = Set("marks")
            ),
            minPrice = minPrice,
            vendor = Some(mark + " vendor"),
            imageWithName = CarouselImage(s"img.host/$mark/$model", s"$model"),
            categoryId = 1,
            target = DistributionTarget.Auto.Marks,
            offersCount = count,
            review = Some(
              Review(
                count = 100,
                url = s"review.host/$mark/$model"
              )
            )
          )

        for {
          a1 <- generateN(5) {
            offerGen("AUDI", "A1")
          }
          a3 <- generateN(20) {
            offerGen("AUDI", "A3")
          }
          a5 <- generateN(15) {
            offerGen("AUDI", "A5")
          }
          a7 <- generateN(10) {
            offerGen("AUDI", "A7")
          }
          offers <- zio.random.shuffle((a1 ++ a3 ++ a5 ++ a7).toList).map(_.toSeq)

          expected = Seq(
            expectedCollapsed("a3", 20, a3.map(_.price).min),
            expectedCollapsed("a5", 15, a5.map(_.price).min),
            expectedCollapsed("a7", 10, a7.map(_.price).min),
            expectedCollapsed("a1", 5, a1.map(_.price).min)
          )
        } yield TestCase(Collapse.ByModel(Some("AUDI")), setUrlPath, offers, expected)
      },
      baseTestM[Sized]("correctly collapse by mark") {
        val setUrlPath = "/moskva/cars/all/"

        def generateMarks(n: Int, mark: String) =
          generateN(n) {
            for {
              model <- Gen.alphaNumericStringBounded(5, 10)
              res <- offerGen(mark, model)
            } yield res
          }

        def expectedCollapsed(mark: String, offersCount: Int, minPrice: Long) =
          CostPlusThumb.Auto.CollapsedCarThumb(
            urlPath = ThumbUrlPath.Default.copy(
              overrideSetPath = Some(s"/moskva/cars/$mark/all/"),
              utmSource = Set("landings"),
              utmTerm = Some(s"utm-term-$mark"),
              utmMedium = Set(DistributionTarget.Auto.Commons.prefix)
            ),
            minPrice = minPrice,
            vendor = Some(mark + " vendor"),
            imageWithName = CarouselImage(s"img.host/$mark", s"$mark"),
            categoryId = 1,
            target = DistributionTarget.Auto.Commons,
            offersCount = offersCount,
            review = Some(
              Review(
                count = 200,
                url = s"review.host/$mark"
              )
            )
          )

        for {
          audi <- generateMarks(10, "AUDI")
          skoda <- generateMarks(20, "SKODA")
          lada <- generateMarks(30, "LADA")

          offers <- zio.random.shuffle((audi ++ skoda ++ lada).toList).map(_.toSeq)

          expected = Seq(
            expectedCollapsed("lada", 30, lada.map(_.price).min),
            expectedCollapsed("skoda", 20, skoda.map(_.price).min),
            expectedCollapsed("audi", 10, audi.map(_.price).min)
          )
        } yield TestCase(Collapse.ByMark, setUrlPath, offers, expected)
      },
      baseTestM[Sized]("correctly collapse by dealer") {
        val setUrlPath = "/dealer-net/kuncevo/"

        def generateDealerOffers(n: Int, dealer: String) =
          generateN(n) {
            for {
              markCode <- Gen.alphaNumericStringBounded(5, 10).map(_.toUpperCase)
              modelCode <- Gen.alphaNumericStringBounded(5, 10).map(_.toUpperCase)
              res <- offerGen(markCode, modelCode, dealer = Some(dealer))
            } yield res
          }

        def expectedCollapsed(dealer: String, offersCount: Int, minPrice: Long) =
          CostPlusThumb.Auto.CollapsedCarThumb(
            urlPath = ThumbUrlPath.Default.copy(
              overrideSetPath = Some(s"dealer.url/$dealer"),
              utmSource = Set("landings"),
              utmTerm = Some(s"utm-term-$dealer"),
              utmMedium = Set(DistributionTarget.Auto.Dealers.prefix),
              from = Set("1")
            ),
            minPrice = minPrice,
            vendor = None,
            imageWithName = CarouselImage(s"img.host/$dealer", s"$dealer title"),
            categoryId = 1,
            target = DistributionTarget.Auto.Dealers,
            offersCount = offersCount,
            review = None
          )

        for {
          d1 <- generateDealerOffers(10, "dealer1")
          d2 <- generateDealerOffers(20, "dealer2")
          d3 <- generateDealerOffers(30, "dealer3")

          offers <- zio.random.shuffle((d1 ++ d2 ++ d3).toList).map(_.toSeq)

          expected = Seq(
            expectedCollapsed("dealer3", 30, d3.map(_.price).min),
            expectedCollapsed("dealer2", 20, d2.map(_.price).min),
            expectedCollapsed("dealer1", 10, d1.map(_.price).min)
          )
        } yield TestCase(Collapse.ByDealer, setUrlPath, offers, expected)
      },
      baseTestM[Sized]("should correctly collapse by model for tags") {
        val mark = "audi"
        val setUrlPath = "/moskva/cars/all/tag/comfort/"

        def expectedCollapsed(model: String, count: Int, minPrice: Long) =
          CostPlusThumb.Auto.CollapsedCarThumb(
            urlPath = ThumbUrlPath.Default.copy(
              overrideSetPath = Some(s"/moskva/cars/$mark/$model/all/"),
              utmSource = Set("landings"),
              utmMedium = Set(DistributionTarget.Auto.Tags.prefix),
              utmTerm = Some(s"utm-term-$mark"),
              from = Set("tags", StubIdSupplier.ReturningString)
            ),
            minPrice = minPrice,
            vendor = Some(mark + " vendor"),
            imageWithName = CarouselImage(s"img.host/$mark/$model", s"$mark $model"),
            categoryId = 1,
            target = DistributionTarget.Auto.Tags,
            offersCount = count,
            review = Some(
              Review(
                count = 100,
                url = s"review.host/$mark/$model"
              )
            )
          )

        for {
          a3 <- generateN(20) {
            offerGen("AUDI", "A3")
          }
          a5 <- generateN(15) {
            offerGen("AUDI", "A5")
          }
          a7 <- generateN(10) {
            offerGen("AUDI", "A7")
          }
          offers <- zio.random.shuffle((a3 ++ a5 ++ a7).toList).map(_.toSeq)
          expected = Seq(
            expectedCollapsed("a3", 20, a3.map(_.price).min),
            expectedCollapsed("a5", 15, a5.map(_.price).min),
            expectedCollapsed("a7", 10, a7.map(_.price).min)
          )
        } yield TestCase(Collapse.Tags(geoCode = Some("moskva"), state = Section.All), setUrlPath, offers, expected)

      },
      baseTestM[Sized]("should correctly collapse by model for url without mark") {
        val mark = "audi"
        val setUrlPath = "/moskva/cars/all/color-red/"

        def expectedCollapsed(model: String, count: Int, minPrice: Long) =
          CostPlusThumb.Auto.CollapsedCarThumb(
            urlPath = ThumbUrlPath.Default.copy(
              overrideSetPath = Some(s"/moskva/cars/$mark/$model/all/color-red/"),
              utmSource = Set("landings"),
              utmMedium = Set(DistributionTarget.Auto.Commons.prefix),
              utmTerm = Some(s"utm-term-$mark"),
              from = Set("without_marks")
            ),
            minPrice = minPrice,
            vendor = Some(mark + " vendor"),
            imageWithName = CarouselImage(s"img.host/$mark/$model", s"$mark $model"),
            categoryId = 1,
            target = DistributionTarget.Auto.Commons,
            offersCount = count,
            review = Some(
              Review(
                count = 100,
                url = s"review.host/$mark/$model"
              )
            )
          )

        for {
          a3 <- generateN(20) {
            offerGen("AUDI", "A3")
          }
          a5 <- generateN(15) {
            offerGen("AUDI", "A5")
          }
          a7 <- generateN(10) {
            offerGen("AUDI", "A7")
          }
          offers <- zio.random.shuffle((a3 ++ a5 ++ a7).toList).map(_.toSeq)
          expected = Seq(
            expectedCollapsed("a3", 20, a3.map(_.price).min),
            expectedCollapsed("a5", 15, a5.map(_.price).min),
            expectedCollapsed("a7", 10, a7.map(_.price).min)
          )
        } yield TestCase(
          Collapse.ByModel(None),
          setUrlPath,
          offers,
          expected
        )
      },
      baseTestM[Sized]("should correctly collapse for vendors") {
        val setUrlPath = "/cars/vendor-european/used/drive-forward_wheel/"

        def expectedCollapsed(mark: String, count: Int, minPrice: Long) =
          CostPlusThumb.Auto.CollapsedCarThumb(
            urlPath = ThumbUrlPath.Default.copy(
              overrideSetPath = Some(s"/cars/$mark/used/drive-forward_wheel/"),
              utmSource = Set("landings"),
              utmMedium = Set(DistributionTarget.Auto.Vendors.prefix),
              utmTerm = Some(s"utm-term-$mark"),
              from = Set("vendors", StubIdSupplier.ReturningString)
            ),
            minPrice = minPrice,
            vendor = Some(mark + " vendor"),
            imageWithName = CarouselImage(s"img.host/$mark", s"$mark"),
            categoryId = 1,
            target = DistributionTarget.Auto.Vendors,
            offersCount = count,
            review = Some(
              Review(
                count = 200,
                url = s"review.host/$mark"
              )
            )
          )

        for {
          a3 <- generateN(20) {
            offerGen("AUDI", "A3")
          }
          a5 <- generateN(15) {
            offerGen("AUDI", "A5")
          }
          audi = a3 ++ a5
          skoda <- generateN(15) {
            offerGen("SKODA", "RAPID")
          }
          bmw <- generateN(10) {
            offerGen("BMW", "3")
          }
          offers <- zio.random.shuffle((audi ++ skoda ++ bmw).toList).map(_.toSeq)
          expected = Seq(
            expectedCollapsed("audi", 35, audi.map(_.price).min),
            expectedCollapsed("skoda", 15, skoda.map(_.price).min),
            expectedCollapsed("bmw", 10, bmw.map(_.price).min)
          )
        } yield TestCase(
          Collapse.Vendor(vendorCode = "european"),
          setUrlPath,
          offers,
          expected
        )
      }
    )
}
