package ru.yandex.realty.rent.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.rent.RentRoommatesServiceClient
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.rent.dao.{CleanSchemaBeforeEach, RentSpecBase}
import ru.yandex.realty.rent.model.converters.FlatShowingEnumConverter
import ru.yandex.realty.rent.model.{FlatShowing, RoommateCandidate, User, UserShowing}
import ru.yandex.realty.rent.model.enums.{FlatShowingStatus, FlatShowingType, GroupStatus}
import ru.yandex.realty.rent.proto.api.internal.roommates.InternalUpdateRoommateGroup
import ru.yandex.realty.rent.proto.api.showing.TenantStructureInfo
import ru.yandex.realty.rent.proto.api.roommates.RoommateValidationErrorNamespace.RoommateValidationError
import ru.yandex.realty.rent.proto.api.roommates.UpdateRoommateGroupRequest
import ru.yandex.realty.rent.proto.model.diffevent.{
  DiffEvent,
  FlatShowingDiffEvent,
  FlatShowingProtoView,
  UserDiffEvent,
  UserProtoView
}
import ru.yandex.realty.rent.proto.model.flat.showing.FlatShowingData
import ru.yandex.realty.rent.proto.model.flat.showing.FlatShowingData.InitialTenantInfo
import ru.yandex.realty.rent.proto.model.user.{TenantQuestionnaire, UserData}
import ru.yandex.realty.rent.service.impl.DiffEventRoommateProcessorImpl
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class DiffEventRoommateProcessorSpec extends AsyncSpecBase with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace = Traced.empty

  "DiffEventRoommateProcessor.process" should {

    "update user, 1 adult" in new Wiring {
      val initialUser = {
        val u = userGen().next
        val data = UserData.newBuilder.setTenantQuestionnaire(TenantQuestionnaire.newBuilder.setNumberOfBabies(5))
        u.copy(data = data.build())
      }
      val initialFlat = flatGen().next
      val ownerRequest = ownerRequestGen.next.copy(flatId = initialFlat.id)
      val flatShowing = flatShowingGen.next.copy(
        ownerRequestId = ownerRequest.ownerRequestId,
        isActive = true,
        groupStatus = GroupStatus.Created,
        status = FlatShowingStatus.NewShowing,
        showingType = Some(FlatShowingType.WithoutShowing),
        data = FlatShowingData.newBuilder
          .setInitialTenantInfo(
            InitialTenantInfo.newBuilder.setTenantStructureSource(
              TenantStructureInfo.newBuilder.setNumberOfAdults(1)
            )
          )
          .build()
      )
      val userShowing = UserShowing(userId = initialUser.userId, showingId = flatShowing.showingId, isMain = true)

      flatDao.create(initialFlat).futureValue
      ownerRequestDao.create(Seq(ownerRequest)).futureValue
      userDao.create(initialUser).futureValue
      flatShowingDao.insert(Seq(flatShowing)).futureValue
      userShowingDao.insert(Seq(userShowing)).futureValue

      (mockRoommatesClient
        .updateFlatRoommateGroup(
          _: PassportUser,
          _: String,
          _: UpdateRoommateGroupRequest,
          _: Seq[RoommateValidationError]
        )(_: Traced))
        .expects(PassportUser(initialUser.uid), initialFlat.flatId, *, *, *)
        .once()
        .returning(Future.successful(InternalUpdateRoommateGroup.Response.getDefaultInstance))

      val userDiffEvent = createUserDiffEvent(initialUser)
      processor.process(userDiffEvent).futureValue
    }

    "update showing, 2 adult" in new Wiring {
      val initialFlat = flatGen().next
      val ownerRequest = ownerRequestGen.next.copy(flatId = initialFlat.id)
      val flatShowing = flatShowingGen.next.copy(
        ownerRequestId = ownerRequest.ownerRequestId,
        isActive = true,
        groupStatus = GroupStatus.Created,
        status = FlatShowingStatus.ConfirmedByTenant,
        showingType = Some(FlatShowingType.Offline),
        data = FlatShowingData.newBuilder
          .setInitialTenantInfo(
            InitialTenantInfo.newBuilder.setTenantStructureSource(
              TenantStructureInfo.newBuilder.setNumberOfAdults(2)
            )
          )
          .build()
      )
      val mainUser = {
        val user = userGen().next
        val data = UserData.newBuilder.setTenantQuestionnaire(TenantQuestionnaire.getDefaultInstance)
        user.copy(data = data.build())
      }
      val secondUser = {
        val user = userGen().next
        val data = UserData.newBuilder.setTenantQuestionnaire(TenantQuestionnaire.getDefaultInstance)
        user.copy(data = data.build())
      }
      val userShowing = UserShowing(userId = mainUser.userId, showingId = flatShowing.showingId, isMain = true)
      val roommateCandidate =
        RoommateCandidate("kek-cheburek", mainUser.userId, Some(secondUser.userId), Some("+79995552233"))

      flatDao.create(initialFlat).futureValue
      ownerRequestDao.create(Seq(ownerRequest)).futureValue
      userDao.create(mainUser).futureValue
      userDao.create(secondUser).futureValue
      flatShowingDao.insert(Seq(flatShowing)).futureValue
      userShowingDao.insert(Seq(userShowing)).futureValue
      roommateCandidateDao.create(Seq(roommateCandidate))

      val expectedGroupRequest =
        UpdateRoommateGroupRequest.newBuilder.setMainUserId(mainUser.userId).addUserIds(secondUser.userId).build()
      (mockRoommatesClient
        .updateFlatRoommateGroup(
          _: PassportUser,
          _: String,
          _: UpdateRoommateGroupRequest,
          _: Seq[RoommateValidationError]
        )(_: Traced))
        .expects(PassportUser(mainUser.uid), initialFlat.flatId, expectedGroupRequest, *, *)
        .once()
        .returning(Future.successful(InternalUpdateRoommateGroup.Response.getDefaultInstance))

      val flatShowingDiffEvent = createFlatShowingDiffEvent(flatShowing)
      processor.process(flatShowingDiffEvent).futureValue
    }
  }

  private def createUserDiffEvent(
    user: User
  ): DiffEvent = {
    val newUserProto = UserProtoView.newBuilder.setUserId(user.userId).setData(user.data)
    val userEvent = UserDiffEvent.newBuilder.setNew(newUserProto)
    DiffEvent.newBuilder().setUserEvent(userEvent).build()
  }

  private def createFlatShowingDiffEvent(
    flatShowing: FlatShowing
  ): DiffEvent = {
    val newFlatShowingProto = FlatShowingProtoView.newBuilder.setShowingId(flatShowing.showingId)
    newFlatShowingProto.setStatus(FlatShowingEnumConverter.FlatShowingStatus.toApi(flatShowing.status))
    flatShowing.showingType.foreach(
      k => newFlatShowingProto.setShowingType(FlatShowingEnumConverter.FlatShowingType.toApi(k))
    )
    val flatShowingEvent = FlatShowingDiffEvent.newBuilder.setNew(newFlatShowingProto)
    DiffEvent.newBuilder().setFlatShowingEvent(flatShowingEvent).build()
  }

  private trait Wiring {
    val mockRoommatesClient: RentRoommatesServiceClient = mock[RentRoommatesServiceClient]

    val processor = new DiffEventRoommateProcessorImpl(
      mockRoommatesClient,
      flatShowingDao,
      ownerRequestDao,
      roommateCandidateDao,
      userDao,
      userShowingDao
    )
  }
}
