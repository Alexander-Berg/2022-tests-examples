package ru.auto.catalog.core.managers

import com.google.protobuf.Empty
import ru.auto.api.ApiOfferModel.Category
import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.verba.VerbaCars
import ru.auto.catalog.core.testkit.{TestDataEngine, _}
import ru.auto.catalog.core.util.ApiExceptions.{BadRequestFilterException, NotFoundFilterException}
import ru.auto.catalog.model.api.ApiModel.CatalogLevel._
import ru.auto.catalog.model.api.ApiModel.ErrorMode._
import ru.auto.catalog.model.api.ApiModel._
import ru.yandex.vertis.baker.util.api.{Request, RequestImpl}
import ru.yandex.vertis.feature.model.Feature

import scala.jdk.CollectionConverters._

class RawCatalogManagerSpec extends BaseSpec {

  private val countersEnricher = mock[CountersEnricher]

  stub(countersEnricher.enrich _) { case (catalog, _) =>
    catalog
  }
  private val AddNameplateToTechParamHumanName = Feature("", _ => true)

  val manager = new RawCatalogManager(
    TestCardCatalogWrapper,
    TestMotoCardCatalogWrapper,
    TestTruckCardCatalogWrapper,
    new VerbaCarsManager(VerbaCars.from(TestDataEngine)),
    countersEnricher
  )

  implicit private val req: Request = new RequestImpl

  "RawCatalogManager" should {
    "filterByTagsAndOffers with empty tags" in {
      val result = manager.filterByTagsAndOffers(
        CatalogByTagRequest.newBuilder().addTags("someTagThatNotExist").build(),
        None,
        None,
        None,
        None
      )

      result.getCatalogByTagCount shouldEqual 0
    }

    "return all marks" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(BreadcrumbsReturnMode.newBuilder())
        .addFilters(RawCatalogFilter.newBuilder())
        .build()
      val result = manager.filter(filter, Category.CARS).futureValue.getOrElse(sys.error("right expected"))
      result.getMarkMap should not be empty
    }

