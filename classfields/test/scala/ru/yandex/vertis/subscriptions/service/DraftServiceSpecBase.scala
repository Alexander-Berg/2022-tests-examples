package ru.yandex.vertis.subscriptions.service

import ru.yandex.vertis.subscriptions.util.test.CoreGenerators.{anySubscriptionsSources, Users}
import ru.yandex.vertis.subscriptions.{Draft, SlowAsyncSpec, SpecBase}

/**
  * Specs on [[DraftService]]
  *
  * @author dimas
  */
trait DraftServiceSpecBase extends SpecBase with SlowAsyncSpec {

  def service: DraftService

  "DraftService" should {
    "create/read/update/delete draft" in {
      val user = Users.next
      val source = anySubscriptionsSources.next

      var draft: Draft = null
      service.create(user, source).futureValue match {
        case d @ Draft(_, `user`, `source`) => draft = d
        case other => fail(s"Unexpected $other")
      }

      service.get(draft.id).futureValue should be(draft)

      service.list(user).futureValue should be(Seq(draft))

      val updatedSource = anySubscriptionsSources.next
      var updatedDraft: Draft = null
      service.update(draft.id, updatedSource).futureValue match {
        case d @ Draft(_, `user`, `updatedSource`) => updatedDraft = d
        case other => fail(s"Unexpected $other")
      }

      service.get(draft.id).futureValue should be(updatedDraft)

      service.remove(draft.id).futureValue
    }

    "delete all by user" in {
      val user = Users.next
      val sources = anySubscriptionsSources.next(5)
      sources.foreach { source =>
        service.create(user, source).futureValue.source should be(source)
      }

      service.list(user).futureValue.size should be(sources.size)

      service.remove(user).futureValue

      service.list(user).futureValue should be(empty)
    }
  }
}
