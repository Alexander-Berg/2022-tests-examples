package ru.yandex.realty.rent.backend.manager

import org.joda.time.DateTime
import realty.palma.rent_user.{PassportData, RentUser}
import realty.palma.rent_user_black_list.RentUserBlackList
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.model.response.ApiUnitResponseBuilder
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.dao.UserDao
import ru.yandex.realty.rent.model.User
import ru.yandex.realty.rent.model.enums.PassportVerificationStatus
import ru.yandex.realty.rent.proto.api.user.blacklist.BlockUserRequest
import ru.yandex.realty.rent.proto.model.user.UserData
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

class BlackListManagerSpec extends AsyncSpecBase with RequestAware {

  private val userDao: UserDao = mock[UserDao]
  private val rentUserClient: PalmaClient[RentUser] = mock[PalmaClient[RentUser]]
  private val blockListClient: PalmaClient[RentUserBlackList] = mock[PalmaClient[RentUserBlackList]]
  private val blm = new BlackListManager(userDao, rentUserClient, blockListClient)

  private val userId = "12345"
  private val uid = 23454634L
  private val comment = "default comment"
  private val name = "Ivan"
  private val surname = "Surname"
  private val patronymic = "Ivanovich"
  private val passportSeries = "1234"
  private val passportNumber = "567890"
  import realty.palma.RentUserBlackListOuterClass.RentUserBlackList.Reason

  private val user = User(
    uid = uid,
    userId = userId,
    phone = None,
    name = Some(name),
    surname = Some(surname),
    patronymic = Some(patronymic),
    fullName = Some(s"$surname $name $patronymic"),
    email = None,
    passportVerificationStatus = PassportVerificationStatus.Unknown,
    roommateLinkId = None,
    roommateLinkExpirationTime = None,
    assignedFlats = Map(),
    data = UserData.getDefaultInstance,
    createTime = DateTime.now(),
    updateTime = DateTime.now(),
    visitTime = None
  )
  private val palmaUser = RentUser(
    uid = uid.toString,
    documents = Nil,
    passportData = Option(
      PassportData(
        passportSeries = passportSeries,
        passportNumber = passportNumber
      )
    ),
    paymentData = None
  )

  def userRequestBuilder() =
    BlockUserRequest
      .newBuilder()
      .setUserId(userId)
      .setReason(Reason.MISSED_PAYMENT)
      .setComment(comment)

  "BlackListManager" when {
    "putUserBlacklist" should {
      "create user in blacklist if it does not exist" in {
        setupUserDaoFindByUserId()
        setupUserDaoUpdate()
        setupRentUserClientGet(Some(palmaUser))
        setupBlockListClientGet(None)
        setupBlockListClientCreate(
          RentUserBlackList(
            uid = uid.toString,
            userId = userId,
            name = "IVAN",
            surname = "SURNAME",
            patronymic = "IVANOVICH",
            passportData = palmaUser.passportData,
            passportHash = passportSeries + passportNumber,
            reason = RentUserBlackList.Reason.MISSED_PAYMENT,
            comment = comment
          )
        )
        blm.putUserBlacklist(userRequestBuilder().build()).futureValue shouldBe ApiUnitResponseBuilder.Instance
      }
      "update user in blacklist if it exists" in {
        setupUserDaoFindByUserId()
        setupUserDaoUpdate()
        setupRentUserClientGet(Some(palmaUser))
        setupBlockListClientGet(Some(RentUserBlackList(uid = uid.toString, userId = userId)))
        setupBlockListClientUpdate(
          RentUserBlackList(
            uid = uid.toString,
            userId = userId,
            name = "IVAN",
            surname = "SURNAME",
            patronymic = "IVANOVICH",
            passportData = palmaUser.passportData,
            passportHash = passportSeries + passportNumber,
            reason = RentUserBlackList.Reason.MISSED_PAYMENT,
            comment = comment
          )
        )
        blm.putUserBlacklist(userRequestBuilder().build()).futureValue shouldBe ApiUnitResponseBuilder.Instance
      }
      "throw error" when {
        "user not found or not specified" in {
          (userDao.findByUserId _)
            .expects(userId)
            .once()
            .returns(Future.failed(new NoSuchElementException(s"User $userId not found")))
          interceptCause[NoSuchElementException] {
            blm.putUserBlacklist(userRequestBuilder().build()).futureValue
          }
        }
        "user without RentUser" in {
          setupUserDaoFindByUserId()
          setupRentUserClientGet(None)
          interceptCause[IllegalArgumentException] {
            blm.putUserBlacklist(userRequestBuilder().build()).futureValue shouldBe ApiUnitResponseBuilder.Instance
          }
        }
        "user without passport data" in {
          setupUserDaoFindByUserId()
          setupRentUserClientGet(Some(palmaUser.copy(passportData = None)))
          interceptCause[IllegalArgumentException] {
            blm.putUserBlacklist(userRequestBuilder().build()).futureValue shouldBe ApiUnitResponseBuilder.Instance
          }
        }
        "reason is not specified" ignore {
          (userDao.findByUserId _)
            .expects(*)
            .never()
          val request = userRequestBuilder().setReason(Reason.UNKNOWN).build()
          interceptCause[NoSuchElementException] {
            blm.putUserBlacklist(request).futureValue
          }
        }
      }
    }
  }

  private def setupBlockListClientCreate(result: RentUserBlackList) = {
    (blockListClient
      .create(_: RentUserBlackList)(_: Traced))
      .expects(result, *)
      .once()
      .onCall((e, _) => Future.successful(e))
  }

  private def setupBlockListClientUpdate(result: RentUserBlackList) = {
    (blockListClient
      .update(_: RentUserBlackList)(_: Traced))
      .expects(result, *)
      .once()
      .onCall((e, _) => Future.successful(e))
  }

  private def setupBlockListClientGet(result: Option[RentUserBlackList] = None) = {
    (blockListClient
      .get(_: String)(_: Traced))
      .expects(uid.toString, *)
      .once()
      .returns(Future.successful(result))
  }

  private def setupRentUserClientGet(result: Option[RentUser] = Some(palmaUser)) = {
    (rentUserClient
      .get(_: String)(_: Traced))
      .expects(uid.toString, *)
      .once()
      .returns(Future.successful(result))
  }

  private def setupUserDaoUpdate() = {
    (userDao
      .update(_: Long)(_: User => User))
      .expects(where { (uid, tr) =>
        uid == this.uid && tr(user).visitTime.nonEmpty
      })
      .once()
      .onCall((_, tr) => Future.successful(tr(user)))
  }

  private def setupUserDaoFindByUserId() = {
    (userDao.findByUserId _)
      .expects(userId)
      .once()
      .returns(Future.successful(user))
  }
}
