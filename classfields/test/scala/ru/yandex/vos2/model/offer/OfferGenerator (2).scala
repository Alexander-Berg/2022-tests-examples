package ru.yandex.vos2.model.offer

import scala.jdk.CollectionConverters._
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel
import ru.yandex.vertis.moderation.proto.Model.{Metadata => ModerationMetadata}
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Booking, ProvenOwnerModerationState}
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.UserModel.User
import ru.yandex.vos2.model.CommonGen.{limitedStr, updateDateGen, CreateDateGen}
import ru.yandex.vos2.model.ids.OfferIdManager
import ru.yandex.vos2.model.user.UserGenerator
import ru.yandex.vos2.model.CommonGen._

/**
  * @author Nataila Ratskevich (reimai@yandex-team.ru)
  */
object OfferGenerator {

  def offerBaseGen(userGen: Gen[User] = UserGenerator.NewUserGen,
                   timestampCreateGen: Gen[Long] = CreateDateGen,
                   timeStampUpdateGen: Long => Gen[Long] = updateDateGen,
                   ttlGen: Gen[Int] = Gen.choose(10, 1000),
                   offerService: OfferService,
                   idManager: OfferIdManager): Gen[Offer.Builder] =
    for {
      user <- userGen
      timeStampCreate <- timestampCreateGen
      timeStampUpdate <- timeStampUpdateGen(timeStampCreate)
      (offerRef, offerIRef) = idManager.allocateNewRef(user.getUserRef)
      offerId = offerRef.offerId
      legacyIRef <- Gen.choose(10000, 1000000)
      userContacts <- UserGenerator.UserContactsGen
      description <- limitedStr()
    } yield Offer
      .newBuilder()
      .setOfferID(offerId.substring(0, Math.min(offerId.length, 10)))
      .setTimestampCreate(timeStampCreate)
      .setTimestampUpdate(timeStampUpdate)
      .setUserRef(user.getUserRef)
      .setUser(user)
      .setOfferIRef(offerIRef)
      .setOfferService(offerService)
      .setRefLegacy(legacyIRef)
      .setUserContacts(userContacts)
      .setDescription(description)

  def offerWithRequiredFields(offerService: OfferService): Gen[Offer.Builder] =
    for {
      timestamp <- Gen.posNum[Long]
      userRef <- UserGenerator.AidRefGen
    } yield {
      Offer.newBuilder
        .setOfferService(offerService)
        .setTimestampUpdate(timestamp)
        .setUserRef(userRef.toPlain)
    }

  def autoruOfferWithRequiredFields(): Gen[AutoruOffer.Builder] =
    for {
      version <- Gen.posNum[Int]
      category <- Gen.oneOf(ApiOfferModel.Category.CARS, ApiOfferModel.Category.MOTO, ApiOfferModel.Category.TRUCKS)
    } yield AutoruOffer.newBuilder.setVersion(version).setCategory(category)

  private val provenOwnerModerationVerdictGen: Gen[ModerationMetadata.ProvenOwnerMetadata.Verdict] =
    Gen.oneOf(ModerationMetadata.ProvenOwnerMetadata.Verdict.values)

  val provenOwnerModerationStateGen: Gen[ProvenOwnerModerationState] =
    for {
      ts <- protobufTimestampGen
      // @see AUTORUBACK-3141
      (verdicts, state) <- Gen.oneOf(
        // Legacy version
        provenOwnerModerationVerdictGen.map { state =>
          (Seq.empty, Some(state))
        },
        // Intermediate version with both fields filled
        Gen.nonEmptyListOf(provenOwnerModerationVerdictGen).map(_.distinct).map { verdicts =>
          val state =
            verdicts.find(_ == ModerationMetadata.ProvenOwnerMetadata.Verdict.PROVEN_OWNER_OK).getOrElse(verdicts.head)
          (verdicts, Some(state))
        },
        // New version
        Gen.nonEmptyListOf(provenOwnerModerationVerdictGen).map(_.distinct).map { verdicts =>
          (verdicts, None)
        }
      )
    } yield {
      val builder = AutoruOffer.ProvenOwnerModerationState
        .newBuilder()
        .setTimestamp(ts)
        .addAllVerdicts(verdicts.asJava)

      state.foreach(builder.setState)

      builder.build
    }

  private val bookingBookedStateGen: Gen[Booking.State.Booked] = for {
    bookingId <- Gen.alphaNumStr
    period <- timeRangeGen
    user <- UserGenerator.NewUserGen
  } yield Booking.State.Booked.newBuilder
    .setBookingId(bookingId)
    .setPeriod(period)
    .setUserRef(user.getUserRef)
    .build()

  private val bookingStateGen: Gen[Booking.State] = for {
    updated <- protobufTimestampGen
    bookedState <- bookingBookedStateGen
    isBooked <- booleanGen
  } yield {
    val builder = AutoruOffer.Booking.State.newBuilder
      .setUpdated(updated)
    if (isBooked) {
      builder.setBooked(bookedState)
    } else {
      builder.setNotBooked(Booking.State.NotBooked.newBuilder)
    }
    builder.build()
  }

  val bookingGen: Gen[Booking] = for {
    allowed <- booleanGen
    state <- bookingStateGen
  } yield AutoruOffer.Booking
    .newBuilder()
    .setAllowed(allowed)
    .setState(state)
    .build()
}
