package ru.yandex.vos2.autoru.utils.converters.formoffer

import java.time.Instant

import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.utils.FormTestUtils
import ru.yandex.vos2.autoru.utils.booking.impl.EmptyDefaultBookingAllowedDeciderImpl
import ru.yandex.vos2.model.offer.OfferGenerator

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class FormOfferMultipostingConverterTest extends AnyWordSpec with InitTestDbs {
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
      description = "classified active",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.getMultipostingBuilder
          .setStatus(ApiOfferModel.OfferStatus.ACTIVE)
          .addClassifieds(
            ApiOfferModel.Multiposting.Classified
              .newBuilder()
              .addServices(
                ApiOfferModel.Multiposting.Classified.Service.newBuilder().setService("ABC").setIsActive(true)
              )
              .setEnabled(true)
              .setName(ApiOfferModel.Multiposting.Classified.ClassifiedName.DROM)
          )
        builder.build
      },
      check = (offer: OfferModel.Offer) => {
        val m = offer.getMultiposting
        val statusOk = m.getStatus == CompositeStatus.CS_ACTIVE
        val classifieds = m.getClassifiedsList.asScala
        val classifiedsOk = classifieds.forall { c =>
          c.getName == OfferModel.Multiposting.Classified.ClassifiedName.DROM &&
          c.getEnabled &&
          c.getServicesList.asScala.forall(s => s.getService == "ABC" && s.getIsActive)
        }
        statusOk && classifiedsOk
      }
    ),
    NewOfferTestCase(
      description = "classified disabled",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.getMultipostingBuilder
          .setStatus(ApiOfferModel.OfferStatus.ACTIVE)
          .addClassifieds(
            ApiOfferModel.Multiposting.Classified
              .newBuilder()
              .addServices(
                ApiOfferModel.Multiposting.Classified.Service.newBuilder().setService("ABC").setIsActive(true)
              )
              .setEnabled(false)
              .setName(ApiOfferModel.Multiposting.Classified.ClassifiedName.DROM)
          )
        builder.getAdditionalInfoBuilder.getBookingBuilder.setAllowed(false).clearState()
        builder.build
      },
      check = (offer: OfferModel.Offer) => {
        val m = offer.getMultiposting
        val statusOk = m.getStatus == CompositeStatus.CS_ACTIVE
        val classifieds = m.getClassifiedsList.asScala
        val classifiedsOk = classifieds.forall { c =>
          c.getName == OfferModel.Multiposting.Classified.ClassifiedName.DROM &&
          !c.getEnabled &&
          c.getServicesList.asScala.forall(s => s.getService == "ABC" && s.getIsActive)
        }
        statusOk && classifiedsOk
      }
    ),
    NewOfferTestCase(
      description = "without multiposting",
      form = {
        val builder = salonOfferForm.toBuilder
        builder.build
      },
      check = (offer: OfferModel.Offer) => {
        !offer.hasMultiposting
      }
    )
  )

  private val existingOfferTestCases: Seq[ExistingOfferTestCase] = Seq(
    {
      val multiposting = OfferModel.Multiposting
        .newBuilder()
        .setStatus(CompositeStatus.CS_ACTIVE)
        .addClassifieds(
          OfferModel.Multiposting.Classified
            .newBuilder()
            .addServices(
              OfferModel.Multiposting.Classified.Service.newBuilder().setService("abc").setIsActive(true)
            )
            .setEnabled(true)
            .setName(OfferModel.Multiposting.Classified.ClassifiedName.DROM)
        )
        .build()
      ExistingOfferTestCase(
        description = "classified enabled",
        form = {
          val builder = salonOfferForm.toBuilder
          builder.clearMultiposting()
          builder.build
        },
        curOffer = {
          val builder = curPrivateProto.toBuilder
          builder.setMultiposting(multiposting)
          builder.build()
        },
        check = (offer: OfferModel.Offer) => {
          val m = offer.getMultiposting
          val statusOk = m.getStatus == CompositeStatus.CS_ACTIVE
          val classifieds = m.getClassifiedsList.asScala
          val classifiedsOk = classifieds.forall { c =>
            c.getName == OfferModel.Multiposting.Classified.ClassifiedName.DROM &&
            c.getEnabled &&
            c.getServicesList.asScala.forall(s => s.getService == "abc" && s.getIsActive)
          }
          statusOk && classifiedsOk
        }
      )
    }, {
      val multiposting = OfferModel.Multiposting
        .newBuilder()
        .setStatus(CompositeStatus.CS_ACTIVE)
        .addClassifieds(
          OfferModel.Multiposting.Classified
            .newBuilder()
            .addServices(
              OfferModel.Multiposting.Classified.Service.newBuilder().setService("abc").setIsActive(true)
            )
            .setEnabled(true)
            .setName(OfferModel.Multiposting.Classified.ClassifiedName.DROM)
        )
        .build()
      ExistingOfferTestCase(
        description = "new classified",
        form = {
          val multiposting = ApiOfferModel.Multiposting
            .newBuilder()
            .setStatus(ApiOfferModel.OfferStatus.ACTIVE)
            .addClassifieds(
              ApiOfferModel.Multiposting.Classified
                .newBuilder()
                .setEnabled(true)
                .setName(ApiOfferModel.Multiposting.Classified.ClassifiedName.AVITO)
            )
            .build()
          val builder = salonOfferForm.toBuilder
          builder.setMultiposting(multiposting)
          builder.build
        },
        curOffer = {
          val builder = curPrivateProto.toBuilder
          builder.setMultiposting(multiposting)
          builder.build()
        },
        check = (offer: OfferModel.Offer) => {
          val m = offer.getMultiposting
          val statusOk = m.getStatus == CompositeStatus.CS_ACTIVE
          val classifieds = m.getClassifiedsList.asScala
          val classifiedsOk = classifieds.forall { c =>
            c.getName == OfferModel.Multiposting.Classified.ClassifiedName.AVITO &&
            c.getEnabled &&
            c.getServicesList.asScala.isEmpty
          }
          statusOk && classifiedsOk
        }
      )
    }, {
      val multiposting = OfferModel.Multiposting
        .newBuilder()
        .setStatus(CompositeStatus.CS_ACTIVE)
        .addClassifieds(
          OfferModel.Multiposting.Classified
            .newBuilder()
            .setEnabled(true)
            .setName(OfferModel.Multiposting.Classified.ClassifiedName.DROM)
        )
        .build()
      ExistingOfferTestCase(
        description = "preserveClassifieds=true",
        form = {
          val multiposting = ApiOfferModel.Multiposting
            .newBuilder()
            .setPreserveClassifieds(true)
            .setStatus(ApiOfferModel.OfferStatus.ACTIVE)
            .build()
          val builder = salonOfferForm.toBuilder
          builder.setMultiposting(multiposting)
          builder.build
        },
        curOffer = {
          val builder = curPrivateProto.toBuilder
          builder.setMultiposting(multiposting)
          builder.build()
        },
        check = (offer: OfferModel.Offer) => {
          val m = offer.getMultiposting
          val statusOk = m.getStatus == CompositeStatus.CS_ACTIVE
          val classifieds = m.getClassifiedsList.asScala
          val classifiedsOk = classifieds.forall { c =>
            c.getName == OfferModel.Multiposting.Classified.ClassifiedName.DROM &&
            c.getEnabled &&
            c.getServicesList.asScala.isEmpty
          }
          statusOk && classifiedsOk
        }
      )
    }
  )

  private val newDraftTestCases: Seq[NewDraftTestCase] = Seq(
    NewDraftTestCase(
      description = "classified",
      form = {
        val multiposting = ApiOfferModel.Multiposting
          .newBuilder()
          .setStatus(ApiOfferModel.OfferStatus.ACTIVE)
          .addClassifieds(
            ApiOfferModel.Multiposting.Classified
              .newBuilder()
              .setEnabled(true)
              .setName(ApiOfferModel.Multiposting.Classified.ClassifiedName.AVITO)
          )
          .build()
        val builder = salonOfferForm.toBuilder
        builder.setMultiposting(multiposting)
        builder.build
      },
      check = (offer: OfferModel.Offer) => {
        val m = offer.getMultiposting
        val statusOk = m.getStatus == CompositeStatus.CS_ACTIVE
        val classifieds = m.getClassifiedsList.asScala
        val classifiedsOk = classifieds.forall { c =>
          c.getName == OfferModel.Multiposting.Classified.ClassifiedName.AVITO &&
          c.getEnabled &&
          c.getServicesList.asScala.isEmpty
        }
        statusOk && classifiedsOk
      }
    )
  )

  private val existingDraftTestCases: Seq[ExistingDraftTestCase] = Seq {
    val multiposting = OfferModel.Multiposting
      .newBuilder()
      .setStatus(CompositeStatus.CS_ACTIVE)
      .addClassifieds(
        OfferModel.Multiposting.Classified
          .newBuilder()
          .addServices(
            OfferModel.Multiposting.Classified.Service.newBuilder().setService("abc").setIsActive(true)
          )
          .setEnabled(true)
          .setName(OfferModel.Multiposting.Classified.ClassifiedName.DROM)
      )
      .build()
    ExistingDraftTestCase(
      description = "new classified",
      form = {
        val multiposting = ApiOfferModel.Multiposting
          .newBuilder()
          .setStatus(ApiOfferModel.OfferStatus.ACTIVE)
          .addClassifieds(
            ApiOfferModel.Multiposting.Classified
              .newBuilder()
              .setEnabled(true)
              .setName(ApiOfferModel.Multiposting.Classified.ClassifiedName.AVITO)
          )
          .build()
        val builder = salonOfferForm.toBuilder
        builder.setMultiposting(multiposting)
        builder.build
      },
      curDraft = {
        val builder = curPrivateProto.toBuilder
        builder.setMultiposting(multiposting)
        builder.build()
      },
      check = (offer: OfferModel.Offer) => {
        val m = offer.getMultiposting
        val statusOk = m.getStatus == CompositeStatus.CS_ACTIVE
        val classifieds = m.getClassifiedsList.asScala
        val classifiedsOk = classifieds.forall { c =>
          c.getName == OfferModel.Multiposting.Classified.ClassifiedName.AVITO &&
          c.getEnabled &&
          c.getServicesList.asScala.isEmpty
        }
        statusOk && classifiedsOk
      }
    )
  }

  private def now(): Long = Instant.now.getEpochSecond

  "FormOfferMultipostingConverter" should {
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
