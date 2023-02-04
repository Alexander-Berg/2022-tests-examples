package ru.yandex.vertis.personal

import ru.yandex.vertis.personal.model.UserRef
import ru.yandex.vertis.personal.util.BaseSpec
import ru.yandex.vertis.personal.model.ModelGenerators._
import ru.yandex.vertis.personal.generators.Producer

/**
  * Base specs on [[PropertyDao]].
  *
  * @author dimas
  */
trait PropertyDaoSpecBase[P] extends BaseSpec {

  def dao: PropertyDao[P]

  def nextProperty(user: UserRef): P

  def emptyProperty(user: UserRef): P

  "PropertyDao" should {
    "respond with empty user property" in {
      val user = UserRefGen.next
      dao.get(user).futureValue shouldBe emptyProperty(user)
    }

    "CRUD user property" in {
      val user = UserRefGen.next
      val property = nextProperty(user)
      dao.updateWithCas(user)(_ => Some(property)).futureValue shouldBe property

      dao.get(user).futureValue shouldBe property
      dao.getAndTouch(user).futureValue shouldBe property

      dao.delete(user).futureValue should be(())

      dao.get(user).futureValue shouldBe emptyProperty(user)
    }

    "CRUD user property (delete by update func return None)" in {
      val user = UserRefGen.next
      val property = nextProperty(user)

      dao.get(user).futureValue shouldBe emptyProperty(user)

      dao.updateWithCas(user)(_ => Some(property)).futureValue shouldBe property
      dao.get(user).futureValue shouldBe property

      dao.updateWithCas(user)(_ => None).futureValue
      dao.get(user).futureValue shouldBe emptyProperty(user)
    }

    "delete non-existent user property" in {
      val user = UserRefGen.next

      dao.updateWithCas(user)(_ => None).futureValue shouldBe emptyProperty(
        user
      )
    }
  }

}
