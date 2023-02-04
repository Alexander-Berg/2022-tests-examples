package ru.vertistraf.cost_plus.builder.auto.reducer

import eu.timepit.refined.api.Refined
import ru.vertistraf.cost_plus.builder.auto.service.Collapsing.Collapsing
import ru.vertistraf.cost_plus.builder.auto.service.Tables.Tables
import ru.vertistraf.cost_plus.builder.auto.service.{AutoUrlViewBuilder, Collapsing, Tables}
import ru.vertistraf.cost_plus.builder.auto.testkit.{
  CollapsingReadAllAndReturnEmptyStub,
  TablesReadAllAndReturnEmptyStub
}
import ru.vertistraf.cost_plus.builder.model.distribution.DistributionTarget
import ru.vertistraf.cost_plus.builder.model.mapper.OfferWithSet
import ru.vertistraf.cost_plus.builder.model.thumb.{CostPlusThumb, ThumbUrlPath}
import ru.vertistraf.cost_plus.builder.model.view.UrlView
import ru.vertistraf.cost_plus.builder.reducer.CostPlusReducer.UrlAsKey
import ru.vertistraf.cost_plus.builder.service.UrlViewBuilder.UrlViewBuilder
import ru.vertistraf.cost_plus.model.ServiceSetInfo.AutoSetInfo
import ru.vertistraf.cost_plus.model.{CarouselImage, Review, ServiceOffer, ServiceSetInfo}
import zio._
import zio.prelude.NonEmptyList
import zio.stream._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock._

object AutoUrlViewBuilderSpec extends DefaultRunnableSpec {

  @mockable[Collapsing.Service]
  object CollapsingMock

  @mockable[Tables.Service]
  object TablesMock

  case class TestCase(
      url: UrlAsKey[ServiceSetInfo.AutoSetInfo],
      input: Seq[OfferWithSet.Auto],
      expected: Either[PartialFunction[Throwable, Unit], UrlView[
        CostPlusThumb.Auto
      ]],
      debugTag: Option[String] = None,
      collapsing: ULayer[Collapsing] = CollapsingMock.empty,
      tabling: ULayer[Tables] = TablesMock.empty)

  private def debugEqCheck(tag: String, expected: UrlView[CostPlusThumb.Auto]) =
    Assertion.assertion[UrlView[CostPlusThumb.Auto]]("debugCheck")() { actual =>
      def log(msg: String): Unit = println(s"[$tag][debug check] $msg")

      def logExpectedActual(title: String, expected: Any, actual: Any): Unit = {
        log(title)
        log(s"expected: $expected")
        log(s"actual:   $actual")
      }

      (actual, expected) match {
        case (UrlView.Carousel(ac), UrlView.Carousel(ec)) =>
          val a = ac.value
          val e = ec.value

          (0 until math.max(a.size, e.size)).foldLeft(true) {
            case (false, _) => false
            case (true, index) if index >= a.size || index >= e.size =>
              logExpectedActual("different sizes", e.size, a.size)
              false
            case (true, index) =>
              if (e(index) != a(index)) {
                logExpectedActual(s"Not equal at $index index", e(index), a(index))
                false
              } else {
                true
              }
          }
        case (e, a) =>
          logExpectedActual(s"inconsistent types", e, a)
          false
      }
    }

  private def matchesPf[A, B](pf: PartialFunction[A, B]): Assertion[A] =
    Assertion.assertion("matchesPf")() { actual =>
      pf.isDefinedAt(actual)
    }

  private def specAction(testCase: TestCase): UIO[TestResult] = {
    ZIO
      .accessM[UrlViewBuilder[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto]] {
        _.get
          .build(
            testCase.url,
            ZStream
              .fromIterable(testCase.input)
          )
          .run
          .map { res =>
            testCase.expected match {
              case Left(err) => assert(res)(fails(matchesPf(err)))
              case Right(value) =>
                assert(res) {
                  succeeds {
                    testCase.debugTag
                      .map {
                        debugEqCheck(_, value)
                      }
                      .getOrElse(equalTo(value))
                  }
                }
            }
          }
      }
      .provideLayer(testCase.collapsing ++ testCase.tabling >>> AutoUrlViewBuilder.live)
  }

  private def baseTest(name: String)(testCase: TestCase) =
    testM(name) {
      specAction(testCase)
    }

  private def makeOffersWithSet(url: UrlAsKey[AutoSetInfo])(offers: ServiceOffer.Auto*) =
    offers.map { o =>
      OfferWithSet(url.urlPath, url.title, url.setInfo, o)
    }

