package ru.yandex.realty.rent.service

import com.google.protobuf.Timestamp
import com.sksamuel.elastic4s.ElasticDate
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.Query
import org.apache.commons.io.IOUtils
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalamock.handlers.CallHandler2
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.model.util.{Page, SlicedResult}
import ru.yandex.realty.proto.phone.PhoneRedirectMessage
import ru.yandex.realty.rent.clients.elastic.ElasticSearchSpecBase
import ru.yandex.realty.rent.clients.elastic.model.user.{User => ElasticUser}
import ru.yandex.realty.rent.dao.{
  FlatDao,
  FlatQuestionnaireDao,
  InventoryDao,
  OwnerRequestDao,
  PaymentDao,
  RentContractDao,
  UserDao
}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.{ContractStatus, OwnerRequestStatus, PaymentStatus, PaymentType, Role}
import ru.yandex.realty.rent.model.{Flat, FlatQuestionnaire, Inventory, OwnerRequest, Payment, RentContract, User}
import ru.yandex.realty.rent.preset.flat.FlatPresetsIndex.Fields.{
  ActiveContract,
  IsInventoryConfirmedByOwner,
  IsInventoryConfirmedByTenant
}
import ru.yandex.realty.rent.preset.flat.FlatPresetsIndex.{Fields, IndexName}
import ru.yandex.realty.rent.proto.api.moderation.ClassifiedTypeNamespace.ClassifiedType.{CIAN, YANDEX_REALTY}
import ru.yandex.realty.rent.proto.model.diffevent.{
  DiffEvent,
  FlatDiffEvent,
  FlatProtoView,
  InventoryDiffEvent,
  InventoryProtoView,
  UserDiffEvent,
  UserProtoView
}
import ru.yandex.realty.rent.proto.model.flat.ClassifiedRedirectNumber
import ru.yandex.realty.rent.proto.model.payment.ManualTenantTransactionInfo
import ru.yandex.realty.rent.proto.model.payment.PayoutErrorNamespace.PayoutError
import ru.yandex.realty.rent.service.impl.DiffEventElasticProcessorImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class DiffEventElasticProcessorSpec extends SpecBase with ElasticSearchSpecBase with RentModelsGen {

  implicit val traced: Traced = Traced.empty

  before {
    val flatJson =
      IOUtils.toString(getClass.getClassLoader.getResourceAsStream("elastic/create-flat-presets-index.json"))
    val createFlatIndexResponse = elasticSearchClient.createIndex {
      createIndex(IndexName).source(flatJson)
    }(Traced.empty).futureValue
    createFlatIndexResponse.acknowledged should be(true)
    val usersJson = IOUtils.toString(getClass.getClassLoader.getResourceAsStream("elastic/create-users-index.json"))
    val createUsersIndexResponse = elasticSearchClient.createIndex {
      createIndex(ElasticUser.IndexName).source(usersJson)
    }(Traced.empty).futureValue
    createUsersIndexResponse.acknowledged should be(true)
  }

  after {
    val deleteFlatIndexResponse = elasticSearchClient.deleteIndex {
      deleteIndex(IndexName)
    }(Traced.empty).futureValue
    deleteFlatIndexResponse.acknowledged should be(true)
    val deleteUserIndexResponse = elasticSearchClient.deleteIndex {
      deleteIndex(ElasticUser.IndexName)
    }(Traced.empty).futureValue
    deleteUserIndexResponse.acknowledged should be(true)
  }

  "DiffEventElasticProcessor" should {

    "set FlatId" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.FlatId, flatId)
      getFromElastic(query).futureValue.toList should not be empty
    }

    "set Address in lowercase" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(
        flatId = flatId,
        address = "String With Alternating Case"
      )
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.Address, flat.address.toLowerCase)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set UnifiedAddress in lowercase" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(
        flatId = flatId,
        unifiedAddress = Some("String With Alternating Case")
      )
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.UnifiedAddress, flat.unifiedAddress.get.toLowerCase)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set Code" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(
        flatId = flatId,
        code = Some(readableString.next)
      )
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.Code, flat.code.get)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set CreateTime" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(
        flatId = flatId,
        createTime = DateTimeUtil.now().minusDays(3)
      )
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.CreateTime, flat.createTime.getMillis)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set OwnerRequestStatus" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.OwnerRequestStatus, flat.getStatus)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set ApprovedForPublishing" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.ApprovedForPublishing, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set NameFromRequest" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(
        flatId = flatId,
        nameFromRequest = Some(readableString.next)
      )
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.NameFromRequest, flat.nameFromRequest.get)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set PhoneFromRequest" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(
        flatId = flatId,
        phoneFromRequest = Some(readableString.next)
      )
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.PhoneFromRequest, flat.phoneFromRequest.get)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set IsRented" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.IsRented, flat.isRented)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set KeyCode" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Confirmed).next.copy(
        flatId = flatId,
        keyCode = Some(readableString.next)
      )
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.KeyCode, flat.keyCode.get)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set NeedRawFlatPhotoLink" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next.copy(flatId = flatId)
      val quiz = {
        val qz = flatQuestionnaireGen.next
        val media = qz.data.getMedia.toBuilder.setPhotoRawUrl("").build()
        val data = qz.data.toBuilder.setMedia(media).build()
        qz.copy(flatId = flatId, data = data)
      }
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.NeedRawFlatPhotoLink, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set NeedVirtualTourLink" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next.copy(flatId = flatId)
      val quiz = {
        val qz = flatQuestionnaireGen.next
        val media = qz.data.getMedia.toBuilder.setTour3DUrl("").build()
        val data = qz.data.toBuilder.setMedia(media).build()
        qz.copy(flatId = flatId, data = data)
      }
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.NeedVirtualTourLink, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set NeedRetouchedPhotoLink" in new Wiring with Data with TestHelper {
      val flat = {
        val fl = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next
        val data = fl.data.toBuilder.clearRetouchedPhotos().build()
        fl.copy(flatId = flatId, data = data)
      }
      val quiz = {
        val qz = flatQuestionnaireGen.next
        val media = qz.data.getMedia.toBuilder.setPhotoRawUrl("lol").build()
        val data = qz.data.toBuilder.setMedia(media).build()
        qz.copy(flatId = flatId, data = data)
      }
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.NeedRetouchedPhotoLink, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set NeedCopywriterReview" in new Wiring with Data with TestHelper {
      val flat = {
        val fl = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next
        val data = fl.data.toBuilder.addRetouchedPhotos(flatDataImage("", 1, "")).build()
        fl.copy(flatId = flatId, data = data)
      }
      val quiz = {
        val qz = flatQuestionnaireGen.next
        val media = qz.data.getMedia.toBuilder.setPhotoRawUrl("lol").setTour3DUrl("kek").build()
        val data = qz.data.toBuilder.setOfferCopyright("").setMedia(media).build()
        qz.copy(flatId = flatId, data = data)
      }
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.NeedCopywriterReview, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set WaitingApprovingForPublishing" in new Wiring with Data with TestHelper {
      val flat = {
        val fl = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next
        val data = fl.data.toBuilder.addRetouchedPhotos(flatDataImage("", 1, "")).build()
        fl.copy(flatId = flatId, data = data)
      }
      val quiz = {
        val qz = flatQuestionnaireGen.next
        val media = qz.data.getMedia.toBuilder.setPhotoRawUrl("lol").setTour3DUrl("kek").build()
        val data = qz.data.toBuilder.setOfferCopyright("azaza").setMedia(media).build()
        qz.copy(flatId = flatId, data = data)
      }
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.WaitingApprovingForPublishing, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set HasVirtualTour" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next.copy(flatId = flatId)
      val quiz = {
        val qz = flatQuestionnaireGen.next
        val media = qz.data.getMedia.toBuilder.setTour3DUrl("kek").build()
        val data = qz.data.toBuilder.setMedia(media).build()
        qz.copy(flatId = flatId, data = data)
      }
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.HasVirtualTour, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set HasRawPhoto" in new Wiring with Data with TestHelper {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next.copy(flatId = flatId)
      val quiz = {
        val qz = flatQuestionnaireGen.next
        val media = qz.data.getMedia.toBuilder.setPhotoRawUrl("lol").build()
        val data = qz.data.toBuilder.setMedia(media).build()
        qz.copy(flatId = flatId, data = data)
      }
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.HasRawPhoto, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set HasRetouchedPhoto" in new Wiring with Data with TestHelper {
      val flat = {
        val fl = flatWithOwnerRequest(OwnerRequestStatus.WorkInProgress).next.copy(flatId = flatId)
        val data = fl.data.toBuilder.addRetouchedPhotos(flatDataImage("", 1, "")).build()
        fl.copy(flatId = flatId, data = data)
      }
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.HasRetouchedPhoto, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "preset PaymentToday" in new Wiring with Data with TestHelper {
      val flat = flatGen(recursive = false).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      val contract = rentContractGen(ContractStatus.Active).next.copy(flatId = flatId)
      val payment = paymentGen(Some(PaymentType.Rent), Some(PaymentStatus.New)).next
        .copy(contractId = contract.contractId, paymentDate = DateTime.now().minusHours(1))
      mockFlatDao(flat, quiz, Some(contract), Seq(payment), None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val elasticOneDayBefore = ElasticDate.now.minus(1, com.sksamuel.elastic4s.Days)
      val query = rangeQuery(Fields.ActiveContract.WaitRentPaymentDate).gt(elasticOneDayBefore).lte(ElasticDate.now)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "preset PaymentOverdue" in new Wiring with Data with TestHelper {
      val flat = flatGen(false).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      val contract = rentContractGen(ContractStatus.Active).next.copy(flatId = flatId)
      val payment = paymentGen(Some(PaymentType.Rent), Some(PaymentStatus.New)).next
        .copy(contractId = contract.contractId, paymentDate = DateTime.now().minusHours(100))
      mockFlatDao(flat, quiz, Some(contract), Seq(payment), None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val elasticOneDayBefore = ElasticDate.now.minus(1, com.sksamuel.elastic4s.Days)
      val query = rangeQuery(Fields.ActiveContract.WaitRentPaymentDate).lte(elasticOneDayBefore)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "preset HouseServicePaymentOverdue" in new Wiring with Data with TestHelper {
      val flat = flatGen(false).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      val contract = rentContractGen(ContractStatus.Active).next.copy(flatId = flatId)
      val payment = paymentGen(Some(PaymentType.HouseServices), Some(PaymentStatus.New)).next
        .copy(contractId = contract.contractId, paymentDate = DateTime.now().minusHours(100))
      mockFlatDao(flat, quiz, Some(contract), Seq(payment), None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val elasticOneDayBefore = ElasticDate.now.minus(1, com.sksamuel.elastic4s.Days)
      val query = rangeQuery(Fields.ActiveContract.WaitHouseServicePaymentDate).lte(elasticOneDayBefore)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set NotPaidUponCheckIn" in new Wiring with Data with TestHelper {
      val flat = flatGen(false).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      val contract = rentContractGen(ContractStatus.Active).next.copy(flatId = flatId)
      val payment = paymentGen(Some(PaymentType.Rent), Some(PaymentStatus.New)).next
        .copy(contractId = contract.contractId, paymentDate = DateTime.now().minusMinutes(1))
      mockFlatDao(flat, quiz, Some(contract), Seq(payment), None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.ActiveContract.NotPaidUponCheckIn, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set InsuranceNotRequested" in new Wiring with Data with TestHelper {
      val flat = flatGen(recursive = false).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      val contract = {
        val cnt = rentContractGen(ContractStatus.Active).next
        val insurance = cnt.data.getInsurance.toBuilder.clearPolicyDate().build()
        val data = cnt.data.toBuilder.setInsurance(insurance).build()
        cnt.copy(flatId = flatId, data = data)
      }
      mockFlatDao(flat, quiz, Some(contract), Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.ActiveContract.InsuranceNotRequested, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "set ReceiptAbsent" in new Wiring with Data with TestHelper {
      val flat = flatGen(recursive = false).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      val contract = rentContractGen(ContractStatus.Active).next.copy(flatId = flatId)
      val payment = {
        val p = paymentGen(Some(PaymentType.Rent), Some(PaymentStatus.PaidToOwner)).next
        val startTimestamp = Timestamp.newBuilder().setSeconds(p.startTime.getMillis / 1000)
        val manualDate = ManualTenantTransactionInfo.newBuilder().setPaymentDate(startTimestamp)
        val data = p.data.toBuilder.setManualTenantTransaction(manualDate).build()
        p.copy(contractId = contract.contractId, data = data)
      }
      mockFlatDao(flat, quiz, Some(contract), Seq(payment), None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = termQuery(Fields.ActiveContract.ReceiptAbsent, true)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "preset PayoutErrors" in new Wiring with Data with TestHelper {
      val flat = flatGen(false).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      val contract = rentContractGen(ContractStatus.Active).next.copy(flatId = flatId)
      val payment = {
        val p = paymentGen(Some(PaymentType.Rent), Some(PaymentStatus.PaidByTenant)).next
        val data = p.data.toBuilder.setPayoutError(PayoutError.BOUND_OWNER_CARD_IS_ABSENT).build()
        p.copy(contractId = contract.contractId, startTime = DateTime.now().minusDays(10), data = data)
      }
      mockFlatDao(flat, quiz, Some(contract), Seq(payment), None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = rangeQuery(Fields.ActiveContract.FailedPayoutDate).lte(ElasticDate.now)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "preset ContractTermination" in new Wiring with Data with TestHelper {
      val flat = flatGen(recursive = false).next.copy(flatId = flatId)
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      val contract =
        rentContractGen(ContractStatus.Active).next.copy(flatId = flatId, terminationDate = Some(DateTime.now()))
      mockFlatDao(flat, quiz, Some(contract), Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val query = existsQuery(Fields.ActiveContract.TerminationDate)
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "index redirect phones" in new Wiring with Data with TestHelper {
      val number1 = "+79215555555"
      val number2 = "+79215555556"
      val number3 = "+79215555557"
      val incorrectNumber = "+79215555558"
      val flat = {
        val nextFlat = flatGen(false).next
        nextFlat.copy(
          flatId = flatId,
          data = nextFlat.data.toBuilder
            .clearRedirectNumbers()
            .addRedirectNumbers(
              ClassifiedRedirectNumber
                .newBuilder()
                .setClassifiedType(YANDEX_REALTY)
                .setRedirect(PhoneRedirectMessage.newBuilder().setSource(number1))
                .setPreviousRedirect(
                  PhoneRedirectMessage.newBuilder().setSource(number2).setDeadline(System.currentTimeMillis)
                )
            )
            .addRedirectNumbers(
              ClassifiedRedirectNumber
                .newBuilder()
                .setClassifiedType(CIAN)
                .setRedirect(PhoneRedirectMessage.newBuilder().setSource(number3))
            )
            .build()
        )
      }
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      getFromElastic(termQuery(Fields.RedirectPhones, number1)).futureValue.toList shouldBe List(flatId)
      getFromElastic(termQuery(Fields.RedirectPhones, number2)).futureValue.toList shouldBe List(flatId)
      getFromElastic(termQuery(Fields.RedirectPhones, number3)).futureValue.toList shouldBe List(flatId)
      getFromElastic(termQuery(Fields.RedirectPhones, incorrectNumber)).futureValue.toList shouldBe empty
    }

    "index assigned users with roles" in new Wiring with Data with TestHelper {
      val randomUser = userGen(false).next
      val user = randomUser.copy(fullName = Some("Тестовый Пользователь"))
      val flat = flatGen(false).next.copy(
        flatId = flatId,
        assignedUsers = Map(Role.Owner -> Seq(user))
      )
      val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)
      mockFlatDao(flat, quiz, None, Seq.empty, None)
      processor.process(getFlatModificationDiffEvent(flatId)).futureValue
      val positiveQuery = nestedQuery(
        path = Fields.User,
        wildcardQuery(Fields.User + "." + Fields.AssignedUser.FullName, s"*тестовый*")
      )
      val negativeQuery = nestedQuery(
        path = Fields.User,
        wildcardQuery(Fields.User + "." + Fields.AssignedUser.FullName, s"*Отрицательный*")
      )
      getFromElastic(positiveQuery).futureValue.toList shouldBe List(flatId)
      getFromElastic(negativeQuery).futureValue.toList shouldBe List.empty
    }

    "index new user" in new Wiring with Data with TestHelper {
      val randomUser = userGen(false).next
      val user = randomUser.copy(uid = 42L, fullName = Some("новый пользователь"))
      mockUserDao(user)
      processor.process(getUserCreationDiffEvent(user.uid)).futureValue
      val positiveQuery = wildcardQuery(ElasticUser.Fields.FullName, "новый*")
      getFromElastic(positiveQuery, ElasticUser.IndexName).futureValue.toList shouldBe List(user.uid.toString)
      val negativeQuery = wildcardQuery(ElasticUser.Fields.FullName, "неизвестный*")
      getFromElastic(negativeQuery, ElasticUser.IndexName).futureValue.toList shouldBe List.empty
    }

    "delete user" in new Wiring with Data with TestHelper {
      val randomUser = userGen(false).next
      val user = randomUser.copy(uid = 22L, fullName = Some("удаляемый"))
      indexUser(user)

      processor.process(getUserDeletionDiffEvent(user.uid)).futureValue

      val q = wildcardQuery(ElasticUser.Fields.FullName, "удаляемый*")
      getFromElastic(q, ElasticUser.IndexName).futureValue.toList shouldBe List.empty
    }

    "index inventory before first confirmation" in new Wiring with Data with InventoryData with TestHelper {
      mockFlatDao(flat, quiz, Some(contract), Seq.empty, Some(ownerRequest))
      mockInventory(None, inventory, ownerRequest)

      processor.process(getInventoryDiffEvent(ownerRequest.ownerRequestId)).futureValue
      val query = must(
        should(
          termQuery(IsInventoryConfirmedByOwner, false),
          termQuery(IsInventoryConfirmedByTenant, false)
        ),
        rangeQuery(ActiveContract.TenantCheckInDate).lt(ElasticDate.now.add(-1, com.sksamuel.elastic4s.Days))
      )
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "index each confirmed inventory" in new Wiring with Data with InventoryData with TestHelper {
      mockFlatDao(flat, quiz, Some(contract), Seq.empty, Some(ownerRequest))
      mockInventory(Some(inventory), inventory, ownerRequest)

      processor.process(getInventoryDiffEvent(ownerRequest.ownerRequestId)).futureValue
      val query = must(
        should(
          termQuery(IsInventoryConfirmedByOwner, false),
          termQuery(IsInventoryConfirmedByTenant, false)
        ),
        rangeQuery(ActiveContract.TenantCheckInDate).lt(ElasticDate.now.add(-1, com.sksamuel.elastic4s.Days))
      )
      getFromElastic(query).futureValue.toList shouldBe List(flatId)
    }

    "do not index inventory when there is confirmed one before" in
      new Wiring with Data with InventoryData with TestHelper {
        mockFlatDao(flat, quiz, Some(contract), Seq.empty, Some(ownerRequest))
        mockInventory(
          Some(inventory),
          inventory.copy(version = inventory.version + 1),
          ownerRequest
        )

        processor.process(getInventoryDiffEvent(ownerRequest.ownerRequestId)).futureValue
        val query = must(
          should(
            termQuery(IsInventoryConfirmedByOwner, false),
            termQuery(IsInventoryConfirmedByTenant, false)
          ),
          rangeQuery(ActiveContract.TenantCheckInDate).lt(ElasticDate.now.add(-1, com.sksamuel.elastic4s.Days))
        )
        getFromElastic(query).futureValue.toList shouldBe List()
      }
  }

  trait Wiring {
    val mockFlatDao: FlatDao = mock[FlatDao]
    val mockFlatQuestionnaireDao: FlatQuestionnaireDao = mock[FlatQuestionnaireDao]
    val mockContractDao: RentContractDao = mock[RentContractDao]
    val mockPaymentDao: PaymentDao = mock[PaymentDao]
    val mockUserDao: UserDao = mock[UserDao]
    val inventoryDao: InventoryDao = mock[InventoryDao]
    val ownerRequestDao: OwnerRequestDao = mock[OwnerRequestDao]
    val features = new SimpleFeatures

    val processor = new DiffEventElasticProcessorImpl(
      mockFlatDao,
      mockFlatQuestionnaireDao,
      mockContractDao,
      mockPaymentDao,
      mockUserDao,
      ownerRequestDao,
      inventoryDao,
      elasticSearchClient
    )
  }

  trait InventoryData {
    this: Data =>
    val tenantCheckInDate = DateTimeFormat.write(DateTimeUtil.now().minusDays(1).minusHours(1))

    val inventory = {
      val i = inventoryGen.next
      i.copy(data = i.data.toBuilder.setConfirmedByOwnerDate(tenantCheckInDate).build())
    }
    val ownerRequest = ownerRequestGen.next.copy(flatId = flatId)
    val flat = flatGen(recursive = false).next.copy(flatId = flatId)
    val quiz = flatQuestionnaireGen.next.copy(flatId = flatId)

    val contract = {
      val c =
        rentContractGen(ContractStatus.Active).next.copy(flatId = flatId)
      c.copy(data = c.data.toBuilder.setTenantCheckInDate(tenantCheckInDate).build())
    }
  }

  trait Data { this: Wiring =>

    val flatId = "test_id_1"

    def getFlatModificationDiffEvent(flatId: String): DiffEvent =
      DiffEvent
        .newBuilder()
        .setFlatEvent(
          FlatDiffEvent
            .newBuilder()
            .setNew(FlatProtoView.newBuilder().setFlatId(flatId))
            .setOld(FlatProtoView.newBuilder().setFlatId(flatId))
        )
        .build()

    def getUserCreationDiffEvent(uid: Long): DiffEvent =
      DiffEvent
        .newBuilder()
        .setUserEvent(
          UserDiffEvent
            .newBuilder()
            .setNew(UserProtoView.newBuilder().setUid(uid))
            .setOld(UserProtoView.newBuilder().setUid(uid))
        )
        .build()

    def getUserDeletionDiffEvent(uid: Long): DiffEvent =
      DiffEvent
        .newBuilder()
        .setUserEvent(
          UserDiffEvent
            .newBuilder()
            .setOld(UserProtoView.newBuilder().setUid(uid))
        )
        .build()

    def getInventoryDiffEvent(ownerRequestId: String): DiffEvent =
      DiffEvent
        .newBuilder()
        .setInventoryEvent(
          InventoryDiffEvent
            .newBuilder()
            .setNew(InventoryProtoView.newBuilder().setOwnerRequestId(ownerRequestId))
            .build()
        )
        .build()
  }

  trait TestHelper { self: Wiring with Data =>

    def mockFlatDao(
      flat: Flat,
      quiz: FlatQuestionnaire,
      contractOpt: Option[RentContract],
      payments: Seq[Payment],
      ownerRequestOpt: Option[OwnerRequest]
    ): Unit = {
      (mockFlatDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(flat.flatId, *)
        .returns(Future.successful(Some(flat)))
        .once()

      (mockFlatQuestionnaireDao
        .findByFlatId(_: String)(_: Traced))
        .expects(flat.flatId, *)
        .returns(Future.successful(Some(quiz)))
        .once()

      (mockContractDao
        .findByFlatIds(_: Seq[String])(_: Traced))
        .expects(Seq(flat.flatId), *)
        .returns(Future.successful(contractOpt))
        .once()

      (mockPaymentDao
        .getContractPayments(_: String)(_: Traced))
        .expects(*, *)
        .returns(Future.successful(payments))
        .anyNumberOfTimes()

      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(*, *)
        .returns(Future.successful(ownerRequestOpt))
        .once()
    }

    def mockUserDao(user: User): Unit =
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(user.uid, *, *)
        .returns(Future.successful(Some(user)))
        .once()

    def mockInventory(confirmed: Option[Inventory], last: Inventory, ownerRequest: OwnerRequest): Unit = {
      (inventoryDao
        .findLastConfirmedByOwnerRequestId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(confirmed))
        .once()
      (inventoryDao
        .findLastByOwnerRequestId(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(last)))
        .once()
      (ownerRequestDao
        .findByIdOpt(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(Some(ownerRequest)))
        .once()
    }

    def indexUser(user: User): Unit = {
      elasticSearchClient
        .index(
          indexInto(ElasticUser.IndexName)
            .id(user.uid.toString)
            .doc(
              ElasticUser(
                user.uid,
                user.userId,
                user.fullName,
                user.phone,
                user.email,
                user.createTime.getMillis
              )
            )
        )(Traced.empty)
        .futureValue
    }

    def getFromElastic(q: Query, indexName: String = IndexName): Future[SlicedResult[String]] = {
      val page = Page(0, 10)
      for {
        _ <- elasticSearchClient.refresh(refreshIndex(indexName))(Traced.empty)
        response <- elasticSearchClient.search {
          search(indexName)
            .query(q)
            .start(page.from)
            .limit(page.size)
        }(Traced.empty)
        slicedFlatIds <- parseESResponse(response, page)
      } yield slicedFlatIds
    }

    private def parseESResponse(r: SearchResponse, page: Page): Future[SlicedResult[String]] = {
      val results = r.hits.hits.map(_.id)
      val total = r.totalHits
      Future.successful(SlicedResult(results, total.toInt, page))
    }
  }

}
