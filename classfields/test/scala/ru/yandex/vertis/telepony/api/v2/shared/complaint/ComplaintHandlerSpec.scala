package ru.yandex.vertis.telepony.api.v2.shared.complaint

import akka.http.scaladsl.marshalling.GenericMarshallers
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import org.joda.time.DateTime
import org.mockito.Mockito
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eql}
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.dao.ComplaintDao
import ru.yandex.vertis.telepony.dao.ComplaintDao._
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.proto.ComplaintsModel
import ru.yandex.vertis.telepony.model.proto.ComplaintsModel.{ComplaintCommentRequest, ComplaintStatusChangeRequest, ComplaintsSliceRequest}
import ru.yandex.vertis.telepony.model.{Complaint, ComplaintStatus}
import ru.yandex.vertis.telepony.proto.ProtoConversions._
import ru.yandex.vertis.telepony.service.SharedComplaintService
import ru.yandex.vertis.telepony.service.SharedComplaintService.{ComplaintCommentNotFound, ComplaintNotFound, SliceRequest}
import ru.yandex.vertis.telepony.util.sliced.SlicedResult
import ru.yandex.vertis.telepony.util.{AuthorizedContext, Page}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.Future
import scala.util.Random

/**
  * @author ponydev
  */
class ComplaintHandlerSpec
  extends RouteTest
  with MockitoSupport
  with ScalaCheckDrivenPropertyChecks
  with GenericMarshallers
  with ProtobufSupport {

  val loginHeader = RawHeader("X-Yandex-Internal-Login", "ponydev")

  private def createHandler(cs: SharedComplaintService): Route = {
    seal(
      new ComplaintHandler {
        override def complaintService: SharedComplaintService = cs
      }.route
    )
  }

  class TestEnv {
    val mockCS: SharedComplaintService = mock[SharedComplaintService]
    val handler: Route = createHandler(mockCS)
    val page = Page(0, 5)

    val sortOrder = ComplaintsSortOrder.Default.copy(complaintsOrderDirection = OrderDirection.DESC)

    def defaultFilter(author: String, status: ComplaintStatus): Seq[ComplaintDao.ComplaintsFilter] = Seq(
      ComplaintsFilter(author = Some(author)),
      ComplaintsFilter(status = Some(status))
    )

    def defaultSliceRequest(author: String, status: ComplaintStatus, withOrder: Boolean): SliceRequest = {
      SliceRequest(
        defaultFilter(author, status),
        if (withOrder) sortOrder else ComplaintsSortOrder.Default
      )
    }

    implicit val ac: AuthorizedContext = AuthorizedContext(
      id = "1",
      login = "testLogin",
      trace = Traced.empty
    )

  }

  "ComplaintHandler" should {
    "get by id correctly" in new TestEnv {
      forAll(ComplaintGen) { complaint =>
        when(mockCS.getById(eql(complaint.id)))
          .thenReturn(Future.successful(complaint))
        Get(s"/${complaint.id}") ~>
          handler ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[Complaint] shouldEqual complaint
        }
        Mockito.verify(mockCS).getById(eql(complaint.id))
      }
    }
  }
  "fail if no such complaint exists" in new TestEnv {
    forAll(ComplaintGen) { complaint =>
      when(mockCS.getById(eql(complaint.id)))
        .thenReturn(Future.failed(ComplaintNotFound(complaint.id)))
      Get(s"/${complaint.id}") ~>
        handler ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockCS).getById(eql(complaint.id))
    }
  }
  "get by redirect id correctly" in new TestEnv {
    forAll(ComplaintGen) { complaint =>
      when(mockCS.getByRedirectId(eql(complaint.redirectId)))
        .thenReturn(Future.successful(Some(complaint)))
      Get(s"/redirect/${complaint.redirectId.value}") ~>
        handler ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Complaint] shouldEqual complaint
      }
      Mockito.verify(mockCS).getByRedirectId(eql(complaint.redirectId))
    }
  }
  "fail if no such complaint exists by redirect id" in new TestEnv {
    forAll(ComplaintGen) { complaint =>
      when(mockCS.getByRedirectId(eql(complaint.redirectId)))
        .thenReturn(Future.successful(None))
      Get(s"/redirect/${complaint.redirectId.value}") ~>
        handler ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockCS).getByRedirectId(eql(complaint.redirectId))
    }
  }
  "add comment to complaint" in new TestEnv {
    forAll(ComplaintGen, ComplaintCommentGen) {
      case (complaint, newComment) =>
        val newComplaintPayload = complaint.payload.copy(
          commentsList = complaint.payload.commentsList ++ Seq(newComment)
        )
        val newComplaint = complaint.copy(payload = newComplaintPayload)
        val newCommentTextRequest = ComplaintCommentRequest.newBuilder().setCommentText(newComment.text).build()
        when(mockCS.addComment(eql(complaint.id), eql(newComment.text))(?))
          .thenReturn(Future.successful(newComplaint))
        val uri = Uri(s"/${complaint.id}/comment")
        Post(uri, newCommentTextRequest).withHeaders(loginHeader) ~>
          handler ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[Complaint] shouldEqual newComplaint
        }
        Mockito.verify(mockCS).addComment(eql(complaint.id), eql(newComment.text))(?)
    }
  }
  "fail add to non existent complaint" in new TestEnv {
    forAll(ComplaintGen, ComplaintCommentGen) {
      case (complaint, newComment) =>
        val newCommentTextRequest = ComplaintCommentRequest.newBuilder().setCommentText(newComment.text).build()
        when(mockCS.addComment(eql(complaint.id), eql(newComment.text))(?))
          .thenReturn(Future.failed(ComplaintNotFound(complaint.id)))
        val uri = Uri(s"/${complaint.id}/comment").withQuery(Query(Map("comment" -> newComment.text)))
        Post(uri, newCommentTextRequest).withHeaders(loginHeader) ~>
          handler ~> check {
          status shouldEqual StatusCodes.NotFound
        }
    }
  }
  "update comment in complaint" in new TestEnv {
    forAll(ComplaintGen, ComplaintCommentGen) {
      case (complaint, newComment) =>
        val comments = complaint.payload.commentsList
        val commentToUpdateId = Random.nextInt(comments.size)
        val updatedComment = comments(commentToUpdateId).copy(text = newComment.text)
        val newComplaintPayload = complaint.payload.copy(
          commentsList = complaint.payload.commentsList.updated(commentToUpdateId, updatedComment)
        )
        val newComplaint = complaint.copy(payload = newComplaintPayload)
        val newCommentTextRequest = ComplaintCommentRequest.newBuilder().setCommentText(newComment.text).build()
        when(mockCS.updateComment(eql(complaint.id), eql(updatedComment.id), eql(updatedComment.text)))
          .thenReturn(Future.successful(newComplaint))
        val uri = Uri(s"/${complaint.id}/comment/${updatedComment.id}")
        Put(uri, newCommentTextRequest) ~>
          handler ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[Complaint] shouldEqual newComplaint
        }
        Mockito.verify(mockCS).updateComment(eql(complaint.id), eql(updatedComment.id), eql(updatedComment.text))
    }
  }
  "fail update comment in non existent complaint" in new TestEnv {
    forAll(ComplaintGen, ComplaintCommentGen) {
      case (complaint, newComment) =>
        val comments = complaint.payload.commentsList
        val commentToUpdateId = Random.nextInt(comments.size)
        val updatedComment = comments(commentToUpdateId).copy(text = newComment.text)
        val newCommentTextRequest = ComplaintCommentRequest.newBuilder().setCommentText(newComment.text).build()
        when(mockCS.updateComment(eql(complaint.id), eql(updatedComment.id), eql(updatedComment.text)))
          .thenReturn(Future.failed(ComplaintNotFound(complaint.id)))
        val uri = Uri(s"/${complaint.id}/comment/${updatedComment.id}")
        Put(uri, newCommentTextRequest) ~>
          handler ~> check {
          status shouldEqual StatusCodes.NotFound
        }
        Mockito.verify(mockCS).updateComment(eql(complaint.id), eql(updatedComment.id), eql(updatedComment.text))
    }
  }
  "fail update non existent comment in complaint" in new TestEnv {
    forAll(ComplaintGen, ComplaintCommentGen) {
      case (complaint, newComment) =>
        val comments = complaint.payload.commentsList
        val commentToUpdateId = Random.nextInt(comments.size)
        val updatedComment = comments(commentToUpdateId).copy(text = newComment.text)
        val newCommentTextRequest = ComplaintCommentRequest.newBuilder().setCommentText(newComment.text).build()
        when(mockCS.updateComment(eql(complaint.id), eql(updatedComment.id), eql(updatedComment.text)))
          .thenReturn(Future.failed(ComplaintCommentNotFound(complaint.id, updatedComment.id)))
        val uri = Uri(s"/${complaint.id}/comment/${updatedComment.id}")
        Put(uri, newCommentTextRequest) ~>
          handler ~> check {
          status shouldEqual StatusCodes.NotFound
        }
        Mockito.verify(mockCS).updateComment(eql(complaint.id), eql(updatedComment.id), eql(updatedComment.text))
    }
  }
  "delete comment in complaint" in new TestEnv {
    forAll(ComplaintGen) { complaint =>
      val comments = complaint.payload.commentsList
      val commentToDeleteId = Random.nextInt(comments.size)
      val newComplaintPayload = complaint.payload.copy(
        commentsList = complaint.payload.commentsList.patch(commentToDeleteId, Nil, 1)
      )
      val newComplaint = complaint.copy(payload = newComplaintPayload)
      when(mockCS.deleteComment(eql(complaint.id), eql(comments(commentToDeleteId).id)))
        .thenReturn(Future.successful(newComplaint))
      val uri = Uri(s"/${complaint.id}/comment/${comments(commentToDeleteId).id}")
      Delete(uri) ~>
        handler ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Complaint] shouldEqual newComplaint
      }
      Mockito.verify(mockCS).deleteComment(eql(complaint.id), eql(comments(commentToDeleteId).id))
    }
  }
  "fail delete comment in non existent complaint" in new TestEnv {
    forAll(ComplaintGen) { complaint =>
      val comments = complaint.payload.commentsList
      val commentToDeleteId = Random.nextInt(comments.size)
      when(mockCS.deleteComment(eql(complaint.id), eql(comments(commentToDeleteId).id)))
        .thenReturn(Future.failed(ComplaintNotFound(complaint.id)))
      val uri = Uri(s"/${complaint.id}/comment/${comments(commentToDeleteId).id}")
      Delete(uri) ~>
        handler ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockCS).deleteComment(eql(complaint.id), eql(comments(commentToDeleteId).id))
    }
  }
  "fail delete non existent comment in complaint" in new TestEnv {
    forAll(ComplaintGen) { complaint =>
      val comments = complaint.payload.commentsList
      val commentToDeleteId = Random.nextInt(comments.size)
      when(mockCS.deleteComment(eql(complaint.id), eql(comments(commentToDeleteId).id)))
        .thenReturn(Future.failed(ComplaintCommentNotFound(complaint.id, comments(commentToDeleteId).id)))
      val uri = Uri(s"/${complaint.id}/comment/${comments(commentToDeleteId).id}")
      Delete(uri) ~>
        handler ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockCS).deleteComment(eql(complaint.id), eql(comments(commentToDeleteId).id))
    }
  }
  "update status with comment in complaint" in new TestEnv {
    import ComplaintStatusProtoConversion._
    forAll(ComplaintGen, ComplaintStatusGen, ComplaintCommentGen) {
      case (complaint, complaintStatus, statusComment) =>
        val complaintStatusChangeRequest =
          ComplaintStatusChangeRequest
            .newBuilder()
            .setStatus(complaintStatus)
            .setComment(statusComment.text)
            .build()
        val newComplaintWithStatus = complaint.copy(status = complaintStatus)
        val newComplaintPayload = complaint.payload.copy(
          commentsList = complaint.payload.commentsList ++ Seq(statusComment)
        )
        val newComplaintWithStatusAndComment = newComplaintWithStatus.copy(payload = newComplaintPayload)
        when(
          mockCS.updateStatus(
            eql(complaint.id),
            eql(complaintStatus),
            eql(Some(statusComment.text))
          )(?)
        ).thenReturn(Future.successful(newComplaintWithStatusAndComment))
        Put(s"/${complaint.id}/status", complaintStatusChangeRequest)
          .withHeaders(loginHeader) ~>
          handler ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[Complaint] shouldEqual newComplaintWithStatusAndComment
        }
        Mockito
          .verify(mockCS)
          .updateStatus(eql(complaint.id), eql(complaintStatus), eql(Some(statusComment.text)))(?)
    }
  }
  "update status without comment in complaint" in new TestEnv {
    import ComplaintStatusProtoConversion._
    forAll(ComplaintGen, ComplaintStatusGen) {
      case (complaint, complaintStatus) =>
        val complaintStatusChangeRequest =
          ComplaintStatusChangeRequest
            .newBuilder()
            .setStatus(complaintStatus)
            .build()
        val newComplaintWithStatus = complaint.copy(status = complaintStatus)
        when(mockCS.updateStatus(eql(complaint.id), eql(complaintStatus), eql(None))(?))
          .thenReturn(Future.successful(newComplaintWithStatus))
        Put(s"/${complaint.id}/status", complaintStatusChangeRequest).withHeaders(loginHeader) ~>
          handler ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[Complaint] shouldEqual newComplaintWithStatus
        }
        Mockito.verify(mockCS).updateStatus(eql(complaint.id), eql(complaintStatus), eql(None))(?)
        Mockito.verify(mockCS, Mockito.never()).addComment(?, ?)(?)
    }
  }
  "fail update status in non existent complaint" in new TestEnv {
    import ComplaintStatusProtoConversion._
    forAll(ComplaintGen, ComplaintStatusGen) {
      case (complaint, complaintStatus) =>
        val complaintStatusChangeRequest =
          ComplaintStatusChangeRequest
            .newBuilder()
            .setStatus(complaintStatus)
            .build()
        when(mockCS.updateStatus(eql(complaint.id), eql(complaintStatus), eql(None))(?))
          .thenReturn(Future.failed(ComplaintNotFound(complaint.id)))
        Put(s"/${complaint.id}/status", complaintStatusChangeRequest).withHeaders(loginHeader) ~>
          handler ~> check {
          status shouldEqual StatusCodes.NotFound
        }
        Mockito.verify(mockCS).updateStatus(eql(complaint.id), eql(complaintStatus), eql(None))(?)
        Mockito.verify(mockCS, Mockito.never()).addComment(?, ?)(?)
    }
  }
  "update cause in complaint" in new TestEnv {
    forAll(ComplaintGen, ComplaintCauseUpdateRequestGen) {
      case (complaint, complaintCauseUpdateRequest) =>
        val complaintCauseUpdateRequestProto =
          ComplaintCauseUpdateRequestProtoConversion.to(complaintCauseUpdateRequest)
        val newComplaintPayload =
          UpdateCause(complaintCauseUpdateRequest).patch(complaint.id, complaint.payload, DateTime.now)
        val newComplaint = complaint.copy(payload = newComplaintPayload)
        when(mockCS.updateCause(eql(complaint.id), eql(complaintCauseUpdateRequest)))
          .thenReturn(Future.successful(newComplaint))
        Put(s"/${complaint.id}/cause", complaintCauseUpdateRequestProto) ~>
          handler ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[Complaint] shouldEqual newComplaint
        }
        Mockito.verify(mockCS).updateCause(eql(complaint.id), eql(complaintCauseUpdateRequest))
    }
  }
  "fail update cause in non existent complaint" in new TestEnv {
    forAll(ComplaintGen, ComplaintCauseUpdateRequestGen) {
      case (complaint, complaintCauseUpdateRequest) =>
        val complaintCauseUpdateRequestProto =
          ComplaintCauseUpdateRequestProtoConversion.to(complaintCauseUpdateRequest)
        when(mockCS.updateCause(eql(complaint.id), eql(complaintCauseUpdateRequest)))
          .thenReturn(Future.failed(ComplaintNotFound(complaint.id)))
        Put(s"/${complaint.id}/cause", complaintCauseUpdateRequestProto) ~>
          handler ~> check {
          status shouldEqual StatusCodes.NotFound
        }
        Mockito.verify(mockCS).updateCause(eql(complaint.id), eql(complaintCauseUpdateRequest))
    }
  }
  "update call info in complaint" in new TestEnv {
    forAll(ComplaintGen, ComplaintCallInfoUpdateRequestGen) {
      case (complaint, complaintCallInfoUpdateRequest) =>
        val complaintCallInfoUpdateRequestProto =
          ComplaintCallInfoUpdateRequestProtoConversion.to(complaintCallInfoUpdateRequest)
        val newComplaintPayload =
          UpdateCallInfo(complaintCallInfoUpdateRequest).patch(complaint.id, complaint.payload, DateTime.now)
        val newComplaint = complaint.copy(payload = newComplaintPayload)
        when(mockCS.updateCallInfo(eql(complaint.id), eql(complaintCallInfoUpdateRequest)))
          .thenReturn(Future.successful(newComplaint))
        Put(s"/${complaint.id}/callinfo", complaintCallInfoUpdateRequestProto) ~>
          handler ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[Complaint] shouldEqual newComplaint
        }
        Mockito.verify(mockCS).updateCallInfo(eql(complaint.id), eql(complaintCallInfoUpdateRequest))
    }
  }
  "list complaints without filters or orders" in new TestEnv {
    private val complaints = ComplaintGen.next(5)
    private val request = ComplaintsSliceRequest.getDefaultInstance
    when(mockCS.list(eql(page), eql(SliceRequest(Seq(), ComplaintsSortOrder.Default))))
      .thenReturn(Future.successful(SlicedResult(complaints, 5, page)))
    private val uri = Uri./.withQuery(Query("pageNum" -> page.number.toString, "pageSize" -> page.size.toString))
    Post(uri, request) ~>
      handler ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[SlicedResult[Complaint]].total shouldEqual 5
    }
    Mockito.verify(mockCS).list(eql(page), eql(SliceRequest(Seq(), ComplaintsSortOrder.Default)))
  }
  "list complaints with filters no order" in new TestEnv {
    import ComplaintFilterListProtoConversion._
    private val complaints = ComplaintGen.next(5)
    private val testAuthor = complaints.toList(3).author
    private val testStatus = complaints.toList(3).status
    private val uri = Uri./.withQuery(Query("pageNum" -> page.number.toString, "pageSize" -> page.size.toString))

    private val request = ComplaintsModel.ComplaintsSliceRequest
      .newBuilder()
      .addAllFilters(defaultFilter(testAuthor, testStatus))
      .build()
    request.toBuilder.setSortOrder(ComplaintsModel.ComplaintsSortOrder.newBuilder()).build()
    private val result = complaints.toList.filter(_.author == testAuthor).filter(_.status == testStatus)
    when(mockCS.list(eql(page), eql(defaultSliceRequest(testAuthor, testStatus, withOrder = false))))
      .thenReturn(Future.successful(SlicedResult(result, result.size, page)))
    Post(uri, request) ~>
      handler ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[SlicedResult[Complaint]].total shouldEqual 1
    }
    Mockito
      .verify(mockCS)
      .list(eql(page), eql(defaultSliceRequest(testAuthor, testStatus, withOrder = false)))
  }
  "list complaints with filters and order" in new TestEnv {
    import ComplaintSliceRequestProtoConversion._

    private val complaints = ComplaintGen.next(5)
    private val testAuthor = complaints.toList(3).author
    private val testStatus = complaints.toList(3).status
    private val uri = Uri./.withQuery(Query("pageNum" -> page.number.toString, "pageSize" -> page.size.toString))
    private val request: ComplaintsSliceRequest = defaultSliceRequest(testAuthor, testStatus, withOrder = true)
    private val result = complaints.toList.filter(_.author == testAuthor).filter(_.status == testStatus)
    when(mockCS.list(eql(page), eql(defaultSliceRequest(testAuthor, testStatus, withOrder = true))))
      .thenReturn(Future.successful(SlicedResult(result, result.size, page)))
    Post(uri, request) ~>
      handler ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[SlicedResult[Complaint]].total shouldEqual 1
    }
    Mockito
      .verify(mockCS)
      .list(eql(page), eql(defaultSliceRequest(testAuthor, testStatus, withOrder = true)))
  }
}
