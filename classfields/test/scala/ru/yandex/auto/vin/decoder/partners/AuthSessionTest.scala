package ru.yandex.auto.vin.decoder.partners

import auto.carfax.common.utils.http.AuthSession
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterEach, Ignore}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.{retry, AwaitableFuture}
import auto.carfax.common.utils.time.DelayStrategy._
import ru.yandex.vertis.tracing.Traced

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.reflectiveCalls

@Ignore
class AuthSessionTest extends AnyFunSuite with BeforeAndAfterEach {

  private val createSessionCounter = new AtomicInteger(0)
  private val aCallCounter = new AtomicInteger(0)
  private val bCallCounter = new AtomicInteger(0)

  override def beforeEach(): Unit = {
    createSessionCounter.set(0)
    aCallCounter.set(0)
    bCallCounter.set(0)
  }

  implicit private val t: Traced = Traced.empty

  /**
    * 1. Сессии нет, за ней приходят несколько тредов. Один из них создаёт сессию,
    *    остальные -- подписываются на результат
    */
  test("Only one thread at a time creates session") {

    val client = new AuthSession[String] {

      protected def createSession(): Future[String] = {
        createSessionCounter.incrementAndGet()
        Thread.sleep(1000)
        Future.successful("test-session-id")
      }
      protected val specificErrorsToRecreateSession = List(classOf[SessionInvalidException])

      def a: Future[Unit] = auth(simulateReq(600, aCallCounter))
      def b: Future[Unit] = auth(simulateReq(100, bCallCounter))
    }

    Future(client.a) // запустит получение сессии
    Future(client.b) // подпишется и будет ждать ту же сессию
    Future(client.b) // подпишется и будет ждать ту же сессию
    Future(client.a) // подпишется и будет ждать ту же сессию

    Thread.sleep(1500)
    assert(createSessionCounter.get() == 1)
    assert(aCallCounter.get() == 2)
    assert(bCallCounter.get() == 2)
  }

  /**
    * 2. Сессия протухла, за ней приходят несколько тредов. Один из них обновляет сессию,
    *    остальные -- подписываются на результат. Обновление сессии выполняется ok только с четвёртой попытки,
    *    все треды дожидаются этого
    */
  test("Only one thread at a time refreshes session") {

    val client = new AuthSession[String] {

      val tries = new AtomicInteger(3)

      protected def createSession(): Future[String] = {
        val getSession = () => {
          createSessionCounter.incrementAndGet()
          if (tries.get() == 3) { // на четвёртый раз запрос отрабатывает ок
            tries.set(0)
            Thread.sleep(1000)
            Future.successful("test-session-id")
          } else { // три раза запрос падает
            tries.incrementAndGet()
            Thread.sleep(100)
            Future.failed(new RuntimeException)
          }
        }
        retry(getSession, maxTries = 4, delays = 500.millis.asStrategy)
      }
      protected val specificErrorsToRecreateSession = List(classOf[SessionInvalidException])
      override protected val sessionTtl: Option[FiniteDuration] = Some(12.seconds)

      def a: Future[Unit] = auth(simulateReq(600, aCallCounter))
      def b: Future[Unit] = auth(simulateReq(100, bCallCounter))
    }

    client.a // первый раз получаем сессию
    Thread.sleep(3000) // ждём, чтобы сессия стала считаться просроченной
    Future(client.b) // запустит получение сессии
    Future(client.b) // подпишется и будет ждать ту же сессию
    Future(client.a) // подпишется и будет ждать ту же сессию
    Future(client.a) // подпишется и будет ждать ту же сессию

    Thread.sleep(10000)
    assert(createSessionCounter.get() == 5)
    assert(aCallCounter.get() == 3)
    assert(bCallCounter.get() == 2)
  }

