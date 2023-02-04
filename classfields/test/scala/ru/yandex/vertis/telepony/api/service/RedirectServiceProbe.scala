package ru.yandex.vertis.telepony.api.service

import akka.actor.ActorRef
import akka.pattern._
import org.joda.time.DateTime
import ru.yandex.vertis.telepony.model
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectInspectService.ComputedCreateRequest
import ru.yandex.vertis.telepony.service.RedirectServiceV2
import ru.yandex.vertis.telepony.service.RedirectServiceV2._
import ru.yandex.vertis.telepony.service.impl.RedirectServiceV2Impl.RedirectResult.Created
import ru.yandex.vertis.telepony.util.sliced.SlicedResult
import ru.yandex.vertis.telepony.util.{RequestContext, Slice}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

/**
  * @author evans
  */
abstract class RedirectServiceProbeBase[T: ClassTag] {
  def probe: ActorRef

  def delete(
      source: model.Phone,
      target: model.Phone,
      clearLastTarget: Boolean
    )(implicit rc: RequestContext): Future[Unit] =
    probe.ask(("delete", source, target, rc)).mapTo[Unit]

  def list(filter: Filter, slice: Slice)(implicit rc: RequestContext): Future[SlicedResult[T]] =
    probe.ask(("list", filter, slice, rc)).mapTo[SlicedResult[T]]

  def delete(
      objectId: ObjectId,
      downtime: Option[FiniteDuration],
      clearLastTarget: Boolean
    )(implicit rc: RequestContext): Future[Boolean] =
    probe.ask(("deleteByObjectId", objectId, downtime, rc)).mapTo[Boolean]

  def delete(
      redirectId: RedirectId,
      downtime: Option[FiniteDuration],
      clearLastTarget: Boolean
    )(implicit rc: RequestContext): Future[Boolean] =
    probe.ask(("deleteByRedirectId", redirectId, downtime, rc)).mapTo[Boolean]

  def getAvailableCount(availableRequest: AvailableRequest)(implicit rc: RequestContext): Future[Int] =
    probe.ask(("available-count", availableRequest, rc)).mapTo[Int]
}

class RedirectServiceProbeV2(val probe: ActorRef)
  extends RedirectServiceProbeBase[ActualRedirect]
  with RedirectServiceV2 {

  override def get(source: Phone)(implicit rc: RequestContext): Future[Option[ActualRedirect]] =
    probe.ask(("get", source, rc)).mapTo[Option[ActualRedirect]]

  override def countAvailable(req: AvailableRequest)(implicit rc: RequestContext): Future[Int] =
    getAvailableCount(req)

  override def getOnTime(phone: Phone, onTime: DateTime)(implicit rc: RequestContext): Future[Option[HistoryRedirect]] =
    ???

  override def getOnTimeDomain(
      phone: Phone,
      onTime: DateTime,
      domain: TypedDomain
    )(implicit rc: RequestContext): Future[Option[HistoryRedirect]] =
    ???

  override def recover(redirect: ActualRedirect)(implicit rc: RequestContext): Future[Unit] =
    probe.ask(("recover", redirect, rc)).mapTo[Unit]

  override def replace(oldRedirect: ActualRedirect)(implicit rc: RequestContext): Future[Option[ActualRedirect]] =
    probe.ask(("replace", oldRedirect, rc)).mapTo[Option[ActualRedirect]]

  override def touch(touchRedirect: TouchRedirectRequest)(implicit rc: RequestContext): Future[Option[ActualRedirect]] =
    probe.ask(("touch", touchRedirect, rc)).mapTo[Option[ActualRedirect]]

  override def updateOptions(
      objectId: ObjectId,
      target: Phone,
      request: RedirectServiceV2.UpdateOptionsRequest
    )(implicit rc: RequestContext): Future[Unit] = ???

  override def updateAntiFraud(
      redirectId: RedirectId,
      newAntiFraud: Set[AntiFraudOption],
      canWeaken: Boolean
    )(implicit rc: RequestContext): Future[Boolean] = ???

  override def get(redirectId: RedirectId)(implicit rc: RequestContext): Future[Option[ActualRedirect]] =
    probe.ask(("getById", redirectId, rc)).mapTo[Option[ActualRedirect]]

  override def complain(source: Phone, request: ComplainRequest)(implicit rc: RequestContext): Future[Unit] = {
    print(s"complain $source $request")
    probe.ask(("complain", source, request, rc)).mapTo[Unit]
  }

  /**
    * Create new redirect based on request.
    * Based on request will be extracted geo id of expected redirect.
    * Based on geo id and domain service will try find suitable proxy number.
    *
    * @param request request for redirect
    * @param rc      request context
    * @return redirect
    */
  def create(
      request: RedirectServiceV2.CreateRequest,
      id: RedirectId
    )(implicit rc: RequestContext): Future[ActualRedirect] =
    probe.ask(("create", request, id, rc)).mapTo[ActualRedirect]

  def create(request: RedirectServiceV2.CreateRequest)(implicit rc: RequestContext): Future[ActualRedirect] =
    probe.ask(("create", request, rc)).mapTo[ActualRedirect]

  override def getOrCreate(
      request: RedirectServiceV2.CreateRequest
    )(implicit rc: RequestContext): Future[ActualRedirect] = {
    probe.ask(("getOrCreate", request, rc)).mapTo[ActualRedirect]
  }

  override protected def doCreate(request: ComputedCreateRequest)(implicit rc: RequestContext): Future[Created] = ???

  /**
    * Delete all redirects that have such object id and target
    *
    * @return the number of deleted redirects
    */
  override def delete(
      filter: RedirectKeyFilter,
      downtime: Option[FiniteDuration],
      clearLastTarget: Boolean
    )(implicit rc: RequestContext): Future[Iterable[ActualRedirect]] =
    probe.ask(("deleteBy", filter, downtime, rc)).mapTo[Iterable[ActualRedirect]]

  override protected def doDelete(
      redirect: ActualRedirect,
      downtime: Option[FiniteDuration],
      clearLastTarget: Boolean
    )(implicit rc: RequestContext): Future[Unit] = ???

  override def updateOptions(
      redirectKey: RedirectKey,
      request: UpdateOptionsRequest
    )(implicit rc: RequestContext): Future[Unit] = ???

  override def updateOptions(
      redirectId: RedirectId,
      request: UpdateOptionsRequest
    )(implicit rc: RequestContext): Future[Unit] = ???

  override protected def updateOptionsInt(
      redirect: ActualRedirect,
      request: UpdateOptionsRequest
    )(implicit rc: RequestContext): Future[Unit] = ???

  override def createFromExisting(
      id: RedirectId,
      update: UpdateRedirectRequest
    )(implicit rc: RequestContext): Future[CreateFromExistingResponse] = ???

  override def get(key: RedirectKey)(implicit rc: RequestContext): Future[Iterable[ActualRedirect]] = ???

  override def touchRedirectAsync(request: CreateRequest, redirect: ActualRedirect): Unit = ???

  override def listHistory(request: ListRedirectRequest)(implicit rc: RequestContext): Future[SlicedResult[Redirect]] =
    ???

  override def getHistoryRedirect(redirectId: RedirectId): Future[HistoryRedirect] = ???

  override def getHistoryRedirectByDomain(redirect: RedirectId, domain: TypedDomain): Future[HistoryRedirect] = ???
}
