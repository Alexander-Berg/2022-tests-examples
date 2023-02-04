package ru.yandex.vertis.telepony.api.service

import akka.actor.ActorRef
import akka.pattern._
import org.joda.time.DateTime
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.CallService.{CreateRequest, Filter}
import ru.yandex.vertis.telepony.service.{ActualCallService, BlockedCallService, CallService}
import ru.yandex.vertis.telepony.util.sliced.{SimpleSlicedResult, SlicedResult}
import ru.yandex.vertis.telepony.util.{RequestContext, Slice}

import scala.concurrent.Future

/**
  * @author evans
  */
class CallServiceProbeV2(probe: ActorRef) extends ActualCallService {

  override def list(filter: Filter, slice: Slice)(implicit rc: RequestContext): Future[SlicedResult[CallV2]] =
    probe.ask(("list", filter, slice, rc)).mapTo[SlicedResult[CallV2]]

  override def listWithoutTotal(
      filter: Filter,
      slice: Slice
    )(implicit rc: RequestContext): Future[SimpleSlicedResult[CallV2]] =
    probe.ask(("list-without-total", filter, slice, rc)).mapTo[SimpleSlicedResult[CallV2]]

  override def list(filter: Filter, callLimit: Int)(implicit rc: RequestContext): Future[Iterable[CallV2]] =
    probe.ask(("list-limit", filter, callLimit, rc)).mapTo[Iterable[CallV2]]

  override protected def truncateSource: PartialFunction[RefinedSource, RefinedSource] =
    throw new UnsupportedOperationException

  override def exists(request: CreateRequest)(implicit rc: RequestContext): Future[Boolean] =
    Future.failed(new UnsupportedOperationException)

  override def callsStats(objectId: ObjectId, tag: Tag)(implicit rc: RequestContext): Future[CallsStats] =
    probe.ask(("stats", objectId, tag, rc)).mapTo[CallsStats]

  override def callResultStats(
      source: RefinedSource,
      objectId: ObjectId,
      target: Phone
    )(implicit rc: RequestContext): Future[CallResultStats] =
    Future.failed(new UnsupportedOperationException)

  override def callsByDay(filter: Filter)(implicit rc: RequestContext): Future[Iterable[CallsCountByDay]] =
    probe.ask(("count-by-day", filter, rc)).mapTo[Iterable[CallsCountByDay]]

  override def get(callId: CallId)(implicit rc: RequestContext): Future[CallV2] =
    Future.failed(new UnsupportedOperationException)

  override def listUpdated(
      from: DateTime,
      nextToCallId: CallId,
      limit: GeoId
    )(implicit rc: RequestContext): Future[Seq[CallV2]] = ???

  override def update(callId: CallId, update: CallService.Update)(implicit rc: RequestContext): Future[CallV2] = ???

  override def create(request: CreateRequest)(implicit rc: RequestContext): Future[CallV2] = ???

  override def findCallToNotFallback(
      source: RefinedSource,
      objectId: ObjectId,
      successCallFrom: DateTime,
      fallbackCallFrom: DateTime): Future[Option[CallV2]] = ???
}

class BlockedCallServiceProbe(probe: ActorRef) extends BlockedCallService {

  override def create(request: CreateRequest)(implicit rc: RequestContext): Future[BannedCall] =
    throw new NotImplementedError

  override def list(filter: Filter, slice: Slice)(implicit rc: RequestContext): Future[SlicedResult[BannedCall]] =
    probe.ask(("list", filter, slice, rc)).mapTo[SlicedResult[BannedCall]]

  override def listWithoutTotal(
      filter: Filter,
      slice: Slice
    )(implicit rc: RequestContext): Future[SimpleSlicedResult[BannedCall]] =
    probe.ask(("list", filter, slice, rc)).mapTo[SimpleSlicedResult[BannedCall]]

  override def list(filter: Filter, callLimit: Int)(implicit rc: RequestContext): Future[Iterable[BannedCall]] =
    probe.ask(("list-limit", filter, callLimit, rc)).mapTo[Iterable[BannedCall]]

  override protected def truncateSource: PartialFunction[RefinedSource, RefinedSource] =
    throw new UnsupportedOperationException

  override def exists(request: CreateRequest)(implicit rc: RequestContext): Future[Boolean] =
    Future.failed(new UnsupportedOperationException)

  override def get(callId: CallId)(implicit rc: RequestContext): Future[BannedCall] =
    Future.failed(new UnsupportedOperationException)

  override def listUpdated(
      from: DateTime,
      nextToCallId: CallId,
      limit: GeoId
    )(implicit rc: RequestContext): Future[Seq[BannedCall]] = ???
}
