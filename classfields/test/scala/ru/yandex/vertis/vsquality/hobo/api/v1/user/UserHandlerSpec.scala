package ru.yandex.vertis.vsquality.hobo.api.v1.user

import ru.yandex.vertis.vsquality.hobo.{UserFilter, UserSort}
import ru.yandex.vertis.vsquality.hobo.model.{SummarySalaryStatistics, User}
import ru.yandex.vertis.vsquality.hobo.util.{HandlerSpecBase, Page, SlicedResult}
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.vsquality.hobo.service.OperatorContext

/**
  * @author semkagtn
  */

class UserHandlerSpec extends HandlerSpecBase {

  private val existentUser = UserGen.next
  private val userToDelete = UserGen.next

  private val userService = backend.userService
  private val summarySalaryStatisticsService = backend.summarySalaryStatisticsService

  override protected def initialUsers: Iterable[User] = Iterable(existentUser, userToDelete)

  override def basePath: String = "/api/1.x/user"

  import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound, OK}
  import ru.yandex.vertis.vsquality.hobo.view.DomainMarshalling.{
    SlicedResultUserUnmarshaller,
    SummarySalaryStatisticsUnmarshaller,
    UserSourceMarshaller,
    UserUnmarshaller
  }

  "putUser" should {

    "invoke correct method" in {
      val userSource = UserSourceGen.next
      implicit val oc = OperatorContextGen.next

      Post(url("/"), userSource) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[User]
        there.was(one(userService).put(userSource)(oc))
      }
    }
  }

  "replaceUser" should {

    "invoke correct method" in {
      val userSource = UserSourceGen.next
      implicit val oc = OperatorContextGen.next

      Put(url(s"/${existentUser.key.key}"), userSource) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[User]
        there.was(one(userService).replace(existentUser.key, userSource)(oc))
      }
    }

    "return 404 if no such user" in {
      val userSource = UserSourceGen.next
      val key = UserIdGen.next
      implicit val oc = OperatorContextGen.next

      Put(url(s"/$key"), userSource) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "getUser" should {

    "invoke correct method" in {
      implicit val oc = OperatorContextGen.next

      Get(url(s"/${existentUser.key.key}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[User]
        there.was(one(userService).get(existentUser.key)(oc))
      }
    }

    "return 404 if no such user" in {
      val nonexistentUserKey = UserIdGen.next
      implicit val oc = OperatorContextGen.next

      Get(url(s"/$nonexistentUserKey")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "findUsers" should {

    "invoke correct method without sort" in {
      val page = 1
      val size = 3
      implicit val oc: OperatorContext = OperatorContextGen.next

      Get(url(s"/?page_number=$page&page_size=$size")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[SlicedResult[User]]
        there.was(one(userService).find(UserFilter.Composite(), UserSort.ByName, Page(page, size))(oc))
      }
    }

    "invoke correct method with sort=by_name" in {
      val page = 1
      val size = 3
      implicit val oc: OperatorContext = OperatorContextGen.next

      Get(url(s"/?page_number=$page&page_size=$size&sort=by_name")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[SlicedResult[User]]
        there.was(one(userService).find(UserFilter.Composite(), UserSort.ByName, Page(page, size))(oc))
      }
    }

    "return error if incorrect sort" in {
      val page = 1
      val size = 3
      implicit val oc: OperatorContext = OperatorContextGen.next

      Get(url(s"/?page_number=$page&page_size=$size&sort=incorrect")) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }
  }

  "delete" should {

    "invoke correct method" in {
      implicit val oc: OperatorContext = OperatorContextGen.next

      Delete(url(s"/${userToDelete.key.key}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(userService).delete(userToDelete.key)(oc))
      }
    }

    "return 404 if no such user" in {
      implicit val oc: OperatorContext = OperatorContextGen.next
      val nonexistentKey = UserIdGen.next

      Delete(url(s"/$nonexistentKey")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "getSalaryStatistics" should {

    "invoke correct method" in {
      implicit val oc = OperatorContextGen.next

      Get(url(s"/${existentUser.key.key}/stats")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[SummarySalaryStatistics]
        there.was(one(summarySalaryStatisticsService).get(existentUser.key)(oc))
      }
    }
  }
}
