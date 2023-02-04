package ru.yandex.vertis.general.rubick.logic.test

import common.clients.uploader.testkit.NoOpUploader
import general.bonsai.public_api.{BreadcrumbsResponse, GetAttributesResponse, InternalAttribute}
import general.common.address_model.{AddressInfo, GeoPoint, SellingAddress}
import general.gost.draft_api.DraftResponse
import general.gost.draft_model.{AdditionalDraftInfo, Draft, DraftView}
import general.gost.offer_model._
import general.gost.seller_model.{Contacts, Seller}
import general.gost.validation_model.ValidationError.Subject
import general.gost.validation_model.{ValidationError, ValidationErrorCode}
import general.rubick.add_form_model.AddForm
import ru.yandex.vertis.general.bonsai.testkit.TestBonsaiService.TestBonsaiService
import ru.yandex.vertis.general.bonsai.testkit.{TestBonsaiService, TestData}
import ru.yandex.vertis.general.classifiers.addform.testkit.TestAddFormClassifier
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.rubick.logic.FormBuilder.{AvatarsConfig, Cachers}
import ru.yandex.vertis.general.rubick.logic.{
  ClassificationResultLogger,
  FormBuilder,
  SectionChains,
  TitleSuggestManager
}
import ru.yandex.vertis.general.rubick.model.EntityId
import ru.yandex.vertis.general.rubick.testkit.TestExperimentsExtractor
import common.zio.logging.Logging
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation
import zio.{Has, ZIO, ZLayer}

object FormBuilderTest extends DefaultRunnableSpec {

  private val draft = Draft(
    title = Some("Title"),
    category = Some(Category(TestData.Bosonozhki.categoryId)),
    video = Some(Video("video")),
    seller = Some(
      Seller(
        addresses = Seq(SellingAddress(Some(GeoPoint(55.7558, 37.6173)), Some(AddressInfo("Москва")))),
        contacts = Some(Contacts(phone = Some("phone")))
      )
    )
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FormBuilderTest")(
      testM("build form without errors") {
        val data = DraftResponse("id", Some(DraftView(Some(draft))), Seq.empty[ValidationError], currentControlNum = 16)
        for {
          formData <- FormBuilder.build(
            EntityId.DraftId("id"),
            data.getDraft,
            AdditionalDraftInfo.defaultInstance,
            data.validationErrors,
            SectionChains.DefaultMainChain(),
            Some(data.currentControlNum)
          )
        } yield {
          val form = AddForm(sections = formData.sections)
          assert(formData.numberOfSections)(equalTo(3)) &&
          assert(form.sections.size)(equalTo(3)) &&
          assert(form.sections.head.controls.head.control.isCategoryControl)(isTrue) &&
          assert(form.sections(1).controls.head.control.isPhotosControl)(isTrue) &&
          assert(form.sections(1).controls(1).control.isTitleControl)(isTrue) &&
          assert(form.sections(1).controls(2).control.isDescriptionControl)(isTrue) &&
          assert(form.sections(1).controls(3).control.isVideoControl)(isTrue) &&
          assert(form.sections(1).controls(4).control.isConditionControl)(isTrue) &&
          assert(form.sections(1).controls(6).control.isAttributeControl)(isTrue) &&
          assert(form.sections(1).controls)(exists(hasField("isPriceControl", _.control.isPriceControl, isTrue))) &&
          assert(form.sections(1).controls)(
            exists(hasField("isContactsControl", _.control.isContactsControl, isTrue))
          ) && assert(form.sections(2).isFinal)(isTrue) &&
          assert(form.sections(2).isCurrent)(isTrue)
        }
      },
      testM("Build form without errors with current control num in the middle") {
        val data = DraftResponse("id", Some(DraftView(Some(draft))), Seq.empty[ValidationError], currentControlNum = 4)
        for {

          formData <- FormBuilder.build(
            EntityId.DraftId("id"),
            data.getDraft,
            AdditionalDraftInfo.defaultInstance,
            data.validationErrors,
            SectionChains.DefaultMainChain(),
            Some(data.currentControlNum)
          )
        } yield {
          val form = AddForm(sections = formData.sections)
          assert(formData.numberOfSections)(equalTo(3)) &&
          assert(form.sections.size)(equalTo(3)) &&
          assert(form.sections(1).isCurrent)(isTrue)
        }
      },
      testM("build form without errors with single control in section") {
        val data = DraftResponse("id", Some(DraftView(Some(draft))), Seq.empty[ValidationError], currentControlNum = 15)
        for {
          formData <- FormBuilder.build(
            EntityId.DraftId("id"),
            data.getDraft,
            AdditionalDraftInfo.defaultInstance,
            data.validationErrors,
            SectionChains.DefaultMobileChain(),
            Some(data.currentControlNum)
          )
        } yield {
          val form = AddForm(sections = formData.sections)
          assert(form.sections.size)(equalTo(12))
        }
      },
      testM("build form") {
        val errors = Seq(
          ValidationError(
            subject = Subject.FieldPath("contacts"),
            code = ValidationErrorCode.INVALID_VALUE,
            message = "error"
          ),
          ValidationError(
            subject = Subject.FieldPath("video"),
            code = ValidationErrorCode.INVALID_VALUE,
            message = "error"
          )
        )

        val data = DraftResponse("id", Some(DraftView(Some(draft))), errors, currentControlNum = 14)
        for {
          formData <- FormBuilder.build(
            EntityId.DraftId("id"),
            data.getDraft,
            AdditionalDraftInfo.defaultInstance,
            data.validationErrors,
            SectionChains.DefaultMainChain(),
            Some(data.currentControlNum)
          )
        } yield {
          val form = AddForm(sections = formData.sections)
          assert(form.sections.size)(equalTo(2)) &&
          assert(form.sections(1).isCurrent)(isTrue)
        }
      }
    ).provideCustomLayer {
      val testBonsaiLayer =
        TestBonsaiService.layer.tap { testService: TestBonsaiService =>
          testService.get.setBreadcrumbsResponse(req =>
            ZIO.succeed {
              BreadcrumbsResponse(
                Map(
                  TestData.Bosonozhki.categoryId -> TestData.category(TestData.Bosonozhki.categoryId)
                )
              )
            }
          ) *>
            testService.get.setGetAttributesResponse(_ =>
              ZIO.succeed(
                GetAttributesResponse(
                  TestData
                    .category(TestData.Bosonozhki.categoryId)
                    .attributes
                    .values
                    .map(a => InternalAttribute(a.attribute))
                    .toSeq
                )
              )
            )
        }

      val cachers = (Cache.noop ++ Logging.live >>> RequestCacher.live)
        .map(cacher => Has(Cachers(cacher.get, cacher.get)))

      val experimentsExtractor =
        TestExperimentsExtractor.GetDisableTitlePrediction(Expectation.value(Option.empty[Boolean])).atMost(5)

      testBonsaiLayer ++ NoOpUploader.live ++ TestAddFormClassifier.layer ++ ClassificationResultLogger.noop ++
        TitleSuggestManager.noop ++ ZLayer.succeed(
          AvatarsConfig("photos")
        ) ++ Logging.live ++
        experimentsExtractor ++ cachers >>> FormBuilder.live
    }
}
