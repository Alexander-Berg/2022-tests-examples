package ru.yandex.vertis.promocoder.service

import org.scalatest.GivenWhenThen
import ru.yandex.vertis.promocoder.FeatureSpecBase
import ru.yandex.vertis.promocoder.model.PromocodeInstance.{ReferringStatuses, Statuses}
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{Constraints, Promocode, PromocodeInstance, User}
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.Filter.{ById, ByPromocode, ByStatus, ByUser}
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.PromocodeConstraintViolationException
import ru.yandex.vertis.promocoder.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil

/** Specs on [[PromocodeInstanceService]]
  *
  * @author alex-kovalenko
  */
trait PromocodeInstanceServiceSpec extends FeatureSpecBase with GivenWhenThen with ModelGenerators {

  type Record = (User, Promocode)

  val EmptyConstraints = Constraints(
    deadline = DateTimeUtil.now().plusDays(2),
    totalActivations = Int.MaxValue,
    userActivations = Int.MaxValue
  )

  implicit val rc: RequestContext = AutomatedContext("PromocodeInstanceServiceSpec")

  def getService(promocodes: Iterable[Promocode], instances: Iterable[Record]): PromocodeInstanceService

  Feature("PromocodeActivationService") {
    val user = "user"
    Scenario("all constraints passed activation") {
      Given("promocode with empty constraints")
      val promocode = PromocodeGen.next.copy(constraints = EmptyConstraints)
      Given("empty service")
      val service = getService(Iterable(promocode), Iterable.empty)

      When("user activate promocode")
      Then("activation successes")
      val instance = service.activate(user, promocode).futureValue
      instance.code shouldBe promocode.code
      instance.user shouldBe user
      instance.status shouldBe PromocodeInstance.Statuses.Created
    }

    Scenario("activate expired promocode") {
      Given("promocode with deadline")
      val promocode =
        PromocodeGen.next.copy(constraints = EmptyConstraints.copy(deadline = DateTimeUtil.now().minusDays(1)))
      Given("empty service")
      val service = getService(Iterable(promocode), Iterable.empty)

      When("user activate expired promocode")
      Then("activation fails")
      shouldFailWith[PromocodeConstraintViolationException] {
        service.activate(user, promocode)
      }
    }

    Scenario("exceed limit of total activations") {
      Given("service with one activation")
      val promocode = PromocodeGen.next.copy(constraints = EmptyConstraints.copy(totalActivations = 1))
      val service = getService(Iterable(promocode), Iterable(("user1", promocode)))
      Given("promocode with total activations limit")

      When("other user activate the same promocode")
      Then("activation fails")
      shouldFailWith[PromocodeConstraintViolationException] {
        service.activate(user, promocode)
      }
    }

    Scenario("exceed limit of per-user activations") {
      Given("service with one activation")
      val promocode = PromocodeGen.next.copy(constraints = EmptyConstraints.copy(userActivations = 1))
      val service = getService(Iterable(promocode), Iterable((user, promocode)))
      Given("promocode with total activations limit")

      When("the same user activate the same promocode")
      Then("activation fails")
      shouldFailWith[PromocodeConstraintViolationException] {
        service.activate(user, promocode)
      }

      When("other user activates promocode")
      Then("activation successes")
      service.activate("user2", promocode).futureValue
    }

    Scenario("user in blacklist activation") {
      Given("promocode with user in blacklist")
      val promocode = PromocodeGen.next.copy(constraints = EmptyConstraints.copy(blacklist = Set(user)))
      Given("empty service")
      val service = getService(Iterable(promocode), Iterable.empty)

      When("user from blacklist activates promocode")
      Then("activation fails")
      shouldFailWith[PromocodeConstraintViolationException] {
        service.activate(user, promocode)
      }
    }

    Scenario("ship features") {
      Given("empty service")
      val promocode = PromocodeGen.next.copy(constraints = EmptyConstraints)
      val service = getService(Iterable(promocode), Iterable.empty)

      When("create activation")
      val instance = service.activate(user, promocode).futureValue
      Then("status should be 'Created'")
      instance.status shouldBe PromocodeInstance.Statuses.Created

      When("complete new activation")
      val completed = service.shipFeatures(instance).futureValue
      Then("status should change to 'Shipped'")
      completed.status shouldBe PromocodeInstance.Statuses.Shipped

      When("complete already completed activation")
      Then("should return the same object")
      service.shipFeatures(completed).futureValue shouldBe completed

      When("complete unexistent activation")
      Then("fail")
      shouldFailWith[NoSuchElementException] {
        service.shipFeatures(instance.copy(id = "-"))
      }
    }

    Scenario("process commission") {
      Given("empty service")
      val noRefPromo = {
        val p = PromocodeGen.next
        p.copy(constraints = EmptyConstraints, features = p.features.map(_.copy(referring = None)))
      }

      val refPromo = {
        val p = PromocodeGen.next
        p.copy(
          constraints = EmptyConstraints,
          features = p.features.map(_.copy(referring = Some(ReferringGen.next)))
        )
      }

      val service = getService(Iterable(refPromo, noRefPromo), Iterable.empty)

      When("create instance of non-referral promocode")
      val noRefInstance = service.activate(user, noRefPromo).futureValue
      Then("instance's referring status should be empty")
      noRefInstance.referringStatus shouldBe None

      When("create instance of referral promocode")
      val refInstance = service.activate(user, refPromo).futureValue
      Then("referring status should be Waiting")
      refInstance.referringStatus shouldBe Some(ReferringStatuses.Waiting)

      When("processCommission of non-referral instance")
      Then("should fail")
      shouldFailWith[IllegalArgumentException] {
        service.shipCommission(noRefInstance)
      }

      When("processCommission of referral instance")
      val processed = service.shipCommission(refInstance).futureValue
      Then("it's status should be Processed")
      processed.referringStatus shouldBe Some(ReferringStatuses.Shipped)

      When("processCommission again")
      val processed2 = service.shipCommission(refInstance).futureValue
      Then("should return the same object")
      processed2 shouldBe processed
    }

    Scenario("get promocode instances") {
      Given("service with some promocode instances")
      val count = 3
      val users = listNUnique(count, UserGen)(identity).next
      val codes = listNUnique(count, PromocodeIdGen)(identity).next
      val service = {
        val promocodes = codes.map(c => PromocodeGen.next.copy(code = c))
        getService(promocodes, users.zip(promocodes))
      }

      When("requested by promocode id")
      Then("return matched instances")
      val gotByCodes = codes.flatMap(c => service.get(ByPromocode(c)).futureValue)
      gotByCodes should have size count
      gotByCodes.map(_.code) shouldBe codes

      When("requested by id")
      Then("return matched instance")
      val first = gotByCodes.head
      val gotById = service.get(ById(first.id)).futureValue
      gotById should have size 1
      gotById.head shouldBe first

      When("requested by status")
      Then("return matched instances")
      val gotByStatusCreated = service.get(ByStatus(Statuses.Created)).futureValue
      gotByStatusCreated should have size count

      When("requested by user")
      Then("return matched instances")
      val gotByUsers = users.flatMap(user => service.get(ByUser(user)).futureValue)
      gotByUsers should have size count
      gotByUsers.map(_.user) shouldBe users

      val completed = service.shipFeatures(first).futureValue
      val gotByStatusShipped = service.get(ByStatus(Statuses.Shipped)).futureValue
      gotByStatusShipped should (have size 1 and contain(completed))
    }
  }
}
