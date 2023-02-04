package ru.yandex.vertis.general.search.testkit

import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import general.search.api.SearchServiceGrpc.SearchService
import general.search.api._
import zio.macros.accessible
import zio.{Has, Ref, Runtime, Task, UIO, ULayer, ZIO, ZRef}

import scala.concurrent.Future

@accessible
object TestSearchService {

  type TestSearchService = Has[Service]

  trait Service {
    def setSearchOffersResponse(response: SearchOffersRequest => Task[SearchOffersResponse]): UIO[Unit]
  }

  val layer: ULayer[GrpcClient[SearchService] with TestSearchService] = {
    val creationEffect = for {
      runtime <- ZIO.runtime[Any]
      searchOffersEffectRef <- ZRef.make[SearchOffersRequest => Task[SearchOffersResponse]] { _ =>
        ZIO.succeed(SearchOffersResponse())
      }

      responseSetter: Service = new ServiceImpl(
        searchOffersEffectRef
      )
      grpcClient: GrpcClient.Service[SearchService] = new GrpcClientLive[SearchService](
        new SearchServiceTestImpl(
          runtime,
          searchOffersEffectRef
        ),
        null
      )
    } yield Has.allOf(responseSetter, grpcClient)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(searchOffersEffectRef: Ref[SearchOffersRequest => Task[SearchOffersResponse]])
    extends Service {

    override def setSearchOffersResponse(response: SearchOffersRequest => Task[SearchOffersResponse]): UIO[Unit] =
      searchOffersEffectRef.set(response)
  }

  private class SearchServiceTestImpl(
      runtime: Runtime[Any],
      getOfferBatchEffectRef: Ref[SearchOffersRequest => Task[SearchOffersResponse]])
    extends SearchService {

    override def searchOffers(request: SearchOffersRequest): Future[SearchOffersResponse] =
      runtime.unsafeRunToFuture(getOfferBatchEffectRef.get.map(_(request)).flatten)

    override def searchStatistics(request: SearchStatisticsRequest): Future[SearchStatisticsResponse] = ???

    override def countOffers(request: CountOffersRequest): Future[CountOffersResponse] = ???

    override def calcFilters(request: FiltersRequest): Future[FiltersResponse] = ???

    override def calcOfflineFilters(request: OfflineFiltersRequest): Future[FiltersResponse] = ???

    override def searchCategories(request: SearchCategoriesRequest): Future[SearchCategoriesResponse] = ???

    override def countOffersInSnapshot(request: CountOffersInSnapshotRequest): Future[CountOffersInSnapshotResponse] =
      ???

    override def getRelatedSearches(request: GetRelatedSearchesRequest): Future[GetRelatedSearchesResponse] = ???

    override def countOffersByRegion(request: CountOffersByRegionRequest): Future[CountOffersByRegionResponse] = ???

    override def getCategoryPopularity(request: GetCategoryPopularityRequest): Future[GetCategoryPopularityResponse] =
      ???
  }

}
