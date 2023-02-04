package ru.yandex.vos2.autoru.utils.testforms

import ru.auto.api.ApiOfferModel
import ru.yandex.vos2.autoru.catalog.CatalogCard
import ru.yandex.vos2.autoru.catalog.cars.model.CarCard
import ru.yandex.vos2.autoru.catalog.moto.model.MotoCard
import ru.yandex.vos2.autoru.catalog.trucks.model.TruckCard
import ru.yandex.vos2.autoru.model.{AutoruUser, SalonPoi}
import ru.yandex.vos2.model.{UserRef, UserRefAutoru, UserRefAutoruClient}
import ru.yandex.vos2.util.{ExternalAutoruUserRef, Protobuf}

/**
  * Created by andrey on 6/5/17.
  */
sealed trait FormInfo {
  type CardType <: CatalogCard

  def optUser: Option[AutoruUser]

  def optSalon: Option[SalonPoi]

  def isDealer: Boolean = optSalon.nonEmpty

  def userRef: UserRef =
    optUser
      .map(u => u.userRef)
      .orElse(optSalon.flatMap(_.client).map(c => c.userRef))
      .getOrElse(
        sys.error("can not get user ref!")
      )

  def extUserId: String = userRef match {
    case UserRefAutoru(id) => ExternalAutoruUserRef.privateRef(id)
    case UserRefAutoruClient(id) => ExternalAutoruUserRef.salonRef(id)
  }

  def ownerId: Long = userRef match {
    case UserRefAutoru(id) => id
    case UserRefAutoruClient(id) => id
  }

  def form: ApiOfferModel.Offer

  lazy val json: String = Protobuf.toJson(form)

  def card: CatalogCard

  def withForm(form: ApiOfferModel.Offer): FormInfo
}

object FormInfo {

  def dealerForm(salon: SalonPoi, form: ApiOfferModel.Offer, card: CatalogCard): FormInfo = {
    card match {
      case x: CarCard => CarFormInfo(None, Some(salon), form, x)
      case x: TruckCard => TruckFormInfo(None, Some(salon), form, x)
      case x: MotoCard => MotoFormInfo(None, Some(salon), form, x)
    }
  }

  def privateForm(user: AutoruUser, form: ApiOfferModel.Offer, card: CatalogCard): FormInfo = {
    card match {
      case x: CarCard => CarFormInfo(Some(user), None, form, x)
      case x: TruckCard => TruckFormInfo(Some(user), None, form, x)
      case x: MotoCard => MotoFormInfo(Some(user), None, form, x)
    }
  }

  def startYear(card: CatalogCard): Int = card match {
    case x: CarCard => x.startYear
    case x: TruckCard => x.startYear
    case x: MotoCard => 1990
  }

  def endYear(card: CatalogCard): Option[Int] = card match {
    case x: CarCard => x.endYear
    case x: TruckCard => x.endYear
    case x: MotoCard => None
  }
}

case class CarFormInfo(optUser: Option[AutoruUser],
                       optSalon: Option[SalonPoi],
                       form: ApiOfferModel.Offer,
                       card: CarCard)
  extends FormInfo {
  override type CardType = CarCard

  def withForm(form: ApiOfferModel.Offer): FormInfo = copy(form = form)
}

case class TruckFormInfo(optUser: Option[AutoruUser],
                         optSalon: Option[SalonPoi],
                         form: ApiOfferModel.Offer,
                         card: TruckCard)
  extends FormInfo {
  override type CardType = TruckCard

  def withForm(form: ApiOfferModel.Offer): FormInfo = copy(form = form)
}

case class MotoFormInfo(optUser: Option[AutoruUser],
                        optSalon: Option[SalonPoi],
                        form: ApiOfferModel.Offer,
                        card: MotoCard)
  extends FormInfo {
  override type CardType = MotoCard

  def withForm(form: ApiOfferModel.Offer): FormInfo = copy(form = form)
}
