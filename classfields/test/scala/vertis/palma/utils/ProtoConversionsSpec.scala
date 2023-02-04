package vertis.palma.utils

import ru.yandex.vertis.palma.samples.{Generation, Model}
import ru.yandex.vertis.palma.service_common.Sorting.Direction._
import ru.yandex.vertis.palma.service_common.{Pagination => ProtoPagination, RequestContext, Sorting => ProtoSorting}
import ru.yandex.vertis.palma.services.dictionary_service.DictionaryApiModel.ListRequest
import vertis.palma.dao.model.{Pagination, Sorting}
import vertis.palma.dao.model.Sorting.{FieldTypes, JsonField}
import vertis.palma.dao.ydb.YdbColumns
import vertis.palma.test.test_samples.SortModel
import vertis.palma.utils.Validations.{UnknownSortingField, UnsupportedSortingFieldType}
import vertis.zio.test.ZioSpecBase

class ProtoConversionsSpec extends ZioSpecBase {

  "Conversions" should {
    "convert empty proto context" in {
      val protoRequestContext = RequestContext.defaultInstance

      for {
        requestContext <- ProtoConversions.requestContextFromMessage(Some(protoRequestContext))
        _ <- check(requestContext.userId.isEmpty shouldBe true)
        _ <- check(requestContext.schemaVersion.isEmpty shouldBe true)
        _ <- check(requestContext.requestId.isEmpty shouldBe true)
        _ <- check(requestContext.serviceName.isEmpty shouldBe true)
      } yield ()
    }
    "convert non empty proto context" in {
      val userId = "user"
      val schemaVersion = "v0"
      val protoRequestContext = RequestContext(
        userId = userId,
        schemaVersion = schemaVersion
      )

      for {
        requestContext <- ProtoConversions.requestContextFromMessage(Some(protoRequestContext))
        _ <- check(requestContext.userId shouldBe Some(userId))
        _ <- check(requestContext.schemaVersion shouldBe Some(schemaVersion))
        _ <- check(requestContext.requestId.isEmpty shouldBe true)
        _ <- check(requestContext.serviceName.isEmpty shouldBe true)
      } yield ()
    }

    "convert sorting" in ioTest {
      val descriptor = SortModel.javaDescriptor
      for {
        s <- ProtoConversions.sortingFromRequestMessage(Some(ProtoSorting("russian_alias", ASCENDING)), descriptor)
        _ <- check("string field") {
          s.get shouldBe Sorting(
            JsonField("russian_alias", FieldTypes.String, YdbColumns.JsonContentView)
          )
        }
        s <- ProtoConversions.sortingFromRequestMessage(Some(ProtoSorting("safety_score", ASCENDING)), descriptor)
        _ <- check("float field") {
          s.get shouldBe Sorting(
            JsonField("safety_score", FieldTypes.Float64, YdbColumns.JsonContentView)
          )
        }
        s <- ProtoConversions.sortingFromRequestMessage(Some(ProtoSorting("release_year", DESCENDING)), descriptor)
        _ <- check("int field") {
          s.get shouldBe Sorting(
            JsonField("release_year", FieldTypes.Int32, YdbColumns.JsonContentView),
            DESCENDING
          )
        }
        s <- ProtoConversions.sortingFromRequestMessage(Some(ProtoSorting("camelCaseField", DESCENDING)), descriptor)
        _ <- check("camel case proto field") {
          s.get shouldBe Sorting(
            JsonField("camelCaseField", FieldTypes.String, YdbColumns.JsonContentView),
            DESCENDING
          )
        }
      } yield ()
    }
    "fail on convert sorting" in ioTest {
      for {
        s <- ProtoConversions
          .sortingFromRequestMessage(Some(ProtoSorting("unknown", ASCENDING)), Model.javaDescriptor)
          .flip
        _ <- check("unknown field") {
          s shouldBe a[UnknownSortingField]
        }
        s <- ProtoConversions
          .sortingFromRequestMessage(Some(ProtoSorting("super_generation", ASCENDING)), Generation.javaDescriptor)
          .flip
        _ <- check("invalid field") {
          s shouldBe a[UnsupportedSortingFieldType]
        }
      } yield ()
    }
    "fail on invalid pagination" in ioTest {
      for {
        e <- ProtoConversions
          .paginationFromRequestMessage(
            ListRequest(
              pagination = Some(ProtoPagination(lastKey = "any", limit = Pagination.Limit + 1))
            )
          )
          .flip
        _ <- check {
          e shouldBe a[IllegalArgumentException]
        }
      } yield ()
    }
  }

}
