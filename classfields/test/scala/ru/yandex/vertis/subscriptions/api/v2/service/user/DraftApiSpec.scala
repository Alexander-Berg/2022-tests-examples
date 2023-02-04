package ru.yandex.vertis.subscriptions.api.v2.service.user

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.api.RouteTestWithConfig
import ru.yandex.vertis.subscriptions.model.UserKey
import ru.yandex.vertis.subscriptions.service.impl.JvmDraftService
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.{anySubscriptionsSources, emailSubscriptionsSources, Users}
import ru.yandex.vertis.subscriptions.view.json.DraftView.viewUnmarshaller
import ru.yandex.vertis.subscriptions.view.json.SourceView.viewMarshaller
import ru.yandex.vertis.subscriptions.view.json.{DraftView, SourceView}
import spray.http.StatusCodes

/**
  * Specs on [[DraftApi]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class DraftApiSpec extends Matchers with WordSpecLike with RouteTestWithConfig {

  private val route = seal(new DraftApi(new JvmDraftService).route)

  "/user/*/draft" should {
    "list empty drafts" in {
      Get(s"/${UserKey(Users.next)}/draft") ~> route ~> check {
        responseAs[Seq[DraftView]] should be(Seq.empty)
      }
    }
    "create/read/update/delete draft (legacy email delivery format)" in testEmailSubscription(legacy = true)

    "create/read/update/delete draft" in testEmailSubscription(legacy = false)
  }

  def testEmailSubscription(legacy: Boolean): Unit = {

    def correctEmail(src: SourceView) =
      if (legacy) src.copy(emailDelivery = None)
      else src.copy(email = None, period = None)

    val sourceGen = if (legacy) emailSubscriptionsSources else anySubscriptionsSources

    val user = Users.next
    val userKey = UserKey(user)
    val source = correctEmail(SourceView(sourceGen.next))
    var createdDraft: DraftView = null

    Post(s"/$userKey/draft", source) ~> route ~> check {
      responseAs[DraftView] match {
        case d @ DraftView(_, `userKey`, src) if correctEmail(src) == source => createdDraft = d
        case other => fail(s"Unexpected $other")
      }
    }
    Get(s"/$userKey/draft") ~> route ~> check {
      responseAs[Seq[DraftView]] should be(Seq(createdDraft))
    }
    Get(s"/$userKey/draft/${createdDraft.id}") ~> route ~> check {
      responseAs[DraftView] should be(createdDraft)
    }

    val updatedSource = correctEmail(
      source.copy(
        email = source.email.map(_ + "@"),
        emailDelivery = source.emailDelivery.map(d => d.copy(address = d.address + "@"))
      )
    )
    var updatedDraft: DraftView = null
    Put(s"/$userKey/draft/${createdDraft.id}", updatedSource) ~> route ~> check {
      responseAs[DraftView] match {
        case d @ DraftView(_, `userKey`, src) if correctEmail(src) == updatedSource => updatedDraft = d
        case other => fail(s"Unexpected $other")
      }
    }
    Get(s"/$userKey/draft/${updatedDraft.id}") ~> route ~> check {
      responseAs[DraftView] should be(updatedDraft)
    }

    Delete(s"/$userKey/draft/${updatedDraft.id}") ~> route ~> check {
      status should be(StatusCodes.OK)
    }
    Get(s"/$userKey/draft/${updatedDraft.id}") ~> route ~> check {
      status should be(StatusCodes.NotFound)
    }
  }

}
