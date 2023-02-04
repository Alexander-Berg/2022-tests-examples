package auto.c2b.reception.model.testkit

import auto.c2b.common.car_info.{CarInfo => CarInfoProto}
import auto.c2b.common.model.ApplicationTypes.{UserId, VIN}
import auto.c2b.common.model.{BuyOutAlg, PriceRange}
import auto.c2b.common.testkit.CommonGenerators._
import auto.c2b.reception.model.Application.InspectTime
import auto.c2b.reception.model.ProAutoReport.RequestId
import auto.c2b.reception.model._
import ru.auto.api.api_offer_model.Offer
import ru.auto.api.c2b.api_application_model.InspectPlace
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{Gen, Sized}

import java.time._

object Generators {

  object Application {

    implicit val proAutoReportReqIdGen: DeriveGen[RequestId] =
      DeriveGen.instance(Gen.anyUUID.map(id => ProAutoReport.RequestId(id.toString)))

    implicit val statusGen: DeriveGen[ApplicationStatus] = DeriveGen.gen[ApplicationStatus]
    implicit val algGen: DeriveGen[BuyOutAlg] = DeriveGen.gen[BuyOutAlg]
    implicit val placeGen: DeriveGen[InspectPlace] = DeriveGen.gen[InspectPlace]
    implicit val dataSourceGen: DeriveGen[DataSource] = DeriveGen.gen[DataSource]
    implicit val proAutoReportGen: DeriveGen[ProAutoReport] = DeriveGen.gen[ProAutoReport]
    implicit val cmeReportGen: DeriveGen[CMEReport] = DeriveGen.gen[CMEReport]
    implicit val inspectTimeAny: DeriveGen[InspectTime] = DeriveGen.instance(Gen.anyString.map(InspectTime(_)))
    implicit val carInfoGen: DeriveGen[CarInfoProto] = DeriveGen.gen[CarInfoProto]

    implicit val pricePredictionGen: Gen[Random, PriceRange] = for {
      from <- Gen.long(0, Long.MaxValue / 2)
      to <- Gen.long(from + 1, Long.MaxValue)
    } yield PriceRange(from, to)

    val applicationAny: Gen[Random with Sized, Application] = DeriveGen.gen[Application].derive

    val paramForNewApplicationAny: Gen[Random with Sized, (VIN, Offer, UserId, List[LocalDate], InspectTime, InspectPlace, PriceRange, Option[Int], Option[CarInfoProto], BuyOutAlg)] =
      DeriveGen
        .gen[
          (
              VIN,
              Offer,
              UserId,
              List[LocalDate],
              InspectTime,
              InspectPlace,
              PriceRange,
              Option[Int],
              Option[CarInfoProto],
              BuyOutAlg
          )
        ]
        .derive
  }
}
