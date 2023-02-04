package ru.auto.catalog.api.handlers.raw

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import com.google.protobuf.Empty
import ru.auto.catalog.api.routes.ApiBaseSpec
import ru.auto.catalog.model.api.ApiModel.CatalogLevel._
import ru.auto.catalog.model.api.ApiModel.ErrorMode._
import ru.auto.catalog.model.api.ApiModel._
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf
import scala.concurrent.duration._
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.testkit.TestDuration

import scala.jdk.CollectionConverters._

class RawCatalogHandlerSpec extends ApiBaseSpec {
  implicit val timeout: RouteTestTimeout = RouteTestTimeout(3.seconds.dilated)

  "RawCatalogHandler" should {
    "return all marks" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(BreadcrumbsReturnMode.newBuilder())
        .addFilters(RawCatalogFilter.newBuilder())
        .build()
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])
          result.getMarkMap should not be empty
        }
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
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])
          result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet shouldEqual Set("X1")
          result.getSuperGenMap.asScala.keys.toSet shouldEqual Set("5017453")
          result.getConfigurationMap.asScala.keys.toSet shouldEqual Set("5018134")
          result.getTechParamMap.asScala.keys.toSet shouldEqual Set("7150206")
          result.getComplectationMap.asScala.keys.toSet shouldEqual Set("5018171")
        }
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
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])
          result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet shouldEqual Set("X1")
          result.getSuperGenMap.asScala.keys.toSet shouldEqual Set("5017453")
          result.getConfigurationMap.asScala.keys.toSet shouldEqual Set("5018134")
          result.getTechParamMap.asScala.keys.toSet shouldEqual Set("7150206")
          result.getComplectationMap.asScala.keys.toSet shouldEqual Set("5018171")
        }
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
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])
          result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet shouldEqual Set("X1")
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
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])
          result.getMarkMap.asScala.keys.toSet shouldEqual Set("BMW")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet shouldEqual Set("X1")
          result.getSuperGenMap.asScala.keys.toSet shouldEqual Set("5017453")
          result.getConfigurationMap shouldBe empty
          result.getTechParamMap shouldBe empty
          result.getComplectationMap shouldBe empty
        }
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
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])
          (result.getMarkMap should contain).key("BMW")
          result.getMarkCount shouldBe >(1)
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet should contain("X1")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).size shouldBe >(1)
          (result.getSuperGenMap should contain).key("5017453")
          (result.getConfigurationMap should contain).key("5018134")
          result.getTechParamCount shouldBe >(1)
          result.getComplectationMap shouldBe empty
        }
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
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])
          (result.getMarkMap should contain).key("BMW")
          (result.getMarkMap should contain).key("ACURA")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet should contain("X1")
        }
    }

    "return ignore one error" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setErrorMode(FAIL_NEVER)
        .setExact(Empty.getDefaultInstance)
        .addFilters(
          RawCatalogFilter
            .newBuilder()
            .setMark("BMW")
            .setModel("X1")
        )
        .addFilters(RawCatalogFilter.newBuilder())
        .build()
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])

          (result.getMarkMap should contain).key("BMW")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet should contain("X1")
          result.getWarnings(0).getFailedOnLevel shouldBe MARK
        }
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
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(ErrorResponse.getDefaultInstance, responseAs[String])
          result.getErrorCode shouldBe ErrorCode.NOT_FOUND
        }
    }

    "return error if filter is empty in exact mode" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance)
        .addFilters(RawCatalogFilter.newBuilder())
        .build()
      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(ErrorResponse.getDefaultInstance, responseAs[String])
          result.getErrorCode shouldBe ErrorCode.BAD_REQUEST
        }
    }

    "return only marks and models for SubTree if the filter only specifies criteria for the mark" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setSubtree(SubTreeReturnMode.newBuilder().setTo(CatalogLevel.COMPLECTATION))
        .addFilters(RawCatalogFilter.newBuilder().setMark("BMW"))
        .build()

      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])

          result.getMarkMap.asScala.keySet shouldBe Set("BMW")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet should contain("X1")
          result.getSuperGenMap shouldBe empty
        }
    }

    "return only marks and models for SubTree if the filter doesn't specify any criteria" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setSubtree(SubTreeReturnMode.newBuilder().setTo(CatalogLevel.COMPLECTATION))
        .addFilters(RawCatalogFilter.newBuilder())
        .build()

      Post("/api/raw/cars/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])

          result.getMarkMap should contain.key("BMW")
          result.getMarkMap.asScala.values.flatMap(_.getModelMap.asScala.keys).toSet should contain("X1")
          result.getSuperGenMap shouldBe empty
        }
    }

    "return exact for trucks" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance())
        .addFilters(RawCatalogFilter.newBuilder().setSubcategory("LCV").setMark("CITROEN").setModel("NEMO"))
        .build()

      Post("/api/raw/trucks/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])

          result.getSubcategoryCount shouldBe 1
          result.getSubcategoryMap should contain.key("LCV")
          val subcategory = result.getSubcategoryOrThrow("LCV")
          subcategory.getMarkCount shouldBe 1
          subcategory.getMarkMap should contain.key("CITROEN")
          val mark = subcategory.getMarkOrThrow("CITROEN")
          mark.getModelCount() shouldBe 1
          mark.getModelMap() should contain.key("NEMO")
        }
    }

    "return breadcrumbs for trucks" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setBreadcrumbs(BreadcrumbsReturnMode.newBuilder())
        .addFilters(RawCatalogFilter.newBuilder().setSubcategory("LCV").setMark("CITROEN"))
        .build()

      Post("/api/raw/trucks/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])

          result.getSubcategoryMap.asScala.keySet should contain.allOf("LCV", "BUS")
          val subcategory = result.getSubcategoryOrThrow("LCV")
          subcategory.getMarkMap.asScala.keySet should contain.allOf("CITROEN", "MERCEDES")
          val mark = subcategory.getMarkOrThrow("CITROEN")
          mark.getModelMap should contain.key("NEMO")
        }
    }

    "return exact for moto" in {
      val filter = RawFilterRequest
        .newBuilder()
        .setExact(Empty.getDefaultInstance())
        .addFilters(RawCatalogFilter.newBuilder().setSubcategory("atv").setMark("HONDA").setModel("ATC_200X"))
        .build()

      Post("/api/raw/moto/filter", filter) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`

          val result = Protobuf.fromJson(RawCatalog.getDefaultInstance, responseAs[String])

          result.getSubcategoryCount shouldBe 1
          result.getSubcategoryMap should contain.key("atv")
          val subcategory = result.getSubcategoryOrThrow("atv")
          subcategory.getMarkCount shouldBe 1
          subcategory.getMarkMap should contain.key("HONDA")
          val mark = subcategory.getMarkOrThrow("HONDA")
          mark.getModelCount() shouldBe 1
          mark.getModelMap() should contain.key("ATC_200X")
        }
    }
  }
}
