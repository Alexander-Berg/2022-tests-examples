package ru.yandex.vos2.autoru.utils.converters.formoffer

import java.time.Instant

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.utils.FormTestUtils
import ru.yandex.vos2.autoru.utils.booking.impl.EmptyDefaultBookingAllowedDeciderImpl
import ru.yandex.vos2.model.offer.OfferGenerator

@RunWith(classOf[JUnitRunner])
class FormOfferBookingConverterTest extends AnyWordSpec with InitTestDbs {
  initDbs()

  private val formTestUtils = new FormTestUtils(components)
  import formTestUtils._

  private val formOfferConverter: FormOfferConverter =
    new FormOfferConverter(
      components.carsCatalog,
      components.recognizedLpUtils,
      EmptyDefaultBookingAllowedDeciderImpl,
      components.featuresManager
    )

  private case class NewOfferTestCase(description: String,
                                      form: ApiOfferModel.Offer,
                                      check: OfferModel.Offer => Boolean)

  private case class ExistingOfferTestCase(description: String,
                                           form: ApiOfferModel.Offer,
                                           curOffer: OfferModel.Offer,
                                           check: OfferModel.Offer => Boolean)

  private case class NewDraftTestCase(description: String,
                                      form: ApiOfferModel.Offer,
                                      check: OfferModel.Offer => Boolean)

  private case class ExistingDraftTestCase(description: String,
                                           form: ApiOfferModel.Offer,
                                           curDraft: OfferModel.Offer,
                                           check: OfferModel.Offer => Boolean)

  private val newOfferTestCases: Seq[NewOfferTestCase] = Seq(
    NewOfferTestCase(
      description = "allowed",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(true).clearState()
        builder.build
      },
      check = (offer: OfferModel.Offer) => {
        val b = offer.getOfferAutoru.getBooking
        b.getAllowed && !b.hasState
      }
    ),
    NewOfferTestCase(
      description = "disallowed",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(false).clearState()
        builder.build
      },
      check = (offer: OfferModel.Offer) => {
        val b = offer.getOfferAutoru.getBooking
        !b.getAllowed && !b.hasState
      }
    ),
    NewOfferTestCase(
      description = "without booking",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.getAdditionalInfoBuilder.clearBooking()
        builder.build
      },
      check = (offer: OfferModel.Offer) => {
        !offer.getOfferAutoru.hasBooking
      }
    )
  )

  private val existingOfferTestCases: Seq[ExistingOfferTestCase] = Seq(
    {
      val booking = OfferGenerator.bookingGen.next
      ExistingOfferTestCase(
        description = "allowed",
        form = {
          val builder = salonOfferForm.toBuilder
          builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(true).clearState()
          builder.build
        },
        curOffer = {
          val builder = curPrivateProto.toBuilder
          builder.getOfferAutoruBuilder.setBooking(booking)
          builder.build()
        },
        check = (offer: OfferModel.Offer) => {
          val b = offer.getOfferAutoru.getBooking
          b.getAllowed && b.getState == booking.getState
        }
      )
    }, {
      val booking = OfferGenerator.bookingGen.next
      ExistingOfferTestCase(
        description = "disallowed",
        form = {
          val builder = salonOfferForm.toBuilder
          builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(false).clearState()
          builder.build
        },
        curOffer = {
          val builder = curPrivateProto.toBuilder
          builder.getOfferAutoruBuilder.setBooking(booking)
          builder.build()
        },
        check = (offer: OfferModel.Offer) => {
          val b = offer.getOfferAutoru.getBooking
          !b.getAllowed && b.getState == booking.getState
        }
      )
    },
    ExistingOfferTestCase(
      description = "without previous state",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(true).clearState()
        builder.build
      },
      curOffer = {
        val builder = curPrivateProto.toBuilder
        builder.getOfferAutoruBuilder.getBookingBuilder.setAllowed(true).clearState()
        builder.build()
      },
      check = (offer: OfferModel.Offer) => {
        val b = offer.getOfferAutoru.getBooking
        b.getAllowed && !b.hasState
      }
    ), {
      val booking = OfferGenerator.bookingGen.next
      ExistingOfferTestCase(
        description = "without new booking",
        form = {
          val builder = salonOfferForm.toBuilder
          builder.getAdditionalInfoBuilder.clearBooking()
          builder.build
        },
        curOffer = {
          val builder = curPrivateProto.toBuilder
          builder.getOfferAutoruBuilder.setBooking(booking)
          builder.build()
        },
        check = (offer: OfferModel.Offer) => {
          offer.getOfferAutoru.hasBooking &&
            offer.getOfferAutoru.getBooking == booking
        }
      )
    }
  )

  private val newDraftTestCases: Seq[NewDraftTestCase] = Seq(
    NewDraftTestCase(
      description = "allowed",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(true).clearState()
        builder.build
      },
      check = (offer: OfferModel.Offer) => {
        val b = offer.getOfferAutoru.getBooking
        b.getAllowed && !b.hasState
      }
    )
  )

  private val existingDraftTestCases: Seq[ExistingDraftTestCase] = Seq {
    val booking = OfferGenerator.bookingGen.next
    ExistingDraftTestCase(
      description = "allowed",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(true).clearState()
        builder.build
      },
      curDraft = {
        val builder = curPrivateProto.toBuilder
        builder.getOfferAutoruBuilder.setBooking(booking)
        builder.build()
      },
      check = (offer: OfferModel.Offer) => {
        val b = offer.getOfferAutoru.getBooking
        b.getAllowed && b.getState == booking.getState
      }
    )
  }

  private def now(): Long = Instant.now.getEpochSecond

  "FormOfferBookingConverter" should {
    "convertNewOffer" when {
      newOfferTestCases.foreach {
        case NewOfferTestCase(description, form, check) =>
          description in {
            val actual = formOfferConverter.convertNewOffer(
              userRef = userRef,
              category = Category.CARS,
              form = form,
              ad = privateAd,
              now = now()
            )
            assert(check(actual))
          }
      }
    }

    "convertExistingOffer" when {
      existingOfferTestCases.foreach {
        case ExistingOfferTestCase(description, form, curOffer, check) =>
          description in {
            val actual = formOfferConverter.convertExistingOffer(
              form = form,
              curOffer = curOffer,
              optDraft = None,
              ad = privateAd,
              now = now(),
              params = FormWriteParams.empty
            )
            assert(check(actual))
          }
      }
    }

    "convertNewDraft" when {
      newDraftTestCases.foreach {
        case NewDraftTestCase(description, form, check) =>
          description in {
            val actual = formOfferConverter.convertNewDraft(
              userRef = userRef,
              category = Category.CARS,
              form = form,
              ad = privateAd,
              now = now()
            )
            assert(check(actual))
          }
      }
    }

    "convertExistingDraft" when {
      existingDraftTestCases.foreach {
        case ExistingDraftTestCase(description, form, curDraft, check) =>
          description in {
            val actual = formOfferConverter.convertExistingDraft(
              form = form,
              curDraft = curDraft,
              ad = privateAd,
              now = now()
            )
            assert(check(actual))
          }
      }
    }
  }
}
