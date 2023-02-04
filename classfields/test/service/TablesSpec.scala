package ru.vertistraf.cost_plus.builder.auto.service

import ru.vertistraf.cost_plus.builder.auto.service.Tables.Tables
import ru.vertistraf.cost_plus.builder.model.distribution.DistributionTarget
import ru.vertistraf.cost_plus.builder.model.thumb.{CostPlusThumb, ThumbUrlPath}
import ru.vertistraf.cost_plus.builder.reducer.CostPlusReducer.UrlAsKey
import ru.vertistraf.cost_plus.model.{Review, ServiceOffer, ServiceSetInfo}
import zio.prelude.NonEmptyList
import zio.random.Random
import zio.stream.ZStream
import zio.test.AssertionM.Render
import zio.test._
import zio.{Has, Tag, URIO, ZIO, ZLayer}

object TablesSpec extends DefaultRunnableSpec {

  final private case class TestCase(
      setInfo: ServiceSetInfo.AutoSetInfo.Table,
      url: String,
      offers: Seq[ServiceOffer.Auto.Car],
      expected: Tables.Result)

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Tables.Service")(
      baseTestM[Sized]("should correctly generate complectations") {
        val mark = "porsche"
        val model = "panamera"
        val superGenId = 22481288L
        val techParamId = 22481337L
        val configurationId = 22481337L
        val transmissionName = "Механическая"
        val complectationId = 20747718L
        val complectationName = "Prestige"
        val enginePower = 160
        val engineDisplacementLiters = 1.6
        val setUrlPath = s"/cars/$mark/$model/new/"

        def expectedRow(offersCount: Int, minPrice: Long): CostPlusThumb.Auto.CarTableRowThumb =
          CostPlusThumb.Auto.CarTableRowThumb(
            urlPath = ThumbUrlPath.Default.copy(
              overrideSetPath = Some(s"/cars/new/group/$mark/$model/$superGenId-$techParamId/"),
              catalogFilter = Map(
                "mark" -> mark,
                "model" -> model,
                "generation" -> superGenId.toString,
                "configuration" -> configurationId.toString,
                "tech_param" -> techParamId.toString,
                "complectation_name" -> complectationName
              ),
              utmSource = Set("landings"),
              utmMedium = Set(DistributionTarget.Auto.Complectations.prefix),
              utmTerm = Some(s"utm-term-$mark")
            ),
            categoryId = 1,
            vendor = s"$mark vendor",
            transmissionName = transmissionName,
            complectationName = complectationName,
            enginePower = enginePower,
            engineDisplacementLiters = engineDisplacementLiters,
            image = s"img.host/$mark/$model/1",
            price = minPrice,
            review = Review(count = 100, url = s"review.host/$mark/$model"),
            offersCount = offersCount,
            target = DistributionTarget.Auto.Complectations
          )

        def expectedHeader(minPrice: Long): CostPlusThumb.Auto.CarTableHeaderThumb =
          CostPlusThumb.Auto
            .CarTableHeaderThumb(
              urlPath = ThumbUrlPath.Default.copy(
                overrideSetPath = Some(setUrlPath),
                utmSource = Set("landings"),
                utmMedium = Set(DistributionTarget.Auto.Complectations.prefix),
                utmTerm = None
              ),
              categoryId = 1,
              vendor = s"$mark vendor",
              price = minPrice,
              images = NonEmptyList(
                s"img.host/$mark/$model/1",
                s"img.host/$mark/$model/2",
                s"img.host/$mark/$model/3",
                s"img.host/$mark/$model/4"
              ),
              target = DistributionTarget.Auto.Complectations
            )
            .get

        for {
          offers <- generateN(20) {
            offerGen(
              mark,
              model,
              superGenId = Some(superGenId),
              techParamId = Some(techParamId),
              configurationId = Some(configurationId),
              transmissionName = Some(transmissionName),
              complectationId = Some(complectationId),
              complectationName = Some(complectationName),
              enginePower = Some(enginePower),
              engineDisplacementLiters = Some(engineDisplacementLiters)
            )
          }
          minPrice = offers.map(_.price).min
          expected = Some(expectedHeader(minPrice) -> Seq(expectedRow(20, minPrice)))
        } yield TestCase(ServiceSetInfo.AutoSetInfo.Table.Complectation, setUrlPath, offers, expected)
      },
      baseTestM[Sized]("should return [[None]] if there are not enough images") {
        val mark = "porsche"
        val model = "panamera"
        val superGenId = 22481288L
        val techParamId = 22481337L
        val configurationId = 22481337L
        val transmissionName = "Механическая"
        val complectationName = "Prestige"
        val enginePower = 160
        val engineDisplacementLiters = 1.6
        val setUrlPath = s"/cars/$mark/$model/new/"

        for {
          offers <- generateN(3) {
            offerGen(
              mark,
              model,
              superGenId = Some(superGenId),
              techParamId = Some(techParamId),
              configurationId = Some(configurationId),
              transmissionName = Some(transmissionName),
              complectationName = Some(complectationName),
              enginePower = Some(enginePower),
              engineDisplacementLiters = Some(engineDisplacementLiters),
              tableImagesCount = 1
            )
          }
          expected = None: Tables.Result
        } yield TestCase(ServiceSetInfo.AutoSetInfo.Table.Complectation, setUrlPath, offers, expected)
      }
    )

  private def baseTestM[R: Tag](
      name: String
    )(testCase: URIO[R with Random, TestCase]
    )(implicit ev: Has.Union[Tables with Random, R]): ZSpec[Any with R, Any] =
    testM(name) {
      testCase
        .flatMap { tc =>
          ZIO
            .accessM[Tables](_.get.tableCars(UrlAsKey(tc.url, "title", tc.setInfo), ZStream.fromIterable(tc.offers)))
            .map(assert(_)(tablesResultAssertion(tc.expected)))
        }
        .provideLayer((UrlModifier.live >>> Tables.live) ++ Random.live ++ ZLayer.requires[R])
    }

  private def tablesResultAssertion(expected: Tables.Result): Assertion[Tables.Result] =
    Assertion.assertion("Tables.Result's assertion")(Render.param(expected)) { actual =>
      (actual, expected) match {
        case (None, None) => true
        case (Some((actualHeader, actualRows)), Some((expectedHeader, expectedRows))) =>
          actualHeader == expectedHeader && actualRows == expectedRows
        case _ => false
      }
    }
}
