package ru.yandex.vertis.punisher

import java.time.ZonedDateTime

import cats.syntax.applicative._
import ru.yandex.vertis.punisher.dao.RecentYandexUidsDao.{All, ByUserId, Filter}
import ru.yandex.vertis.punisher.dao.impl._
import ru.yandex.vertis.punisher.dao.impl.mysql.AutoruActivityAll7SalesDaoImpl
import ru.yandex.vertis.punisher.dao.{OffersDao, RecentYandexUidsDao}
import ru.yandex.vertis.punisher.model._

import scala.concurrent.duration._

/**
  * @author devreggs
  */
object MockAutoruDaoBuilder extends BaseSpec {

  private val RecentYandexuidsFile: String = "/csv/autoru-recent-yandex-uid.csv"
  private val OffersFile: String = "/csv/autoru-offers.csv"

  lazy val activityDao: AutoruActivityAll7SalesDaoImpl[F] = new AutoruActivityAll7SalesDaoImpl(AutoruMySqlSpec.db)

  object UidOfferRecord {

    def unapply(arg: String): AutoruOffer = {
      val splitted = arg.split(";")
      val regionSplitted = splitted(5).split("/")
      val vin = Option(splitted(6)).filter(_.nonEmpty)
      val isFree = splitted(7) == "free"
      AutoruOffer(
        userId = Some(splitted(0)),
        clientId = None,
        offerId = splitted(1),
        lastActivityDate = ZonedDateTime.now().minusMonths(splitted(2).toInt),
        mark = splitted(3),
        model = splitted(4),
        region =
          Some(
            OfferRegion(
              geoId = regionSplitted(0).toInt,
              city = regionSplitted(0).toInt,
              subjectFederation = regionSplitted(1).toInt,
              countryDistrict = regionSplitted(2).toInt
            )
          ),
        vin = vin,
        isPlacedForFree = Some(isFree)
      )
    }
  }

  lazy val offersDao: OffersDao[F, AutoruOffer] =
    new OffersDao[F, AutoruOffer] {

      override def offers(userIds: Set[UserId])(implicit context: TaskContext.Batch): F[Set[AutoruOffer]] =
        resourceLines(OffersFile).map(UidOfferRecord.unapply).toSet.pure[F]
    }

  lazy val recentYandexUidsDao: RecentYandexUidsDao[F] =
    new RecentYandexUidsDao[F] {

      private val Recents: Iterable[RecentYandexUids] =
        resourceLines(RecentYandexuidsFile).flatMap(RecentYandexUidUserRecord.unapply).iterator.to(Iterable)

      override def get(filter: Filter)(implicit context: TaskContext.Batch): F[Iterable[RecentYandexUids]] = {
        filter match {
          case ByUserId(u) => Recents.filter(_.user == u)
          case All         => Recents
        }
      }.pure[F]
    }
}

object RecentYandexUidUserRecord {

  private val FieldDelimiter: String = ";"
  private val SubFieldDelimiter: String = "#"

  def unapply(in: String): Option[RecentYandexUids] =
    for {
      v <- Option(in)
      parts = v.split(FieldDelimiter)
      if parts.length == 4 && !parts.exists(_.isEmpty)
      userId     = parts(0)
      offerIds   = parts(1).split(SubFieldDelimiter).toSet
      yandexUids = parts(2).split(SubFieldDelimiter).toSet
      minAge     = Duration(parts(3).toLong, SECONDS)
    } yield RecentYandexUids(userId, offerIds, yandexUids, minAge)
}
