package ru.yandex.realty.rent.backend.manager

import com.google.protobuf.Empty
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.WordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.errors.{ConflictApiException, ForbiddenApiException, InvalidParamsApiException}
import ru.yandex.realty.rent.backend.converter.showing.FlatShowingConverter.ShowingWithAdditionalInfo
import ru.yandex.realty.rent.dao.RentSpecBase
import ru.yandex.realty.rent.model.enums.GroupStatus.{Approved, Consideration, Created, Declined}
import ru.yandex.realty.rent.model.enums.StatusAction.StatusAction
import ru.yandex.realty.rent.model.enums.{FlatShowingStatus, GroupStatus, OwnerRequestStatus, Role}
import ru.yandex.realty.rent.model.{
  Flat,
  FlatShowing,
  OwnerRequest,
  RoommateCandidate,
  RoommateCandidateUtils,
  StaffUser,
  StatusAuditLog,
  User,
  UserFlat,
  UserShowing
}
import ru.yandex.realty.rent.proto.api.common.TenantQuestionnaireModerationStatusNamespace.TenantQuestionnaireModerationStatus
import ru.yandex.realty.rent.proto.api.flats.AssignedGroupUsers
import ru.yandex.realty.rent.proto.api.internal.roommates.{
  InternalConfirmCandidateLink,
  InternalRoommateLink,
  InternalUpdateRoommateGroup,
  InternalUpdateRoommateGroupStatus
}
import ru.yandex.realty.rent.proto.api.roommates.RoommateValidationErrorNamespace.RoommateValidationError
import ru.yandex.realty.rent.proto.api.roommates.{UpdateRoommateGroupRequest, UpdateRoommateGroupStatusRequest}
import ru.yandex.realty.rent.proto.api.showing.ChangeRoommateGroupStatusRequest
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheck.FsspDebtCheck
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckResolutionNamespace.NaturalPersonCheckResolution
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.NaturalPersonCheckStatusNamespace.NaturalPersonCheckStatus
import ru.yandex.realty.rent.proto.model.user.natural_person_checks.{NaturalPersonCheck, NaturalPersonChecks}
import ru.yandex.realty.rent.proto.model.user.{PersonalDataTransferAgreement, TenantQuestionnaire}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.gen.IdGenerator
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class RoommatesManagerSpec extends WordSpec with RentSpecBase with MockFactory {

  "RoommatesManager" should {
    "getOrCreateRoommateLink for user without link" in new Wiring with Data {
      val userWithoutLink: User = userGen().next.copy(roommateLinkId = None)
      (mockUserDao
        .findByUid(_: Long)(_: Traced))
        .expects(userWithoutLink.uid, *)
        .once()
        .returning(Future.successful(userWithoutLink))
      (mockUserDao
        .findByRoommateLinkId(_: String)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(None))
      (mockUserDao
        .update(_: Long)(_: User => User)(_: Traced))
        .expects(userWithoutLink.uid, *, *)
        .once()
        .returning(Future.successful(userWithoutLink))
      val response: InternalRoommateLink.Response =
        roommatesManager.getOrCreateRoommateLink(userWithoutLink.uid).futureValue
      response.getResultCase shouldBe InternalRoommateLink.Response.ResultCase.SUCCESS
    }

    "getOrCreateRoommateLink for user with expired link" in new Wiring with Data {
      val userWithExpiredLink: User = userGen().next.copy(
        roommateLinkId = Some(IdGenerator.generateLongNumber(19)),
        roommateLinkExpirationTime = Some(DateTimeUtil.now().minusHours(1))
      )
      (mockUserDao
        .findByUid(_: Long)(_: Traced))
        .expects(userWithExpiredLink.uid, *)
        .once()
        .returning(Future.successful(userWithExpiredLink))
      (mockUserDao
        .findByRoommateLinkId(_: String)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(None))
      (mockUserDao
        .update(_: Long)(_: User => User)(_: Traced))
        .expects(userWithExpiredLink.uid, *, *)
        .once()
        .returning(Future.successful(userWithExpiredLink))
      val response: InternalRoommateLink.Response =
        roommatesManager.getOrCreateRoommateLink(userWithExpiredLink.uid).futureValue
      response.getResultCase shouldBe InternalRoommateLink.Response.ResultCase.SUCCESS
    }

    "getOrCreateRoommateLink for user with valid link" in new Wiring with Data {
      val userWithLink: User = userGen().next.copy(
        roommateLinkId = Some(IdGenerator.generateLongNumber(19)),
        roommateLinkExpirationTime = Some(DateTimeUtil.now().plusHours(1))
      )
      (mockUserDao
        .findByUid(_: Long)(_: Traced))
        .expects(userWithLink.uid, *)
        .once()
        .returning(Future.successful(userWithLink))
      val response: InternalRoommateLink.Response =
        roommatesManager.getOrCreateRoommateLink(userWithLink.uid).futureValue
      response.getResultCase shouldBe InternalRoommateLink.Response.ResultCase.SUCCESS
    }

    "confirmRoommateCandidate without existing candidate with creating" in new Wiring with Data {
      val roommateUser: User = userGen().next
      val roommateLinkId: String = IdGenerator.generateLongNumber(19)
      val user: User = userGen().next
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(roommateUser.uid, *, *)
        .once()
        .returning(Future.successful(Some(roommateUser)))
      (mockUserDao
        .findByRoommateLinkId(_: String)(_: Traced))
        .expects(roommateLinkId, *)
        .once()
        .returning(Future.successful(Some(user)))
      (mockRoommateCandidateDao
        .find(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .twice()
        .returning(Future.successful(None))
      (mockRoommateCandidateDao
        .countLinks(_: String)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(0))
      (mockRoommateCandidateDao
        .findByPhone(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .once()
        .returning(Future.successful(None))
      (mockRoommateCandidateDao
        .create(_: Seq[RoommateCandidate])(_: Traced))
        .expects(where { (candidates: Seq[RoommateCandidate], _) =>
          candidates.head.userId == user.userId &&
          candidates.head.roommateUserId.contains(roommateUser.userId)
        })
        .once()
        .returning(Future.unit)
      val response: InternalConfirmCandidateLink.Response =
        roommatesManager.confirmRoommateCandidate(roommateUser.uid, roommateLinkId).futureValue
      response.getResultCase shouldBe InternalConfirmCandidateLink.Response.ResultCase.SUCCESS
    }

    "confirmRoommateCandidate with existing candidate" in new Wiring with Data {
      val roommateUser: User = userGen().next
      val roommateLinkId: String = IdGenerator.generateLongNumber(19)
      val user: User = userGen().next
      val candidate: RoommateCandidate =
        RoommateCandidate(RoommateCandidateUtils.generateCandidateId(), user.userId, Some(roommateUser.userId), None)
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(roommateUser.uid, *, *)
        .once()
        .returning(Future.successful(Some(roommateUser)))
      (mockUserDao
        .findByRoommateLinkId(_: String)(_: Traced))
        .expects(roommateLinkId, *)
        .once()
        .returning(Future.successful(Some(user)))
      (mockRoommateCandidateDao
        .find(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .once()
        .returning(Future.successful(Some(candidate)))
      val response: InternalConfirmCandidateLink.Response =
        roommatesManager.confirmRoommateCandidate(roommateUser.uid, roommateLinkId).futureValue
      response.getResultCase shouldBe InternalConfirmCandidateLink.Response.ResultCase.SUCCESS
    }

    "confirmRoommateCandidate with multiple links" in new Wiring with Data {
      val roommateUser: User = userGen().next
      val roommateLinkId: String = IdGenerator.generateLongNumber(19)
      val user: User = userGen().next
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(roommateUser.uid, *, *)
        .once()
        .returning(Future.successful(Some(roommateUser)))
      (mockUserDao
        .findByRoommateLinkId(_: String)(_: Traced))
        .expects(roommateLinkId, *)
        .once()
        .returning(Future.successful(Some(user)))
      (mockRoommateCandidateDao
        .find(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .twice()
        .returning(Future.successful(None))
      (mockRoommateCandidateDao
        .countLinks(_: String)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(30))
      (mockRoommateCandidateDao
        .findByPhone(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .once()
        .returning(Future.successful(None))
      interceptCause[ForbiddenApiException] {
        roommatesManager.confirmRoommateCandidate(roommateUser.uid, roommateLinkId).futureValue
      }
    }

    "confirmRoommateCandidate the same user" in new Wiring with Data {
      val roommateLinkId: String = IdGenerator.generateLongNumber(19)
      val user: User = userGen().next
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(user.uid, *, *)
        .once()
        .returning(Future.successful(Some(user)))
      (mockUserDao
        .findByRoommateLinkId(_: String)(_: Traced))
        .expects(roommateLinkId, *)
        .once()
        .returning(Future.successful(Some(user)))
      (mockRoommateCandidateDao
        .findByPhone(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .once()
        .returning(Future.successful(None))
      (mockRoommateCandidateDao
        .find(_: String, _: String)(_: Traced))
        .expects(*, *, *)
        .twice()
        .returning(Future.successful(None))
      interceptCause[InvalidParamsApiException] {
        roommatesManager.confirmRoommateCandidate(user.uid, roommateLinkId).futureValue
      }
    }

    "getUserGroups for owner" in new Wiring with Data {
      val ownerRequest: OwnerRequest = getUnrentedOwnerRequest().next
      val flat: Flat = flatGen(recursive = false).next.copy(ownerRequests = Seq(ownerRequest))
      val activeFlatShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = true,
          groupStatus = Consideration,
          status = FlatShowingStatus.QuestionnaireReceived
        )
      val notActiveFlatShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = false,
          status = FlatShowingStatus.Settled
        )
      val createdFlatShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = true,
          groupStatus = Created,
          status = FlatShowingStatus.NewShowing
        )
      val user: User = getApprovedUser()
      (mockFlatShowingDao
        .findByOwnerRequests(_: Set[String])(_: Traced))
        .expects(Set(ownerRequest.ownerRequestId), *)
        .once()
        .returning(Future.successful(Seq(activeFlatShowing, notActiveFlatShowing, createdFlatShowing)))
      (mockUserShowingDao
        .findByShowings(_: Seq[String])(_: Traced))
        .expects(Seq(activeFlatShowing.showingId), *)
        .once()
        .returning(Future.successful(Seq(UserShowing(activeFlatShowing.showingId, user.userId, isMain = true))))
      (mockUserDao
        .findByUserIds(_: Set[String])(_: Traced))
        .expects(Set(user.userId), *)
        .once()
        .returning(Future.successful(Seq(user)))
      val result: Map[String, Seq[AssignedGroupUsers]] = roommatesManager
        .getUserGroups(user.userId, Seq((flat, Some(Role.Owner))))
        .futureValue
      result.keySet == Set(flat.flatId)
      result.contains(flat.flatId) shouldBe true
      result(flat.flatId).size shouldBe 1
      result(flat.flatId).head.getAssignedUserList.size() shouldBe 1
      result(flat.flatId).head.getShowingId shouldBe activeFlatShowing.showingId
      result(flat.flatId).head.getAssignedUserList.asScala.head.getUserId shouldBe user.userId
    }

    "getUserGroups for tenant candidate" in new Wiring with Data {
      val ownerRequest: OwnerRequest = getUnrentedOwnerRequest().next
      val flat: Flat = flatGen(recursive = false).next.copy(ownerRequests = Seq(ownerRequest))
      val tenantActiveShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = true,
          groupStatus = Declined
        )
      val activeFlatShowing: FlatShowing =
        flatShowingGen.next.copy(ownerRequestId = ownerRequest.ownerRequestId, isActive = true, groupStatus = Approved)
      val notActiveFlatShowing: FlatShowing =
        flatShowingGen.next.copy(ownerRequestId = ownerRequest.ownerRequestId, isActive = false)
      val tenant: User = getApprovedUser()
      val user: User = userGen().next
      (mockFlatShowingDao
        .findByOwnerRequests(_: Set[String])(_: Traced))
        .expects(Set(ownerRequest.ownerRequestId), *)
        .once()
        .returning(Future.successful(Seq(tenantActiveShowing, activeFlatShowing, notActiveFlatShowing)))
      (mockUserShowingDao
        .findByShowings(_: Seq[String])(_: Traced))
        .expects(Seq(tenantActiveShowing.showingId, activeFlatShowing.showingId), *)
        .once()
        .returning(
          Future.successful(
            Seq(
              UserShowing(activeFlatShowing.showingId, user.userId, isMain = true),
              UserShowing(tenantActiveShowing.showingId, tenant.userId, isMain = true)
            )
          )
        )
      (mockUserDao
        .findByUserIds(_: Set[String])(_: Traced))
        .expects(Set(user.userId, tenant.userId), *)
        .once()
        .returning(Future.successful(Seq(user, tenant)))
      val result: Map[String, Seq[AssignedGroupUsers]] = roommatesManager
        .getUserGroups(tenant.userId, Seq((flat, Some(Role.TenantCandidate))))
        .futureValue
      result.keySet == Set(flat.flatId)
      result.contains(flat.flatId) shouldBe true
      result(flat.flatId).size shouldBe 1
      result(flat.flatId).head.getAssignedUserList.size() shouldBe 1
      result(flat.flatId).head.getShowingId shouldBe tenantActiveShowing.showingId
      result(flat.flatId).head.getAssignedUserList.asScala.head.getUserId shouldBe tenant.userId
    }

    "updateRoommateGroup in invalid status" in new Wiring with Data {
      val user: User = userGen().next
      val ownerRequest: OwnerRequest = ownerRequestGen.next
      val flatShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = true,
          groupStatus = GroupStatus.Declined
        )
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(user.uid, *, *)
        .once()
        .returning(Future.successful(Some(user)))
      (ownerRequestDao
        .findCurrentUnrentedByFlatId(_: String)(_: Traced))
        .expects(ownerRequest.flatId, *)
        .once()
        .returning(Future.successful(Some(ownerRequest)))
      (mockFlatShowingDao
        .findActualByOwnerRequestAndUser(_: String, _: String)(_: Traced))
        .expects(ownerRequest.ownerRequestId, user.userId, *)
        .once()
        .returning(Future.successful(Some(flatShowing)))
      interceptCause[ConflictApiException] {
        roommatesManager
          .updateRoommateGroup(user.uid, ownerRequest.flatId, UpdateRoommateGroupRequest.getDefaultInstance)
          .futureValue
      }
    }

    "updateRoommateGroup by regular user in group " in new Wiring with Data {
      val user: User = userGen().next
      val ownerRequest: OwnerRequest = ownerRequestGen.next
      val flatShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = true,
          groupStatus = GroupStatus.Created
        )
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(user.uid, *, *)
        .once()
        .returning(Future.successful(Some(user)))
      (ownerRequestDao
        .findCurrentUnrentedByFlatId(_: String)(_: Traced))
        .expects(ownerRequest.flatId, *)
        .once()
        .returning(Future.successful(Some(ownerRequest)))
      (mockFlatShowingDao
        .findActualByOwnerRequestAndUser(_: String, _: String)(_: Traced))
        .expects(ownerRequest.ownerRequestId, user.userId, *)
        .once()
        .returning(Future.successful(Some(flatShowing)))
      (mockUserShowingDao
        .findByShowing(_: String)(_: Traced))
        .expects(flatShowing.showingId, *)
        .once()
        .returning(Future.successful(Seq(UserShowing(flatShowing.showingId, user.userId, isMain = false))))
      interceptCause[ForbiddenApiException] {
        roommatesManager
          .updateRoommateGroup(user.uid, ownerRequest.flatId, UpdateRoommateGroupRequest.getDefaultInstance)
          .futureValue
      }
    }

    "updateRoommateGroup validation" in new Wiring with Data {
      val user: User = userGen().next
      val mainUser: User = userGen().next.copy(
        data = userGen().next.data.toBuilder
          .setTenantQuestionnaireModerationStatus(TenantQuestionnaireModerationStatus.INVALID)
          .build()
      )
      val otherUser: User = userGen().next
      val ownerRequest: OwnerRequest = ownerRequestGen.next
      val otherFlatShowing: FlatShowing = flatShowingGen.next.copy(
        ownerRequestId = ownerRequest.ownerRequestId,
        isActive = true,
        groupStatus = GroupStatus.Consideration
      )
      val flatShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = true,
          groupStatus = GroupStatus.Created
        )
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(user.uid, *, *)
        .once()
        .returning(Future.successful(Some(user)))
      (ownerRequestDao
        .findCurrentUnrentedByFlatId(_: String)(_: Traced))
        .expects(ownerRequest.flatId, *)
        .once()
        .returning(Future.successful(Some(ownerRequest)))
      (mockFlatShowingDao
        .findActualByOwnerRequestAndUser(_: String, _: String)(_: Traced))
        .expects(ownerRequest.ownerRequestId, user.userId, *)
        .once()
        .returning(Future.successful(Some(flatShowing)))
      (mockUserShowingDao
        .findByShowing(_: String)(_: Traced))
        .expects(flatShowing.showingId, *)
        .once()
        .returning(Future.successful(Seq(UserShowing(flatShowing.showingId, user.userId, isMain = true))))
      (mockFlatShowingDao
        .findActiveByOwnerRequest(_: String, _: Boolean)(_: Traced))
        .expects(ownerRequest.ownerRequestId, *, *)
        .once()
        .returning(Future.successful(Seq(otherFlatShowing)))
      (mockUserShowingDao
        .findByShowings(_: Seq[String])(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(Seq(UserShowing(readableString.next, otherUser.userId, isMain = false))))
      (mockUserShowingDao
        .findActiveByUsers(_: Seq[String])(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(Seq(UserShowing(readableString.next, otherUser.userId, isMain = false))))
      (mockUserDao
        .findByUserIds(_: Set[String])(_: Traced))
        .expects(Set(otherUser.userId), *)
        .once()
        .returning(Future.successful(Set(otherUser)))
      (mockUserDao
        .findByUserId(_: String)(_: Traced))
        .expects(mainUser.userId, *)
        .once()
        .returning(Future.successful(mainUser))
      (mockRoommateCandidateDao
        .findAllLinkedUserIds(_: String)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(Set.empty))

      val request: UpdateRoommateGroupRequest =
        UpdateRoommateGroupRequest.newBuilder().setMainUserId(mainUser.userId).addUserIds(otherUser.userId).build()
      val result: InternalUpdateRoommateGroup.Response =
        roommatesManager.updateRoommateGroup(user.uid, ownerRequest.flatId, request).futureValue
      result.getResultCase shouldBe InternalUpdateRoommateGroup.Response.ResultCase.ERROR
      result.getError.getData.getValidationErrorsCount shouldBe 7
    }

    "updateRoommateGroup validation with ignore some errors" in new Wiring with Data {
      val user: User = userGen().next
      val mainUser: User = userGen().next.copy(
        data = userGen().next.data.toBuilder
          .setTenantQuestionnaireModerationStatus(TenantQuestionnaireModerationStatus.INVALID)
          .build()
      )
      val otherUser: User = userGen().next
      val ownerRequest: OwnerRequest = ownerRequestGen.next
      val otherFlatShowing: FlatShowing = flatShowingGen.next.copy(
        ownerRequestId = ownerRequest.ownerRequestId,
        isActive = true,
        groupStatus = GroupStatus.Consideration
      )
      val flatShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = true,
          groupStatus = GroupStatus.Created
        )
      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(user.uid, *, *)
        .once()
        .returning(Future.successful(Some(user)))
      (ownerRequestDao
        .findCurrentUnrentedByFlatId(_: String)(_: Traced))
        .expects(ownerRequest.flatId, *)
        .once()
        .returning(Future.successful(Some(ownerRequest)))
      (mockFlatShowingDao
        .findActualByOwnerRequestAndUser(_: String, _: String)(_: Traced))
        .expects(ownerRequest.ownerRequestId, user.userId, *)
        .once()
        .returning(Future.successful(Some(flatShowing)))
      (mockUserShowingDao
        .findByShowing(_: String)(_: Traced))
        .expects(flatShowing.showingId, *)
        .once()
        .returning(Future.successful(Seq(UserShowing(flatShowing.showingId, user.userId, isMain = true))))
      (mockFlatShowingDao
        .findActiveByOwnerRequest(_: String, _: Boolean)(_: Traced))
        .expects(ownerRequest.ownerRequestId, *, *)
        .once()
        .returning(Future.successful(Seq(otherFlatShowing)))
      (mockUserShowingDao
        .findByShowings(_: Seq[String])(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(Seq(UserShowing(readableString.next, otherUser.userId, isMain = false))))
      (mockUserShowingDao
        .findActiveByUsers(_: Seq[String])(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(Seq(UserShowing(readableString.next, otherUser.userId, isMain = false))))
      (mockUserDao
        .findByUserIds(_: Set[String])(_: Traced))
        .expects(Set(otherUser.userId), *)
        .once()
        .returning(Future.successful(Set(otherUser)))
      (mockUserDao
        .findByUserId(_: String)(_: Traced))
        .expects(mainUser.userId, *)
        .once()
        .returning(Future.successful(mainUser))
      (mockRoommateCandidateDao
        .findAllLinkedUserIds(_: String)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(Set.empty))

      val request: UpdateRoommateGroupRequest =
        UpdateRoommateGroupRequest.newBuilder().setMainUserId(mainUser.userId).addUserIds(otherUser.userId).build()
      val result: InternalUpdateRoommateGroup.Response =
        roommatesManager
          .updateRoommateGroup(
            user.uid,
            ownerRequest.flatId,
            request,
            Seq(RoommateValidationError.NOT_APPROVED_USER, RoommateValidationError.INVALID_CHECKS)
          )
          .futureValue
      result.getResultCase shouldBe InternalUpdateRoommateGroup.Response.ResultCase.ERROR
      result.getError.getData.getValidationErrorsCount shouldBe 3
    }

    "updateRoommateGroupStatus approve" in new Wiring with Data {
      val user: User = userGen().next
      val mainUser: User = userGen().next
      val ownerRequest: OwnerRequest = ownerRequestGen.next
      val flatShowing: FlatShowing =
        flatShowingGen.next.copy(
          ownerRequestId = ownerRequest.ownerRequestId,
          isActive = true,
          groupStatus = GroupStatus.Consideration
        )
      (mockUserFlatDao
        .findByFlatAndUsers(_: String, _: Seq[Long])(_: Traced))
        .expects(ownerRequest.flatId, Seq(user.uid), *)
        .once()
        .returning(Future.successful(Seq(UserFlat(user.uid, ownerRequest.flatId, Role.Owner))))
      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(ownerRequest.flatId, *)
        .once()
        .returning(Future.successful(Some(ownerRequest)))
      (mockFlatShowingDao
        .find(_: String, _: Boolean)(_: Traced))
        .expects(flatShowing.showingId, *, *)
        .twice()
        .returning(Future.successful(Some(flatShowing)))
      (mockUserShowingDao
        .findByShowing(_: String)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(Seq(UserShowing(flatShowing.showingId, mainUser.userId, isMain = true))))
      (mockUserDao
        .findByUserId(_: String)(_: Traced))
        .expects(mainUser.userId, *)
        .once()
        .returning(Future.successful(mainUser))
      (mockFlatShowingDao
        .update(_: String)(_: FlatShowing => FlatShowing)(_: Traced))
        .expects(*, *, *)
        .twice()
        .returning(Future.successful(flatShowing))
      (mockUserDao
        .findByUserIds(_: Set[String])(_: Traced))
        .expects(Set.empty[String], *)
        .once()
        .returning(Future.successful(Seq.empty))
      (mockFlatShowingDao
        .findByOwnerRequests(_: Set[String])(_: Traced))
        .expects(Set(ownerRequest.ownerRequestId), *)
        .once()
        .returning(Future.successful(Seq.empty))
      (statusAuditLogDao
        .create(_: StatusAuditLog)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.unit)
      val request: UpdateRoommateGroupStatusRequest =
        UpdateRoommateGroupStatusRequest
          .newBuilder()
          .setApprove(UpdateRoommateGroupStatusRequest.Approve.newBuilder().addUserIds(mainUser.userId))
          .build()
      val result: InternalUpdateRoommateGroupStatus.Response =
        roommatesManager
          .updateRoommateGroupStatus(user.uid, ownerRequest.flatId, flatShowing.showingId, request)
          .futureValue
      result.getResultCase shouldBe InternalUpdateRoommateGroupStatus.Response.ResultCase.SUCCESS
    }
  }

  "updateRoommateGroupStatus decline" in new Wiring with Data {
    val user: User = userGen().next
    val mainUser: User = userGen().next
    val ownerRequest: OwnerRequest = ownerRequestGen.next

    val flatShowing: FlatShowing =
      flatShowingGen.next.copy(
        ownerRequestId = ownerRequest.ownerRequestId,
        isActive = true,
        groupStatus = GroupStatus.Consideration
      )

    (mockUserFlatDao
      .findByFlatAndUsers(_: String, _: Seq[Long])(_: Traced))
      .expects(ownerRequest.flatId, Seq(user.uid), *)
      .once()
      .returning(Future.successful(Seq(UserFlat(user.uid, ownerRequest.flatId, Role.Owner))))
    (ownerRequestDao
      .findLastByFlatId(_: String)(_: Traced))
      .expects(ownerRequest.flatId, *)
      .once()
      .returning(Future.successful(Some(ownerRequest)))
    (mockFlatShowingDao
      .find(_: String, _: Boolean)(_: Traced))
      .expects(flatShowing.showingId, *, *)
      .twice()
      .returning(Future.successful(Some(flatShowing)))
    (mockUserShowingDao
      .findByShowing(_: String)(_: Traced))
      .expects(*, *)
      .once()
      .returning(Future.successful(Seq(UserShowing(flatShowing.showingId, mainUser.userId, isMain = true))))
    (mockUserDao
      .findByUserIdOpt(_: String)(_: Traced))
      .expects(mainUser.userId, *)
      .once()
      .returning(Future.successful(Some(mainUser)))
    (mockFlatShowingDao
      .update(_: String)(_: FlatShowing => FlatShowing)(_: Traced))
      .expects(*, *, *)
      .twice()
      .returning(Future.successful(flatShowing))
    (mockUserDao
      .findByUserIds(_: Set[String])(_: Traced))
      .expects(Set.empty[String], *)
      .once()
      .returning(Future.successful(Seq.empty))
    (statusAuditLogDao
      .create(_: StatusAuditLog)(_: Traced))
      .expects(*, *)
      .once()
      .returning(Future.unit)

    val request: UpdateRoommateGroupStatusRequest =
      UpdateRoommateGroupStatusRequest
        .newBuilder()
        .setDecline(UpdateRoommateGroupStatusRequest.Decline.newBuilder().addUserIds(mainUser.userId))
        .build()

    val result: InternalUpdateRoommateGroupStatus.Response =
      roommatesManager
        .updateRoommateGroupStatus(user.uid, ownerRequest.flatId, flatShowing.showingId, request)
        .futureValue
    result.getResultCase shouldBe InternalUpdateRoommateGroupStatus.Response.ResultCase.SUCCESS
  }

  "change roommate group status" in new Wiring {
    val showingId = "showingId"
    val request = ChangeRoommateGroupStatusRequest.newBuilder().setToConsideration(Empty.getDefaultInstance).build()
    val showing = flatShowingGen.next.copy(groupStatus = GroupStatus.Consideration)
    val ownerRequest = ownerRequestGen.next
    val flat = flatGen().next

    (mockFlatShowingDao
      .updateAndLog(_: String, _: StatusAction, _: StaffUser, _: StaffUser)(_: FlatShowing => FlatShowing)(_: Traced))
      .expects(showingId, *, *, *, *, *)
      .returning(Future.successful(showing))
      .once()

    (mockShowingEnricher
      .enrichWithAdditionalInfo(_: FlatShowing)(_: Traced))
      .expects(showing, *)
      .returning(Future.successful(ShowingWithAdditionalInfo(showing, ownerRequest, flat, None, None, None, None, Nil)))
      .twice()

    roommatesManager.changeRoommateGroupStatus(showingId, request).futureValue
  }

  private def getUnrentedOwnerRequest(): Gen[OwnerRequest] = {
    for {
      status <- Gen.oneOf(OwnerRequestStatus.UnrentedStatuses.toSeq)
      ownerRequest <- ownerRequestGen
    } yield ownerRequest.copy(status = status)
  }

  private def getApprovedUser(): User = {
    val user = userGen().next
    val approvedUserData = user.data.toBuilder
      .setApprovedTenantQuestionnaire(TenantQuestionnaire.getDefaultInstance)
      .setPersonalDataTransferAgreement(PersonalDataTransferAgreement.getDefaultInstance)
      .setNaturalPersonChecks {
        NaturalPersonChecks
          .newBuilder()
          .setStatus(NaturalPersonCheckStatus.READY)
          .setResolution(NaturalPersonCheckResolution.VALID)
          .addChecks {
            NaturalPersonCheck
              .newBuilder()
              .setFsspDebtCheck(FsspDebtCheck.getDefaultInstance)
              .setStatus(NaturalPersonCheckStatus.READY)
          }
      }
      .build()
    user.copy(data = approvedUserData)
  }
}
