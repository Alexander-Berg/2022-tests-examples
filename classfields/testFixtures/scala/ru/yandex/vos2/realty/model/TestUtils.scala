package ru.yandex.vos2.realty.model

import java.util.concurrent.ThreadLocalRandom

import ru.yandex.vos2.BasicsModel.{Location, TrustLevel}
import ru.yandex.vos2.OfferModel._
import ru.yandex.vertis.vos2.model.realty.RealtyOffer
import ru.yandex.vertis.vos2.model.realty.RealtyOffer.{RealtyCategory, RealtyPropertyType}
import ru.yandex.vos2.UserModel.{User, UserType}
import ru.yandex.vos2.realty.model.offer.OfferStatusHistoryGenerator

import scala.collection.JavaConverters._

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
object TestUtils {

  def createUser(): User.Builder = {
    User
      .newBuilder()
      .setUserRef("user")
      .setAlternativeIds(User.AlternativeIds.newBuilder().setExternal(1))
      .setUserType(UserType.UT_OWNER)
  }

  def createLocation(): Location.Builder = {
    Location
      .newBuilder()
      .setAddress("Some address")
      .setRegion("Region")
      .setRgid(1)
  }

  def createRealtyOffer(): RealtyOffer.Builder = {
    RealtyOffer
      .newBuilder()
      .setPropertyType(RealtyPropertyType.PT_LIVING)
      .setCategory(RealtyCategory.CAT_APARTMENT)
      .setAddress(createLocation())
  }

  def createOffer(): Offer.Builder = {
    val id = ThreadLocalRandom.current().nextLong()
    Offer
      .newBuilder()
      .setUser(createUser())
      .setOfferID(s"i_$id")
      .setOfferIRef(id)
      .setUserRef("uid_1")
      .setOfferService(OfferService.OFFER_REALTY)
      .setTimestampCreate(System.currentTimeMillis())
      .setTimestampUpdate(System.currentTimeMillis())
      .setOfferRealty(createRealtyOffer())
      .setFinalTrustLevel(TrustLevel.TL_MEDIUM)
      .addAllStatusHistory(OfferStatusHistoryGenerator.OshListGen.sample.get.asJava)
      .clearFlag()
      .setOfferTTLHours(128)
  }
}
