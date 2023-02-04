package ru.yandex.vertis.telepony.dao

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.ComplaintDao.{ComplaintsFilter, ComplaintsSortOrder, DeleteComment, OrderDirection, OrderField, UpdateCallInfo, UpdateCause, UpdateComment}
import ru.yandex.vertis.telepony.dao.jdbc.JdbcComplaintDao.ComplaintRow
import ru.yandex.vertis.telepony.dao.jdbc.SharedOperatorNumberDao
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Complaint.ComplaintStatusValues
import ru.yandex.vertis.telepony.service.SharedComplaintService.ComplaintDuplicate
import ru.yandex.vertis.telepony.util.Page
import ru.yandex.vertis.telepony.util.Range

/**
  * @author ponydev
  */
trait ComplaintDaoSpec extends SpecBase with BeforeAndAfterEach {

  def dao: ComplaintDao

  def numberDao: SharedOperatorNumberDao

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dao.clear().futureValue
  }

  private def insert(c: ComplaintRow) = {
    val son = SharedOperatorNumberGen.next.copy(number = c.operatorNumber)
    numberDao.upsert(son).futureValue
    dao.insert(c).futureValue
  }

  "ComplaintDao" should {
    "create new complaint" in {
      val row = ComplaintRowGen.next
      val son = SharedOperatorNumberGen.next.copy(number = row.operatorNumber)
      numberDao.upsert(son).futureValue
      dao.insert(row).futureValue
      val list = dao.list(Page(0, 10)).futureValue
      list should have size 1
      inside(list.headOption) {
        case Some(c) =>
          c.redirectId should ===(row.redirectId)
          c.operatorNumber should ===(row.operatorNumber)
          c.targetNumber should ===(row.targetNumber)
          c.author should ===(row.author)
          c.status should ===(row.status)
          c.createTime should ===(row.createTime)
          c.updateTime should ===(row.updateTime)
          c.domain should ===(row.domain)
          c.payload should ===(row.payload)
          c.operator should ===(son.account.operator)
      }
    }
    "get by complaint id" in {
      val complaint = ComplaintRowGen.next
      val inserted = insert(complaint)
      dao.getById(inserted.id).futureValue.value
    }
    "get by redirect id" in {
      val complaint = ComplaintRowGen.next
      insert(complaint)
      dao.getByRedirectId(complaint.redirectId).futureValue.value
    }
    "update complaint status" in {
      val expectedStatus = ComplaintStatusValues.Fixed
      val inserted = insert(ComplaintRowGen.suchThat(_.status != expectedStatus).next)
      val updated = dao.updateStatus(inserted.id, expectedStatus).futureValue
      updated.status should ===(expectedStatus)
    }
    "add comment" in {
      val complaint = ComplaintRowGen.next
      val oldComplaint = insert(complaint)
      val newCommentPatch = AddCommentPayloadPatchGen.next
      dao.updatePayload(oldComplaint.id, newCommentPatch).futureValue
      val newComplaint = dao.getById(oldComplaint.id).futureValue.getOrElse(fail("No such complaint exist"))
      newComplaint.payload.commentsList should have size oldComplaint.payload.commentsList.size + 1
      (newComplaint.payload.commentsList should contain)
        .theSameElementsInOrderAs(oldComplaint.payload.commentsList ++ Seq(newComplaint.payload.commentsList.last))
    }
    "update comment" in {
      val complaint = insert(ComplaintRowGen.next)
      val newCommentPatch = AddCommentPayloadPatchGen.next
      val newComplaint = dao.updatePayload(complaint.id, newCommentPatch).futureValue
      newComplaint.payload.commentsList should have size complaint.payload.commentsList.size + 1
      val commentIdToChange = newComplaint.payload.commentsList.last.id
      val newComplaint2 = dao.updatePayload(complaint.id, UpdateComment(commentIdToChange, "test")).futureValue
      newComplaint2.payload.commentsList should have size complaint.payload.commentsList.size + 1
      newComplaint2.payload.commentsList.last.text shouldBe "test"
    }
    "remove comment" in {
      val complaint = insert(ComplaintRowGen.next)
      val newCommentPatch = AddCommentPayloadPatchGen.next
      val newComplaint = dao.updatePayload(complaint.id, newCommentPatch).futureValue
      newComplaint.payload.commentsList should have size complaint.payload.commentsList.size + 1
      val commentIdToChange = newComplaint.payload.commentsList.last.id
      val newComplaint2 = dao.updatePayload(complaint.id, DeleteComment(commentIdToChange)).futureValue
      (newComplaint2.payload.commentsList should contain)
        .theSameElementsInOrderAs(complaint.payload.commentsList)
    }
    "update cause" in {
      val complaint = insert(ComplaintRowGen.next)
      val newCauseUpdateRequest = ComplaintCauseUpdateRequestGen.next
      val newComplaint = dao.updatePayload(complaint.id, UpdateCause(newCauseUpdateRequest)).futureValue
      newCauseUpdateRequest.causeDescOpt.map(_ shouldBe newComplaint.payload.complaintCause.causeDescription)
      newCauseUpdateRequest.causeTypeOpt.map(_ shouldBe newComplaint.payload.complaintCause.causeType)
    }
    "update call info" in {
      val complaint = insert(ComplaintRowGen.next)
      val newCallInfoUpdateRequest = ComplaintCallInfoUpdateRequestGen.next
      val newComplaint = dao.updatePayload(complaint.id, UpdateCallInfo(newCallInfoUpdateRequest)).futureValue
      newCallInfoUpdateRequest.callSourceOpt
        .map(_ shouldBe newComplaint.payload.complaintCallInfo.callerSource)
      newCallInfoUpdateRequest.callTimeOpt.map(_ shouldBe newComplaint.payload.complaintCallInfo.callTime)
    }
    "fail to insert the same complaint" in {
      val son = SharedOperatorNumberGen.next
      val row = ComplaintRowGen.next.copy(operatorNumber = son.number)
      numberDao.upsert(son).futureValue
      dao.insert(row).futureValue
      dao.insert(row).failed.futureValue shouldBe a[ComplaintDuplicate]
      dao.list(Range.Full).futureValue should have size 1
    }
    "list complaints with slice" in {
      val complaint1 = insert(ComplaintRowGen.next)
      val complaint2 = insert(ComplaintRowGen.suchThat(_.createTime.isAfter(complaint1.createTime)).next)

      val pageResult1 = dao.list(Page(0, 1)).futureValue
      pageResult1.values should contain theSameElementsAs (Seq(complaint1))

      val pageResult2 = dao.list(Page(1, 1)).futureValue
      pageResult2.values should contain theSameElementsAs (Seq(complaint2))
    }

    "list complaints with order" in {
      val count = 4
      ComplaintRowGen.next(count).foreach(insert)
      val all = dao.list(Page(0, 15)).futureValue
      all should have size count

      val sortedUpdateTimeAsc = all.toList.sortBy(_.updateTime.getMillis)
      dao
        .list(Page(0, 15), Seq(), ComplaintsSortOrder(OrderField.UpdatedTime, OrderDirection.ASC))
        .futureValue
        .values
        .toList shouldBe sortedUpdateTimeAsc

      val sortedUpdateDesc = sortedUpdateTimeAsc.reverse
      dao
        .list(Page(0, 15), Seq(), ComplaintsSortOrder(OrderField.UpdatedTime, OrderDirection.DESC))
        .futureValue
        .values
        .toList shouldBe sortedUpdateDesc

      val sortedCreatedTimeAsc = all.toList.sortBy(_.createTime.getMillis)
      dao
        .list(Page(0, 15), Seq(), ComplaintsSortOrder(OrderField.CreatedTime, OrderDirection.ASC))
        .futureValue
        .values
        .toList shouldBe sortedCreatedTimeAsc

      val sortedCreatedTimeDesc = sortedCreatedTimeAsc.reverse
      dao
        .list(Page(0, 15), Seq(), ComplaintsSortOrder(OrderField.CreatedTime, OrderDirection.DESC))
        .futureValue
        .values
        .toList shouldBe sortedCreatedTimeDesc
    }

    "list complaints with filter" in {
      val count = 15
      ComplaintRowGen.next(count).foreach(insert)
      val all = dao.list(Page(0, 15)).futureValue
      all should have size count

      val allNew = dao.list(Page(0, 15)).futureValue
      allNew should have size count

      val domain = DomainGen.next
      val domainFiltered = dao.list(Page(0, 15), Seq(ComplaintsFilter(domain = Some(domain)))).futureValue
      allNew.filter(_.domain == domain) should contain theSameElementsAs domainFiltered

      val status = ComplaintStatusGen.next
      val statusFiltered = dao.list(Page(0, 15), Seq(ComplaintsFilter(status = Some(status)))).futureValue
      allNew.filter(_.status == status) should contain theSameElementsAs statusFiltered

      val tnFiltered =
        dao
          .list(Page(0, 15), Seq(ComplaintsFilter(targetNumber = Some(allNew.values.head.targetNumber))))
          .futureValue
      allNew.filter(_.targetNumber == allNew.values.head.targetNumber) should contain theSameElementsAs tnFiltered

      val opnFiltered = dao
        .list(Page(0, 15), Seq(ComplaintsFilter(operatorNumber = Some(allNew.values.head.operatorNumber))))
        .futureValue
      allNew.filter(_.operatorNumber == allNew.values.head.operatorNumber) should contain theSameElementsAs opnFiltered

      val authorFiltered =
        dao.list(Page(0, 15), Seq(ComplaintsFilter(author = Some(allNew.values.head.author)))).futureValue
      allNew.filter(_.author == allNew.values.head.author) should contain theSameElementsAs authorFiltered

      val operator = OperatorGen.next
      val operatorFiltered =
        dao.list(Page(0, 15), Seq(ComplaintsFilter(operator = Some(operator)))).futureValue
      allNew.filter(_.operator == operator) should contain theSameElementsAs operatorFiltered

      val cct = ComplaintCauseTypeGen.next
      val cctFiltered =
        dao.list(Page(0, 15), Seq(ComplaintsFilter(complaintCauseType = Some(cct)))).futureValue
      allNew.filter(_.payload.complaintCause.causeType == cct) should contain theSameElementsAs cctFiltered

      val statusFilteredOrdered = dao
        .list(
          Page(0, 15),
          Seq(ComplaintsFilter(status = Some(status))),
          ComplaintsSortOrder(OrderField.CreatedTime, OrderDirection.ASC)
        )
        .futureValue
      (allNew
        .filter(_.status == status)
        .toList
        .sortBy(_.createTime.getMillis) should contain)
        .theSameElementsInOrderAs(statusFilteredOrdered)

      val statusAndAuthorFilteredOrdered = dao
        .list(
          Page(0, 15),
          Seq(ComplaintsFilter(author = Some(allNew.values.head.author), status = Some(allNew.values.head.status))),
          ComplaintsSortOrder(OrderField.CreatedTime, OrderDirection.ASC)
        )
        .futureValue
      (allNew
        .filter(_.status == allNew.values.head.status)
        .filter(_.author == allNew.values.head.author)
        .toList
        .sortBy(_.createTime.getMillis) should contain)
        .theSameElementsInOrderAs(statusAndAuthorFilteredOrdered)

      val statusOrAuthorFilteredOrdered = dao
        .list(
          Page(0, 15),
          Seq(
            ComplaintsFilter(author = Some(allNew.values.head.author)),
            ComplaintsFilter(status = Some(allNew.values.head.status))
          ),
          ComplaintsSortOrder(OrderField.CreatedTime, OrderDirection.ASC)
        )
        .futureValue

      val withStatus = allNew
        .filter(_.status == allNew.values.head.status)
        .toList

      val withAuthor = allNew
        .filter(_.author == allNew.values.head.author)
        .filterNot(withStatus.contains)
        .toList
      (withAuthor ++ withStatus).sortBy(_.createTime.getMillis) should contain
        .theSameElementsInOrderAs(statusOrAuthorFilteredOrdered)
    }
  }
}
