package ru.yandex.vertis.general.globe.testkit

import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import general.globe.api.GeoServiceGrpc.GeoService
import general.globe.api._
import zio.{Has, Ref, Runtime, Task, UIO, ZIO, ZLayer, ZRef}
import zio.macros.accessible

import scala.concurrent.Future

@accessible
object TestGeoService {

  type TestGeoService = Has[Service]

  trait Service {
    def setListNearRegionsResponse(response: ListNearRegionsRequest => Task[ListNearRegionsResponse]): UIO[Unit]
    def setListChiefCitiesResponse(response: ListTopCitiesRequest => Task[ListTopCitiesResponse]): UIO[Unit]
    def setGetParentRegionsResponse(response: GetParentRegionsRequest => Task[GetParentRegionsResponse]): UIO[Unit]
    def setGetRegionsResponse(response: GetRegionsRequest => Task[GetRegionsResponse]): UIO[Unit]
    def setGetAllParentRegionsResponse(resonse: GetRegionsRequest => Task[GetRegionsResponse]): UIO[Unit]

    def setGetSearchableRegionsResponse(
        response: GetSearchableRegionsRequest => Task[GetSearchableRegionsResponse]
      ): UIO[Unit]
    def setSuggestRegionsResponse(response: SuggestRegionsRequest => Task[SuggestRegionsResponse]): UIO[Unit]

    def setSuggestMetroStationsResponse(
        response: SuggestMetroStationsRequest => Task[SuggestMetroStationsResponse]
      ): UIO[Unit]
    def setListDistrictsResponse(response: ListDistrictsRequest => Task[ListDistrictsResponse]): UIO[Unit]
    def setSuggestDistrictsResponse(response: SuggestDistrictsRequest => Task[SuggestDistrictsResponse]): UIO[Unit]
    def setSuggestItemsResponse(response: SuggestItemsRequest => Task[SuggestItemsResponse]): UIO[Unit]
    def setSuggestToponymsResponse(response: SuggestToponymsRequest => Task[SuggestToponymsResponse]): UIO[Unit]
    def setReverseGeocodeResponse(response: ReverseGeocodeRequest => Task[ReverseGeocodeResponse]): UIO[Unit]
    def setGetDistrictByIdResponse(response: GetDistrictByIdRequest => Task[GetDistrictByIdResponse]): UIO[Unit]

    def setGetMetroStationByIdResponse(
        response: GetMetroStationByIdRequest => Task[GetMetroStationByIdResponse]
      ): UIO[Unit]

    def setGetToponymDescriptions(
        response: GetToponymDescriptionsRequest => Task[GetToponymDescriptionsResponse]
      ): UIO[Unit]

    def setGetFederalSubjects(response: GetFederalSubjectsRequest => Task[GetFederalSubjectsResponse]): UIO[Unit]
  }

