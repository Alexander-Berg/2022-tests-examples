package ru.vertistraf.cost_plus.builder.reducer

import common.tagged.tag
import common.yt.live.operations.Typing.{tableIndex, TableIndex}
import ru.vertistraf.cost_plus.builder.model.distribution.DistributionTarget
import ru.vertistraf.cost_plus.builder.model.mapper.OfferWithSet
import ru.vertistraf.cost_plus.builder.model.reducer.ReducerOutput
import ru.vertistraf.cost_plus.builder.model.thumb.{CostPlusThumb, ThumbUrlPath}
import ru.vertistraf.cost_plus.builder.model.view.UrlView
import ru.vertistraf.cost_plus.model.ServiceSetInfo.AutoSetInfo
import ru.vertistraf.cost_plus.model.result.{CostPlusOffer, CostPlusOfferRow, CostPlusPrice, CostPlusSet}
import ru.vertistraf.cost_plus.model._
import ru.vertistraf.cost_plus.builder.reducer.CostPlusReducer.UrlAsKey
import ru.vertistraf.cost_plus.builder.service.{IdSupplier, ToCostPlusOfferConverter, UrlBuilder, UrlViewBuilder}
import ru.yandex.inside.yt.kosher.impl.operations.StatisticsImpl
import ru.yandex.inside.yt.kosher.operations.reduce.Reducer
import ru.yandex.inside.yt.kosher.operations.{OperationContext, Yield}
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._
import zio.test.mock.{Expectation, Mock}
import zio.{stream, Has, Task, UIO, ULayer, URLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters._

object CostPlusReducerSpec extends DefaultRunnableSpec {

  private val RelevanceParam = "relev"

  implicit class StringSugar(val s: String) extends AnyVal {
    def asId: Id = tag[IdTag][String](s)
  }

  implicit class ReducerSugar[I, O](val reducer: Reducer[I, O]) extends AnyVal {

    // читаем только из 0 таблицы
    def reduceAndGetFirstOutput(input: Seq[I]): Seq[O] = {
      val index = tableIndex(0)

      reduce(input)
        .filter(_._1 == index)
        .map(_._2)
    }

    def reduce(input: Seq[I]): Seq[(TableIndex, O)] = {
      val b = Seq.newBuilder[(TableIndex, O)]

      val `yield` = new Yield[O] {
        override def `yield`(index: Int, value: O): Unit = {
          val e: (TableIndex, O) = tableIndex(index) -> value
          b += e
        }

        override def close(): Unit = ()
      }

      reducer.reduce(input.iterator.asJava, `yield`, new StatisticsImpl, new OperationContext())

      b.result()
    }

  }

  private object SpecIdSupplier extends IdSupplier.Service {
    override def get(string: String): UIO[Id] = UIO.effectTotal(string.asId)
  }

  private object SpecToCostPlusConverter extends ToCostPlusOfferConverter.Service[CostPlusThumb.Auto] {

    override def convert(
        thumbId: Id,
        thumbUrl: String,
        computedRelevance: Int,
        thumb: CostPlusThumb.Auto,
        sets: Seq[CostPlusSet]): UIO[CostPlusOfferRow] =
      UIO.effectTotal {
        CostPlusOfferRow(
          offer = CostPlusOffer(
            id = thumbId,
            name = thumb.asInstanceOf[CostPlusThumb.Auto.CarOfferThumb].imageWithName.title,
            url = thumbUrl,
            price = CostPlusPrice.From(0),
            currency = "RUR",
            categoryId = 1,
            pictures = Seq.empty,
            vendor = None,
            description = None,
            params = Map(RelevanceParam -> computedRelevance.toString),
            sets = sets
          ),
          fileName = "file name"
        )
      }
  }

  object SpecUrlBuilder extends UrlBuilder.Service {
    override def buildSetUrls(setUrlPath: String): UIO[Seq[String]] = UIO.effectTotal(Seq.empty)

    override def buildThumbUrl(setUrlPath: String, thumbUrlPath: ThumbUrlPath): UIO[String] = UIO.effectTotal("url")
  }

  private def makeDeps(
      urlViewBuilder: UrlViewBuilder.Service[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto]) =
    CostPlusReducerDependencies[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto](
      idSupplier = SpecIdSupplier,
      urlViewBuilder = urlViewBuilder,
      toCostPlusConverter = SpecToCostPlusConverter,
      SpecUrlBuilder,
      onlyReduceOutput = false
    )

  final private class SpecReducer(
      urlViewBuilder: UrlViewBuilder.Service[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto])
    extends CostPlusReducer[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto](makeDeps(urlViewBuilder))

  private def makeOffersWithSet(urlPath: String, info: AutoSetInfo, count: Int): Seq[OfferWithSet.Auto] = {
    (0 until count).map { _ =>
      OfferWithSet(urlPath, s"title for $urlPath", info, ServiceOffer.Auto.Moto(1))
    }
  }

  private def runSpecReduce(input: Seq[OfferWithSet.Auto]) =
    for {
      viewBuilder <- ZIO
        .service[UrlViewBuilder.Service[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto]]
    } yield new SpecReducer(viewBuilder).reduceAndGetFirstOutput(input)

  type SpecUrlViewBuilder =
    Has[UrlViewBuilder.Service[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto]]

  object UrlViewBuilderMock
    extends Mock[Has[UrlViewBuilder.Service[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto]]] {

    type Url = UrlAsKey[ServiceSetInfo.AutoSetInfo]
    type Offers = zio.stream.Stream[Throwable, OfferWithSet.Auto]
    type Result = UrlView[CostPlusThumb.Auto]

    object Build extends Effect[(Url, Offers), Throwable, Result]

    private def tupleAssertion[A, B](a: Assertion[A], b: Assertion[B]): Assertion[(A, B)] =
      hasField[(A, B), A]("first", _._1, a) &&
        hasField[(A, B), B]("second", _._2, b)

    def expectedCall(
        url: Assertion[Url],
        offers: Assertion[zio.stream.Stream[Throwable, OfferWithSet.Auto]]
      )(f: ((Url, Offers)) => UrlView[CostPlusThumb.Auto]): Expectation[SpecUrlViewBuilder] = {
      Build(tupleAssertion(url, offers), valueF(f))
    }

    def expectedCallWithUrlAndOffersCount(
        urlPath: String,
        count: Int
      )(f: String => Result): Expectation[SpecUrlViewBuilder] = {

      Build(
        tupleAssertion(
          hasField("urlPath", _.urlPath, equalTo(urlPath)),
          anything
        ),
        valueM { case (url, offers) =>
          offers.runCount
            .flatMap { cnt =>
              ZIO.when(count != cnt.toInt)(ZIO.dieMessage(s"Expected $count offers for ${url.urlPath}, but found $cnt"))
            }
            .as(f(url.urlPath))
            .orDie
        }
      )

    }

    override val compose: URLayer[Has[mock.Proxy], SpecUrlViewBuilder] =
      ZLayer.fromServiceM { proxy =>
        withRuntime.map(rts => (reduceKey: Url, offers: Offers) => proxy(Build, reduceKey, offers))
      }
  }

  object DieUrlViewBuilder
    extends UrlViewBuilder.Service[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto] {

    override def build(
        reduceKey: UrlAsKey[AutoSetInfo],
        offers: stream.Stream[Throwable, OfferWithSet[AutoSetInfo, ServiceOffer.Auto]]): Task[UrlView[CostPlusThumb.Auto]] =
      ZIO.dieMessage("This is DieUrlViewBuilder")
  }

  private def makeCarousel(thumbs: CostPlusThumb.Auto*) =
    UrlView.makeCarouselOrEmpty {
      thumbs
    } match {
      case c: UrlView.Carousel[CostPlusThumb.Auto] => c
      case other => throw new RuntimeException(s"Bad configured spec. Not carousel $other")
    }

  private def makeThumb(url: ThumbUrlPath, name: String): CostPlusThumb.Auto =
    CostPlusThumb.Auto.CarOfferThumb(
      urlPath = url,
      price = 0,
      vendor = "",
      imageWithName = CarouselImage("", name),
      review = Review(1, "review"),
      categoryId = 1,
      target = DistributionTarget.Auto.ModelFilters
    )

  private def collectOffersOnlyOrThrow(output: Seq[ReducerOutput]): Seq[CostPlusOfferRow] =
    output.map {
      case ReducerOutput.Offer(row) => row
      case other => throw new RuntimeException(s"Expected offers only, but $other was found")
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CostPlusReducer")(
      // проверяет корректность вызовов метода makeReduce(urlKey, offers)
      testM("should be correctly called for different grouped offers") {
        val input =
          makeOffersWithSet(urlPath = "/url1/", AutoSetInfo.Carousel, 1) ++
            makeOffersWithSet(urlPath = "/url2/", AutoSetInfo.Carousel, 2) ++
            makeOffersWithSet(urlPath = "/url3/", AutoSetInfo.Carousel, 3)

        val builder =
          UrlViewBuilderMock.expectedCallWithUrlAndOffersCount("/url1/", 1)(_ => UrlView.Empty) ++
            UrlViewBuilderMock.expectedCallWithUrlAndOffersCount("/url2/", 2)(_ => UrlView.Empty) ++
            UrlViewBuilderMock.expectedCallWithUrlAndOffersCount("/url3/", 3)(_ => UrlView.Empty)

        runSpecReduce(input)
          .provideLayer(builder)
          .map(res => assertTrue(res.isEmpty))
      },
      testM("should return empty result when builder return empty view") {
        val input =
          makeOffersWithSet(urlPath = "/url1/", AutoSetInfo.Carousel, 20)

        val builder =
          UrlViewBuilderMock.expectedCallWithUrlAndOffersCount("/url1/", 20)(_ => UrlView.Empty)

        runSpecReduce(input)
          .provideLayer(builder)
          .map(res => assertTrue(res.isEmpty))
      },
      // проверяем, что релевантность для офферов проставляется исходя из порядка офферов от viewBuilder
      testM("should return carousel view with order as relevance") {
        val input =
          makeOffersWithSet(urlPath = "/url1/", AutoSetInfo.Carousel, 20)

        val builder =
          UrlViewBuilderMock.expectedCallWithUrlAndOffersCount("/url1/", 20) { _ =>
            makeCarousel(
              makeThumb(ThumbUrlPath.Default, "t1"),
              makeThumb(ThumbUrlPath.Default, "t3"),
              makeThumb(ThumbUrlPath.Default, "t2"),
              makeThumb(ThumbUrlPath.Default, "t4")
            )
          }

        runSpecReduce(input)
          .provideLayer(builder)
          .mapEffect(collectOffersOnlyOrThrow)
          .map(_.map(o => o.offer.name -> o.offer.params(RelevanceParam)).toMap)
          .map(res => assertTrue(res == Map("t1" -> "3", "t2" -> "1", "t3" -> "2", "t4" -> "0")))
      },
      testM("reducer should correctly return error on die and fail effects") {

        val failBuilder: ULayer[SpecUrlViewBuilder] = UrlViewBuilderMock.Build(anything, failure(new RuntimeException))
        val dieBuilder: ULayer[SpecUrlViewBuilder] = ZLayer.succeed(DieUrlViewBuilder)

        val builders =
          Iterable(
            failBuilder -> false,
            dieBuilder -> true
          )

        checkAllM(Gen.fromIterable(builders)) { case (builderLayer, isFatalError) =>
          val input =
            makeOffersWithSet(urlPath = "/url1/", AutoSetInfo.Carousel, 20)

          ZIO
            .service[UrlViewBuilder.Service[ServiceSetInfo.AutoSetInfo, ServiceOffer.Auto, CostPlusThumb.Auto]]
            .map(new SpecReducer(_))
            .map(_.reduce(input))
            .provideLayer(builderLayer)
            .map { res =>
              assertTrue(res.size == 1) &&
              assertTrue(res.head._2.isInstanceOf[ReducerOutput.UrlError]) &&
              assertTrue(res.head._2.asInstanceOf[ReducerOutput.UrlError].isFatal == isFatalError)
            }
        }

      }
    )
}
