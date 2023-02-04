package ru.yandex.vertis.passport.scheduler.tasks

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryMarkerDao
import ru.yandex.vertis.passport.integration.email.{EmailTemplates, TemplatedLetter}
import ru.yandex.vertis.passport.model.{FullUser, UserId}
import ru.yandex.vertis.passport.scheduler.AkkaSupport
import ru.yandex.vertis.passport.service.communication.UserCommunicationService
import ru.yandex.vertis.passport.service.user.UserBackendService
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

/**
  *
  * @author zvez
  */
class PasswordExpireTaskSpec extends WordSpec with Matchers with ScalaFutures with MockitoSupport with AkkaSupport {

  import MockitoSupport.{eq => eqm}

  implicit val mat = ActorMaterializer()

  val PasswordTtl = 90.days

  trait Test {
    val userService = mock[UserBackendService]
    val commService = mock[UserCommunicationService]

    val markerDao = new InMemoryMarkerDao
    val emailTemplates = new EmailTemplates("zzz")

    def expireFrom: DateTime = DateTime.now.minusDays(365)

    private val tracing = LocalTracingSupport(EndpointConfig.Empty)

    def runTask(user: FullUser): Unit = {
      when(userService.get(eqm(user.id))(?)).thenReturn(Future.successful(user))
      val task = new PasswordExpireTask(
        userService,
        commService,
        markerDao,
        emailTemplates,
        PasswordTtl,
        () => Source[UserId](collection.immutable.Iterable(user.id)),
        expireFrom,
        Set.empty,
        tracing
      )
      task.run().futureValue
    }
  }

  "PasswordExpireTask" should {
    "not do anything if user's password is ok" in new Test {
      val user = ModelGenerators.legacyUser.next.copy(
        passwordDate = Some(DateTime.now)
      )

      runTask(user)

      verify(userService, VerificationModeFactory.atMost(0))
        .markPasswordExpired(?)(?)
      verifyNoMoreInteractions(commService)
    }

    "send notification about password expires soon" in new Test {
      val user = ModelGenerators.legacyUser.next.copy(
        passwordDate = Some(DateTime.now.minusDays(PasswordTtl.toDays.toInt - 3))
      )
      when(commService.sendEmailToUser(?, ?)(?)).thenReturn(Future.successful(true))

      runTask(user)

      verify(userService, VerificationModeFactory.atMost(0))
        .markPasswordExpired(?)(?)
      verify(commService).sendEmailToUser(eqm(user), argThat[TemplatedLetter] { lt =>
        lt.templateName == "passport.password_reminder" && lt.args("days") == 3.toString
      })(?)
    }

    "not send the same notification twice" in new Test {
      val user = ModelGenerators.legacyUser.next.copy(
        passwordDate = Some(DateTime.now.minusDays(PasswordTtl.toDays.toInt - 3))
      )
      when(commService.sendEmailToUser(?, ?)(?)).thenReturn(Future.successful(true))

      runTask(user)

      verify(userService, VerificationModeFactory.atMost(0))
        .markPasswordExpired(?)(?)
      verify(commService).sendEmailToUser(eqm(user), ?)(?)

      runTask(user)

      verify(userService, VerificationModeFactory.atMost(0))
        .markPasswordExpired(?)(?)
      verifyNoMoreInteractions(commService)
    }

    "mark password expired" in new Test {
      val user = ModelGenerators.legacyUser.next.copy(
        passwordDate = Some(DateTime.now.minusDays(PasswordTtl.toDays.toInt + 1))
      )
      when(commService.sendEmailToUser(?, ?)(?)).thenReturn(Future.successful(true))
      when(userService.markPasswordExpired(?)(?)).thenReturn(Future.unit)

      runTask(user)

      verify(userService).markPasswordExpired(eqm(user.id))(?)
      verifyNoMoreInteractions(commService)
    }

    "use different expire date" in new Test {
      override val expireFrom = DateTime.now.plusDays(8)

      val user = ModelGenerators.legacyUser.next.copy(
        passwordDate = Some(DateTime.now.minusDays(PasswordTtl.toDays.toInt + 10))
      )
      when(commService.sendEmailToUser(?, ?)(?)).thenReturn(Future.successful(true))

      runTask(user)

      verify(userService, VerificationModeFactory.atMost(0))
        .markPasswordExpired(?)(?)
    }
  }

}
