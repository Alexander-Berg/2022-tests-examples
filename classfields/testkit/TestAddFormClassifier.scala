package ru.yandex.vertis.general.classifiers.addform.testkit

import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.GrpcClientLive
import general.classifiers.add_form_classifier_api.AddFormClassifierServiceGrpc.AddFormClassifierService
import general.classifiers.add_form_classifier_api._
import zio.{ULayer, ZLayer}

import scala.concurrent.Future

object TestAddFormClassifier extends AddFormClassifierService {

  override def predictCategories(request: PredictCategoriesRequest): Future[PredictCategoriesResponse] =
    Future.successful(
      PredictCategoriesResponse(
        List("a_id", "b_id"),
        Map("a_id" -> 0.7, "b_id" -> 0.3),
        Map("a_id" -> 0.7, "b_id" -> 0.3),
        Map("a_id" -> 0.7, "b_id" -> 0.3)
      )
    )

  val layer: ULayer[GrpcClient[AddFormClassifierService]] =
    ZLayer.succeed(
      new GrpcClientLive[AddFormClassifierService](TestAddFormClassifier, null)
    )
}
