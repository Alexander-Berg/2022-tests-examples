package vsquality.complaints.logic

import common.clients.moderation.ModerationClient
import common.clients.moderation.model.{
  Context,
  ExternalId,
  Instance,
  ModerationInstancesResponse,
  Opinion,
  OpinionType,
  SignalSourceView,
  Visibility
}
import ru.yandex.vertis.moderation.proto.model.Service
import vertis.complaints.v2.model
import vsquality.complaints.model.{Complaint, Domain, QueueRecord, Reason, Resolution, Source, Status, User, UserType}
import vsquality.complaints.model.errors.ComplaintsError
import zio.{IO, Task, ZIO}
import java.time.Instant
import java.time.temporal.ChronoUnit

package object test {

  def createComplaint(id: String, offerId: String, ownerId: String, complainantId: String) = Complaint(
    id = id,
    offerId = offerId,
    offerOwner = User(ownerId, UserType.Regular),
    complainant = User(complainantId, UserType.Regular),
    reasons = List(Reason.AbusiveCommunication),
    source = Source.Description,
    resolution = Resolution(confirmed = None, false),
    comment = "",
    domain = Domain.AUTORU,
    createdAt = Instant.now.truncatedTo(ChronoUnit.SECONDS),
    updatedAt = Instant.now.truncatedTo(ChronoUnit.SECONDS),
    hoboKey = None
  )

  def toRecord(c: Complaint) =
    QueueRecord(
      id = c.id,
      offerId = c.offerId,
      offerOwnerId = c.offerOwner.id,
      complainantId = c.complainant.id,
      domain = c.domain,
      status = Status.IN_PROGRESS,
      processAfter = Instant.now.truncatedTo(ChronoUnit.SECONDS),
      updatedAt = Instant.now.truncatedTo(ChronoUnit.SECONDS)
    )

  val ExistsInModId = "exists"
  val EmptyInModId = "empty"
  val NonVisible = "non-visible"
  val FailedOpinion = "failed-opinion"

  val modClientMock = new ModerationClient.Service {
    override def remoderateInstance(service: Service, id: ExternalId, diff: Set[String]): Task[Unit] = ZIO.unit

    override def getInstances(service: Service, externalIds: Seq[String]): Task[ModerationInstancesResponse] = {
      val res =
        externalIds.head match {
          case ExistsInModId =>
            Seq(
              Instance(
                ExistsInModId,
                ExistsInModId,
                Opinion(`type` = OpinionType.Ok),
                context = Context(Visibility.VISIBLE)
              )
            )
          case NonVisible =>
            Seq(
              Instance(NonVisible, NonVisible, Opinion(`type` = OpinionType.Ok), context = Context(Visibility.INVALID))
            )

          case FailedOpinion =>
            Seq(
              Instance(
                FailedOpinion,
                FailedOpinion,
                Opinion(`type` = OpinionType.Failed),
                context = Context(Visibility.VISIBLE)
              )
            )

          case EmptyInModId => Seq.empty
        }

      ZIO.succeed(ModerationInstancesResponse(res))
    }

    override def appendSignals(service: Service, objectId: String, signals: Seq[SignalSourceView]): Task[Unit] =
      ZIO.unit

    override def banUsers(service: Service, userIds: Set[String], signals: Seq[SignalSourceView]): Task[Unit] =
      ZIO.unit
  }

  val producerMock = new ComplaintsEventProducer.Service {
    override def send(events: Seq[model.Complaint]): IO[ComplaintsError, Unit] = ZIO.unit
  }

}
