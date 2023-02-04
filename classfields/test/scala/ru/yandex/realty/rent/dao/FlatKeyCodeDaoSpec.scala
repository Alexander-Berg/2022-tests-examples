package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.dao.actions.impl.FlatKeyCodeDbActionsImpl
import ru.yandex.realty.rent.model.{FlatKeyCode, RoommateCandidate, RoommateCandidateUtils, User}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.gen.IdGenerator
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class FlatKeyCodeDaoSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace: Traced = Traced.empty

  "FlatKeyCodeDao.generate" should {

    "generate code list" in new Wiring with Data {
      val firstGeneratedIds = flatKeyCodeDao.generate(userUid, 2).futureValue
      val secondGeneratedIds = flatKeyCodeDao.generate(userUid, 3).futureValue
      firstGeneratedIds shouldEqual List(1, 2)
      secondGeneratedIds shouldEqual List(3, 4, 5)
    }

    "find by id" in new Wiring with Data {
      flatKeyCodeDao.generate(userUid, 1).futureValue
      val keyOpt = flatKeyCodeDao.find(1).futureValue
      keyOpt.map(_.uid) shouldEqual Some(userUid)
    }

  }

  trait Wiring {}

  trait Data {
    this: Wiring =>

    val userUid = 101L

  }

}