  val layer: ZLayer[Any, Nothing, TestGeoService with GrpcClient[GeoService]] = {
    val creationEffect = for {
      runtime <- ZIO.runtime[Any]
      listNearRegionsEffectRef <- ZRef.make[ListNearRegionsRequest => Task[ListNearRegionsResponse]] { _ =>
        ZIO.succeed(ListNearRegionsResponse())
      }
      listChiefCitiesEffectRef <- ZRef.make[ListTopCitiesRequest => Task[ListTopCitiesResponse]] { _ =>
        ZIO.succeed(ListTopCitiesResponse())
      }
      getParentRegionsEffectRef <- ZRef.make[GetParentRegionsRequest => Task[GetParentRegionsResponse]] { _ =>
        ZIO.succeed(GetParentRegionsResponse())
      }
      getAllParentRegionsEffectRef <- ZRef.make[GetRegionsRequest => Task[GetRegionsResponse]] { _ =>
        ZIO.succeed(GetRegionsResponse())
      }
      getRegionsEffectRef <- ZRef.make[GetRegionsRequest => Task[GetRegionsResponse]] { _ =>
        ZIO.succeed(GetRegionsResponse())
      }
      getSearchableRegionsEffectRef <- ZRef.make[GetSearchableRegionsRequest => Task[GetSearchableRegionsResponse]] {
        _ =>
          ZIO.succeed(GetSearchableRegionsResponse())
      }
      suggestRegionsEffectRef <- ZRef.make[SuggestRegionsRequest => Task[SuggestRegionsResponse]] { _ =>
        ZIO.succeed(SuggestRegionsResponse())
      }
      suggestMetroStationsEffectRef <- ZRef.make[SuggestMetroStationsRequest => Task[SuggestMetroStationsResponse]] {
        _ =>
          ZIO.succeed(SuggestMetroStationsResponse())
      }
      listMetroStationsEffectRef <- ZRef.make[ListMetroStationsRequest => Task[ListMetroStationsResponse]] { _ =>
        ZIO.succeed(ListMetroStationsResponse())
      }
      listDistrictsEffectRef <- ZRef.make[ListDistrictsRequest => Task[ListDistrictsResponse]] { _ =>
        ZIO.succeed(ListDistrictsResponse())
      }
      suggestDistrictsEffectRef <- ZRef.make[SuggestDistrictsRequest => Task[SuggestDistrictsResponse]] { _ =>
        ZIO.succeed(SuggestDistrictsResponse())
      }
      suggestItemsEffectRef <- ZRef.make[SuggestItemsRequest => Task[SuggestItemsResponse]] { _ =>
        ZIO.succeed(SuggestItemsResponse())
      }
      suggestToponymsEffectRef <- ZRef.make[SuggestToponymsRequest => Task[SuggestToponymsResponse]] { _ =>
        ZIO.succeed(SuggestToponymsResponse())
      }
      reverseGeocodeEffectRef <- ZRef.make[ReverseGeocodeRequest => Task[ReverseGeocodeResponse]] { _ =>
        ZIO.succeed(ReverseGeocodeResponse())
      }
      getDistrictByIdEffectRef <- ZRef.make[GetDistrictByIdRequest => Task[GetDistrictByIdResponse]] { _ =>
        ZIO.succeed(GetDistrictByIdResponse())
      }
      getMetroStationByIdEffectRef <- ZRef.make[GetMetroStationByIdRequest => Task[GetMetroStationByIdResponse]] { _ =>
        ZIO.succeed(GetMetroStationByIdResponse())
      }
      getToponymDescriptionsEffectRef <-
        ZRef.make[GetToponymDescriptionsRequest => Task[GetToponymDescriptionsResponse]] { _ =>
          ZIO.succeed(GetToponymDescriptionsResponse())
        }
      getPositionInfoEffectRef <- ZRef.make[GetPositionInfoRequest => Task[GetPositionInfoResponse]] { _ =>
        ZIO.succeed(GetPositionInfoResponse())
      }
      getFederalSubjectsEffectRef <- ZRef.make[GetFederalSubjectsRequest => Task[GetFederalSubjectsResponse]] { _ =>
        ZIO.succeed(GetFederalSubjectsResponse())
      }

      responseSetter: Service = new ServiceImpl(
        listNearRegionsEffectRef,
        listChiefCitiesEffectRef,
        getParentRegionsEffectRef,
        getAllParentRegionsEffectRef,
        getRegionsEffectRef,
        getSearchableRegionsEffectRef,
        suggestRegionsEffectRef,
        suggestMetroStationsEffectRef,
        listDistrictsEffectRef,
        suggestDistrictsEffectRef,
        suggestItemsEffectRef,
        suggestToponymsEffectRef,
        reverseGeocodeEffectRef,
        getDistrictByIdEffectRef,
        getMetroStationByIdEffectRef,
        getToponymDescriptionsEffectRef,
        getFederalSubjectsEffectRef
      )

      grpcClient: GrpcClient.Service[GeoService] = new GrpcClientLive[GeoService](
        new GeoServiceImpl(
          runtime,
          listNearRegionsEffectRef,
          listChiefCitiesEffectRef,
          getParentRegionsEffectRef,
          getAllParentRegionsEffectRef,
          getRegionsEffectRef,
          getSearchableRegionsEffectRef,
          suggestRegionsEffectRef,
          suggestMetroStationsEffectRef,
          listMetroStationsEffectRef,
          listDistrictsEffectRef,
          suggestDistrictsEffectRef,
          suggestItemsEffectRef,
          suggestToponymsEffectRef,
          reverseGeocodeEffectRef,
          getDistrictByIdEffectRef,
          getMetroStationByIdEffectRef,
          getToponymDescriptionsEffectRef,
          getPositionInfoEffectRef,
          getFederalSubjectsEffectRef
        ),
        null
      )
    } yield Has.allOf(responseSetter, grpcClient)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(
      listNearRegionsEffectRef: Ref[ListNearRegionsRequest => Task[ListNearRegionsResponse]],
      listChiefCitiesEffectRef: Ref[ListTopCitiesRequest => Task[ListTopCitiesResponse]],
      getParentRegionsEffectRef: Ref[GetParentRegionsRequest => Task[GetParentRegionsResponse]],
      getAllParentRegionsEffectRef: Ref[GetRegionsRequest => Task[GetRegionsResponse]],
      getRegionsEffectRef: Ref[GetRegionsRequest => Task[GetRegionsResponse]],
      getSearchableRegionsEffectRef: Ref[GetSearchableRegionsRequest => Task[GetSearchableRegionsResponse]],
      suggestRegionsEffectRef: Ref[SuggestRegionsRequest => Task[SuggestRegionsResponse]],
      suggestMetroStationsEffectRef: Ref[SuggestMetroStationsRequest => Task[SuggestMetroStationsResponse]],
      listDistrictsEffectRef: Ref[ListDistrictsRequest => Task[ListDistrictsResponse]],
      suggestDistrictsEffectRef: Ref[SuggestDistrictsRequest => Task[SuggestDistrictsResponse]],
      suggestItemsEffectRef: Ref[SuggestItemsRequest => Task[SuggestItemsResponse]],
      suggestToponymsEffectRef: Ref[SuggestToponymsRequest => Task[SuggestToponymsResponse]],
      reverseGeocodeEffectRef: Ref[ReverseGeocodeRequest => Task[ReverseGeocodeResponse]],
      getDistrictByIdEffectRef: Ref[GetDistrictByIdRequest => Task[GetDistrictByIdResponse]],
      getMetroStationByIdEffectRef: Ref[GetMetroStationByIdRequest => Task[GetMetroStationByIdResponse]],
      getToponymDescriptionsEffectRef: Ref[GetToponymDescriptionsRequest => Task[GetToponymDescriptionsResponse]],
      getFederalSubjectsEffectRef: Ref[GetFederalSubjectsRequest => Task[GetFederalSubjectsResponse]])
    extends Service {

    override def setListNearRegionsResponse(
        response: ListNearRegionsRequest => Task[ListNearRegionsResponse]): UIO[Unit] =
      listNearRegionsEffectRef.set(response)

    override def setListChiefCitiesResponse(response: ListTopCitiesRequest => Task[ListTopCitiesResponse]): UIO[Unit] =
      listChiefCitiesEffectRef.set(response)

    override def setGetParentRegionsResponse(
        response: GetParentRegionsRequest => Task[GetParentRegionsResponse]): UIO[Unit] =
      getParentRegionsEffectRef.set(response)

    override def setGetRegionsResponse(response: GetRegionsRequest => Task[GetRegionsResponse]): UIO[Unit] =
      getRegionsEffectRef.set(response)

    override def setGetSearchableRegionsResponse(
        response: GetSearchableRegionsRequest => Task[GetSearchableRegionsResponse]): UIO[Unit] =
      getSearchableRegionsEffectRef.set(response)

    override def setSuggestRegionsResponse(response: SuggestRegionsRequest => Task[SuggestRegionsResponse]): UIO[Unit] =
      suggestRegionsEffectRef.set(response)

    override def setSuggestMetroStationsResponse(
        response: SuggestMetroStationsRequest => Task[SuggestMetroStationsResponse]): UIO[Unit] =
      suggestMetroStationsEffectRef.set(response)

    override def setListDistrictsResponse(response: ListDistrictsRequest => Task[ListDistrictsResponse]): UIO[Unit] =
      listDistrictsEffectRef.set(response)

    override def setSuggestDistrictsResponse(
        response: SuggestDistrictsRequest => Task[SuggestDistrictsResponse]): UIO[Unit] =
      suggestDistrictsEffectRef.set(response)

    override def setSuggestItemsResponse(
        response: SuggestItemsRequest => Task[SuggestItemsResponse]): UIO[Unit] =
      suggestItemsEffectRef.set(response)

    override def setSuggestToponymsResponse(
        response: SuggestToponymsRequest => Task[SuggestToponymsResponse]): UIO[Unit] =
      suggestToponymsEffectRef.set(response)

    override def setReverseGeocodeResponse(response: ReverseGeocodeRequest => Task[ReverseGeocodeResponse]): UIO[Unit] =
      reverseGeocodeEffectRef.set(response)

    override def setGetDistrictByIdResponse(
        response: GetDistrictByIdRequest => Task[GetDistrictByIdResponse]): UIO[Unit] =
      getDistrictByIdEffectRef.set(response)

    override def setGetMetroStationByIdResponse(
        response: GetMetroStationByIdRequest => Task[GetMetroStationByIdResponse]): UIO[Unit] =
      getMetroStationByIdEffectRef.set(response)

    override def setGetToponymDescriptions(
        response: GetToponymDescriptionsRequest => Task[GetToponymDescriptionsResponse]): UIO[Unit] =
      getToponymDescriptionsEffectRef.set(response)

    override def setGetFederalSubjects(
        response: GetFederalSubjectsRequest => Task[GetFederalSubjectsResponse]): UIO[Unit] =
      getFederalSubjectsEffectRef.set(response)

    override def setGetAllParentRegionsResponse(response: GetRegionsRequest => Task[GetRegionsResponse]): UIO[Unit] =
      getAllParentRegionsEffectRef.set(response)
  }

  private class GeoServiceImpl(
      runtime: Runtime[Any],
      listNearRegionsEffectRef: Ref[ListNearRegionsRequest => Task[ListNearRegionsResponse]],
      listChiefCitiesEffectRef: Ref[ListTopCitiesRequest => Task[ListTopCitiesResponse]],
      getParentRegionsEffectRef: Ref[GetParentRegionsRequest => Task[GetParentRegionsResponse]],
      getAllParentRegionsEffectRef: Ref[GetRegionsRequest => Task[GetRegionsResponse]],
      getRegionsEffectRef: Ref[GetRegionsRequest => Task[GetRegionsResponse]],
      getSearchableRegionsEffectRef: Ref[GetSearchableRegionsRequest => Task[GetSearchableRegionsResponse]],
      suggestRegionsEffectRef: Ref[SuggestRegionsRequest => Task[SuggestRegionsResponse]],
      suggestMetroStationsEffectRef: Ref[SuggestMetroStationsRequest => Task[SuggestMetroStationsResponse]],
      listMetroStationsEffectRef: Ref[ListMetroStationsRequest => Task[ListMetroStationsResponse]],
      listDistrictsEffectRef: Ref[ListDistrictsRequest => Task[ListDistrictsResponse]],
      suggestDistrictsEffectRef: Ref[SuggestDistrictsRequest => Task[SuggestDistrictsResponse]],
      suggestItemsEffectRef: Ref[SuggestItemsRequest => Task[SuggestItemsResponse]],
      suggestToponymsEffectRef: Ref[SuggestToponymsRequest => Task[SuggestToponymsResponse]],
      reverseGeocodeEffectRef: Ref[ReverseGeocodeRequest => Task[ReverseGeocodeResponse]],
      getDistrictByIdEffectRef: Ref[GetDistrictByIdRequest => Task[GetDistrictByIdResponse]],
      getMetroStationByIdEffectRef: Ref[GetMetroStationByIdRequest => Task[GetMetroStationByIdResponse]],
      getToponymDescriptionsEffectRef: Ref[GetToponymDescriptionsRequest => Task[GetToponymDescriptionsResponse]],
      getPositionInfoEffectRef: Ref[GetPositionInfoRequest => Task[GetPositionInfoResponse]],
      getFederalSubjectsEffectRef: Ref[GetFederalSubjectsRequest => Task[GetFederalSubjectsResponse]])
    extends GeoService {

    override def listNearRegions(request: ListNearRegionsRequest): Future[ListNearRegionsResponse] =
      runtime.unsafeRunToFuture(listNearRegionsEffectRef.get.flatMap(_(request)))

    override def listChiefCities(request: ListTopCitiesRequest): Future[ListTopCitiesResponse] =
      runtime.unsafeRunToFuture(listChiefCitiesEffectRef.get.flatMap(_(request)))

    override def getParentRegions(request: GetParentRegionsRequest): Future[GetParentRegionsResponse] =
      runtime.unsafeRunToFuture(getParentRegionsEffectRef.get.flatMap(_(request)))

    override def getRegions(request: GetRegionsRequest): Future[GetRegionsResponse] =
      runtime.unsafeRunToFuture(getRegionsEffectRef.get.flatMap(_(request)))

    override def getAllParentRegions(request: GetRegionsRequest): Future[GetRegionsResponse] =
      runtime.unsafeRunToFuture(getAllParentRegionsEffectRef.get.flatMap(_(request)))

    override def getSearchableRegions(request: GetSearchableRegionsRequest): Future[GetSearchableRegionsResponse] =
      runtime.unsafeRunToFuture(getSearchableRegionsEffectRef.get.flatMap(_(request)))

    override def suggestRegions(request: SuggestRegionsRequest): Future[SuggestRegionsResponse] =
      runtime.unsafeRunToFuture(suggestRegionsEffectRef.get.flatMap(_(request)))

    override def suggestMetroStations(request: SuggestMetroStationsRequest): Future[SuggestMetroStationsResponse] =
      runtime.unsafeRunToFuture(suggestMetroStationsEffectRef.get.flatMap(_(request)))

    override def listMetroStations(request: ListMetroStationsRequest): Future[ListMetroStationsResponse] =
      runtime.unsafeRunToFuture(listMetroStationsEffectRef.get.flatMap(_(request)))

    override def listDistricts(request: ListDistrictsRequest): Future[ListDistrictsResponse] =
      runtime.unsafeRunToFuture(listDistrictsEffectRef.get.flatMap(_(request)))

    override def suggestDistricts(request: SuggestDistrictsRequest): Future[SuggestDistrictsResponse] =
      runtime.unsafeRunToFuture(suggestDistrictsEffectRef.get.flatMap(_(request)))

    override def suggestItems(request: SuggestItemsRequest): Future[SuggestItemsResponse] =
      runtime.unsafeRunToFuture(suggestItemsEffectRef.get.flatMap(_(request)))

    override def suggestToponyms(request: SuggestToponymsRequest): Future[SuggestToponymsResponse] =
      runtime.unsafeRunToFuture(suggestToponymsEffectRef.get.flatMap(_(request)))

    override def reverseGeocode(request: ReverseGeocodeRequest): Future[ReverseGeocodeResponse] =
      runtime.unsafeRunToFuture(reverseGeocodeEffectRef.get.flatMap(_(request)))

    override def getMetroStationById(request: GetMetroStationByIdRequest): Future[GetMetroStationByIdResponse] =
      runtime.unsafeRunToFuture(getMetroStationByIdEffectRef.get.flatMap(_(request)))

    override def getDistrictById(request: GetDistrictByIdRequest): Future[GetDistrictByIdResponse] =
      runtime.unsafeRunToFuture(getDistrictByIdEffectRef.get.flatMap(_(request)))

    override def getToponymDescriptions(
        request: GetToponymDescriptionsRequest): Future[GetToponymDescriptionsResponse] =
      runtime.unsafeRunToFuture(getToponymDescriptionsEffectRef.get.flatMap(_(request)))

    override def getPositionInfo(request: GetPositionInfoRequest): Future[GetPositionInfoResponse] =
      runtime.unsafeRunToFuture(getPositionInfoEffectRef.get.flatMap(_(request)))

    override def getNearestMetroStation(
        request: GetNearestMetroStationRequest): Future[GetNearestMetroStationResponse] = ???

    override def getDistrictByPosition(request: GetDistrictByPositionRequest): Future[GetDistrictByPositionResponse] =
      ???

    override def getFederalSubjects(request: GetFederalSubjectsRequest): Future[GetFederalSubjectsResponse] =
      runtime.unsafeRunToFuture(getFederalSubjectsEffectRef.get.flatMap(_(request)))

    override def geocode(request: GeocodeRequest): Future[GeocodeResponse] = ???
  }
}
