package ru.auto.comeback.storage.comeback

import common.zio.doobie.testkit.TestPostgresql
import doobie.syntax.connectionio._
import doobie.util.transactor.Transactor
import ru.auto.api.api_offer_model.OfferStatus
import ru.auto.comeback.model.Filters._
import ru.auto.comeback.model.testkit.{ComebackGen, CommonGen, FiltersGen}
import ru.auto.comeback.model.{ComebackT, Filters}
import ru.auto.comeback.storage.{Schema, TestComebackDao}
import zio.interop.catz._
import zio.test.Assertion.hasSameElements
import zio.test.TestAspect.{after, beforeAll, samples, sequential}
import zio.test.{assert, assertTrue, checkM, DefaultRunnableSpec}
import zio.{Task, ZIO}

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 22/01/2020
  */
object DbFiltersSpec extends DefaultRunnableSpec {

  def spec = {
    suite("DbComebackDao Filters")(
      testM("filter by client id") {
        checkM(
          ComebackGen.newComebacksFromOneClient,
          FiltersGen.anyFilters,
          CommonGen.anySorting,
          CommonGen.anyPagination
        ) { case ((clientId, comebacks), filters, sorting, pagination) =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            _ <- dao.insert(comebacks).transact(xa)
            res <- dao.get(clientId + 1, filters, sorting, Some(pagination)).transact(xa)
          } yield assertTrue(res.isEmpty)
        }
      } :: testM("filters, sorting and pagination works like in-memory dao") {
        checkM(
          ComebackGen.newComebacksFromOneClient,
          FiltersGen.anyFilters,
          CommonGen.anySorting,
          CommonGen.anyPagination
        ) { case ((clientId, comebacks), filters, sorting, pagination) =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            _ <- dao.insert(comebacks).transact(xa)
            testDao <- TestComebackDao.make
            _ <- testDao.insert(comebacks, scheduleComebackUpdateEvent = false)
            res <- dao.get(clientId, filters, sorting, Some(pagination)).transact(xa)
            expected <- testDao.get(clientId, filters, sorting, Some(pagination))
          } yield assertTrue(res.map(_.withoutId) == expected.map(_.withoutId))
        }
      } :: testM("count works like in-memory dao") {
        checkM(
          ComebackGen.newComebacksFromOneClient,
          FiltersGen.anyFilters
        ) { case ((clientId, comebacks), filters) =>
          for {
            _ <- Schema.cleanup
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[ComebackDao.Service]
            _ <- dao.insert(comebacks).transact(xa)
            testDao <- TestComebackDao.make
            _ <- testDao.insert(comebacks, scheduleComebackUpdateEvent = false)
            res <- dao.count(clientId, filters).transact(xa)
            expected <- testDao.count(clientId, filters)
          } yield assertTrue(res.toInt == expected)
        }
      } :: suites
        .map { spec =>
          testM(spec.desc) {
            checkM(
              ComebackGen.newComebacksFromOneClient,
              CommonGen.anySorting
            ) { case ((clientId, comebacks), sorting) =>
              for {
                _ <- Schema.cleanup
                xa <- ZIO.service[Transactor[Task]]
                dao <- ZIO.service[ComebackDao.Service]
                _ <- dao.insert(comebacks).transact(xa)
                filter = spec.unique(comebacks)
                res <- dao.get(clientId, filter, sorting, None).transact(xa)
                withoutId = res.map(_.withoutId)
              } yield assert(withoutId)(
                hasSameElements(comebacks.filter(c => spec.assertion(c, filter)))
              )
            }
          }
        }: _*
    ) @@ after(Schema.cleanup) @@ beforeAll(Schema.init) @@ sequential @@ samples(10)
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor(version = "12") >+> ComebackDao.live
  )

  case class FilterSpec(
      desc: String,
      unique: List[ComebackT[None.type]] => Filters,
      assertion: (ComebackT[None.type], Filters) => Boolean)

  private val suites = List(
    FilterSpec(
      "filter by year from works",
      { c =>
        val unique = c.map(_.offer.car.year).find(_ != 0)
        Filters.Empty.copy(year = unique.map(y => LeftRange(y)).getOrElse(EmptyRange))
      },
      { case (c, filter) =>
        filter.year match {
          case LeftRange(from) => c.offer.car.year >= from
          case EmptyRange => true
          case _ => false
        }
      }
    ),
    FilterSpec(
      "filter by year to works",
      { c =>
        val unique = c.map(_.offer.car.year).find(_ != 0)
        Filters.Empty.copy(year = unique.map(y => RightRange(y)).getOrElse(EmptyRange))
      },
      { case (c, filter) =>
        filter.year match {
          case RightRange(to) => c.offer.car.year <= to
          case EmptyRange => true
          case _ => false
        }
      }
    ),
    FilterSpec(
      "filter by vin works",
      { c =>
        val unique = c
          .map(_.offer.car.vin)
          .find(_.nonEmpty)
        Filters.Empty.copy(vins = unique.toList.toSet)
      },
      { case (c, filter) => filter.vins.contains(c.offer.car.vin) }
    ),
    FilterSpec(
      "filter by mileage from works",
      { c =>
        val unique = c
          .map(_.offer.mileage)
          .find(_ != 0)
        Filters.Empty.copy(mileage = unique.map(v => LeftRange(v)).getOrElse(EmptyRange))
      },
      { case (c, filter) =>
        filter.mileage match {
          case LeftRange(from) => c.offer.mileage >= from
          case EmptyRange => true
          case _ => false
        }
      }
    ),
    FilterSpec(
      "filter by mileage to works",
      { c =>
        val unique = c
          .map(_.offer.mileage)
          .find(_ != 0)
        Filters.Empty.copy(mileage = unique.map(v => RightRange(v)).getOrElse(EmptyRange))
      },
      { case (c, filter) =>
        filter.mileage match {
          case RightRange(to) => c.offer.mileage <= to
          case EmptyRange => true
          case _ => false
        }
      }
    ),
    FilterSpec(
      "filter by price from works",
      { c =>
        val unique = c
          .map(_.offer.priceRub)
          .find(_ != 0)
        Filters.Empty.copy(priceRub = unique.map(v => LeftRange(v)).getOrElse(EmptyRange))
      },
      { case (c, filter) =>
        filter.priceRub match {
          case LeftRange(from) => c.offer.priceRub >= from
          case EmptyRange => true
          case _ => false
        }
      }
    ),
    FilterSpec(
      "filter by price to works",
      { c =>
        val unique = c
          .map(_.offer.priceRub)
          .find(_ != 0)
        Filters.Empty.copy(priceRub = unique.map(v => RightRange(v)).getOrElse(EmptyRange))
      },
      { case (c, filter) =>
        filter.priceRub match {
          case RightRange(to) => c.offer.priceRub <= to
          case EmptyRange => true
          case _ => false
        }
      }
    ),
    FilterSpec(
      "filter by creationDate from works",
      { c =>
        val unique = c.map(_.offer.activated).headOption
        Filters.Empty.copy(creationDate = unique.map(v => LeftRange(v)).getOrElse(EmptyRange))
      },
      { case (c, filter) =>
        filter.creationDate match {
          case LeftRange(from) => c.offer.activated.toEpochMilli >= from.toEpochMilli
          case EmptyRange => true
          case _ => false
        }
      }
    ),
    FilterSpec(
      "filter by creationDate to works",
      { c =>
        val unique = c
          .map(_.offer.activated)
          .headOption
        Filters.Empty.copy(creationDate = unique.map(v => RightRange(v)).getOrElse(EmptyRange))
      },
      { case (c, filter) =>
        filter.creationDate match {
          case RightRange(to) => c.offer.activated.toEpochMilli <= to.toEpochMilli
          case EmptyRange => true
          case _ => false
        }
      }
    ),
    FilterSpec(
      "filter by rid works",
      { c =>
        val unique = c
          .map(_.offer.rid)
          .find(_ != 0)
        Filters.Empty.copy(rid = unique.toList.toSet)
      },
      { case (c, filter) => filter.rid.contains(c.offer.rid) }
    ),
    FilterSpec(
      "filter by pastSection works",
      { c =>
        val unique = c
          .flatMap(_.pastEvents.pastOffer.map(_.section))
          .headOption
        Filters.Empty.copy(pastSection = unique.map(v => EqValue(v)).getOrElse(EmptyEq))
      },
      { case (c, filter) =>
        filter.pastSection match {
          case Filters.EmptyEq => true
          case EqValue(value) => c.pastEvents.pastOffer.map(_.section).contains(value)
        }
      }
    ),
    FilterSpec(
      "filter by isSold works",
      { c =>
        val unique = c
          .map(_.offer.status match {
            case OfferStatus.ACTIVE => false
            case _ => true
          })
          .headOption
        Filters.Empty.copy(isSold = unique)
      },
      { case (c, filter) =>
        filter.isSold match {
          case Some(true) => c.offer.status != OfferStatus.ACTIVE
          case Some(false) => c.offer.status == OfferStatus.ACTIVE
          case None => true
        }
      }
    ),
    FilterSpec(
      "filter by onlyLastSeller works",
      { c =>
        val unique = c.map(_.meta.sellersCountAfterPast).find(_.isDefined).flatten
        Filters.Empty.copy(onlyLastSeller = unique.contains(1))
      },
      { case (c, filter) =>
        if (filter.onlyLastSeller) c.meta.sellersCountAfterPast.contains(1)
        else true
      }
    ),
    FilterSpec(
      "filter by sellerType works",
      { c =>
        val unique = c
          .map(_.offer.sellerType)
          .headOption
        Filters.Empty.copy(sellerType = unique.map(v => EqValue(v)).getOrElse(EmptyEq))
      },
      { case (c, filter) =>
        filter.sellerType match {
          case Filters.EmptyEq => true
          case EqValue(value) => value == c.offer.sellerType
        }
      }
    ),
    FilterSpec(
      "filter by lastEventTypes works",
      { c =>
        val unique = c
          .map(_.meta.lastEventType)
          .headOption
        Filters.Empty.copy(lastEventTypes = unique.toList.toSet)
      },
      { case (c, filter) => filter.lastEventTypes.contains(c.meta.lastEventType) }
    )
  )
}
