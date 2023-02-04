package ru.yandex.realty.rent.api.grpc

import io.grpc.stub.StreamObserver
import io.opentracing.Tracer
import ru.yandex.realty.api.ProtoResponse.ApiUnit
import ru.yandex.realty.errors.NotFoundApiException
import ru.yandex.realty.grpc.server.ServiceUtils
import ru.yandex.realty.logging.TracedLogging
import ru.yandex.realty.rent.dao.OwnerRequestDao
import ru.yandex.realty.rent.proto.api.internal.{SetSmsRequestExpirationTime, TestingUtilsServiceGrpc}

import scala.async.Async.{async, await}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class TestingUtilsServiceImpl(
  ownerRequestDao: OwnerRequestDao
)(
  implicit ec: ExecutionContext,
  tracer: Tracer
) extends TestingUtilsServiceGrpc.TestingUtilsServiceImplBase
  with TracedLogging {

  override def setSmsRequestExpirationTime(
    request: SetSmsRequestExpirationTime.Request,
    responseObserver: StreamObserver[ApiUnit]
  ): Unit = ServiceUtils.observeFutureWithTracing(responseObserver) { implicit t =>
    async {
      val updateResult = await(ownerRequestDao.updateIfExistsR(request.getOwnerRequestId) { or =>
        or.doWithDataBuilder { ord =>
          ord.getSmsConfirmationsBuilderList.asScala
            .find(_.getRequestId == request.getSmsRequestId)
            .exists { sc =>
              sc.setExpirationTime(request.getNewExpirationTime)
              true
            }
        }
      })
      updateResult match {
        case Some((_, modified)) if modified => ApiUnit.getDefaultInstance
        case Some(_) => throw new NotFoundApiException("sms request")
        case None => throw new NotFoundApiException("owner request")
      }
    }
  }

}
