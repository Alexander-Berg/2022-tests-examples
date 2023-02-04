package ru.yandex.vertis.telepony.api.service

import akka.actor.ActorRef
import akka.pattern.ask
import ru.yandex.vertis.telepony.dao.OperatorNumberDaoV2.Filter
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2.{CreateRequest, DistributionKey, UpdateRequestV2}
import ru.yandex.vertis.telepony.util.sliced.SlicedResult
import ru.yandex.vertis.telepony.util.{RequestContext, Slice}
import slick.basic.DatabasePublisher

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * @author evans
  */

abstract class OperatorNumberServiceProbeBase[T: ClassTag] {

  def probe: ActorRef

  def list(filter: Filter, slice: Slice)(implicit rc: RequestContext): Future[SlicedResult[T]] =
    probe.ask(("list", filter, slice, rc)).mapTo[SlicedResult[T]]

  def get(phone: Phone)(implicit rc: RequestContext): Future[T] =
    probe.ask(("get", phone, rc)).mapTo[T]

  def create(request: CreateRequest)(implicit rc: RequestContext): Future[T] =
    probe.ask(("create", request, rc)).mapTo[T]

  def complain(number: Phone)(implicit rc: RequestContext): Future[T] =
    probe.ask(("complain", number, rc)).mapTo[T]
}

class OperatorNumberServiceProbeV2(override val probe: ActorRef)
  extends OperatorNumberServiceProbeBase[OperatorNumber]
  with OperatorNumberServiceV2 {

  override def list(
      filter: Filter,
      useDbStream: Boolean
    )(implicit rc: RequestContext): DatabasePublisher[OperatorNumber] =
    throw new UnsupportedOperationException()

  def update(phone: Phone, request: UpdateRequestV2)(implicit rc: RequestContext): Future[OperatorNumber] =
    probe.ask(("update", phone, request, rc)).mapTo[OperatorNumber]

  override def compareStatusAndSet(
      prevStatus: StatusValues.Value,
      operatorNumber: OperatorNumber
    )(implicit rc: RequestContext): Future[Boolean] =
    throw new UnsupportedOperationException()

  override def statusDistributions()(implicit rc: RequestContext): Future[Map[DistributionKey, StatusCount]] = ???

  override def find(number: Phone)(implicit rc: RequestContext): Future[Option[OperatorNumber]] = ???

  override def getNotMttReadyNumber(
      geoId: GeoId,
      phoneType: PhoneType
    )(implicit rc: RequestContext): Future[Option[OperatorNumber]] = ???
}
