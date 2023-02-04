package ru.yandex.vertis.general.rubick.logic.test.control

import common.clients.uploader.Uploader
import common.clients.uploader.testkit.NoOpUploader
import common.zio.grpc.client.GrpcClient
import general.bonsai.attribute_model.{AddFormSettings, AttributeDefinition, DictionarySettings}
import general.bonsai.category_model.{Category => FullCategory, CategoryAttribute}
import general.bonsai.public_api.{AttributeData, CategoryData}
import general.classifiers.add_form_classifier_api.AddFormClassifierServiceGrpc.AddFormClassifierService
import general.common.address_model.{AddressInfo, GeoPoint, SellingAddress}
import general.common.delivery_model.{DeliveryInfo, SelfDelivery}
import general.common.price_model.Price.Price.PriceInCurrency
import general.common.price_model.{PriceInCurrency => PriceData, _}
import general.gost.draft_model.{AdditionalDraftInfo, Draft, DraftAttribute, DraftAttributeValue, DraftView}
import general.gost.offer_model._
import general.gost.seller_model._
import general.gost.validation_model.ValidationError.Subject
import general.gost.validation_model.{ValidationError, ValidationErrorCode}
import ru.yandex.vertis.general.classifiers.addform.testkit.TestAddFormClassifier
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.rubick.logic.control.ControlBuilder.Controls
import ru.yandex.vertis.general.rubick.logic.control.{ControlBuilder, ControlBuilderDependencies}
import ru.yandex.vertis.general.rubick.logic.control.builders._
import ru.yandex.vertis.general.rubick.logic.experiments.ExperimentsExtractor
import ru.yandex.vertis.general.rubick.logic.ClassificationResultLogger
import ru.yandex.vertis.general.rubick.logic.control.ControlBuilderDependencies.CategoryDependency
import ru.yandex.vertis.general.rubick.model.EntityId
import ru.yandex.vertis.general.rubick.logic.{ClassificationResultLogger, TitleSuggestManager}
import ru.yandex.vertis.general.rubick.model.Control
import ru.yandex.vertis.general.rubick.model.EntityId.DraftId
import ru.yandex.vertis.general.rubick.testkit.TestExperimentsExtractor
import common.zio.logging.Logging
import zio.ZIO
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation
import com.google.protobuf.empty.Empty
import general.common.contact_way.ContactWayEnum.ContactWay
import general.rubick.add_form_model.SalaryControl
import ru.yandex.vertis.general.rubick.logic.control.ControlBuilder.NotReady

object ControlBuildersTest extends DefaultRunnableSpec {

