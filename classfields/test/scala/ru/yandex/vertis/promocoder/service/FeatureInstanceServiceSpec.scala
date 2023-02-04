package ru.yandex.vertis.promocoder.service

import java.util.concurrent.Executors

import org.scalatest.GivenWhenThen
import ru.yandex.vertis.promocoder.FeatureSpecBase
import ru.yandex.vertis.promocoder.model.FeatureInstance
import ru.yandex.vertis.promocoder.model.FeatureInstance.ReferringOrigin
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Filter.{ActiveForUser, ById}
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.{FeatureNotAvailableException, Filter, Mode}
import ru.yandex.vertis.promocoder.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

/** Specs on [[FeatureInstanceService]]
  *
  * @author alex-kovalenko
  */
trait FeatureInstanceServiceSpec extends FeatureSpecBase with GivenWhenThen with ModelGenerators {

  def getService(
      initial: Iterable[FeatureInstance],
      archived: Iterable[FeatureInstance] = Iterable.empty
    ): FeatureInstanceService

  implicit val rc: RequestContext = AutomatedContext("FeatureInstanceServiceSpec")

  Feature("AsyncFeatureService") {
    Scenario("create features") {
      Given("some features for users")
      val (user1, origin1) = ("user1", OriginGen.next)
      val (user2, origin2) = ("user2", OriginGen.next)

      val features1 = uniqueFeaturesGen(3).next
        .map(_.copy(lifetime = 10.days, startTime = None))
      val f2Active = FeatureGen.next
        .copy(lifetime = 10.days, startTime = None)

      val f2ExpiredInstance = FeatureInstanceGen.next
        .copy(
          user = user2,
          origin = origin2,
          createTs = DateTimeUtil.now().minusDays(4),
          startTime = Some(DateTimeUtil.now().minusDays(4)),
          deadline = DateTimeUtil.now().minusDays(1)
        )

      Given("service with one expired feature")
      val service = getService(Iterable(f2ExpiredInstance))

      When("create feature for user1")
      service.create(user1, origin1, Iterable(features1.head)).futureValue

      Then("get them by user1")
      val first = service.get(ActiveForUser(user1), Mode.Default).futureValue.toList match {
        case f1 :: Nil =>
          val d1 = features1.head
          f1.tag shouldBe d1.tag
          f1.count shouldBe d1.count
          f1.payload shouldBe d1.payload
          (f1.deadline.getMillis - f1.createTs.getMillis) shouldBe d1.lifetime.toMillis
          f1
        case other => fail(s"Unexpected $other")
      }

      When("create more features for user1 including existent one")
      service.create(user1, origin1, features1).futureValue
      Then("get features available for user1")
      service.get(ActiveForUser(user1), Mode.Default).futureValue.toSet match {
        case features =>
          features should have size features1.size
          features.forall(_.user == user1) shouldBe true
          features.exists(f => f.id == first.id) shouldBe true
      }

      When("create features for user2")
      service.create(user2, origin2, Iterable(f2Active)).futureValue
      Then("get features available for user2")
      service.get(ActiveForUser(user2), Mode.Default).futureValue.toList match {
        case f :: Nil =>
          f.user shouldBe user2
          f.origin shouldBe origin2
          f.count shouldBe f2Active.count
          f.tag shouldBe f2Active.tag
        case other => fail(s"Unexpected $other")
      }

      When("get expired feature by id")
      val expired = service.get(ById(f2ExpiredInstance.id), Mode.Default).futureValue.head
      Then("return correct")
      expired shouldBe f2ExpiredInstance
    }

    Scenario("get with mode") {
      Given("service with active and archived features")
      val features = uniqueFeatureInstanceGen(50).next
      val (active, archived) = features.splitAt(features.size / 2)
      val service = getService(active, archived)

      service.get(Filter.All, Mode.Default).futureValue should contain theSameElementsAs active
      service.get(Filter.All, Mode.Archive).futureValue should contain theSameElementsAs archived
      service.get(Filter.All, Mode.Both).futureValue should contain theSameElementsAs (active ++ archived)
    }

    Scenario("get or create feature for commission") {
      Given("empty service")
      val service = getService(Iterable())

      When("getOrCreate first time")
      val user = "user"
      val origin = ReferringOrigin("code")
      val feature = FeatureGen.next.copy(count = 0, referring = None)

      val first = service.getOrCreate(user, origin, feature).futureValue
      Then("feature instance will be created")
      first.user shouldBe user
      first.origin shouldBe origin
      first.count shouldBe 0
      first.tag shouldBe feature.tag

      When("getOrCreate with the same arguments")
      val second = service.getOrCreate(user, origin, feature).futureValue

      Then("get the same feature")
      second shouldBe first

      When("getOrCreate with other arguments")
      val third = service.getOrCreate("user2", origin, feature).futureValue
      Then("create another feature instance")
      third should not be first
    }

    Scenario("decrement feature's count") {
      Given("service with expired and active features")
      val count = 10

      val expiredId = "expired"
      val activeId = "active"

      val expiredFeatureInstance = FeatureInstanceGen.next.copy(
        id = expiredId,
        createTs = DateTimeUtil.now().minusDays(4),
        startTime = Some(DateTimeUtil.now().minusDays(4)),
        deadline = DateTimeUtil.now().minusDays(1),
        count = count
      )

      val activeFeatureInstance =
        FeatureInstanceGen.next.copy(id = activeId, count = count)
      val service = getService(
        Iterable(
          activeFeatureInstance,
          expiredFeatureInstance
        )
      )

      When("decrement not existent feature")
      Then("fail with NoSuchElementException")
      shouldFailWith[NoSuchElementException] {
        service.decrement("", 10)
      }

      When("decrement expired feature")
      Then("throw exception with feature's actual state")
      val e1 = shouldFailWith[FeatureNotAvailableException] {
        service.decrement(expiredId, 1)
      }
      e1.feature shouldBe expiredFeatureInstance

      When("decrement active feature for more than feature's count")
      Then("throw exception with feature's actual state")
      val e2 = shouldFailWith[FeatureNotAvailableException] {
        service.decrement(activeId, 20)
      }
      e2.feature shouldBe activeFeatureInstance

      When("decrement active feature with correct delta")
      Then("decrement count and return modified feature state")
      service.decrement(activeId, 5).futureValue match {
        case f: FeatureInstance if f.id == activeId && f.count == 5 =>
        case other => fail(s"Unexpected $other")
      }

      When("decrement with negative delta")
      Then("increment count and return modified feature state")
      service.decrement(activeId, -10).futureValue match {
        case f: FeatureInstance if f.id == activeId && f.count == 15 =>
        case other => fail(s"Unexpected $other")
      }
    }

    Scenario("concurrent decrement") {
      val concurrency = 32
      val iterations = 1000
      implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(concurrency))

      Given("service with one feature that has enough count for concurrency test")
      val feature = FeatureInstanceGen.next
        .copy(count = iterations, deadline = DateTimeUtil.now().plusDays(10))
      val service = getService(Iterable(feature))

      When("get multiple concurrent decrement requests")
      Future.sequence {
        (1 to iterations).map(_ => service.decrement(feature.id, 1))
      }.futureValue

      val featureAfterDecrements = service.get(Filter.ById(feature.id), Mode.Default).futureValue.head
      featureAfterDecrements.count shouldBe 0
    }
  }
}
