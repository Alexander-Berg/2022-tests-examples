package ru.yandex.vertis.telepony.api.service

import akka.actor.ActorRef
import akka.pattern.ask
import org.joda.time.DateTime
import ru.yandex.vertis.telepony.model.{CallId, Operator, Record, RecordId, RecordMeta, Url}
import ru.yandex.vertis.telepony.service.RecordService
import ru.yandex.vertis.telepony.service.RecordService.{CreateRequest, PatchRequest}
import ru.yandex.vertis.telepony.util.{RequestContext, Slice}
import slick.basic.DatabasePublisher

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * @author evans
  */
class RecordServiceProbe(probe: ActorRef) extends RecordService {

  override def save(createRequest: CreateRequest)(implicit rc: RequestContext): Future[Unit] =
    throw new NotImplementedError

  override def get(id: CallId)(implicit rc: RequestContext): Future[Record] =
    probe.ask(("get", id, rc)).mapTo[Record]

  override def getOriginal(id: CallId)(implicit rc: RequestContext): Future[Record] =
    probe.ask(("get", id, rc)).mapTo[Record]

  override def listLoadedBefore(
      before: DateTime,
      slice: Slice
    )(implicit rc: RequestContext): Future[Iterable[RecordMeta]] =
    throw new NotImplementedError

  override def listLoadedBetween(
      startTime: DateTime,
      endTime: DateTime
    )(implicit rc: RequestContext): Future[Iterable[RecordMeta]] =
    throw new NotImplementedError

  override def patch(patch: PatchRequest)(implicit rc: RequestContext): Future[Unit] =
    throw new NotImplementedError

  override def listForLoad(
      operator: Operator,
      after: DateTime,
      attemptPeriod: FiniteDuration,
      windowNoLimit: FiniteDuration
    )(implicit rc: RequestContext): DatabasePublisher[RecordMeta] =
    throw new NotImplementedError

  override def existsLoaded(id: CallId)(implicit rc: RequestContext): Future[Boolean] =
    throw new NotImplementedError

  override def getS3Url(id: RecordId, ttl: FiniteDuration)(implicit rc: RequestContext): Future[Url] =
    throw new NotImplementedError

  override def patchToS3(patch: PatchRequest)(implicit rc: RequestContext): Future[Unit] =
    throw new NotImplementedError

  override def updateLastUploadTime(recordMeta: RecordMeta)(implicit rc: RequestContext): Future[Unit] =
    throw new NotImplementedError
}
