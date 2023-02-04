package ru.auto.api.managers.complaints

import ru.auto.api.complaints.ComplaintsModel
import ru.auto.api.exceptions.ComplaintsBadRequestException
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.ComplaintsGenerators.ComplaintsManagerRequestGen
import ru.auto.api.services.complaints.ComplaintsClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.FutureMatchers._
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.yandex.vertis.complaints.proto.ComplaintsModel.Complaint.Reason.ANOTHER
import ru.yandex.vertis.mockito.MockitoSupport
import org.mockito.Mockito._

/**
  * @author potseluev
  */
class ComplaintsManagerSpec extends BaseSpec with MockitoSupport {

  private val complaintsClient: ComplaintsClient = mock[ComplaintsClient]
  private val vosClient: VosClient = mock[VosClient]
  private val offer: ApiOfferModel.Offer = OfferGen.next

  when(vosClient.getOffer(?, ?, ?, ?, ?)(?))
    .thenReturnF(offer)

  when(complaintsClient.createComplaint(?)(?))
    .thenReturnF(())

  private val complaintsManager: ComplaintsManager =
    new ComplaintsManager(vosClient, complaintsClient)

  "ComplaintsManager" should {

    "fail with ComplaintsBadRequestException when reason is ANOTHER and text is empty" in {
      val request = ComplaintsManagerRequestGen.next.copy(
        complaintRequest = ComplaintsModel.ComplaintRequest.newBuilder().build()
      )
      complaintsManager.createComplaint(request)(request.apiRequest) should
        failWith[ComplaintsBadRequestException]
      verifyNoMoreInteractions(complaintsClient)
    }

    "accept correct request" in {
      val request = ComplaintsManagerRequestGen
        .suchThat(r => r.complaintRequest.getText.nonEmpty || r.complaintRequest.getReason != ANOTHER)
        .next
      complaintsManager.createComplaint(request)(request.apiRequest) should beSuccessful
      verify(complaintsClient).createComplaint(?)(?)
    }
  }
}