  private def stubCarServiceOffer(i: Int) = {
    val img = CarouselImage(
      url = i.toString,
      title = i.toString
    )

    val review = Review(i, "review-url" + i)

    ServiceOffer.Auto.Car(
      relevance = i,
      offerId = i.toString,
      price = i,
      vendor = "vendor" + i,
      offerImage = img,
      modelImage = img,
      markImage = img,
      dealerImage = None,
      tableImages = NonEmptyList("imgt"),
      modelReview = review,
      markReview = review,
      markUrlCode = "MARK-CODE" + i,
      modelUrlCode = "MODEL-CODE" + i,
      dealerDirectUrl = None,
      superGenId = Some(i),
      techParamId = i,
      configurationId = i,
      transmissionName = s"transmission-name-$i",
      complectationId = Some(i),
      complectationName = Some(s"complectation-name-$i"),
      enginePower = i,
      engineDisplacementLiters = i,
      utmTerm = Some(s"utm-term-$i")
    )
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("AutoUrlViewBuilderSpec")(
      baseTest("return empty table on empty offers") {
        TestCase(
          url = UrlAsKey(
            "path",
            "title",
            ServiceSetInfo.AutoSetInfo.Table.Complectation
          ),
          input = Seq.empty,
          expected = Right(UrlView.Empty),
          tabling = TablesReadAllAndReturnEmptyStub.layer
        )
      },
      baseTest("return empty carousel on empty offers") {
        TestCase(
          url = UrlAsKey(
            "path",
            "title",
            ServiceSetInfo.AutoSetInfo.Carousel
          ),
          input = Seq.empty,
          expected = Right(UrlView.Empty)
        )
      },
      baseTest("return empty carousel on 2 offers") {
        val url = UrlAsKey[AutoSetInfo](
          "path",
          "title",
          ServiceSetInfo.AutoSetInfo.Carousel
        )

        TestCase(
          url = url,
          input = makeOffersWithSet(url)(
            stubCarServiceOffer(1),
            stubCarServiceOffer(2)
          ),
          expected = Right(UrlView.Empty)
        )
      },
      baseTest("return carousel") {
        val url = UrlAsKey[AutoSetInfo](
          "path",
          "title",
          ServiceSetInfo.AutoSetInfo.Carousel
        )

        TestCase(
          url = url,
          input = makeOffersWithSet(url)(
            stubCarServiceOffer(1),
            stubCarServiceOffer(2),
            stubCarServiceOffer(3),
            stubCarServiceOffer(4)
          ),
          expected = Right(
            UrlView.Carousel(
              Refined.unsafeApply[Seq[CostPlusThumb.Auto], UrlView.CarouselRefine] {

                def thumbFromStub(i: Int) =
                  CostPlusThumb.Auto.CarOfferThumb(
                    urlPath = ThumbUrlPath.Default
                      .copy(
                        pinnedOfferId = Some(i.toString),
                        utmSource = Set("landings"),
                        utmMedium = Set("model_filters"),
                        utmTerm = Some(s"utm-term-$i")
                      ),
                    price = i,
                    vendor = "vendor" + i,
                    imageWithName = CarouselImage(i.toString, i.toString),
                    review = Review(i, "review-url" + i),
                    categoryId = 1,
                    target = DistributionTarget.Auto.ModelFilters
                  )

                Seq(
                  thumbFromStub(4),
                  thumbFromStub(3),
                  thumbFromStub(2),
                  thumbFromStub(1)
                )
              }
            )
          )
        )
      },
      baseTest("should fail on different collapsing features") {
        val url = UrlAsKey[AutoSetInfo](
          "path",
          "title",
          ServiceSetInfo.AutoSetInfo.Collapse.ByMark
        )

        val offers =
          makeOffersWithSet(url)(stubCarServiceOffer(1), stubCarServiceOffer(2), stubCarServiceOffer(3))

        TestCase(
          url = url,
          input = offers.take(2) :+ offers.last
            .copy(info = ServiceSetInfo.AutoSetInfo.Collapse.ByDealer),
          expected = Left {
            case AutoUrlViewBuilder.ViewBuilderException.DifferentCollapsingFeatures(
                  AutoSetInfo.Collapse.ByMark,
                  ServiceSetInfo.AutoSetInfo.Collapse.ByDealer,
                  _
                ) =>
              ()
          },
          collapsing = CollapsingReadAllAndReturnEmptyStub.layer
        )
      },
      baseTest("should correctly call Collapsing") {
        val url = UrlAsKey[AutoSetInfo](
          "path",
          "title",
          ServiceSetInfo.AutoSetInfo.Collapse.ByMark
        )

        val offers =
          makeOffersWithSet(url)(stubCarServiceOffer(1), stubCarServiceOffer(2), stubCarServiceOffer(3))

        TestCase(
          url = url,
          input = offers,
          expected = Right(UrlView.Empty),
          collapsing = CollapsingReadAllAndReturnEmptyStub.layer
        )
      }
    )
}
