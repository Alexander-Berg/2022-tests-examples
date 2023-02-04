package ru.yandex.realty.rent.backend.manager

import com.sksamuel.elastic4s.ElasticDsl.{createIndex, deleteIndex, indexInto}
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.model.SortDirection
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.rent.backend.manager.moderation.ModerationFlatsManager
import ru.yandex.realty.rent.clients.elastic.ElasticSearchSpecBase
import ru.yandex.realty.rent.dao.FlatDao
import ru.yandex.realty.rent.model.Flat
import ru.yandex.realty.rent.model.enums.FlatOrdering
import ru.yandex.realty.rent.preset.flat.FlatPresetsIndex.Fields.{AssignedUser, ContractsFields}
import ru.yandex.realty.rent.preset.flat.FlatPresetsIndex.{Fields, IndexName}
import ru.yandex.realty.rent.proto.api.internal.moderation.flats.InternalGetModerationFlatsRequest
import ru.yandex.realty.rent.proto.api.search.filter.SearchFilter.{And, Exist}
import ru.yandex.realty.rent.proto.api.search.filter.{Between, Equal, RawValue, SearchFilter, Wildcard}
import ru.yandex.realty.rent.proto.model.flat.FlatData
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class ModerationFlatsManagerSpec
  extends SpecBase
  with ElasticSearchSpecBase
  with RequestAware
  with PropertyChecks
  with Logging {

  before {
    val json = {
      val file = "../realty-rent-core/src/main/resources/elastic/create-flat-presets-index.json"
      val source = scala.io.Source.fromFile(file)
      try source.mkString
      finally source.close()
    }
    val createIndexResponse = elasticSearchClient.createIndex {
      createIndex(IndexName).source(json)
    }(Traced.empty).futureValue
    createIndexResponse.acknowledged should be(true)
  }

  after {
    val deleteIndexResponse = elasticSearchClient.deleteIndex {
      deleteIndex(IndexName)
    }(Traced.empty).futureValue
    deleteIndexResponse.acknowledged should be(true)
  }

  "ModerationFlatsManager" should {
    "search flat by status" in new Wiring with Data with TestHelper {
      fillElastic()
      mockFlatDaoWithHollowFlat()
      checkSearch(
        equalQuery("owner_request_status", "looking_for_tenant"),
        List("1")
      )
    }

    "search flat by user and role" in new Wiring with Data with TestHelper {
      fillElastic()
      mockFlatDaoWithHollowFlat()
      checkSearch(
        exist(
          field = Fields.User,
          filter = and(
            equalQuery(Fields.User + "." + AssignedUser.Role, "tenant"),
            wildcardQuery(Fields.User + "." + AssignedUser.FullName, "*norris*")
          )
        ),
        List("2")
      )
    }

    "search flat by status, contract status and contract_sign_date" in new Wiring with Data with TestHelper {
      fillElastic()
      mockFlatDaoWithHollowFlat()
      checkSearch(
        and(
          equalQuery(Fields.OwnerRequestStatus, "completed"),
          exist(
            field = Fields.Contracts,
            filter = and(
              equalQuery(Fields.Contracts + "." + ContractsFields.Status, "fixed_term"),
              between(
                Fields.Contracts + "." + ContractsFields.ContractSignDate,
                "2022-01-22T00:00:00+03:00",
                "2022-01-24T00:00:00+03:00"
              )
            )
          )
        ),
        List("3")
      )
    }

    "search flat by status and query" in new Wiring with Data with TestHelper {
      fillElastic()
      mockFlatDaoWithHollowFlat()
      checkSearch(
        and(
          equalQuery(Fields.OwnerRequestStatus, "completed"),
          equalQuery("query", "foobar")
        ),
        List("2", "4", "5")
      )
    }
  }

  trait Wiring {
    val mockFlatDao: FlatDao = mock[FlatDao]

    val presetManager = new PresetManager(mockFlatDao, elasticSearchClient)

    val moderationFlatsManager: ModerationFlatsManager =
      new ModerationFlatsManager(mockFlatDao, presetManager, elasticSearchClient)
  }

  trait Data {
    case class ElasticDoc(data: (String, Any)*)

    val Docs: Map[String, ElasticDoc] = Seq(
      ElasticDoc(
        Fields.FlatId -> "1",
        Fields.Code -> "01-TEST".toUpperCase,
        Fields.Address -> "Москва, проспект Ленина, дом 33".toLowerCase,
        Fields.OwnerRequestStatus -> "looking_for_tenant"
      ),
      ElasticDoc(
        Fields.FlatId -> "2",
        Fields.Code -> "02-TEST".toUpperCase,
        Fields.Address -> "Москва, Кутузовский проспект, дом 11".toLowerCase,
        Fields.OwnerRequestStatus -> "completed",
        Fields.User -> Seq(
          Map(
            AssignedUser.FullName -> "chuck norris",
            AssignedUser.Role -> "tenant",
            AssignedUser.Phone -> "+70000000000",
            AssignedUser.Email -> "chuck@norris.us"
          ),
          Map(
            AssignedUser.FullName -> "foobar baz",
            AssignedUser.Role -> "owner",
            AssignedUser.Phone -> "+70000000123",
            AssignedUser.Email -> "fb@fb.com"
          )
        )
      ),
      ElasticDoc(
        Fields.FlatId -> "3",
        Fields.Code -> "03-TEST".toUpperCase,
        Fields.Address -> "Москва, Садовая улица, дом 22".toLowerCase,
        Fields.OwnerRequestStatus -> "completed",
        Fields.Contracts -> Seq(
          Map(
            ContractsFields.ContractId -> "3-1",
            ContractsFields.Status -> "fixed_term",
            ContractsFields.ContractSignDate -> new DateTime("2022-01-23T00:00:00+03:00").getMillis
          )
        )
      ),
      ElasticDoc(
        Fields.FlatId -> "4",
        Fields.Code -> "04-TEST".toUpperCase,
        Fields.Address -> "Москва, Неизвестная улица, дом 6".toLowerCase,
        Fields.OwnerRequestStatus -> "completed",
        Fields.Contracts -> Seq(
          Map(
            ContractsFields.ContractId -> "4-1",
            ContractsFields.Status -> "active",
            ContractsFields.OwnerName -> "foobar baz"
          )
        )
      ),
      ElasticDoc(
        Fields.FlatId -> "5",
        Fields.Code -> "05-TEST".toUpperCase,
        Fields.Address -> "Москва, Большой Патриарший переулок, дом 10".toLowerCase,
        Fields.OwnerRequestStatus -> "completed",
        Fields.NameFromRequest -> "foobar baz"
      )
    ).map(doc => doc.data.toMap.apply(Fields.FlatId).asInstanceOf[String] -> doc).toMap

    def getHollowFlat(id: String): Flat = {
      Flat(
        flatId = id,
        code = None,
        data = FlatData.newBuilder().build(),
        address = "",
        unifiedAddress = None,
        flatNumber = "",
        nameFromRequest = None,
        phoneFromRequest = None,
        isRented = false,
        keyCode = None,
        ownerRequests = Seq.empty,
        assignedUsers = Map.empty,
        createTime = DateTime.now(),
        updateTime = DateTime.now(),
        visitTime = None,
        shardKey = 0
      )
    }
  }

  trait TestHelper {
    self: Wiring with Data =>

    def fillElastic(): Unit = {
      Docs.foreach {
        case (id, doc) =>
          val res = elasticSearchClient.index {
            indexInto(IndexName).id(id).fields(doc.data).copy(refresh = Some(RefreshPolicy.Immediate))
          }(Traced.empty).futureValue
          res.index should be(IndexName)
          res.id should be(id)
      }
    }

    protected def mockFlatDaoWithHollowFlat() = {
      (mockFlatDao
        .findByIdsInTheSameOrder(_: Seq[String])(_: Traced))
        .expects(*, *)
        .onCall { (seq: Seq[String], _) =>
          Future.successful(seq.map(getHollowFlat))
        }
    }

    protected def checkSearch(filter: SearchFilter, expectedFlatIds: List[String]): Unit = {
      val results = moderationFlatsManager
        .searchFlat(
          InternalGetModerationFlatsRequest.newBuilder().setSearchFilter(filter).build(),
          FlatOrdering.FlatCreationDate,
          SortDirection.Asc,
          Page(0, 10)
        )
        .futureValue
      results.map(_.flatId).toList.sorted shouldBe expectedFlatIds
    }

    protected def equalQuery(field: String, value: String): SearchFilter =
      SearchFilter
        .newBuilder()
        .setEqual(
          Equal
            .newBuilder()
            .setField(field)
            .setValue(RawValue.newBuilder().setString(value))
        )
        .build()

    protected def wildcardQuery(field: String, value: String): SearchFilter =
      SearchFilter
        .newBuilder()
        .setWildcard(
          Wildcard
            .newBuilder()
            .setField(field)
            .setValue(value)
        )
        .build()

    protected def and(operands: SearchFilter*): SearchFilter =
      SearchFilter
        .newBuilder()
        .setAnd(
          And
            .newBuilder()
            .addAllOperands(operands.toList.asJava)
        )
        .build()

    protected def exist(field: String, filter: SearchFilter): SearchFilter =
      SearchFilter
        .newBuilder()
        .setExist(
          Exist.newBuilder().setField(field).setFilter(filter)
        )
        .build()

    protected def between(field: String, from: String, to: String): SearchFilter =
      SearchFilter
        .newBuilder()
        .setBetween(
          Between
            .newBuilder()
            .setField(field)
            .setFrom(RawValue.newBuilder().setString(from))
            .setTo(RawValue.newBuilder().setString(to))
        )
        .build()
  }
}
