package auto.dealers.calltracking.storage.testkit

import java.time.{Instant, LocalDate, ZoneId}

import common.collections.syntax._
import auto.common.pagination.RequestPagination
import auto.dealers.calltracking.model.testkit.Fits
import auto.dealers.calltracking.model.testkit.Sorting._
import auto.dealers.calltracking.model._
import ru.auto.calltracking.proto.filters_model.{FullTextFilter, Sorting}
import ru.auto.calltracking.proto.model.Call.CallResult
import ru.auto.api.api_offer_model.{Category, Section}
import auto.dealers.calltracking.storage.CalltrackingDao
import auto.dealers.calltracking.storage.CalltrackingDao.CallNotFound
import zio.stream.Stream
import zio._
import zio.stm.{STM, TMap, TRef, ZSTM}
import cats.data.NonEmptySet
import java.time.ZonedDateTime

object TestCalltrackingDao {
  type TestCalltrackingDao = Has[Service]

  trait Service extends CalltrackingDao.Service {
    def allCalls: UIO[Seq[Call]]

    def removeCall(externalId: ExternalId): UIO[Unit]
  }

  def make: UIO[TestService] =
    STM.mapN(TMap.empty[ExternalId, Call], TRef.make(0L))(TestService).commit

  val live: ZLayer[Any, Nothing, Has[TestService] with Has[CalltrackingDao.Service]] =
    ZLayer.fromEffectMany {
      make.map(s => Has(s) ++ Has(s: CalltrackingDao.Service))
    }

