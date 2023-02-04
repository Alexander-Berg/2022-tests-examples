package ru.yandex.vertis.punisher

import java.time.ZonedDateTime
import cats.syntax.applicative._
import ru.yandex.realty.proto.offer.{OfferCategory, OfferType}
import ru.yandex.vertis.punisher.config.EventEodConfig
import ru.yandex.vertis.punisher.dao._
import ru.yandex.vertis.punisher.dao.impl._
import ru.yandex.vertis.punisher.model.{OfferRegion, RealtyOffer, TaskContext, UserId}
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.quality.yql_utils.YqlQueryExecutor
import ru.yandex.vertis.quality.yql_utils.config.{YqlExecutorConfig, YqlQueryConfig}
import ru.yandex.vertis.quality.yql_utils.impl.YqlQueryExecutorImpl

object MockRealtyDaoBuilder extends BaseSpec {

  private val OffersFile: String = "/csv/realty-offers.csv"

  lazy val offersActivityDao: ActivityDao[F] =
    new ActivityDao[F] {

      override def activeUsers(timeInterval: TimeInterval)(implicit context: TaskContext.Batch): F[Set[UserId]] =
        offersDao.offers(Set.empty).map { offers =>
          offers.flatMap(_.userId) ++ Set("14", "15")
        }
    }

  lazy val infectionActivityDao: ActivityDao[F] =
    new ActivityDao[F] {

      override def activeUsers(timeInterval: TimeInterval)(implicit context: TaskContext.Batch): F[Set[UserId]] =
        Set("1", "2", "4").pure[F]
    }

  object UidOfferRecord {

    def unapply(arg: String): RealtyOffer = {
      val splitted = arg.split(';')
      val regionSplitted = splitted(7).split('/')
      RealtyOffer(
        userId = Some(splitted(0)),
        offerId = splitted(1),
        offerClusterId = splitted(2),
        lastActivityDate = ZonedDateTime.now().minusMonths(splitted(3).toInt),
        offerType = OfferType.valueOf(splitted(4)),
        categoryType = OfferCategory.valueOf(splitted(5)),
        isQuotaIssued = Some(splitted(6).toBoolean),
        regionInfo =
          OfferRegion(
            geoId = regionSplitted(0).toInt,
            city = regionSplitted(0).toInt,
            subjectFederation = regionSplitted(1).toInt,
            countryDistrict = regionSplitted(2).toInt
          )
      )
    }
  }

  lazy val offersDao: OffersDao[F, RealtyOffer] =
    new OffersDao[F, RealtyOffer] {
      override def offers(userIds: Set[UserId])(implicit context: TaskContext.Batch): F[Set[RealtyOffer]] =
        resourceLines(OffersFile).map(UidOfferRecord.unapply).toSet.pure[F]
    }
}
