package ru.yandex.vertis.vsquality.hobo.service

import ru.yandex.vertis.vsquality.hobo.exception.NotExistException
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.model.{User, UserId}
import ru.yandex.vertis.vsquality.hobo.util.{Range, SpecBase}
import ru.yandex.vertis.vsquality.hobo.{UserFilter, UserSort}

/**
  * Base specs on [[ReadUserSupport]]
  *
  * @author semkagtn
  */
trait ReadUserSupportSpecBase extends SpecBase {

  def newReadUserSupport(users: User*): ReadUserSupport

  implicit private val rc: AutomatedContext = AutomatedContext("test")

  "get" should {

    "correctly get user by Hobo key" in {
      val expectedUser = UserGen.next
      val readUserSupport = newReadUserSupport(expectedUser)

      val actualUser = readUserSupport.get(expectedUser.key).futureValue
      actualUser should smartEqual(expectedUser)
    }

    "correctly get user by Yandex key" in {
      val expectedUser = UserGen.next.copy(yandexId = Some(UserIdYandexGen.next))
      val readUserSupport = newReadUserSupport(expectedUser)

      val actualUser = readUserSupport.get(expectedUser.yandexId.get).futureValue
      actualUser should smartEqual(expectedUser)
    }

    "correctly get user by AutoRu key" in {
      val expectedUser = UserGen.next.copy(autoRuId = Some(UserIdAutoRuGen.next))
      val readUserSupport = newReadUserSupport(expectedUser)

      val actualUser = readUserSupport.get(expectedUser.autoRuId.get).futureValue
      actualUser should smartEqual(expectedUser)
    }

    "throw an exception if key doesn't exist" in {
      val nonexistentKey = UserId.Hobo(stringGen(4, 5).next)
      val readUserSupport = newReadUserSupport()

      whenReady(readUserSupport.get(nonexistentKey).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "find" should {

    "correctly find all users" in {
      val numUsers = 2
      val slice = Range(0, numUsers)
      val expectedUsers = UserGen.next(numUsers).toList.sortBy(_.name)
      val readUserSupport = newReadUserSupport(expectedUsers: _*)

      val actualResult = readUserSupport.find(UserFilter.Composite(), UserSort.ByName, slice).futureValue

      actualResult.values.toList should smartEqual(expectedUsers)
      actualResult.total should smartEqual(numUsers)
      actualResult.slice should smartEqual(slice)
    }

    "returns no more than limit" in {
      val numUsers = 2
      val users = UserGen.next(numUsers).toList.sortBy(_.name)
      val readUserSupport = newReadUserSupport(users: _*)
      val filter = UserFilter.Composite()
      val sort = UserSort.ByName

      val allUsers = readUserSupport.find(filter, sort, Range(0, numUsers)).futureValue.values.toList

      val firstUser = readUserSupport.find(filter, sort, Range(0, 1)).futureValue.values.toList
      firstUser should smartEqual(allUsers.dropRight(1))

      val secondUser = readUserSupport.find(filter, sort, Range(1, 2)).futureValue.values.toList
      secondUser should smartEqual(allUsers.drop(1))
    }
  }
}