    "return exact full filter" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance)
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("BMW")
            .setModel("X1")
            .setSuperGen("5017453")
            .setConfiguration("5018134")
            .setTechParam("7150206")
            .setComplectation("5018171")
        )
        .build()
      val result = manager.filter(filter, Category.CARS).futureValue.getOrElse(sys.error("right expected"))

      result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
      result.getMarkMap.asScala.values
        .flatMap(_.getModelMap.asScala.keys)
        .toSet shouldEqual Set("X1")
      result.getSuperGenMap.asScala.keys.toSet shouldEqual Set("5017453")
      result.getConfigurationMap.asScala.keys.toSet shouldEqual Set("5018134")
      result.getTechParamMap.asScala.keys.toSet shouldEqual Set("7150206")
      result.getComplectationMap.asScala.keys.toSet shouldEqual Set("5018171")
    }

    "return exact full filter with missed ids" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance)
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setTechParam("7150206")
            .setComplectation("5018171")
        )
        .build()
      val result = manager.filter(filter, Category.CARS).futureValue.getOrElse(sys.error("right expected"))

      result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
      result.getMarkMap.asScala.values
        .flatMap(_.getModelMap.asScala.keys)
        .toSet shouldEqual Set("X1")
      result.getSuperGenMap.asScala.keys.toSet shouldEqual Set("5017453")
      result.getConfigurationMap.asScala.keys.toSet shouldEqual Set("5018134")
      result.getTechParamMap.asScala.keys.toSet shouldEqual Set("7150206")
      result.getComplectationMap.asScala.keys.toSet shouldEqual Set("5018171")
    }

    "return subtree from configuration " in {
      val filter = RawFilterRequest
        .newBuilder()
        .setSubtree(
          SubTreeReturnMode
            .newBuilder()
            .setFrom(MARK)
            .setTo(COMPLECTATION)
        )
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setConfiguration("5018134")
        )
        .build()
      val result = manager.filter(filter, Category.CARS).futureValue.getOrElse(sys.error("right expected"))

      result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
      result.getMarkMap.asScala.values
        .flatMap(_.getModelMap.asScala.keys)
        .toSet shouldEqual Set("X1")
      result.getSuperGenMap.asScala.keys.toSet shouldEqual Set("5017453")
      result.getConfigurationMap.asScala.keys.toSet shouldEqual Set("5018134")
      result.getTechParamMap.asScala.keys.toSet shouldEqual Set(
        "6335891",
        "20390100",
        "7150207",
        "7752400",
        "5018168",
        "5018167",
        "20390088",
        "7752398",
        "7150206",
        "6335907",
        "20390055",
        "20390081",
        "5018166",
        "5018165",
        "8476970",
        "20390063",
        "7150205",
        "6335896",
        "20390064",
        "20390089",
        "8476974",
        "20390068"
      )
      result.getComplectationMap.asScala.keys.toSet shouldEqual Set(
        "5018170",
        "7753433",
        "7753434",
        "7753439",
        "7753410",
        "5018171",
        "6336042"
      )
    }

    "return only requested layers" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setSubtree(
          SubTreeReturnMode
            .newBuilder()
            .setFrom(MARK)
            .setTo(SUPER_GEN)
        )
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setConfiguration("5018134")
        )
        .build()
      val result = manager.filter(filter, Category.CARS).futureValue.getOrElse(sys.error("right expected"))

      result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
      result.getMarkMap.asScala.values
        .flatMap(_.getModelMap.asScala.keys)
        .toSet shouldEqual Set("X1")
      result.getSuperGenMap.asScala.keys.toSet shouldEqual Set("5017453")
      result.getConfigurationMap shouldBe empty
      result.getTechParamMap shouldBe empty
      result.getComplectationMap shouldBe empty
    }

    "return breadcrumbs" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(BreadcrumbsReturnMode.newBuilder())
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setConfiguration("5018134")
        )
        .build()
      val result = manager.filter(filter, Category.CARS).futureValue.getOrElse(sys.error("right expected"))

      (result.getMarkMap should contain).key("BMW")
      result.getMarkCount shouldBe >(1)
      result.getMarkMap.asScala.values
        .flatMap(_.getModelMap.asScala.keys)
        .toSet should contain("X1")
      result.getMarkMap.asScala.values
        .flatMap(_.getModelMap.asScala.keys)
        .size shouldBe >(1)
      (result.getSuperGenMap should contain).key("5017453")
      (result.getConfigurationMap should contain).key("5018134")
      result.getTechParamCount shouldBe >(1)
      result.getComplectationMap shouldBe empty
    }

    "return batch" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance)
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("BMW")
            .setModel("X1")
        )
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("ACURA")
        )
        .build()
      val resultEither = manager.filter(filter, Category.CARS).futureValue
      val result = resultEither.getOrElse(sys.error("right expected"))

      (result.getMarkMap should contain).key("BMW")
      (result.getMarkMap should contain).key("ACURA")
      result.getMarkMap.asScala.values
        .flatMap(_.getModelMap.asScala.keys)
        .toSet should contain("X1")
    }

    "properly merge results (cars)" in {
      val filter = RawFilterRequest
        .newBuilder()
        // this mode means we extract overlapping marks for different filters, so the implementation will have to merge
        // them.
        .setBreadcrumbs(BreadcrumbsReturnMode.newBuilder())
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("BMW")
            .setModel("X1")
        )
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("ACURA")
            .setModel("CSX")
        )
        .build()
      val resultEither = manager.filter(filter, Category.CARS).futureValue
      val result = resultEither.getOrElse(sys.error("right expected"))

      (result.getMarkMap should contain).key("BMW")
      val bmw = result.getMarkOrThrow("BMW")
      (bmw.getModelMap should contain).key("X1")
      result.getSuperGenMap.values.asScala.filter(_.getParentModel() == "X1") should not be empty

      result.getMarkMap should contain.key("ACURA")
      result.getMarkOrThrow("ACURA").getModelMap should contain.key("CSX")
      result.getSuperGenMap.values.asScala.filter(_.getParentModel() == "CSX") should not be empty
    }

    "properly merge results (trucks)" in {
      val filter = RawFilterRequest
        .newBuilder()
        // this mode means we extract overlapping categories and marks for different filters, so the implementation will
        // have to merge them.
        .setBreadcrumbs(BreadcrumbsReturnMode.newBuilder())
        .setErrorMode(ErrorMode.FAIL_FAST)
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("MERCEDES")
            .setModel("INTEGRO")
        )
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("CITROEN")
            .setModel("JUMPER_BUS")
        )
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("CITROEN")
            .setModel("NEMO")
        )
        .build()
      val resultEither = manager.filter(filter, Category.TRUCKS).futureValue
      val result = resultEither.getOrElse(sys.error("right expected"))

      (result.getSubcategoryMap() should contain).key("BUS")
      val bus = result.getSubcategoryOrThrow("BUS")

      (result.getSubcategoryMap should contain).key("LCV")
      val lcv = result.getSubcategoryOrThrow("LCV")

      (bus.getMarkMap should contain).key("MERCEDES")
      val mercedes = bus.getMarkOrThrow("MERCEDES")
      (mercedes.getModelMap should contain).key("INTEGRO")

      (bus.getMarkMap should contain).key("CITROEN")
      val citroenBus = bus.getMarkOrThrow("CITROEN")
      (citroenBus.getModelMap should contain).key("JUMPER_BUS")

      (lcv.getMarkMap should contain).key("CITROEN")
      val citroenLcv = lcv.getMarkOrThrow("CITROEN")
      (citroenLcv.getModelMap should contain).key("NEMO")
    }

    "return ignore one error" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance)
        .setErrorMode(FAIL_NEVER)
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("BMW")
            .setModel("X1")
        )
        .addFilters(RawCatalogFilter.newBuilder())
        .build()
      val result = manager.filter(filter, Category.CARS).futureValue.getOrElse(sys.error("right expected"))

      (result.getMarkMap should contain).key("BMW")
      result.getMarkMap.asScala.values
        .flatMap(_.getModelMap.asScala.keys)
        .toSet should contain("X1")
    }

    "return error if id combination do not exist" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(BreadcrumbsReturnMode.newBuilder())
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setConfiguration("bla")
        )
        .build()
      manager
        .filter(filter, Category.CARS)
        .futureValue
        .left
        .value shouldBe an[NotFoundFilterException]
    }

    "return error if filter is empty in exact mode" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance)
        .addFilters(RawCatalogFilter.newBuilder())
        .build()
      manager
        .filter(filter, Category.CARS)
        .futureValue
        .left
        .value shouldBe an[BadRequestFilterException]
    }

    "return error if filter list is empty" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance)
        .build()
      manager
        .filter(filter, Category.CARS)
        .futureValue
        .left
        .value shouldBe an[BadRequestFilterException]
    }
  }
}
