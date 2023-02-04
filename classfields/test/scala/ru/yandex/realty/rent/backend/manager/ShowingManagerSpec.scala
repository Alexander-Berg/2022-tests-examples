package ru.yandex.realty.rent.backend.manager

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.WordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.abc.model.{AbcPerson, AbcShift, DutyPerson}
import ru.yandex.realty.errors.ConflictApiException
import ru.yandex.realty.model.duration.LocalDateInterval
import ru.yandex.realty.rent.backend.manager.{Data => CommonData}
import ru.yandex.realty.rent.dao.RentSpecBase
import ru.yandex.realty.rent.model.enums.FlatShowingStatus.openStatuses
import ru.yandex.realty.rent.model.enums.{FlatShowingStatus, GroupStatus}
import ru.yandex.realty.rent.model.{FlatQuestionnaire, FlatShowing, OwnerRequest, User, UserShowing}
import ru.yandex.realty.rent.proto.api.moderation.{
  CreateFlatShowingsError,
  CreateFlatShowingsErrorNamespace,
  CreateFlatShowingsResponse
}
import ru.yandex.realty.rent.proto.api.showing.FillOutTenantCheckInDateRequest
import ru.yandex.realty.rent.proto.api.showing.FillOutTenantCheckInDateValidationErrorData.ErrorCode
import ru.yandex.realty.rent.proto.model.flat.showing.FlatShowingData
import ru.yandex.realty.rent.proto.model.flat.showing.FlatShowingData.InitialTenantInfo
import ru.yandex.realty.telepony.PhoneInfo
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.gen.IdGenerator
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil

