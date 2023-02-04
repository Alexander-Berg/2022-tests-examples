package ru.yandex.vos2.model.offer

import org.scalacheck.Gen
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.UserModel.User
import ru.yandex.vos2.model.CommonGen.{limitedStr, updateDateGen, CreateDateGen}
import ru.yandex.vos2.model.ids.OfferIdManager
import ru.yandex.vos2.model.user.UserGenerator

/**
  * @author Nataila Ratskevich (reimai@yandex-team.ru)
  */
object OfferGenerator {

  def offerBaseGen(
    userGen: Gen[User] = UserGenerator.NewUserGen,
    timestampCreateGen: Gen[Long] = CreateDateGen,
    timeStampUpdateGen: Long ⇒ Gen[Long] = updateDateGen,
    ttlGen: Gen[Int] = Gen.choose(10, 1000),
    offerService: OfferService,
    idManager: OfferIdManager
  ): Gen[Offer.Builder] =
    for {
      user ← userGen
      timeStampCreate ← timestampCreateGen
      timeStampUpdate ← timeStampUpdateGen(timeStampCreate)
      (offerRef, offerIRef) = idManager.allocateNewRef(user.getUserRef)
      offerId = offerRef.offerId
      legacyIRef ← Gen.choose(10000, 1000000)
      ttl ← ttlGen
      userContacts ← UserGenerator.UserContactsGen
      description ← limitedStr()
    } yield Offer
      .newBuilder()
      .setOfferID(offerId.substring(0, Math.min(offerId.length, 10)))
      .setTimestampCreate(timeStampCreate)
      .setTimestampUpdate(timeStampUpdate)
      .setOfferTTLHours(ttl)
      .setUserRef(user.getUserRef)
      .setUser(user)
      .setOfferIRef(offerIRef)
      .setOfferService(offerService)
      .setRefLegacy(legacyIRef)
      .setUserContacts(userContacts)
      .setDescription(description)
}
