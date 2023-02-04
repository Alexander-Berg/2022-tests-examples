package ru.yandex.vertis.subscriptions.storage

import org.scalatest.BeforeAndAfter
import ru.yandex.vertis.subscriptions.{SlowAsyncSpec, SpecBase}
import ru.yandex.vertis.subscriptions.model.UserLink
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.Users

import scala.util.Success

/**
  * Base specs on [[UserLinks]].
  *
  * @author dimas
  */
trait UserLinksSpecBase extends SpecBase with SlowAsyncSpec with BeforeAndAfter {

  def userLinks: UserLinks

  def cleanTestData(): Unit

  before(cleanTestData())

  "UserLinks" should {
    "create link and provide link" in {
      val origin = Users.next
      val alias = Users.next
      userLinks.link(origin, alias).futureValue
      userLinks.find(alias).futureValue should be(Some(origin))
      userLinks.find(origin).futureValue should be(None)
    }

    "create link and asynchronously provide link" in {
      val origin = Users.next
      val alias = Users.next
      userLinks.link(origin, alias).futureValue
      userLinks.asyncFind(alias).futureValue should be(origin)
      intercept[NoSuchElementException] {
        cause(userLinks.asyncFind(origin).futureValue)
      }
    }

    "support multiple links on user" in {
      val origin = Users.next
      val alias1 = Users.next

      userLinks.link(origin, alias1).futureValue
      userLinks.find(alias1).futureValue should be(Some(origin))

      val alias2 = Users.next
      userLinks.find(alias2).futureValue should be(None)
      userLinks.link(origin, alias2).futureValue
      userLinks.find(alias1).futureValue should be(Some(origin))
      userLinks.find(alias2).futureValue should be(Some(origin))
    }

    "update link" in {
      val origin1 = Users.next
      val alias = Users.next

      userLinks.link(origin1, alias).futureValue
      userLinks.find(alias).futureValue should be(Some(origin1))

      val origin2 = Users.next
      userLinks.link(origin2, alias).futureValue
      userLinks.find(alias).futureValue should be(Some(origin2))
    }

    "remove link" in {
      val origin = Users.next
      val alias = Users.next

      userLinks.link(origin, alias).futureValue
      userLinks.find(alias).futureValue should be(Some(origin))

      userLinks.unlink(alias).futureValue
      userLinks.find(alias).futureValue should be(None)
    }

    "list all links" in {
      val n = 10

      val bindings = for {
        (user, alias) <- Users.values.zip(Users.values).take(n).toMap
      } yield UserLink(user, alias)

      bindings.foreach {
        case UserLink(user, alias) =>
          userLinks.link(user, alias).futureValue
      }

      userLinks.list().map(_.toSet) should be(Success(bindings.toSet))
    }
  }
}