  case class TestService(data: TMap[ExternalId, Call], counter: TRef[Long]) extends Service {

    private val MoscowTimeZone = ZoneId.of("Europe/Moscow")

    private def getCallSTM(clientIds: NonEmptySet[ClientId], id: CallId): ZSTM[Any, CallNotFound, Call] = {
      data.values
        .map(_.find(c => c.id == id.id && clientIds.exists(c.clientId == _.id)))
        .someOrFail(new CallNotFound)
    }

    override def getCall(clientIds: NonEmptySet[ClientId], id: CallId): IO[CallNotFound, Call] =
      getCallSTM(clientIds, id).commit

    override def getCallByExternalId(externalId: ExternalId): IO[CallNotFound, Call] =
      data.get(externalId).someOrFail(new CallNotFound).commit

    override def allCalls: UIO[Seq[Call]] = data.values.commit

    override def getCalls(
        clientIds: NonEmptySet[ClientId],
        filters: Filters,
        pagination: RequestPagination,
        sorting: Sorting,
        textFilter: Option[FullTextFilter]): UIO[Seq[Call]] = {
      data.values
        .map(
          _.filter(call => clientIds.exists(call.clientId == _.id))
            .filter(Fits(filters, _))
            .sorted(orderingFromSorting(sorting))
            .slice(pagination.pageSize * (pagination.page - 1), pagination.pageSize * pagination.page)
        )
        .commit
    }

    override def countCalls(
        clientIds: NonEmptySet[ClientId],
        filters: Filters,
        textFilter: Option[FullTextFilter]): UIO[Int] = {
      data.values
        .map(
          _.filter(call => clientIds.exists(call.clientId == _.id)).count(Fits(filters, _))
        )
        .commit
    }

    override def countCallsBatch(clientId: ClientId, offerIds: NonEmptySet[OfferId]): UIO[Seq[(OfferId, Long)]] = {
      val result = offerIds.toSortedSet.view.map(_ -> 0L).toMap
      data.values.map { calls =>
        calls
          .foldLeft(result) { case (counters, call) =>
            call.offer match {
              case None => counters
              case Some(offer) =>
                counters.updatedWith(OfferId(offer.id)) { counter =>
                  counter.map(_ + 1)
                }
            }
          }
          .toSeq
      }.commit

    }

    private def calcCallStats(calls: Seq[Call]): CallStats = {
      calls.foldLeft(CallStats(0, 0)) { case (stats, call) =>
        if (call.callResult == CallResult.SUCCESS) stats.copy(answered = stats.answered + 1)
        else stats.copy(notAnswered = stats.notAnswered + 1)
      }
    }

    override def getDailyCallStats(
        clientId: ClientId,
        filters: Filters,
        textFilter: Option[FullTextFilter] = None): UIO[Seq[(LocalDate, CallStats)]] = {
      data.values
        .map(_.filter(_.clientId == clientId.id).filter(Fits(filters, _)))
        .map(_.groupBy(_.callTime.atZone(MoscowTimeZone).toLocalDate).view.mapValues(calcCallStats).toVector)
        .commit
    }

    override def insertCalls(calls: Seq[Call]): UIO[Unit] = {
      STM
        .foreach(calls) { call =>
          for {
            existing <- data.get(call.externalId)
            id <- existing match {
              case None => counter.updateAndGet(_ + 1)
              case Some(_) => STM.dieMessage(s"Call with externalId ${call.externalId} already exists")
            }
            _ <- data.put(call.externalId, call.copy(id = id))
          } yield ()

        }
        .commit
        .unit
    }

    override def updateCalls(update: Seq[Call.Update]): UIO[Unit] = {
      val updateMap = update.toMapWithKey(_.callId)
      data.transformValues { call =>
        updateMap.get(call.id) match {
          case Some(patch) =>
            patch(call)
          case None => call
        }
      }.commit
    }

    override def upsertCalls(calls: Seq[Call]): UIO[Unit] = {
      STM
        .foreach(calls) { call =>
          for {
            existing <- data.get(call.externalId)
            id <- existing match {
              case None => counter.updateAndGet(_ + 1)
              case Some(value) => STM.succeed(value.id)
            }
            _ <- data.put(call.externalId, call.copy(id = id))
          } yield ()
        }
        .commit
        .unit
    }

    override def removeCall(externalId: ExternalId): UIO[Unit] =
      data.removeIf((id, _) => id == externalId).commit

    override def addTags(clientId: ClientId, callId: CallId, tags: Seq[String]): IO[CallNotFound, Unit] = {
      (for {
        call <- getCallSTM(NonEmptySet.one(clientId), callId)
        _ <- data.put(call.externalId, call.withTags(tags))
      } yield ()).commit
    }

    override def removeTags(clientId: ClientId, callId: CallId, tags: Seq[String]): IO[CallNotFound, Unit] = {
      (for {
        call <- getCallSTM(NonEmptySet.one(clientId), callId)
        _ <- data.put(call.externalId, call.withoutTags(tags))
      } yield ()).commit
    }

    override def listTags(clientId: ClientId, prefix: String): UIO[Seq[String]] = {
      allCalls.map(_.filter(_.clientId == clientId.id).flatMap(_.tags).filter(_.startsWith(prefix)).distinct)
    }

    override def removeClientTag(clientId: ClientId, tag: String): UIO[Unit] = {
      data.values
        .flatMap(STM.foreach_(_) { call =>
          if (call.clientId == clientId.id && call.tags.contains(tag))
            data.put(call.externalId, call.withTag(tag))
          else STM.unit
        })
        .commit
    }

    override def getCallsForMatching(
        range: Filters.Range[Instant],
        sourcePhones: Seq[String],
        callkeeperIds: Seq[ExternalId]): UIO[Seq[Call]] =
      data.values
        .map(
          _.filter(c =>
            callkeeperIds.contains(c.externalId) ||
              (c.sourcePhone.exists(sourcePhones.contains) && Fits(Filters(callTime = range), c))
          )
        )
        .commit

    override def countRelevantCallsPerOffer(
        offerIds: NonEmptySet[OfferId],
        from: Instant,
        to: Instant): IO[Throwable, Map[OfferId, Int]] =
      data.values
        .map(
          _.filter { call =>
            val created = call.created
            val containsId = call.offer
              .exists(offer => offerIds.contains(OfferId(offer.id)))
            val isAfterOrEqFrom = created.isAfter(from) || created == from

            call.isRelevant.getOrElse(false) && containsId && isAfterOrEqFrom && created.isBefore(to)
          }
            .groupBy(_.offer.get.id)
            .map { case (k, v) => OfferId(k) -> v.length }
        )
        .commit

    override def lastRelevantCallPerOffer(
        from: Instant,
        to: Instant,
        category: Filters.MultiFilter[Category] = Filters.EmptyMultiFilter,
        section: Filters.MultiFilter[Section] = Filters.EmptyMultiFilter): Stream[Throwable, (OfferId, Instant)] =
      Stream.fromIterableM {
        data.values
          .map(
            _.filter { call =>
              val created = call.created

              lazy val isAfterOrEqFrom =
                created.isAfter(from) || created == from

              lazy val categoryFits = category match {
                case Filters.EmptyMultiFilter => true
                case Filters.AnyOf(vals) =>
                  call.category.map(c => vals.exists(_ == c)).getOrElse(false)
              }

              lazy val sectionFits = section match {
                case Filters.EmptyMultiFilter => true
                case Filters.AnyOf(vals) =>
                  call.section.map(s => vals.exists(_ == s)).getOrElse(false)
              }

              call.isRelevant.getOrElse(
                false
              ) && call.offer.isDefined && categoryFits && sectionFits && isAfterOrEqFrom && created.isBefore(to)
            }
              .groupBy(_.offer.get.id)
              .map { case (k, v) => OfferId(k) -> v.map(_.created).max }
          )
          .commit
      }

  }

}
