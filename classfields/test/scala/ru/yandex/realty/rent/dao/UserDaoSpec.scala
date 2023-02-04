package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.dao.actions.impl.{RoommateCandidateDbActionsImpl, UserDbActionsImpl}
import ru.yandex.realty.rent.dao.impl.{RoommateCandidateDaoImpl, UserDaoImpl}
import ru.yandex.realty.rent.model.{RoommateCandidate, RoommateCandidateUtils, User}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.gen.IdGenerator
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class UserDaoSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace: Traced = Traced.empty

  "UserDao.findByRoommateLink" should {
    "return user by roommate link" in new Wiring with Data {
      val roommateLinkId: String = IdGenerator.generateLongNumber(19)
      val usersWithLink: User = userGen().next.copy(
        roommateLinkId = Some(roommateLinkId),
        roommateLinkExpirationTime = Some(DateTimeUtil.now().plusHours(1)),
        assignedFlats = Map.empty
      )
      userDao.create(usersWithLink).futureValue

      val foundedUserOpt: Option[User] = userDao.findByRoommateLinkId(roommateLinkId).futureValue

      foundedUserOpt.nonEmpty shouldEqual true
      foundedUserOpt.get shouldEqual usersWithLink
    }

    "no user found by roommate link because of expiration tome" in new Wiring with Data {
      val roommateLinkId: String = IdGenerator.generateLongNumber(19)
      val usersWithLink: User = userGen().next.copy(
        roommateLinkId = Some(roommateLinkId),
        roommateLinkExpirationTime = Some(DateTimeUtil.now().minusHours(1)),
        assignedFlats = Map.empty
      )
      userDao.create(usersWithLink).futureValue

      val foundedUserOpt: Option[User] = userDao.findByRoommateLinkId(roommateLinkId).futureValue

      foundedUserOpt.nonEmpty shouldEqual false
    }

    "return roommates" in new Wiring with Data {
      val usersToConnectAsRoommates: Iterable[User] = userGen().next(2)
      val usersToConnectAsUser: Iterable[User] = userGen().next(2)
      val usersToConnect: Iterable[User] = usersToConnectAsRoommates ++ usersToConnectAsUser
      usersToConnect.foreach(userDao.create(_).futureValue)
      val userId: String = users.head.userId
      roommateCandidateDao
        .create(
          usersToConnectAsRoommates
            .map { user =>
              RoommateCandidate(RoommateCandidateUtils.generateCandidateId(), userId, Some(user.userId), None)
            }
        )
        .futureValue
      roommateCandidateDao
        .create(usersToConnectAsUser.map { user =>
          RoommateCandidate(RoommateCandidateUtils.generateCandidateId(), user.userId, Some(userId), None)
        })
        .futureValue

      val roommates: Seq[User] = userDao.findRoommates(userId).futureValue

      roommates.size shouldBe usersToConnect.size
      roommates.foreach { roommate =>
        usersToConnect.exists(_.userId == roommate.userId) shouldBe true
      }
    }
  }

  trait Wiring {
    val userDbActions = new UserDbActionsImpl()
    val roommateCandidateDbActions = new RoommateCandidateDbActionsImpl()
    val userDao = new UserDaoImpl(userDbActions, masterSlaveDb2, daoMetrics)
    val roommateCandidateDao = new RoommateCandidateDaoImpl(roommateCandidateDbActions, masterSlaveDb2, daoMetrics)
  }

  trait Data {
    this: Wiring =>

    val userCount = 10

    val users: Iterable[User] = userGen().next(userCount)

    users.foreach(userDao.create(_).futureValue)
  }

}