  private def buildDependencies(
      entityId: EntityId,
      draft: DraftView,
      avatarsNamespace: String = "o-yandex",
      validationErrors: Map[Subject, Seq[ValidationError]] = Map.empty,
      categoryOpt: Option[CategoryDependency] = None) = for {
    uploader <- ZIO.service[Uploader.Service]
    classifier <- ZIO.service[GrpcClient.Service[AddFormClassifierService]]
    requestCacher <- ZIO.service[RequestCacher.Service]
    classificationResultLogger <- ZIO.service[ClassificationResultLogger.Service]
    experimentsExtractor <- ZIO.service[ExperimentsExtractor.Service]
    titleSuggestManager <- ZIO.service[TitleSuggestManager.Service]
  } yield ControlBuilderDependencies(
    entityId = entityId,
    draft = draft,
    additionalDraftInfo = AdditionalDraftInfo.defaultInstance,
    validationErrors = validationErrors,
    uploader = uploader,
    avatarsNamespace = avatarsNamespace,
    classifier = classifier,
    categoryOpt = categoryOpt,
    distributedCacher = requestCacher,
    memoryCacher = requestCacher,
    classificationResultLogger = classificationResultLogger,
    experimentsExtractor = experimentsExtractor,
    titleSuggestManager = titleSuggestManager,
    previousControls = Nil
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ControlBuilders")(
      testM("build address control") {
        val addresses = Seq(
          SellingAddress(Some(GeoPoint(55.7558, 37.6173)), Some(AddressInfo("Москва"))),
          SellingAddress(Some(GeoPoint(55.7558, 37.6173)), Some(AddressInfo("Москва")))
        )
        val draft = DraftView(
          Some(
            Draft(seller =
              Some(
                Seller(addresses = addresses)
              )
            )
          )
        )
        val subject = Subject.FieldPath("addresses")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.INVALID_VALUE, message = "error")
          )
        )
        for {
          control <- AddressControlBuilder.build(draft, errors)
          addressControl = control.controlValue.addressControl.get
        } yield assert(control.containsError)(isTrue) &&
          assert(addressControl.values)(hasSameElements(addresses)) &&
          assert(addressControl.errors.head.error.isFieldValidation)(isTrue) &&
          assert(addressControl.errors.head.error.fieldValidation.get.comment)(equalTo("error"))
      },
      testM("build category control") {
        val draft = DraftView(Some(Draft(category = Some(Category("id")))))
        val subject = Subject.FieldPath("category")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.INVALID_VALUE, message = "error")
          )
        )
        for {
          control <- CategoryControlBuilder.build(draft, errors)
          categoryControl = control.controlValue.categoryControl.get
        } yield assert(control.containsError)(isTrue) &&
          assert(categoryControl.categoryId)(equalTo("id")) &&
          assert(categoryControl.errors.head.error.isCategoryForbidden)(isTrue)
      },
      testM("build contacts control") {
        val draft = DraftView(
          Some(
            Draft(seller =
              Some(
                Seller(contacts = Some(Contacts(phone = Some("phone"))))
              )
            )
          )
        )
        val subject = Subject.FieldPath("contacts")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.INVALID_VALUE, message = "error")
          )
        )
        for {
          control <- ContactsControlBuilder.build(draft, errors)
          contactsControl = control.controlValue.contactsControl.get
        } yield assert(control.containsError)(isTrue) &&
          assert(contactsControl.getValue.getPhone)(equalTo("phone")) &&
          assert(contactsControl.errors.head.error.isFieldValidation)(isTrue) &&
          assert(contactsControl.errors.head.error.fieldValidation.get.comment)(equalTo("error"))
      },
      testM("build photos control") {
        val draft = DraftView(Some(Draft()))
        val subject = Subject.FieldPath("photo")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.VALUE_REQUIRED, message = "error")
          )
        )
        for {
          uploader <- ZIO.service[Uploader.Service]
          classifier <- ZIO.service[GrpcClient.Service[AddFormClassifierService]]
          requestCacher <- ZIO.service[RequestCacher.Service]
          classificationResultLogger <- ZIO.service[ClassificationResultLogger.Service]
          experimentsExtractor <- ZIO.service[ExperimentsExtractor.Service]
          deps <- buildDependencies(DraftId(""), draft, "photos", errors)
          output <- PhotosControlBuilder.build(deps)
          controlOpt = output match {
            case ControlBuilder.Controls(controls) => controls.headOption.filter(_ => controls.size == 1)
            case ControlBuilder.NotReady => None
          }
          photosControl = controlOpt.map(_.controlValue.photosControl.get)
        } yield assert(controlOpt.exists(_.containsError))(isTrue) &&
          assert(photosControl.map(_.photos.toList))(equalTo(Some(Nil))) &&
          assert(photosControl.exists(_.errors.head.error.isValueRequired))(isTrue)
      },
      testM("build title control") {
        val draft = DraftView(Some(Draft(title = Some("Title"))))
        for {
          uploader <- ZIO.service[Uploader.Service]
          classifier <- ZIO.service[GrpcClient.Service[AddFormClassifierService]]
          requestCacher <- ZIO.service[RequestCacher.Service]
          classificationResultLogger <- ZIO.service[ClassificationResultLogger.Service]
          titleSuggestManager <- ZIO.service[TitleSuggestManager.Service]
          experimentsExtractor <- ZIO.service[ExperimentsExtractor.Service]
          dependencies = ControlBuilderDependencies(
            DraftId(""),
            draft,
            AdditionalDraftInfo.defaultInstance,
            Map.empty[Subject, Seq[ValidationError]],
            uploader,
            "photos",
            classifier,
            None,
            requestCacher,
            requestCacher,
            classificationResultLogger,
            experimentsExtractor,
            titleSuggestManager,
            Nil
          )
          result <- TitleControlBuilder.build(dependencies)
        } yield assert(result)(
          isSubtype[Controls](
            hasField[Controls, List[Control]](
              "controls",
              _.controls,
              hasFirst(
                hasField[Control, Boolean]("containsError", _.containsError, isFalse) &&
                  hasField[Control, Option[String]](
                    "title",
                    _.controlValue.titleControl.flatMap(_.value),
                    equalTo[Option[String], Option[String]](Some("Title"))
                  )
              )
            )
          )
        )
      },
      testM("build video control") {

        val draft = DraftView(Some(Draft(video = Some(Video(url = "url")))))
        val subject = Subject.FieldPath("video")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.INVALID_VALUE, message = "error")
          )
        )
        for {
          control <- VideoControlBuilder.build(draft, errors)
          videoControl = control.controlValue.videoControl.get
        } yield assert(control.containsError)(isTrue) &&
          assert(videoControl.getVideo.url)(equalTo("url")) &&
          assert(videoControl.errors.head.error.isFieldValidation)(isTrue) &&
          assert(videoControl.errors.head.error.fieldValidation.get.comment)(equalTo("error"))
      },
      testM("build price control") {

        val draft = DraftView(Some(Draft(price = Some(Price(PriceInCurrency(PriceData(100)))))))
        val subject = Subject.FieldPath("price")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.INVALID_VALUE, message = "error")
          )
        )
        for {
          control <- PriceControlBuilder.build(draft, errors)
          priceControl = control.controlValue.priceControl.get
        } yield assert(control.containsError)(isTrue) &&
          assert(priceControl.getValue.getPriceInCurrency.rurPrice)(equalTo(100L)) &&
          assert(priceControl.errors.head.error.isFieldValidation)(isTrue) &&
          assert(priceControl.errors.head.error.fieldValidation.get.comment)(equalTo("error"))
      },
      testM("build description control") {

        val draft = DraftView(Some(Draft(description = "Description")))
        val subject = Subject.FieldPath("description")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.VALUE_REQUIRED, message = "error")
          )
        )
        for {
          control <- DescriptionControlBuilder.build(draft, errors)
          descriptionControl = control.controlValue.descriptionControl.get
        } yield assert(control.containsError)(isTrue) &&
          assert(descriptionControl.value)(equalTo("Description")) &&
          assert(descriptionControl.errors.head.error.isValueRequired)(isTrue)
      },
      testM("build condition control") {

        val draft = DraftView(Some(Draft(condition = Offer.Condition.USED)))
        val subject = Subject.FieldPath("condition")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.VALUE_REQUIRED, message = "error")
          )
        )
        for {
          control <- ConditionControlBuilder.build(draft, errors)
          conditionControl = control.controlValue.conditionControl.get
        } yield assert(control.containsError)(isTrue) &&
          assert(conditionControl.value)(equalTo(Offer.Condition.USED)) &&
          assert(conditionControl.errors.head.error.isValueRequired)(isTrue)
      },
      testM("build salary control") {

        val salary = Salary(salaryRur = 17L)
        val draft = DraftView(Some(Draft(price = Some(Price(Price.Price.Salary(salary))))))
        val subject = Subject.FieldPath("salary")
        val errors: Map[Subject, Seq[ValidationError]] = Map(
          subject -> Seq(
            ValidationError(subject = subject, code = ValidationErrorCode.VALUE_REQUIRED, message = "error")
          )
        )
        for {
          control <- SalaryControlBuilder.build(draft, errors)
          salaryControl = control.controlValue.salaryControl.get
        } yield assert(control.containsError)(isTrue) && assert(salaryControl.value)(isSome(equalTo(salary))) &&
          assert(salaryControl.salaryData)(equalTo(SalaryControl.SalaryData.Salary(salary))) &&
          assert(salaryControl.errors.head.error.isValueRequired)(isTrue)
      },
      testM("build self delivery control") {
        val draft = DraftView(
          Some(Draft(deliveryInfo = Some(DeliveryInfo(selfDelivery = Some(SelfDelivery(sendByCourier = true))))))
        )
        for {
          control <- SelfDeliveryControlBuilder.build(draft, Map.empty)
          deliveryControl = control.controlValue.selfDeliveryControl.get
        } yield assert(deliveryControl.getSelfDelivery.sendWithinRussia)(isFalse) &&
          assert(deliveryControl.getSelfDelivery.sendByCourier)(isTrue)
      },
      testM("Предсказывать значения атрибутов кроме очищенных пользователем") {
        val draft = DraftView(
          Some(
            Draft(
              title = Some("самсунг android"),
              description = "красный октябрь",
              attributes = Seq(
                DraftAttribute(
                  id = "color",
                  value = Some(DraftAttributeValue(DraftAttributeValue.Value.ErasedByUser(Empty())))
                )
              )
            )
          )
        )
        val attributes = Map(
          "brand" -> AttributeData(
            Some(
              AttributeDefinition(
                id = "brand",
                version = 1,
                attributeSettings = AttributeDefinition.AttributeSettings.DictionarySettings(
                  DictionarySettings(
                    Seq(DictionarySettings.DictionaryValue("samsung", "Samsung", synonyms = Seq("самсунг")))
                  )
                ),
                addFormSettings = Some(AddFormSettings(AddFormSettings.AddFormControl.SELECT))
              )
            )
          ),
          "color" -> AttributeData(
            Some(
              AttributeDefinition(
                id = "color",
                version = 1,
                attributeSettings = AttributeDefinition.AttributeSettings.DictionarySettings(
                  DictionarySettings(
                    Seq(DictionarySettings.DictionaryValue("red", "красный"))
                  )
                ),
                addFormSettings = Some(AddFormSettings(AddFormSettings.AddFormControl.SELECT))
              )
            )
          ),
          "software" -> AttributeData(
            Some(
              AttributeDefinition(
                id = "software",
                version = 1,
                attributeSettings = AttributeDefinition.AttributeSettings.DictionarySettings(
                  DictionarySettings(
                    Seq(DictionarySettings.DictionaryValue("android", "android"))
                  )
                ),
                addFormSettings = Some(AddFormSettings(AddFormSettings.AddFormControl.SELECT))
              )
            )
          )
        )
        val category =
          FullCategory(attributes =
            attributes.flatMap(_._2.attribute).map(a => CategoryAttribute(a.id, a.version)).toSeq
          )

        for {
          deps <- buildDependencies(
            DraftId("onone"),
            draft,
            categoryOpt = Some(CategoryDependency(CategoryData(Some(category), attributes, true), Nil))
          )
          out <- AttributeControlBuilder.build(deps)
          controls = out match {
            case Controls(c) => c
            case _ => Nil
          }
          attributeValues = controls.flatMap(_.controlValue.attributeControl.flatMap(_.value))
        } yield assert(attributeValues)(
          hasSameElements(
            Seq(
              DraftAttributeValue(
                DraftAttributeValue.Value.Present(AttributeValue(AttributeValue.Value.Dictionary("samsung")))
              ),
              DraftAttributeValue(
                DraftAttributeValue.Value.Present(AttributeValue(AttributeValue.Value.Dictionary("android")))
              ),
              DraftAttributeValue(DraftAttributeValue.Value.ErasedByUser(com.google.protobuf.empty.Empty()))
            )
          )
        )
      }
    ).provideCustomLayer {
      val experimentsExtractor =
        TestExperimentsExtractor.GetDisableTitlePrediction(Expectation.value(Option.empty[Boolean])).optional

      NoOpUploader.live ++ ClassificationResultLogger.noop ++ TestAddFormClassifier.layer ++ TitleSuggestManager.noop ++
        (Cache.noop ++ Logging.live >>> RequestCacher.live) ++ experimentsExtractor
    }
}
