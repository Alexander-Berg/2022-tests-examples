package ru.yandex.vertis.telepony.service.impl

import org.mockito.Mockito
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Complaint.ComplaintCauseTypeValues.{NotSuitableForExternalFeed, TooManyNonTargetCalls}
import ru.yandex.vertis.telepony.model.Complaint.ComplaintStatusValues.{Fixed, Open}
import ru.yandex.vertis.telepony.model.{Complaint, TypedDomain, TypedDomains}
import ru.yandex.vertis.telepony.service.RedirectServiceV2.ComplainRequest
import ru.yandex.vertis.telepony.service.{ProcessComplaintsService, RedirectServiceV2, SharedComplaintService}
import ru.yandex.vertis.telepony.util.AuthorizedContext
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration._

class ProcessComplaintsServiceImplSpec extends SpecBase with MockitoSupport with ScalaCheckDrivenPropertyChecks {

  val typedDomain: TypedDomain = TypedDomains.autoru_def

  val ttl: FiniteDuration = 10.days

  implicit val ac: AuthorizedContext = AuthorizedContext(
    id = "1",
    login = "testLogin",
    trace = Traced.empty
  )

  def processComplaintService(
      sharedComplaintService: SharedComplaintService,
      redirectServiceV2: RedirectServiceV2): ProcessComplaintsService =
    new ProcessComplaintsServiceImpl(sharedComplaintService, redirectServiceV2, typedDomain, ttl)

  "ProcessComplaintService" should {
    "get active complaints with type" in {
      val mockSCS = mock[SharedComplaintService]
      val mockRSV2 = mock[RedirectServiceV2]
      val slicedResult = ComplaintSlicedResultGen.next
      when(mockSCS.list(?, ?)).thenReturn(Future.successful(slicedResult))
      val service = processComplaintService(
        mockSCS,
        mockRSV2
      )
      val causeType = TooManyNonTargetCalls
      val result = service.getActiveComplaints(Set(causeType)).futureValue

      result.filterNot(_.payload.complaintCause.causeType == causeType) shouldBe empty

      Mockito.verify(mockSCS).list(?, ?)
    }

    "fail when trying to fix complaint with status != Open" in {
      forAll(ComplaintStatusGen.suchThat(_ != Open)) { status =>
        val mockSCS = mock[SharedComplaintService]
        val mockRSV2 = mock[RedirectServiceV2]
        val service = processComplaintService(
          mockSCS,
          mockRSV2
        )
        val cause = ComplaintCauseGen.next.copy(causeType = TooManyNonTargetCalls)
        val payload = ComplaintPayloadGen.next.copy(complaintCause = cause)
        val complaint: Complaint = ComplaintGen.next.copy(status = status, payload = payload)
        service.fix(complaint).failed.futureValue shouldBe a[IllegalArgumentException]
      }
    }

    "fail when trying to fix complaint with cause != TooManyNonTargetCalls/NotSuitableForExternalFeed" in {
      forAll(ComplaintCauseTypeGen.suchThat(c => c != TooManyNonTargetCalls && c != NotSuitableForExternalFeed)) {
        causeType =>
          val mockSCS = mock[SharedComplaintService]
          val mockRSV2 = mock[RedirectServiceV2]
          val service = processComplaintService(
            mockSCS,
            mockRSV2
          )
          val cause = ComplaintCauseGen.next.copy(causeType)
          val payload = ComplaintPayloadGen.next.copy(complaintCause = cause)
          val complaint: Complaint = ComplaintGen.next.copy(status = Open, payload = payload)
          service.fix(complaint).failed.futureValue shouldBe a[IllegalArgumentException]
      }
    }

    "fix opened complaints with cause == TooManyNonTargetCalls" in {
      val mockSCS = mock[SharedComplaintService]
      val mockRSV2 = mock[RedirectServiceV2]
      val service = processComplaintService(
        mockSCS,
        mockRSV2
      )
      val cause = ComplaintCauseGen.next.copy(causeType = TooManyNonTargetCalls)
      val payload = ComplaintPayloadGen.next.copy(complaintCause = cause)
      val complaint = ComplaintGen.next.copy(status = Open, payload = payload)
      val request = ComplainRequest.WithTtl(Some(ttl))
      when(mockRSV2.complain(eq(complaint.operatorNumber), eq(request))(eq(ac)))
        .thenReturn(Future.unit)
      when(mockSCS.updateStatus(eq(complaint.id), eq(Fixed), ?)(eq(ac)))
        .thenReturn(Future.successful(complaint))

      service.fix(complaint).futureValue shouldEqual complaint
      Mockito.verify(mockRSV2).complain(eq(complaint.operatorNumber), eq(request))(eq(ac))
      Mockito.verify(mockSCS).updateStatus(eq(complaint.id), eq(Fixed), ?)(eq(ac))
    }
  }
}
