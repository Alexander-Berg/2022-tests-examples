package ru.yandex.vertis.passport.service

import org.mockito.Mockito.times
import org.mockito.{Answers, Mockito}
import org.scalatest.{BeforeAndAfter, WordSpec}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryUserEssentialsCache
import ru.yandex.vertis.passport.integration.{CachedUserProviderFallbackToProvider, CachedUserProviderImpl, UserProvider}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.concurrent.Future

/**
  * Tests for [[CachedUserProviderFallbackToProvider]]
  *
  * @author zvez
  */
class CachedUserProviderFallbackToProviderSpec extends WordSpec with SpecBase with MockitoSupport with BeforeAndAfter {

  import scala.concurrent.ExecutionContext.Implicits.global

  val UserId = "1"
  val userProvider = mock[UserProvider](Answers.RETURNS_SMART_NULLS)
  val userDao = Mockito.spy(new InMemoryUserEssentialsCache)
  val service = new CachedUserProviderImpl(userDao, userProvider) with CachedUserProviderFallbackToProvider

  after {
    Mockito.verifyNoMoreInteractions(userProvider)
    Mockito.reset(userProvider)
  }

  "UserServiceFallbackToProvider" should {
    "not fallback when user was not found" in {
      when(userProvider.get(eq(UserId), ?, ?)(?)).thenReturn(Future.failed(new NoSuchElementException))
      service.get(UserId).failed.futureValue shouldBe a[NoSuchElementException]
      Mockito.verify(userProvider, times(1)).get(eq(UserId), ?, ?)(?)
    }

    "not interfere if everything works" in {
      val user = ModelGenerators.userEssentials.next
      when(userProvider.get(eq(UserId), ?, ?)(?)).thenReturn(Future.successful(user))
      service.get(UserId).futureValue shouldBe user
      Mockito.verify(userProvider, times(1)).get(eq(UserId), ?, ?)(?)
    }

    "fallback to provider when storage is not available" in {
      val user = ModelGenerators.userEssentials.next
      when(userProvider.get(eq(UserId), ?, ?)(?)).thenReturn(Future.successful(user))
      when(userDao.get(UserId))
        .thenReturn(Future.failed(new RuntimeException("storage is down")))
      service.get(UserId).futureValue shouldBe user
      Mockito.verify(userProvider, times(1)).get(eq(UserId), ?, ?)(?)
    }
  }

}
