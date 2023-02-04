package ru.yandex.vertis.billing.banker.tasks

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/**
  * Base class for specs on effect tasks
  *
  * @author alex-kovalenko
  */
abstract class EffectAsyncTaskSpecBase(name: String)
  extends TestKit(ActorSystem(name, ConfigFactory.empty()))
  with Matchers
  with MockitoSupport
  with AnyWordSpecLike
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with AsyncSpecBase {

  implicit val timeout = Timeout(1.second)
  implicit override val ec = ExecutionContext.global

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

}
