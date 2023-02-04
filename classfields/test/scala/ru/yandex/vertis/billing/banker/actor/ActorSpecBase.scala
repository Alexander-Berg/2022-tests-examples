package ru.yandex.vertis.billing.banker.actor

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Basic class for specs on funds movement actors
  *
  * @author alex-kovalenko
  */
abstract class ActorSpecBase(name: String)
  extends TestKit(ActorSystem(name))
  with Matchers
  with AnyWordSpecLike
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ScalaCheckPropertyChecks {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(
      minSuccessful = 100,
      // never change workers count because these tests are not thread-safe
      workers = 1
    )
}
