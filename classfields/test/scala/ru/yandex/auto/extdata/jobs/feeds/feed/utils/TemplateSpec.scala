package ru.yandex.auto.extdata.jobs.feeds.feed.utils

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.auto.core.catalog.model.{MarkImpl, ModelImpl}
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.core.region.RegionService
import ru.yandex.auto.eds.catalog.cars.structure.{MarkStructure, ModelStructure}
import ru.yandex.auto.eds.service.cars.CarsCatalogGroupingService
import ru.yandex.auto.extdata.jobs.feeds.feed.{FeedProperties, TemplateWriter}
import ru.yandex.auto.extdata.jobs.feeds.feed.model.DescriptionTemplate
import ru.yandex.auto.extdata.jobs.feeds.feed.model.DescriptionTemplate.{
  UnexistingPlaceholderEntryString,
  WrongPlaceholderEntryString
}
import ru.yandex.auto.extdata.jobs.feeds.feed.model.CarAdMessagePlaceholder._
import ru.yandex.auto.message.CarAdSchema
import ru.yandex.auto.message.CarAdSchema.DiscountPrice
import ru.yandex.auto.message.CarAdSchema.DiscountPrice.DiscountPriceStatus
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.{Failure, Try}

/**
  * Created by theninthowl on 12/13/21
  */
@RunWith(classOf[JUnitRunner])
class TemplateSpec extends WordSpec with Matchers with MockitoSupport with FeedPropertiesTest {

  private val template: String = "Test description with ${MARK} and ${MODEL}"
  private val templateWithEmptyPlaceholders: String = "Test description with ${} and ${}"
  private val templateWithWrongPlaceholders: String = "Test description with ${MARK} and ${MODEL} and ${PEPYAKA}"

  private val description = DescriptionTemplate.fromString(template)

  private val id = "1"
  private val mark = "testMark"
  private val model = "testModel"
  private val discountPrice = 200
  private val autoruHash = "123"
  private val state = Search.NEW

  private val markName = "testMarkName"
  private val modelName = "testModelName"

  private val msgBuilder = {
    val builder = CarAdSchema.CarAdMessage.newBuilder().setVersion(AutoSchemaVersions.CAR_AD_VERSION)
    import builder._
    setId(id)
    setMark(mark)
    setModel(model)
    setSearchState(state.name().toLowerCase)
    setAutoruHashCode(autoruHash)
    setDiscount(
      DiscountPrice
        .newBuilder()
        .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
        .setPrice(discountPrice)
        .setStatus(DiscountPriceStatus.ACTIVE)
    )
    builder
  }

  private val msg = msgBuilder.build()
  private val templateProperties = properties.copy(descriptionTemplate = Some(description))

  private val markImpl = mock[MarkImpl]
  when(markImpl.getMarkName).thenReturn(markName)
  private val modelImpl = mock[ModelImpl]
  when(modelImpl.getModelName).thenReturn(modelName)

  private val modelStruct = mock[ModelStructure]
  when(modelStruct.entity).thenReturn(modelImpl)

  private val markStruct = mock[MarkStructure]
  when(markStruct.entity).thenReturn(markImpl)
  when(markStruct.getModelByCode).thenReturn((_: String) => Some(modelStruct))

  private val groupingService = mock[CarsCatalogGroupingService]
  when(groupingService.getMarkByCode).thenReturn((_: String) => Some(markStruct))

  class Writer(
      override val props: FeedProperties,
      override val catalog: CarsCatalogGroupingService,
      override val regionService: RegionService
  ) extends TemplateWriter

  "DescriptionTemplate.fromString" should {
    "parse placeholders" in {
      description.placeholders shouldBe
        Set(
          MarkPlaceholder,
          ModelPlaceholder
        )
    }
    "throw entry exception" in {
      Try(DescriptionTemplate.fromString(templateWithEmptyPlaceholders)) shouldBe Failure(
        WrongPlaceholderEntryString("${}")
      )
    }
    "throw unexisting placeholder exception" in {
      Try(DescriptionTemplate.fromString(templateWithWrongPlaceholders)) shouldBe Failure(
        UnexistingPlaceholderEntryString("PEPYAKA")
      )
    }
  }

  // region placeholder cannot be set in tests - WriterUtils.regionService is not initialized
  "TemplateWriter.buildWithTemplate" should {
    "build description from CarAdMessage" in {
      val writer = new Writer(templateProperties, groupingService, mock[RegionService])
      writer.buildWithTemplate(msg) should equal(
        Some("Test description with testMarkName and testModelName")
      )
    }
  }
}