  /**
    * 3. За сессией приходят несколько тредов, создание сессии не работает (ретраи не помогают).
    *    Все треды ждут ретраи, а затем падают вместе с фьючей получения сессии. Следующие треды подписываются на
    *    новую фьючу -- попытку получить сессию
    */
  test("If session creation failed, all subscribers notified") {

    val client = new AuthSession[String] {

      protected def createSession(): Future[String] = {
        val getSession = () => {
          createSessionCounter.incrementAndGet()
          Thread.sleep(100)
          Future.failed(new RuntimeException)
        }
        retry(getSession, maxTries = 3, delays = Linear(SECONDS))
      }
      protected val specificErrorsToRecreateSession = List(classOf[SessionInvalidException])

      def a: Future[Unit] = auth(simulateReq(600, aCallCounter))
      def b: Future[Unit] = auth(simulateReq(100, bCallCounter))
    }

    val res1 = Future(client.a).flatten
    val res2 = Future(client.b).flatten
    val res3 = Future(client.a).flatten
    val res4 = Future(client.b).flatten

    Thread.sleep(4000)
    assert(createSessionCounter.get() == 3)
    assert(aCallCounter.get() == 0)
    assert(bCallCounter.get() == 0)
    assert(List(res1, res2, res3, res4).forall(_.value.exists(_.isFailure)))

    Future(client.a)
    Future(client.b)

    Thread.sleep(4000)
    assert(createSessionCounter.get() == 6)
    assert(aCallCounter.get() == 0)
    assert(bCallCounter.get() == 0)
  }

  /**
    * 4. Сессия протухла неожиданно -- при попытке её использовать, сервер ответил что она больше невалидна.
    *    В эту ситуацию попали одновременно несколько тредов. Один из них пробует обновить сессию, остальные подписываются
    *    и ждут. Когда сессия получена, все пробуют повторить свои не получившиеся действия с новой сессией
    */
  test("If session unexpectedly expired, only one thread is trying to refresh session") {

    val client = new AuthSession[String] {

      val sessionFlaky = "test-session-id-1"
      val session = "test-session-id-2"
      val flakyDone = new AtomicBoolean(false)

      protected def createSession(): Future[String] = {
        createSessionCounter.incrementAndGet()
        Thread.sleep(100)
        if (flakyDone.get()) {
          Future.successful(session)
        } else {
          flakyDone.set(true)
          Future.successful(sessionFlaky)
        }
      }
      protected val specificErrorsToRecreateSession = List(classOf[SessionInvalidException])

      def a: Future[Unit] = auth { s =>
        aCallCounter.incrementAndGet()
        Thread.sleep(200)
        if (s == sessionFlaky) {
          Future.failed(new SessionInvalidException)
        } else {
          Future.unit
        }
      }
      def b: Future[Unit] = auth(simulateReq(100, bCallCounter))
    }

    val res1 = Future(client.b).flatten
    val res2 = Future(client.a).flatten
    val res3 = Future(client.a).flatten
    val res4 = Future(client.a).flatten
    val res5 = Future(client.b).flatten

    Thread.sleep(800)
    assert(createSessionCounter.get() == 2)
    assert(aCallCounter.get() == 6)
    assert(bCallCounter.get() == 2)
    assert(List(res1, res2, res3, res4, res5).forall(_.value.exists(_.isSuccess)))
  }

  test("if specificErrors are not specified, any action error triggers single try to refresh session") {
    val client = new AuthSession[String] {

      protected def createSession(): Future[String] = {
        createSessionCounter.incrementAndGet()
        Future.successful("test-session-id")
      }
      protected val specificErrorsToRecreateSession = Nil

      def a(): Future[Unit] = auth { _ =>
        aCallCounter.incrementAndGet()
        if (createSessionCounter.get == 1) throw new RuntimeException else Future.unit
      }
    }

    Future
      .sequence(
        List(
          client.a(), // создали сессию, action кинул ошибку, пересоздали сессию
          client.a() // action отработал с сессией полученной на предыдущем вызове
        )
      )
      .await
    assert(createSessionCounter.get == 2)
    assert(aCallCounter.get == 3)
  }

  private def simulateReq(timing: Int, counter: AtomicInteger)(session: String): Future[Unit] = {
    counter.incrementAndGet()
    Thread.sleep(timing)
    Future.unit
  }

  private class SessionInvalidException extends RuntimeException
}
