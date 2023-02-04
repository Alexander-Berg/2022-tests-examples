package ru.yandex.vertis.general.rubick.logic.test.section

import common.clients.uploader.Uploader
import common.clients.uploader.testkit.NoOpUploader
import common.zio.grpc.client.GrpcClient
import general.classifiers.add_form_classifier_api.AddFormClassifierServiceGrpc.AddFormClassifierService
import general.common.address_model.{AddressInfo, GeoPoint, SellingAddress}
import general.gost.draft_model.{AdditionalDraftInfo, Draft, DraftView}
import general.gost.offer_model._
import general.gost.seller_model._
import general.gost.validation_model.{ValidationError, ValidationErrorCode}
import general.gost.validation_model.ValidationError.Subject
import ru.yandex.vertis.general.classifiers.addform.testkit.TestAddFormClassifier
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.rubick.logic.{ClassificationResultLogger, TitleSuggestManager}
import ru.yandex.vertis.general.rubick.logic.control.ControlBuilderDependencies
import ru.yandex.vertis.general.rubick.logic.control.builders._
import ru.yandex.vertis.general.rubick.logic.experiments.ExperimentsExtractor
import ru.yandex.vertis.general.rubick.logic.section.SectionBuilder
import ru.yandex.vertis.general.rubick.logic.section.builders.SimpleSectionBuilder
import ru.yandex.vertis.general.rubick.model.DefaultSection
import ru.yandex.vertis.general.rubick.model.EntityId.DraftId
import ru.yandex.vertis.general.rubick.testkit.TestExperimentsExtractor
import common.zio.logging.Logging
import zio.ZIO
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation

object SimpleSectionBuilderTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("SimpleSectionBuilder")(
      testM("build section") {
        val draft = DraftView(
          Some(
            Draft(
              title = Some("Title"),
              category = Some(Category("category")),
              video = Some(Video("video")),
              seller = Some(
                Seller(
                  addresses = Seq(SellingAddress(Some(GeoPoint(55.7558, 37.6173)), Some(AddressInfo("Москва")))),
                  contacts = Some(Contacts(phone = Some("phone")))
                )
              )
            )
          )
        )

        val sectionBuilder = SimpleSectionBuilder(
          CategoryControlBuilder,
          TitleControlBuilder,
          VideoControlBuilder,
          ContactsControlBuilder,
          AddressControlBuilder
        )

        val errors: Map[Subject, Seq[ValidationError]] = Map(
          Subject.FieldPath("contacts") -> Seq(
            ValidationError(code = ValidationErrorCode.INVALID_VALUE, message = "error")
          ),
          Subject.FieldPath("video") -> Seq(
            ValidationError(code = ValidationErrorCode.INVALID_VALUE, message = "error")
          )
        )

        for {
          uploader <- ZIO.service[Uploader.Service]
          classifier <- ZIO.service[GrpcClient.Service[AddFormClassifierService]]
          requestCacher <- ZIO.service[RequestCacher.Service]
          classificationResultLogger <- ZIO.service[ClassificationResultLogger.Service]
          experimentsExtractor <- ZIO.service[ExperimentsExtractor.Service]
          titleSuggestManager <- ZIO.service[TitleSuggestManager.Service]
          result <- sectionBuilder.build(
            ControlBuilderDependencies(
              DraftId(""),
              draft,
              AdditionalDraftInfo.defaultInstance,
              errors,
              uploader,
              "",
              classifier,
              None,
              requestCacher,
              requestCacher,
              classificationResultLogger,
              experimentsExtractor,
              titleSuggestManager,
              Nil
            )
          )
          section = result match {
            case SectionBuilder.Sections(sections) =>
              sections.headOption.filter(_ => sections.size == 1).getOrElse(DefaultSection(Nil))
            case SectionBuilder.NotReady(_) => DefaultSection(Nil)
          }
        } yield assert(section.controls.size)(equalTo(5)) &&
          assert(section.controls.head.controlValue.isCategoryControl)(isTrue) &&
          assert(section.controls(1).controlValue.isTitleControl)(isTrue) &&
          assert(section.controls(2).controlValue.isVideoControl)(isTrue) &&
          assert(section.controls(3).controlValue.isContactsControl)(isTrue) &&
          assert(section.controls(4).controlValue.isAddressControl)(isTrue) &&
          assert(section.controls(2).containsError)(isTrue) &&
          assert(section.controls(3).containsError)(isTrue)
      }
    ).provideCustomLayer {
      val experimentsExtractor =
        TestExperimentsExtractor.GetDisableTitlePrediction(Expectation.value(Option.empty[Boolean])).optional
      NoOpUploader.live ++ TestAddFormClassifier.layer ++ TitleSuggestManager.noop ++
        (Cache.noop ++ Logging.live >>> RequestCacher.live) ++ ClassificationResultLogger.noop ++ experimentsExtractor
    }
}
