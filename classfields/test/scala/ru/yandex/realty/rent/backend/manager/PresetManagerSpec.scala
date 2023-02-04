package ru.yandex.realty.rent.backend.manager

import com.sksamuel.elastic4s.ElasticDsl.{createIndex, deleteIndex, indexInto}
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.logging.TracedLogging
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.rent.clients.elastic.ElasticSearchSpecBase
import ru.yandex.realty.rent.dao._
import ru.yandex.realty.rent.model._
import ru.yandex.realty.rent.model.enums.FlatPreset.FlatPreset
import ru.yandex.realty.rent.model.enums.{FlatPreset, OwnerRequestStatus}
import ru.yandex.realty.rent.preset.flat.FlatPresetsIndex._
import ru.yandex.realty.rent.proto.model.flat.FlatData
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PresetManagerSpec
  extends SpecBase
  with ElasticSearchSpecBase
  with RequestAware
  with PropertyChecks
  with TracedLogging {

  override protected def beforeAll(): Unit = {
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

    (new Data with IndexHelper).fillElastic()
  }

  override protected def afterAll(): Unit = {
    val deleteIndexResponse = elasticSearchClient.deleteIndex {
      deleteIndex(IndexName)
    }(Traced.empty).futureValue
    deleteIndexResponse.acknowledged should be(true)
  }

  "PresetManager.getFlatsPreset" should {

    "return expected results for HasVirtualTour" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.HasVirtualTour).sorted shouldBe List("12", "15")
    }

    "return expected results for NoRetouchedPhoto" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NoRetouchedPhoto).sorted shouldBe List("14", "15")
    }

    "return expected results for NoRawPhoto" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NoRawPhoto).sorted shouldBe List("11", "12")
    }

    "return expected results for AwaitingForContent" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.AwaitingForContent).sorted shouldBe List("1", "12", "15", "18")
    }

    "return expected results for NeedRawFlatPhotoLink" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NeedRawFlatPhotoLink).sorted shouldBe List("1")
    }

    "return expected results for NeedVirtualTourLink" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NeedVirtualTourLink).sorted shouldBe List("1")
    }

    "return expected results for NeedRetouchedPhotoLink" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NeedRetouchedPhotoLink).sorted shouldBe List("1")
    }

    "return expected results for NeedCopywriterReview" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NeedCopywriterReview).sorted shouldBe List("1", "2", "3")
    }

    "return expected results for WaitingApprovingForPublishing" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.WaitingApprovingForPublishing).sorted shouldBe List("1")
    }

    "return expected results for ApprovedForPublishing" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.ApprovedForPublishing).sorted shouldBe List("2")
    }

    "return expected results for InsuranceNotRequested" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.InsuranceNotRequested).sorted shouldBe List("4")
    }

    "return expected results for ReceiptAbsent" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.ReceiptAbsent).sorted shouldBe List("1")
    }

    "return expected results for PayoutErrors" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.PayoutErrors).sorted shouldBe List("1", "3")
    }

    "return expected results for ContractTermination" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.ContractTermination).sorted shouldBe List("2", "3", "4")
    }

    "return expected results for PaymentToday" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.PaymentToday).sorted shouldBe List("31", "34")
    }

    "return expected results for PaymentOverdue" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.PaymentOverdue).sorted shouldBe List("32", "35")
    }

    "return expected results for HouseServicePaymentOverdue" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.HouseServicePaymentOverdue).sorted shouldBe List("32", "35")
    }

    "return expected results for NotPaidUponCheckIn" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NotPaidUponCheckIn).sorted shouldBe List("37", "38", "39", "40")
    }

    "search by code ignoring case" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NeedCopywriterReview, Some("02-test")).sorted shouldBe List("2")
    }

    "search by address ignoring case" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NeedCopywriterReview, Some("леНин")).sorted shouldBe List("1", "2")
    }

    "sort ContractTermination preset by termination date ascending" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.ContractTermination) shouldBe List("3", "4", "2")
    }

    "sort other presets by flat creation time descending" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NeedCopywriterReview) shouldBe List("2", "1", "3")
    }

    "return expected results for NeedConfirmInventory" in new Wiring with Data with TestHelper {
      mockFlatDaoWithHollowFlat()
      queryPreset(FlatPreset.NeedConfirmInventory) shouldBe List("5", "6")
    }
  }

  trait Wiring {
    val mockFlatDao: FlatDao = mock[FlatDao]

    val presetManager: PresetManager = new PresetManager(
      mockFlatDao,
      elasticSearchClient
    )
  }

  trait Data {

    case class ElasticDoc(data: (String, Any)*)

    val Docs: Map[String, ElasticDoc] = Seq(
      ElasticDoc(
        Fields.FlatId -> "1",
        Fields.Code -> "01-TEST".toUpperCase,
        Fields.Address -> "Москва, проспект Ленина, дом 33".toLowerCase,
        Fields.CreateTime -> DateTime.now().minusMonths(4).getMillis,
        Fields.NeedRawFlatPhotoLink -> true,
        Fields.NeedVirtualTourLink -> true,
        Fields.NeedRetouchedPhotoLink -> true,
        Fields.NeedCopywriterReview -> true,
        Fields.WaitingApprovingForPublishing -> true,
        Fields.ActiveContract.ReceiptAbsent -> true,
        Fields.ActiveContract.FailedPayoutDate -> DateTime.now().minusHours(1).getMillis,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.Confirmed.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "2",
        Fields.Code -> "02-TEST".toUpperCase,
        Fields.Address -> "Москва, проспект Ленина, дом 34".toLowerCase,
        Fields.CreateTime -> DateTime.now().minusMonths(2).getMillis,
        Fields.NeedCopywriterReview -> true,
        Fields.ApprovedForPublishing -> true,
        Fields.ActiveContract.TerminationDate -> DateTime.now().plusWeeks(2).getMillis,
        Fields.ActiveContract.TenantCheckInDate -> DateTime.now().minusDays(1).minusHours(1),
        Fields.IsInventoryConfirmedByOwner -> true,
        Fields.IsInventoryConfirmedByTenant -> true
      ),
      ElasticDoc(
        Fields.FlatId -> "3",
        Fields.Code -> "03-TEST".toUpperCase,
        Fields.Address -> "спб рубика 10".toLowerCase,
        Fields.CreateTime -> DateTime.parse("2021-04-01").getMillis,
        Fields.NeedCopywriterReview -> true,
        Fields.ActiveContract.FailedPayoutDate -> DateTime.parse("2021-11-20").minusHours(3).getMillis,
        Fields.ActiveContract.TerminationDate -> DateTime.parse("2021-11-30").getMillis,
        Fields.ActiveContract.NowForTesting -> DateTime.parse("2021-11-20").getMillis
      ),
      ElasticDoc(
        Fields.FlatId -> "4",
        Fields.Code -> "04-TEST".toUpperCase,
        Fields.Address -> "спб рубика 10".toLowerCase,
        Fields.ActiveContract.InsuranceNotRequested -> true,
        Fields.ActiveContract.FailedPayoutDate -> DateTime.parse("2021-11-20").plusHours(1).getMillis,
        Fields.ActiveContract.TerminationDate -> DateTime.parse("2021-12-10").getMillis,
        Fields.ActiveContract.NowForTesting -> DateTime.parse("2021-11-20").getMillis
      ),
      ElasticDoc(
        Fields.FlatId -> "5",
        Fields.ActiveContract.TenantCheckInDate -> DateTime.now().minusDays(1).minusHours(1),
        Fields.IsInventoryConfirmedByOwner -> true,
        Fields.IsInventoryConfirmedByTenant -> false
      ),
      ElasticDoc(
        Fields.FlatId -> "6",
        Fields.ActiveContract.TenantCheckInDate -> DateTime.now().minusDays(1).minusHours(1),
        Fields.IsInventoryConfirmedByOwner -> false,
        Fields.IsInventoryConfirmedByTenant -> true
      ),
      ElasticDoc(
        Fields.FlatId -> "11",
        Fields.Code -> "11-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> true,
        Fields.HasRawPhoto -> false,
        Fields.HasRetouchedPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.LookingForTenant.toString,
        Fields.ActiveContract.TenantCheckInDate -> DateTime.now(),
        Fields.IsInventoryConfirmedByOwner -> false,
        Fields.IsInventoryConfirmedByTenant -> false
      ),
      ElasticDoc(
        Fields.FlatId -> "12",
        Fields.Code -> "12-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> true,
        Fields.HasRawPhoto -> false,
        Fields.HasRetouchedPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.WorkInProgress.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "13",
        Fields.Code -> "13-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> true,
        Fields.HasRawPhoto -> false,
        Fields.HasRetouchedPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.WaitingForConfirmation.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "14",
        Fields.Code -> "14-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> true,
        Fields.HasRawPhoto -> true,
        Fields.HasRetouchedPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.LookingForTenant.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "15",
        Fields.Code -> "15-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> true,
        Fields.HasRawPhoto -> true,
        Fields.HasRetouchedPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.WorkInProgress.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "16",
        Fields.Code -> "16-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> true,
        Fields.HasRawPhoto -> true,
        Fields.HasRetouchedPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.WaitingForConfirmation.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "17",
        Fields.Code -> "17-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> false,
        Fields.HasRawPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.LookingForTenant.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "18",
        Fields.Code -> "18-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> false,
        Fields.HasRawPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.WorkInProgress.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "19",
        Fields.Code -> "19-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> false,
        Fields.HasRawPhoto -> false,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.WaitingForConfirmation.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "20",
        Fields.Code -> "20-TEST".toUpperCase,
        Fields.Address -> "тесты про фотографии и статус".toLowerCase,
        Fields.HasVirtualTour -> true,
        Fields.HasRawPhoto -> true,
        Fields.HasRetouchedPhoto -> true,
        Fields.OwnerRequestStatus -> OwnerRequestStatus.LookingForTenant.toString
      ),
      ElasticDoc(
        Fields.FlatId -> "31",
        Fields.Code -> "31-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.now().minusHours(1).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.now().minusHours(1).getMillis
      ),
      ElasticDoc(
        Fields.FlatId -> "32",
        Fields.Code -> "32-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.now().minusHours(30).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.now().minusHours(30).getMillis
      ),
      ElasticDoc(
        Fields.FlatId -> "33",
        Fields.Code -> "33-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.now().plusHours(1).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.now().plusHours(1).getMillis
      ),
      ElasticDoc(
        Fields.FlatId -> "34",
        Fields.Code -> "34-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.parse("2021-11-27").minusHours(1).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.parse("2021-11-27").minusHours(1).getMillis,
        Fields.ActiveContract.NowForTesting -> DateTime.parse("2021-11-27").getMillis
      ),
      ElasticDoc(
        Fields.FlatId -> "35",
        Fields.Code -> "35-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.parse("2021-11-21").minusHours(30).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.parse("2021-11-21").minusHours(30).getMillis,
        Fields.ActiveContract.NowForTesting -> DateTime.parse("2021-11-21").getMillis
      ),
      ElasticDoc(
        Fields.FlatId -> "36",
        Fields.Code -> "36-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.parse("2021-11-27").plusHours(1).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.parse("2021-11-27").plusHours(1).getMillis,
        Fields.ActiveContract.NowForTesting -> DateTime.parse("2021-11-27").getMillis
      ),
      ElasticDoc(
        Fields.FlatId -> "37",
        Fields.Code -> "37-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.now().minusHours(1).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.now().minusHours(1).getMillis,
        Fields.ActiveContract.NotPaidUponCheckIn -> true
      ),
      ElasticDoc(
        Fields.FlatId -> "38",
        Fields.Code -> "38-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.now().minusHours(30).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.now().minusHours(30).getMillis,
        Fields.ActiveContract.NotPaidUponCheckIn -> true
      ),
      ElasticDoc(
        Fields.FlatId -> "39",
        Fields.Code -> "39-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.parse("2021-11-27").minusHours(1).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.parse("2021-11-27").minusHours(1).getMillis,
        Fields.ActiveContract.NowForTesting -> DateTime.parse("2021-11-27").getMillis,
        Fields.ActiveContract.NotPaidUponCheckIn -> true
      ),
      ElasticDoc(
        Fields.FlatId -> "40",
        Fields.Code -> "40-TEST".toUpperCase,
        Fields.Address -> "тесты про даты платежей".toLowerCase,
        Fields.ActiveContract.WaitRentPaymentDate -> DateTime.parse("2021-11-27").minusHours(30).getMillis,
        Fields.ActiveContract.WaitHouseServicePaymentDate -> DateTime.parse("2021-11-27").minusHours(30).getMillis,
        Fields.ActiveContract.NowForTesting -> DateTime.parse("2021-11-27").getMillis,
        Fields.ActiveContract.NotPaidUponCheckIn -> true
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

  trait IndexHelper { self: Data =>

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

  }

  trait TestHelper { self: Wiring with Data =>

    def mockFlatDaoWithHollowFlat(): Unit = {
      (mockFlatDao
        .findByIdsInTheSameOrder(_: Seq[String])(_: Traced))
        .expects(*, *)
        .onCall { (seq: Seq[String], _) =>
          Future.successful(seq.map(getHollowFlat))
        }
        .repeat(1)
    }

    def queryPreset(preset: FlatPreset, query: Option[String] = None): List[String] = {
      val page = Page(0, 10)
      val result = presetManager.getFlatsPreset(preset, page, query).futureValue
      result.map(_.flatId).toList
    }

  }

}
