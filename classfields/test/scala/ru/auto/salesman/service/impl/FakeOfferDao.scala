package ru.auto.salesman.service.impl

import ru.auto.salesman.TaskManaged
import ru.auto.salesman.dao.OfferDao
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao._
import ru.auto.salesman.model.{Offer, OfferId}
import ru.auto.salesman.tasks.Partition

import scala.util.{Success, Try}

class FakeOfferDao extends OfferDao {

  private var offers = Map.empty[OfferId, Offer]

  def upsert(offer: Offer): Unit = offers += offer.id -> offer

  def getById(id: OfferId): Option[Offer] = offers.get(id)

  def get(filter: Filter): Try[List[Offer]] = {

    def predicate(offer: Offer): Boolean = filter match {
      case _ => ???
    }

    Success(offers.values.filter(predicate).toList)
  }

  def update(condition: Condition, patch: OfferPatch): Try[Unit] = {

    def predicate(offer: Offer): Boolean = condition match {
      case OfferIdCategory(offerId, offerCategory) =>
        offer.id == offerId && offer.categoryId == offerCategory
      case _ => ???
    }

    def patched(offer: Offer): Offer =
      offer.copy(
        expireDate = patch.expireDate.getOrElse(offer.expireDate),
        status = patch.status.getOrElse(offer.status),
        setDate = patch.setDate.getOrElse(offer.setDate),
        freshDate = patch.freshDate.orElse(offer.freshDate)
      )

    Success(offers.values.filter(predicate).map(patched).foreach(upsert))
  }

  def getSortedUsersWithActiveOffers(
      partition: Partition,
      withUserIdMoreThan: Option[UserId]
  ): TaskManaged[Stream[UserId]] = ???
}
