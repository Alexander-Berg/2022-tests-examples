package ru.yandex.vertis.general.bonsai.testkit

import common.collections.syntax._
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import general.bonsai.public_api.PublicBonsaiServiceGrpc.PublicBonsaiService
import general.bonsai.public_api._
import zio._
import zio.macros.accessible

import scala.concurrent.Future

@accessible
object TestBonsaiService {

  type TestBonsaiService = Has[Service]

  trait Service {
    def setListCategoriesResponse(response: ListCategoriesRequest => Task[CategoriesMapResponse]): UIO[Unit]
    def setSearchCategoriesResponse(response: SearchCategoriesRequest => Task[CategoriesMapResponse]): UIO[Unit]
    def setBreadcrumbsResponse(response: BreadcrumbsRequest => Task[BreadcrumbsResponse]): UIO[Unit]
    def setGetAttributesResponse(response: GetAttributesRequest => Task[GetAttributesResponse]): UIO[Unit]
    def setGetCategoryResponse(response: GetCategoryRequest => Task[GetCategoryResponse]): UIO[Unit]
  }

  val layer: ULayer[GrpcClient[PublicBonsaiService] with TestBonsaiService] = {
    val creationEffect = for {
      runtime <- ZIO.runtime[Any]
      listCategoriesEffectRef <- ZRef.make[ListCategoriesRequest => Task[CategoriesMapResponse]] { request =>
        val categories = request.entityRefs.map(r => TestData.category(r.id))
        ZIO.succeed(CategoriesMapResponse(categories.toMapWithKey(_.getCategory.id)))
      }
      searchCategoriesEffectRef <- ZRef.make[SearchCategoriesRequest => Task[CategoriesMapResponse]] { _ =>
        ZIO.succeed(CategoriesMapResponse())
      }
      breadcrumbsEffectRef <- ZRef.make[BreadcrumbsRequest => Task[BreadcrumbsResponse]] { _ =>
        ZIO.succeed(BreadcrumbsResponse())
      }
      getAttributesEffectRef <- ZRef.make[GetAttributesRequest => Task[GetAttributesResponse]] { _ =>
        ZIO.succeed(GetAttributesResponse())
      }
      getCategoryEffectRef <- ZRef.make[GetCategoryRequest => Task[GetCategoryResponse]] { _ =>
        ZIO.succeed(GetCategoryResponse())
      }
      responseSetter: Service = new ServiceImpl(
        listCategoriesEffectRef,
        searchCategoriesEffectRef,
        breadcrumbsEffectRef,
        getAttributesEffectRef,
        getCategoryEffectRef
      )
      grpcClient: GrpcClient.Service[PublicBonsaiService] = new GrpcClientLive[PublicBonsaiService](
        new PublicBonsaiServiceImpl(
          runtime,
          listCategoriesEffectRef,
          searchCategoriesEffectRef,
          breadcrumbsEffectRef,
          getAttributesEffectRef,
          getCategoryEffectRef
        ),
        null
      )
    } yield Has.allOf(responseSetter, grpcClient)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(
      listCategoriesEffectRef: Ref[ListCategoriesRequest => Task[CategoriesMapResponse]],
      searchCategoriesEffectRef: Ref[SearchCategoriesRequest => Task[CategoriesMapResponse]],
      breadcrumbsEffectRef: Ref[BreadcrumbsRequest => Task[BreadcrumbsResponse]],
      getAttributesEffectRef: Ref[GetAttributesRequest => Task[GetAttributesResponse]],
      getCategoryEffectRef: Ref[GetCategoryRequest => Task[GetCategoryResponse]])
    extends Service {

    override def setListCategoriesResponse(
        response: ListCategoriesRequest => Task[CategoriesMapResponse]): UIO[Unit] =
      listCategoriesEffectRef.set(response)

    override def setSearchCategoriesResponse(
        response: SearchCategoriesRequest => Task[CategoriesMapResponse]): UIO[Unit] =
      searchCategoriesEffectRef.set(response)

    override def setBreadcrumbsResponse(response: BreadcrumbsRequest => Task[BreadcrumbsResponse]): UIO[Unit] =
      breadcrumbsEffectRef.set(response)

    override def setGetAttributesResponse(response: GetAttributesRequest => Task[GetAttributesResponse]): UIO[Unit] =
      getAttributesEffectRef.set(response)

    override def setGetCategoryResponse(response: GetCategoryRequest => Task[GetCategoryResponse]): UIO[Unit] =
      getCategoryEffectRef.set(response)
  }

  private class PublicBonsaiServiceImpl(
      runtime: Runtime[Any],
      listCategoriesEffectRef: Ref[ListCategoriesRequest => Task[CategoriesMapResponse]],
      searchCategoriesEffectRef: Ref[SearchCategoriesRequest => Task[CategoriesMapResponse]],
      breadcrumbsEffectRef: Ref[BreadcrumbsRequest => Task[BreadcrumbsResponse]],
      getAttributesEffectRef: Ref[GetAttributesRequest => Task[GetAttributesResponse]],
      getCategoryEffectRef: Ref[GetCategoryRequest => Task[GetCategoryResponse]])
    extends PublicBonsaiService {

    override def listCategories(request: ListCategoriesRequest): Future[CategoriesMapResponse] =
      runtime.unsafeRunToFuture(listCategoriesEffectRef.get.map(_(request)).flatten)

    override def searchCategories(request: SearchCategoriesRequest): Future[CategoriesMapResponse] =
      runtime.unsafeRunToFuture(searchCategoriesEffectRef.get.map(_(request)).flatten)

    override def breadcrumbs(request: BreadcrumbsRequest): Future[BreadcrumbsResponse] =
      runtime.unsafeRunToFuture(breadcrumbsEffectRef.get.map(_(request)).flatten)

    override def getAttributes(request: GetAttributesRequest): Future[GetAttributesResponse] =
      runtime.unsafeRunToFuture(getAttributesEffectRef.get.map(_(request)).flatten)

    override def getCategory(request: GetCategoryRequest): Future[GetCategoryResponse] =
      runtime.unsafeRunToFuture(getCategoryEffectRef.get.map(_(request)).flatten)

    override def suggestCategories(request: SuggestCategoryRequest): Future[CategoriesListResponse] = ???
  }
}
