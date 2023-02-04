package ru.yandex.realty.rent.backend.manager

import com.google.protobuf.Timestamp
import org.junit.runner.RunWith
import org.scalatest.PrivateMethodTester
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.dao.UserDao
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{Flat}
import ru.yandex.realty.rent.model.enums.Role
import ru.yandex.realty.rent.proto.api.user.SendNetPromoterScoreError
import ru.yandex.realty.rent.proto.model.user.UserData
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class ScoreManagerSpec extends AsyncSpecBase with PrivateMethodTester with RentModelsGen {

  "ScoreManager.getUserRole" should {
    "return Set[Roles] of 1 element" in {
      val scoreManager = new ScoreManager(mock[UserDao], mock[BrokerClient])
      val getUserRoles = PrivateMethod[Either[SendNetPromoterScoreError, Set[String]]]('getUserRoles)
      val flat = mock[Flat]
      val assignedFlats = Map(Role.Owner -> Seq(flat))
      val data = UserData
        .newBuilder()
        .setLastNpsFeedbackDate(
          Timestamp
            .newBuilder()
            .setSeconds(10L)
            .build()
        )
        .build()
      val user = userGen().next.copy(assignedFlats = assignedFlats, data = data)
      val result = scoreManager invokePrivate getUserRoles(user, DateTimeUtil.now())
      result.right.get.mkString(",").shouldBe("owner")
    }
  }

  "ScoreManager.getUserRole" should {
    "return Set[Roles] of 2 elements" in {
      val scoreManager = new ScoreManager(mock[UserDao], mock[BrokerClient])
      val getUserRoles = PrivateMethod[Either[SendNetPromoterScoreError, Set[String]]]('getUserRoles)
      val flat = mock[Flat]
      val assignedFlats = Map(Role.Owner -> Seq(flat), Role.Tenant -> Seq(flat))
      val data = UserData
        .newBuilder()
        .setLastNpsFeedbackDate(
          Timestamp
            .newBuilder()
            .setSeconds(10L)
            .build()
        )
        .build()
      val user = userGen().next.copy(assignedFlats = assignedFlats, data = data)
      val result = scoreManager invokePrivate getUserRoles(user, DateTimeUtil.now())
      result.right.get.mkString(",").shouldBe("owner,tenant")
    }
  }
}
