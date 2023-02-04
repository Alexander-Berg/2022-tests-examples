package ru.yandex.vertis.chat.api.v1.domain.cache

import akka.http.scaladsl.model.StatusCodes
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.prop.PropertyChecks.forAll
import ru.yandex.vertis.chat.api.HandlerSpecBase
import ru.yandex.vertis.chat.components.cache.CacheService
import ru.yandex.vertis.chat.components.time.{DefaultTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.generators.BasicGenerators.list

/**
  * Specs on [[CacheHandler]].
  *
  * @author dimas
  */
class CacheHandlerSpec extends HandlerSpecBase with RequestContextAware with MockitoSupport {

  private val support = mock[CacheService]
  when(support.invalidate(?, ?)(?))
    .thenAnswer(new Answer[Unit] {
      def answer(invocationOnMock: InvocationOnMock): Unit = ()
    })

  private val route = seal(new CacheHandler(support).route)

  s"DELETE $root" should {
    "invalidate cache" in {

      forAll(userId, list(0, 10, cacheRecord).map(_.toSet)) { (user, records) =>
        val query = records
          .map { record =>
            s"record=${record.plain}"
          }
          .mkString("&")
        Delete(s"$root?user_id=$user&$query")
          .withUser(user)
          .withSomePassportUser ~> route ~> check {
          status should be(StatusCodes.OK)
          verify(support).invalidate(eq(user), eq(records))(?)
        }
      }
    }
  }

}