import java.time.LocalDate
import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ShowingManagerSpec extends WordSpec with RentSpecBase with MockFactory {

  "ShowingManager.createFlatShowings" should {
    "return error code if showing already exists" in new Wiring with CommonData {
      (mockPhoneUnifierClient
        .unify(_: String))
        .expects(sampleTenantPhone)
        .returning(PhoneInfo(sampleTenantPhone, 0, ""))

      (ownerRequestDao
        .findLastByFlatId(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning {
          val ownerRequest = ownerRequestGen.next.copy(ownerRequestId = sampleOwnerRequestId)
          Future.successful(Some(ownerRequest))
        }

      (mockFlatShowingDao
        .findActiveByOwnerRequest(_: String, _: Boolean)(_: Traced))
        .expects(sampleOwnerRequestId, *, *)
        .returning {
          val flatShowingData = FlatShowingData
            .newBuilder()
            .setInitialTenantInfo(InitialTenantInfo.newBuilder().setTenantPhone(sampleTenantPhone))
            .build()
          val flatShowing = sampleFlatShowing.copy(
            status = Gen.oneOf(openStatuses.toSeq).next,
            ownerRequestId = sampleOwnerRequestId,
            data = flatShowingData
          )
          Future.successful(Seq(flatShowing))
        }

      val flatQuestionnaire = flatQuestionnaireGen.next
      (flatQuestionnaireDao
        .findByFlatId(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(Some(flatQuestionnaire)))

      val result: CreateFlatShowingsResponse =
        showingManager.createFlatShowings(sampleFlatId, sampleCreateShowingsRequest)(traced).futureValue

      val expectedResponse: CreateFlatShowingsResponse = CreateFlatShowingsResponse
        .newBuilder()
        .setError(
          CreateFlatShowingsError
            .newBuilder()
            .setErrorCode(CreateFlatShowingsErrorNamespace.CreateFlatShowingsErrorCode.SHOWING_ALREADY_EXIST)
            .setShortMessage("Showing already exists")
            .setDetails(s"Showing lead with flatId=[${sampleFlat.flatId}] and phone=[$sampleTenantPhone] already exist")
            .build()
        )
        .build()

      result shouldEqual expectedResponse
    }

    //TODO enable after AmoCrmManager can be mocked
//    "create showing of flat" in new Wiring with CommonData {
//      (mockLeadDao.leadByFlatAndPhoneExists _)
//        .expects(sampleFlatId, sampleTenantPhone)
//        .returning(Future.successful(false))
//
//      (mockFlatDao.findById _)
//        .expects(sampleFlatId)
//        .returning(Future.successful(sampleFlat))
//
//      (mockAmocrmManager.createShowingLead _)
//        .expects(sampleFlat, sampleCreateShowingsRequest)
//        .returning(Future.successful(sampleLeadId))
//
//      moderationManager.createFlatShowings(sampleFlatId, sampleCreateShowingsRequest)(traced).futureValue
//    }
  }

  "ShowingManager" should {
    "linkUserWithShowings" in new Wiring with Data {
      (mockUserShowingDao
        .findByShowings(_: Seq[String])(_: Traced))
        .expects(where { (showingIds: Seq[String], _) =>
          showingIds.size == 1 && showingIds.contains(showingId)
        })
        .once()
        .returning(Future.successful(Seq.empty))

      (mockUserShowingDao
        .insert(_: Seq[UserShowing])(_: Traced))
        .expects(where { (userShowing: Seq[UserShowing], _) =>
          userShowing.size == 1 && userShowing.exists(s => s.userId == userId && s.showingId == showingId)
        })
        .once()
        .returning(Future.unit)

      (mockFlatShowingDao
        .find(_: Seq[String])(_: Traced))
        .expects(where { (showingIds: Seq[String], _) =>
          showingIds.size == 1 && showingIds.contains(showingId)
        })
        .once()
        .returning(Future.successful(Seq(flatShowing)))

      val showingIds = Set(showingId)
      showingManager.linkUserWithShowings(showingIds, userId, linkOpenShowings = false).futureValue
    }

    "linkUserWithShowing" in new Wiring with Data {
      (mockUserShowingDao
        .findByShowing(_: String)(_: Traced))
        .expects(where { (id: String, _) =>
          showingId == id
        })
        .once()
        .returning(Future.successful(Seq(userShowing.copy(isMain = true))))

      (mockFlatShowingDao
        .find(_: String, _: Boolean)(_: Traced))
        .expects(where { (id: String, _, _) =>
          id == showingId
        })
        .once()
        .returning(Future.successful(None))

      (mockFlatShowingDao
        .insert(_: Seq[FlatShowing])(_: Traced))
        .expects(where { (showings: Seq[FlatShowing], _) =>
          showings.size == 1 && showings.exists { showing =>
            showing.showingId == showingId &&
            showing.ownerRequestId == ownerRequest.ownerRequestId &&
            showing.groupStatus == GroupStatus.Created
          }
        })
        .once()
        .returning(Future.unit)

      val userByPhone: User = userGen().next.copy(phone = Some(readableString.next))
      (mockPhoneUnifierClient
        .unify(_: String))
        .expects(*)
        .returning(PhoneInfo(readableString.next, 213, "mobile"))
      (mockUserDao
        .findByPhones(_: Seq[String])(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(Seq(userByPhone)))

      (mockUserShowingDao
        .insert(_: Seq[UserShowing])(_: Traced))
        .expects(where { (showings: Seq[UserShowing], _) =>
          showings.size == 1 && showings.exists(g => g.userId == userByPhone.userId && g.showingId == showingId)
        })
        .once()
        .returning(Future.unit)

      showingCreator.linkUserWithShowing(flatShowing, readableString.next).futureValue
    }
  }

  "ShowingManager.convertAbcShiftsToDutyPersons" should {
    def makeInterval(from: Int, to: Int): LocalDateInterval = {
      val fromStr = s"0$from".takeRight(2)
      val toStr = s"0$to".takeRight(2)
      LocalDateInterval(LocalDate.parse(s"2022-12-$fromStr"), LocalDate.parse(s"2022-12-$toStr"))
    }

    def makeShift(interval: LocalDateInterval, uid: String) = AbcShift(
      start = Some(interval.from),
      end = Some(interval.to),
      person = Some(AbcPerson(uid = Some(uid))),
      isApproved = Some(true)
    )

    "convert zero abc shifts" in new Wiring {
      showingManager.convertAbcShiftsToDutyPersons(Nil) shouldEqual Nil
    }

    "convert one abc shift" in new Wiring {
      val dateInterval = makeInterval(5, 10)
      val uid = "123"
      val shift = makeShift(dateInterval, uid)
      showingManager.convertAbcShiftsToDutyPersons(List(shift)) shouldEqual
        List(DutyPerson(uid.toLong, dateInterval.from, dateInterval.to))
    }

    "convert multiple abc shifts" in new Wiring {
      val dateInterval1 = makeInterval(5, 10)
      val dateInterval2 = makeInterval(15, 20)
      val dateInterval3 = makeInterval(3, 17)
      val uid1 = "1"
      val uid2 = "2"
      val shift1 = makeShift(dateInterval1, uid1)
      val shift2 = makeShift(dateInterval2, uid1)
      val shift3 = makeShift(dateInterval3, uid2)
      showingManager.convertAbcShiftsToDutyPersons(List(shift1, shift2, shift3)).toSet shouldEqual
        List(
          DutyPerson(uid1.toLong, dateInterval1.from, dateInterval1.to),
          DutyPerson(uid1.toLong, dateInterval2.from, dateInterval2.to),
          DutyPerson(uid2.toLong, dateInterval3.from, dateInterval3.to)
        ).toSet
    }

    "convert abc shifts with replacements" in new Wiring {
      val mainInterval = makeInterval(5, 15)
      val replacementInterval1 = makeInterval(5, 6)
      val replacementInterval2 = makeInterval(10, 10)

      val mainManager1 = "1"
      val mainManager2 = "2"
      val replacementOnlyManager = "3"

      val mainManager1Shift = makeShift(mainInterval, mainManager1).copy(
        replaces = Some(
          List(makeShift(replacementInterval1, replacementOnlyManager), makeShift(replacementInterval2, mainManager2))
        )
      )
      val mainManager2Shift = makeShift(mainInterval, mainManager2)

      showingManager.convertAbcShiftsToDutyPersons(List(mainManager1Shift, mainManager2Shift)).toSet shouldEqual
        List(
          DutyPerson(mainManager1.toLong, makeInterval(7, 9).from, makeInterval(7, 9).to),
          DutyPerson(mainManager1.toLong, makeInterval(11, 15).from, makeInterval(11, 15).to),
          DutyPerson(mainManager2.toLong, mainInterval.from, mainInterval.to),
          DutyPerson(replacementOnlyManager.toLong, replacementInterval1.from, replacementInterval1.to)
        ).toSet
    }
  }

  "ShowingManager.fillOutTenantCheckInDate" should {
    val date = DateTimeUtil.now().withDate(2022, 6, 1)
    val user = userGen().next
    val showing = flatShowingGen.next

    trait Common {
      self: Wiring =>

      def mocks(ownerRequest: OwnerRequest, flatQuestionnaire: FlatQuestionnaire) = {
        (mockUserDao
          .findByUid(_: Long)(_: Traced))
          .expects(user.uid, *)
          .returning(Future.successful(user))
          .once()
        (mockFlatShowingDao
          .findActiveByUser(_: String)(_: Traced))
          .expects(user.userId, *)
          .returning(Future.successful(Seq(showing)))
          .once()
        (ownerRequestDao
          .findByIdOpt(_: String)(_: Traced))
          .expects(showing.ownerRequestId, *)
          .returning(Future.successful(Some(ownerRequest)))
          .once()
        (flatQuestionnaireDao
          .findByFlatId(_: String)(_: Traced))
          .expects(ownerRequest.flatId, *)
          .returning(Future.successful(Some(flatQuestionnaire)))
          .once()
      }
    }

    "save tenant check-in date to showing" in new Common with Wiring {
      val flatQuestionnaire = {
        val fq = flatQuestionnaireGen.next
        fq.copy(data = fq.data.toBuilder.setPossibleCheckInDate(DateTimeFormat.write(date.minusDays(1))).build())
      }
      val ownerRequest = ownerRequestGen.next.copy(flatId = flatQuestionnaire.flatId)
      mocks(ownerRequest, flatQuestionnaire)

      (mockFlatShowingDao
        .update(_: String)(_: FlatShowing => FlatShowing)(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(showing))
        .once()

      val response =
        showingManager
          .fillOutTenantCheckInDate(
            user.uid,
            showing.showingId,
            FillOutTenantCheckInDateRequest.newBuilder().setTenantCheckInDate(date.toLocalDate.toString).build()
          )
          .futureValue

      response.hasSuccess shouldBe true
    }

    "return error in" in new Common with Wiring {
      val flatQuestionnaire = {
        val fq = flatQuestionnaireGen.next
        fq.copy(data = fq.data.toBuilder.setPossibleCheckInDate(DateTimeFormat.write(date.plusDays(1))).build())
      }
      val ownerRequest = ownerRequestGen.next.copy(flatId = flatQuestionnaire.flatId)
      mocks(ownerRequest, flatQuestionnaire)

      val response =
        showingManager
          .fillOutTenantCheckInDate(
            user.uid,
            showing.showingId,
            FillOutTenantCheckInDateRequest.newBuilder().setTenantCheckInDate(date.toLocalDate.toString).build()
          )
          .futureValue

      response.getValidationError.getValidationErrorsList.asScala.map(_.getCode) shouldBe Seq(
        ErrorCode.INVALID_CHECK_IN_DATE
      )
    }

    "throw exception" in new Common with Wiring {
      (mockUserDao
        .findByUid(_: Long)(_: Traced))
        .expects(user.uid, *)
        .returning(Future.successful(user))
        .once()
      (mockFlatShowingDao
        .findActiveByUser(_: String)(_: Traced))
        .expects(user.userId, *)
        .returning(Future.successful(Seq(showing)))
        .once()

      interceptCause[ConflictApiException](
        showingManager
          .fillOutTenantCheckInDate(user.uid, "", FillOutTenantCheckInDateRequest.getDefaultInstance)
          .futureValue
      )
    }
  }

  trait Data {
    this: Wiring =>
    val now: DateTime = DateTimeUtil.now

    val userShowing: UserShowing =
      UserShowing(IdGenerator.generateUuid(), IdGenerator.generateUuid(), isMain = false)
    val ownerRequest: OwnerRequest = ownerRequestGen.next
    val userId: String = userShowing.userId
    val showingId: String = userShowing.showingId

    val flatShowing: FlatShowing = FlatShowing(
      showingId,
      ownerRequest.ownerRequestId,
      FlatShowingStatus.NewShowing,
      None,
      GroupStatus.Created,
      None,
      isActive = true,
      FlatShowingData.getDefaultInstance,
      None,
      now,
      now
    )
    val userIdWithoutShowing: String = IdGenerator.generateUuid()

    (ownerRequestDao
      .findLastByFlatId(_: String)(_: Traced))
      .expects(where { (id: String, _) =>
        id == ownerRequest.flatId
      })
      .anyNumberOfTimes()
      .returning(Future.successful(Some(ownerRequest)))
    (ownerRequestDao
      .findLastByFlatIds(_: Set[String])(_: Traced))
      .expects(where { (ids: Set[String], _) =>
        ids.contains(ownerRequest.flatId)
      })
      .anyNumberOfTimes()
      .returning(Future.successful(Seq(ownerRequest)))
  }

  "registerOnlineShowing" should {
    "register online showing successfully" in {
      //TODO REALTYBACK-5822 implement test
    }
  }
}
