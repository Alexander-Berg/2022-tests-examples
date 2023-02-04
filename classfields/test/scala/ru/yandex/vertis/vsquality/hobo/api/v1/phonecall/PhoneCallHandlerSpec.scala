package ru.yandex.vertis.vsquality.hobo.api.v1.phonecall

import ru.yandex.vertis.vsquality.hobo.PhoneCallFilter
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.model.{PhoneCall, User}
import ru.yandex.vertis.vsquality.hobo.util.{HandlerSpecBase, Page, SlicedResult, Use}

/**
  * @author semkagtn
  */

class PhoneCallHandlerSpec extends HandlerSpecBase {

  val user: User = UserGen.next.copy(phoneSettings = Some(PhoneSettingsGen.next))
  val userWithoutPhone: User = UserGen.next.copy(phoneSettings = None)
  val nonexistentUser: User = UserGen.next

  private val phoneCallService = backend.phoneCallService

  override protected def initialUsers: Iterable[User] = Iterable(user, userWithoutPhone)

  override def basePath: String = "/api/1.x/phone-call"

  import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound, OK}

  "makeCall" should {

    import ru.yandex.vertis.vsquality.hobo.view.DomainMarshalling.PhoneCallUnmarshaller

    "invoke correct method without provider and queue" in {
      val task = TaskKeyGen.next
      val resourceId = ResourceIdGen.next
      val recipient = PhoneGen.next
      implicit val oc = OperatorContextGen.next

      val path = s"?task=$task&resource_id=$resourceId&user=${user.key.key}&recipient=$recipient"
      Post(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[PhoneCall]
        there.was(one(phoneCallService).call(user.key, None, Some(task), None, Some(resourceId), recipient)(oc))
      }
    }

    "invoke correct method with provider" in {
      val task = TaskKeyGen.next
      val provider = ProviderGen.filter(_ != user.phoneSettings.get.provider).next
      val resourceId = ResourceIdGen.next
      val recipient = PhoneGen.next
      implicit val oc = OperatorContextGen.next

      val path = s"?task=$task&resource_id=$resourceId&user=${user.key.key}&recipient=$recipient&provider=$provider"
      Post(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[PhoneCall]
        there.was(
          one(phoneCallService).call(user.key, Some(provider), Some(task), None, Some(resourceId), recipient)(oc)
        )
      }
    }

    "invoke correct method with queue" in {
      val task = TaskKeyGen.next
      val queue = QueueIdGen.next
      val resourceId = ResourceIdGen.next
      val recipient = PhoneGen.next
      implicit val oc = OperatorContextGen.next

      val path = s"?task=$task&resource_id=$resourceId&user=${user.key.key}&recipient=$recipient&queue=$queue"
      Post(url(path)) ~> defaultHeaders ~> route ~> check {
        there.was(one(phoneCallService).call(user.key, None, Some(task), Some(queue), Some(resourceId), recipient)(oc))
      }
    }

    "return 400 if user doen't have phone" in {
      val task = TaskKeyGen.next
      val resourceId = ResourceIdGen.next
      val recipient = PhoneGen.next
      implicit val oc = OperatorContextGen.next

      val path = s"?task=$task&resource_id=$resourceId&user=${userWithoutPhone.key.key}&recipient=$recipient"
      Post(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }

    "return 404 if user doesn't exist" in {
      val task = TaskKeyGen.next
      val resourceId = ResourceIdGen.next
      val recipient = PhoneGen.next
      implicit val operatorContext = OperatorContextGen.next

      val path = s"?task=$task&resource_id=$resourceId&user=${nonexistentUser.key.key}&recipient=$recipient"
      Post(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "findCalls" should {

    import ru.yandex.vertis.vsquality.hobo.view.DomainMarshalling.SlicedResultPhoneCallUnmarshaller

    "invoke correct method with task parameter" in {
      val task = TaskKeyGen.next
      val filter = PhoneCallFilter.Composite(task = Use(Some(task)))
      val page = Page(0, 10)
      implicit val oc = OperatorContextGen.next

      val path = s"?task=$task&page_number=${page.number}&page_size=${page.size}"
      Get(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[SlicedResult[PhoneCall]]
        there.was(one(phoneCallService).find(filter, page)(oc))
      }
    }

    "invoke correct method with resource_id parameter" in {
      val resourceId = ResourceIdGen.next
      val filter = PhoneCallFilter.Composite(resourceId = Use(Some(resourceId)))
      val page = Page(0, 10)
      implicit val oc = OperatorContextGen.next

      val path = s"?resource_id=$resourceId&page_number=${page.number}&page_size=${page.size}"
      Get(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[SlicedResult[PhoneCall]]
        there.was(one(phoneCallService).find(filter, page)(oc))
      }
    }

    "return 400 if no task and resource_id parameters" in {
      val page = Page(0, 10)
      implicit val oc = OperatorContextGen.next

      Get(url(s"?page_number=${page.number}&page_size=${page.size}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }

    "return 400 if task and resource_id parameters are set" in {
      val task = TaskKeyGen.next
      val resourceId = ResourceIdGen.next
      val page = Page(0, 10)
      implicit val oc = OperatorContextGen.next

      val path = s"?task=$task&resource_id=$resourceId&page_number=${page.number}&page_size=${page.size}"
      Get(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }

    "return 404 if no page_number parameter" in {
      val task = TaskKeyGen.next
      val page = Page(0, 10)
      implicit val oc = OperatorContextGen.next

      Get(url(s"task=$task&page_size=${page.size}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if no page_size parameter" in {
      val task = TaskKeyGen.next
      val page = Page(0, 10)
      implicit val oc = OperatorContextGen.next

      Get(url(s"task=$task&page_number=${page.number}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }
}
