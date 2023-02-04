package ru.yandex.realty.rent.backend.manager

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import realty.palma.rent_common.RentDocument
import realty.palma.rent_flat.{RentFlat => PalmaRentFlat}
import ru.yandex.realty.amohub.proto.api.internal.InternalGetLeadsByFlatIds
import ru.yandex.realty.amohub.proto.api.internal.PipelineNamespace.Pipeline
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.errors.ConflictApiException
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.logging.TracedLogging
import ru.yandex.realty.rent.backend.manager.moderation.ModerationManager
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus.OwnerRequestStatus
import ru.yandex.realty.rent.model.{Flat, FlatKeyCode, FlatQuestionnaire, OwnerRequest}
import ru.yandex.realty.rent.proto.api.moderation._
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ModerationManagerSpec
  extends SpecBase
  with AsyncSpecBase
  with RequestAware
  with PropertyChecks
  with SearcherResponseModelGenerators
  with TracedLogging {

//  "ModerationManager.setFlatShowingStatus" should {
//    "set flat showing status successfully" in new Wiring with SetFlatShowingStatusDataset {
//
//      sampleRequests.foreach { request =>
//        log.info(s"for request [$request]")
//
//        (mockLeadDao.findLeadIdByFlatAndShowingId _)
//          .expects(sampleFlatId, request.getShowingId)
//          .returning(Future.successful(Some(sampleLeadId)))
//
//        (mockLeadDao
//          .update(_: Long)(_: Lead => Lead))
//          .expects(where {
//            case (leadId, updater) =>
//              val updatedLead = updater(sampleLead)
//
//              leadId == sampleLeadId && updatedLead.statusId == LeadStatusConverter
//                .toLeadStatusId(request.getStatusCase)
//          })
//          .returning(Future.unit)
//
//        moderationManager.setFlatShowingStatus(sampleFlatId, request)(traced).futureValue
//      }
//    }
//
//    "throw exception if leadDao produces an exception" in new Wiring with SetFlatShowingStatusDataset {
//
//      (mockLeadDao.findLeadIdByFlatAndShowingId _)
//        .expects(sampleFlatId, setTimeMatcingRequest.getShowingId)
//        .returning(Future.failed(new NoSuchElementException()))
//
//      interceptCause[NoSuchElementException] {
//        moderationManager.setFlatShowingStatus(sampleFlatId, setTimeMatcingRequest)(traced).futureValue
//      }
//    }
//  }

  "ModerationManager.getFlatsByAddress" should {
    "return successful result" in new Wiring with GetFlatsByAddressData {
      (mockFlatDao
        .findByUnifiedAddress(_: String, _: Map[Option[Boolean], Set[OwnerRequestStatus]], _: Int)(_: Traced))
        .expects(sampleAddress, sampleStatuses, sampleLimit, *)
        .returning(Future.successful(sampleFlats))

      val flatIds: Set[String] = sampleFlats.map(_.flatId).toSet

      (mockFlatQuestionnaireDao
        .findByFlatIds(_: Set[String])(_: Traced))
        .expects(flatIds, *)
        .returning(Future.successful(sampleQuestionnaires))

      (ownerRequestDao
        .findLastByFlatIds(_: Set[String])(_: Traced))
        .expects(flatIds, *)
        .returning(Future.successful(sampleOwnerRequests))

      val ownerRequestIds = sampleOwnerRequests.map(_.ownerRequestId).toSet

      (mockFlatShowingDao
        .findByOwnerRequests(_: Set[String])(_: Traced))
        .expects(ownerRequestIds, *)
        .returning(Future.successful(Nil))

      val result: FlatsInfoSuggestResponse =
        moderationManager.getFlatsByAddress(sampleAddress, sampleStatuses).futureValue

      result.getFlatsList.asScala should contain theSameElementsAs expectedResult.getFlatsList.asScala
    }
  }

//  trait SetFlatShowingStatusDataset {
//    this: Wiring =>
//
//    val sampleFlatId: String = readableString.next
//    val sampleShowingId: String = readableString.next
//    val sampleLeadId: Long = posNum[Long].next
//
//    val sampleLead: Lead = Lead(
//      leadId = sampleLeadId,
//      flatId = Some(sampleFlatId),
//      statusId = 0,
//      source = None,
//      pipelineId = None,
//      lossReasonId = None,
//      isConfirmed = true,
//      showingId = None,
//      deleted = false,
//      markedDeletedInWebhook = false,
//      createTime = DateTime.now(),
//      updateTime = DateTime.now(),
//      ytExportHash = None,
//      managerId = None,
//      lastEventTimestamp = None,
//      visitTime = None,
//      shardKey = 0
//    )
//
//    val setTimeMatcingRequest: SetFlatShowingStatusRequest = SetFlatShowingStatusRequest
//      .newBuilder()
//      .setShowingId(sampleShowingId)
//      .setTimeMatching(SetFlatShowingStatusRequest.TimeMatching.newBuilder().build())
//      .build()
//
//    val setOnlineRequest: SetFlatShowingStatusRequest = SetFlatShowingStatusRequest
//      .newBuilder()
//      .setShowingId(sampleShowingId)
//      .setOnline(SetFlatShowingStatusRequest.Online.newBuilder().build())
//      .build()
//
//    val setOfflineRequest: SetFlatShowingStatusRequest = SetFlatShowingStatusRequest
//      .newBuilder()
//      .setShowingId(sampleShowingId)
//      .setOffline(SetFlatShowingStatusRequest.Offline.newBuilder().build())
//      .build()
//
//    val setPrepareAccountRequest: SetFlatShowingStatusRequest = SetFlatShowingStatusRequest
//      .newBuilder()
//      .setShowingId(sampleShowingId)
//      .setPrepareAccount(SetFlatShowingStatusRequest.PrepareAccount.newBuilder().build())
//      .build()
//
//    val setValidateRequest: SetFlatShowingStatusRequest = SetFlatShowingStatusRequest
//      .newBuilder()
//      .setShowingId(sampleShowingId)
//      .setValidate(SetFlatShowingStatusRequest.Validate.newBuilder().build())
//      .build()
//
//    val setOwnerAgreementsRequest: SetFlatShowingStatusRequest = SetFlatShowingStatusRequest
//      .newBuilder()
//      .setShowingId(sampleShowingId)
//      .setOwnerAgreements(SetFlatShowingStatusRequest.OwnerAgreements.newBuilder().build())
//      .build()
//
//    val sampleRequests = Seq(
//      setTimeMatcingRequest,
//      setOnlineRequest,
//      setOfflineRequest,
//      setPrepareAccountRequest,
//      setValidateRequest,
//      setOwnerAgreementsRequest
//    )
//
//  }

  "ModerationManager.updateModerationFlat" should {
    "throw error if keys photo documentId is not in palma dictionary" in
      new Wiring with Data with UpdateModerationFlatData {
        (mockPalmaRentFlatClient
          .get(_: String)(_: Traced))
          .expects(sampleFlatId, *)
          .returning(Future.successful(Some(PalmaRentFlat(flatId = sampleFlatId))))

        val request = updateModerationFlatRequest(documentId)
        val error = moderationManager.updateModerationFlat(sampleFlatId, request).failed.futureValue
        error shouldBe a[IllegalArgumentException]
        error.getMessage.contains(documentId) shouldBe true
      }

    "update flat if documentId is in palma dictionary" in new Wiring with Data with UpdateModerationFlatData {

      (mockPalmaRentFlatClient
        .get(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(
          Future.successful(Some(PalmaRentFlat(flatId = sampleFlatId, documents = List(RentDocument(documentId)))))
        )

      val request = updateModerationFlatRequest(documentId)
      val result = moderationManager.updateModerationFlat(sampleFlatId, request).futureValue

      result.getFlat.getDocuments.getFlatKeysPhotoDocumentId shouldEqual documentId
    }

    "update flat if documentId is empty" in new Wiring with Data with UpdateModerationFlatData {
      (mockPalmaRentFlatClient
        .get(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(
          Future.successful(Some(PalmaRentFlat(flatId = sampleFlatId)))
        )

      val request = updateModerationFlatRequest("")
      val result = moderationManager.updateModerationFlat(sampleFlatId, request).futureValue

      result.getFlat.getDocuments.getFlatKeysPhotoDocumentId shouldEqual ""
    }
  }

  "ModerationManager.setKeyCodeForFlat" should {
    "set full key code for flat" in new Wiring with DataKeyCode {
      val newKeyCode = "A00000000002"
      (mockFlatDao
        .findByKeyCode(_: String)(_: Traced))
        .expects(newKeyCode, *)
        .once()
        .returning(Future.successful(None))

      (mockFlatDao
        .update(_: String, _: Boolean)(_: Flat => Flat)(_: Traced))
        .expects(flatWithKeyCode1.flatId, *, *, *)
        .once()
        .onCall(
          (_, _, f, _) =>
            Future.successful {
              flatWithKeyCode1 = f(flatWithKeyCode1)
              flatWithKeyCode1
            }
        )

      val keyCode = FlatKeyCode(newKeyCode.drop(1).toLong, 123, DateTime.now())
      (mockFlatKeyCodeDao
        .find(_: Long)(_: Traced))
        .expects(keyCode.id, *)
        .once()
        .returning(Future.successful(Some(keyCode)))

      moderationManager.setKeyCodeForFlat(flatWithKeyCode1.flatId, newKeyCode).futureValue
      flatWithKeyCode1.keyCode shouldBe Some(newKeyCode)
    }

    "set short key code for flat" in new Wiring with DataKeyCode {
      val shortKeyCode = "123"
      val fullKeyCode = "A00000000123"
      (mockFlatDao
        .findByKeyCode(_: String)(_: Traced))
        .expects(fullKeyCode, *)
        .once()
        .returning(Future.successful(None))

      (mockFlatDao
        .update(_: String, _: Boolean)(_: Flat => Flat)(_: Traced))
        .expects(flatWithKeyCode1.flatId, *, *, *)
        .once()
        .onCall(
          (_, _, f, _) =>
            Future.successful {
              flatWithKeyCode1 = f(flatWithKeyCode1)
              flatWithKeyCode1
            }
        )

      val keyCode = FlatKeyCode(shortKeyCode.toLong, 123, DateTime.now())
      (mockFlatKeyCodeDao
        .find(_: Long)(_: Traced))
        .expects(keyCode.id, *)
        .once()
        .returning(Future.successful(Some(keyCode)))

      moderationManager.setKeyCodeForFlat(flatWithKeyCode1.flatId, shortKeyCode).futureValue
      flatWithKeyCode1.keyCode shouldBe Some(fullKeyCode)
    }

    "key code is busy" in new Wiring with DataKeyCode {
      val newKeyCode = "A00000000002"

      val keyCode = FlatKeyCode(newKeyCode.drop(1).toLong, 123, DateTime.now())
      (mockFlatKeyCodeDao
        .find(_: Long)(_: Traced))
        .expects(keyCode.id, *)
        .once()
        .returning(Future.successful(Some(keyCode)))

      (mockFlatDao
        .findByKeyCode(_: String)(_: Traced))
        .expects(newKeyCode, *)
        .once()
        .returning(Future.successful(Some(flatWithKeyCode1)))

      val result = moderationManager.setKeyCodeForFlat(flatWithKeyCode1.flatId, newKeyCode).failed.futureValue
      result shouldBe a[ConflictApiException]
    }

    "key code does not exist" in new Wiring with DataKeyCode {
      val newKeyCode = "A00000000666"

      val keyCode = FlatKeyCode(newKeyCode.drop(1).toLong, 123, DateTime.now())
      (mockFlatKeyCodeDao
        .find(_: Long)(_: Traced))
        .expects(keyCode.id, *)
        .once()
        .returning(Future.successful(None))

      val result = moderationManager.setKeyCodeForFlat(flatWithKeyCode1.flatId, newKeyCode).failed.futureValue
      result shouldBe a[ConflictApiException]
    }

  }

  trait DataKeyCode extends RentModelsGen {
    this: Wiring =>

    var flatWithKeyCode1 = {
      val flat = flatGen().next
      flat.copy(flatId = "flat-keycode-1", keyCode = Some("A00000000001"))
    }

  }

  trait UpdateModerationFlatData extends RentModelsGen {
    this: Wiring with Data =>

    val documentId = "document_id"

    (mockFlatDao
      .updateFR(_: String, _: Boolean)(_: Flat => Future[(Flat, Boolean)])(_: Traced))
      .expects(sampleFlatId, *, *, *)
      .onCall((_, _, f, _) => f(sampleFlat))

    val leadsRequest = InternalGetLeadsByFlatIds.Request
      .newBuilder()
      .addFlatId(sampleFlatId)
      .setPipeline(Pipeline.REQUESTS)
      .build()

    (mockAmohubLeadClient
      .getLeadsByFlatIds(_: InternalGetLeadsByFlatIds.Request)(_: Traced))
      .expects(leadsRequest, *)
      .returning(Future.successful(InternalGetLeadsByFlatIds.Response.getDefaultInstance))

    (mockContractDao
      .findContractsWithPaymentsByFlatIds(_: Iterable[String])(_: Traced))
      .expects(*, *)
      .returning(Future.successful(Nil))

    def updateModerationFlatRequest(document: String): UpdateModerationFlatRequest =
      UpdateModerationFlatRequest
        .newBuilder()
        .setFlat(
          FlatModeratedFields
            .newBuilder()
            .setAddress(
              ru.yandex.realty.rent.proto.api.flats.Flat.Address
                .newBuilder()
                .setAddress(sampleFlat.address)
                .setFlatNumber(sampleFlat.flatNumber)
            )
            .setDocuments(FlatDocuments.newBuilder().setFlatKeysPhotoDocumentId(document))
        )
        .build()
  }

  trait GetFlatsByAddressData extends RentModelsGen {
    this: Wiring =>

    val sampleAddress = "sample-address"

    val sampleStatuses: Map[Option[Boolean], Set[OwnerRequestStatus]] = Map(
      (None, Set(OwnerRequestStatus.WorkInProgress))
    )
    val sampleLimit: Int = ModerationManager.FlatSuggestParams.DefaultFlatsLimit
    val sampleFlats: Seq[Flat] = flatGen().next(10).toList.sortBy(_.flatId)
    val sampleFlatsMap: Map[String, Flat] = sampleFlats.groupBy(_.flatId).mapValues(_.head)

    val sampleQuestionnaires: Seq[FlatQuestionnaire] =
      flatQuestionnaireGen
        .next(sampleFlats.size)
        .zip(sampleFlats)
        .map {
          case (q, f) => q.copy(flatId = f.flatId)
        }
        .toSeq
        .sortBy(_.flatId)

    val sampleOwnerRequests: Seq[OwnerRequest] =
      ownerRequestGen
        .next(sampleFlats.size)
        .zip(sampleFlats)
        .map {
          case (q, f) => q.copy(flatId = f.flatId)
        }
        .toSeq
        .sortBy(_.flatId)

    val expectedEntriesResult: Seq[FlatInfoSuggestEntry] = sampleQuestionnaires
      .map { q =>
        val flat = sampleFlatsMap(q.flatId)
        FlatInfoSuggestEntry
          .newBuilder()
          .setFlatId(q.flatId)
          .setAddress(flat.unifiedAddress.get)
          .setArea(q.data.getFlat.getArea)
          .setFloor(q.data.getFlat.getFloor)
          .setCost(q.data.getPayments.getAdValue)
          .setQuestionnaire(q.data)
          .setShowings(
            SuggestShowingStatistic
              .newBuilder()
              .build()
          )
          .setTimezone(Timezone.newBuilder().setName("Unknown").build())
          .build()
      }
      .sortBy(_.getFlatId)

    val expectedResult: FlatsInfoSuggestResponse =
      FlatsInfoSuggestResponse
        .newBuilder()
        .addAllFlats(expectedEntriesResult.asJava)
        .build()
  }
}
