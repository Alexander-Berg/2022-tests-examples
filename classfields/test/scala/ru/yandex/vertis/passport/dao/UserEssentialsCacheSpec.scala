package ru.yandex.vertis.passport.dao

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

/**
  * Tests for [[UserEssentialsCache]]
  *
  * @author zvez
  */
trait UserEssentialsCacheSpec extends WordSpec with SpecBase {

  def userDao: UserEssentialsCache

  private val someUser = ModelGenerators.userEssentials.next

  "UserEssentialsCache" should {
    "return None if there is no such user" in {
      userDao.get(someUser.id).futureValue should be(None)
    }

    "save and return saved user" in {
      userDao.upsert(someUser).futureValue should be(())
      userDao.get(someUser.id).futureValue should be(Some(someUser))
    }

    "allow update user" in {
      val userToUpdate = someUser.copy(email = Some("somethingelse"))
      userDao.upsert(userToUpdate).futureValue should be(())
      userDao.get(someUser.id).futureValue should be(Some(userToUpdate))
    }
  }

}
