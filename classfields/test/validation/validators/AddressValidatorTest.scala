package ru.yandex.vertis.general.gost.logic.test.validation.validators

import common.geobase.model.RegionIds.RegionId
import common.zio.grpc.client.GrpcClient
import general.globe.api.GeoServiceGrpc.GeoService
import general.globe.api.{GetFederalSubjectsResponse, GetRegionsResponse}
import general.globe.model.Region
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit.validatorTest
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.logic.validation.validators.AddressValidator
import ru.yandex.vertis.general.gost.model.SellingAddress
import ru.yandex.vertis.general.gost.model.SellingAddress.{AddressInfo, RegionInfo}
import ru.yandex.vertis.general.gost.model.validation.fields._
import common.zio.logging.Logging
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZLayer}

object AddressValidatorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AddressValidator")(
      validatorTest("Fail validation when has empty addresses")(
        _.copy(addresses = Seq.empty)
      )(contains(AddressesEmpty)),
      validatorTest("Fail validation when more than maximum addresses presented")(
        _.copy(addresses =
          Seq.fill(AddressValidator.MaxAddressesPerOffer + 1)(
            SellingAddress(SellingAddress.GeoPoint(0, 0), None, None, None, None)
          )
        )
      )(contains(TooManyAddresses)),
      validatorTest("Fail when has address with non-town region")(
        _.copy(
          addresses = Seq(
            SellingAddress(
              SellingAddress.GeoPoint(0, 0),
              None,
              None,
              None,
              region = Some(RegionInfo(RegionId(10), isEnriched = false, name = "name", Some(false)))
            )
          )
        )
      )(contains(RegionInvalidType(0))),
      validatorTest("Не падать, если кроме regionId не являющегося населенным пунктом передали адрес")(
        _.copy(
          addresses = Seq(
            SellingAddress(
              SellingAddress.GeoPoint(0, 0),
              Some(AddressInfo("ул. Ленина")),
              None,
              None,
              region = Some(RegionInfo(RegionId(10), isEnriched = false, name = "name", Some(false)))
            )
          )
        )
      )(equalTo(Seq.empty)),
      validatorTest("Падать если регион не из России")(
        _.copy(
          addresses = Seq(
            SellingAddress(
              SellingAddress.GeoPoint(0, 0),
              None,
              None,
              None,
              region = Some(RegionInfo(RegionId(10000), isEnriched = false, name = "name", Some(false)))
            )
          )
        )
      )(contains(ForeignAddress(0))),
      validatorTest("Work fine with valid entity")(identity)(isEmpty)
    ).provideCustomLayer {
      val geoService = TestGeoService.layer
        .tap(_.get.setGetFederalSubjects { request =>
          ZIO.succeed(GetFederalSubjectsResponse(request.regionIds.map(id => id -> Region(id)).toMap))
        })
        .tap(_.get.setGetAllParentRegionsResponse { request =>
          ZIO.succeed(
            GetRegionsResponse(
              request.ids.map(id => (id -> Region(id = id, parentId = 225))).toMap ++ Map(
                10000L -> Region(id = 10000, parentId = 0)
              )
            )
          )
        })

      val requestCacher = (Cache.noop ++ Logging.live) >>> RequestCacher.live

      (requestCacher ++ geoService ++ Logging.live) >>>
        ZLayer.fromServices[RequestCacher.Service, GrpcClient.Service[GeoService], Logging.Service, Validator](
          (grpcCacher, geoService, logging) => new AddressValidator(grpcCacher, geoService, logging)
        )
    }
}
