package ru.yandex.auto.vin.decoder.hydra

import auto.carfax.common.clients.hydra.HydraClient
import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.{times, verify}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.partners.audatex.AudatexError
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AudatexClickerSpec extends AnyWordSpecLike with MockitoSupport {

  implicit val t = Traced.empty
  private val client = mock[HydraClient]
  private val clicker = new AudatexClicker("", 20000, client)

  "AudatexClicker" should {

    "not click on hydra on invalid user error" in {
      when(client.getClicker(?, ?)(?)).thenReturn(Future.successful(0))
      val error = new AudatexError("B2BService.UserNotAuthenticated", "Invalid user or password")
      intercept[AudatexError] {
        clicker.limitedFutureAction(Future.failed(error)).await
      }
      verify(client, times(0)).incClicker(?, ?)(?)
    }

    "do click on hydra on error" in {
      when(client.getClicker(?, ?)(?)).thenReturn(Future.successful(0))
      when(client.incClicker(?, ?)(?)).thenReturn(Future.successful(()))
      val error = new Exception("Any other error")
      intercept[Exception] {
        clicker.limitedFutureAction(Future.failed(error)).await
      }
      verify(client, times(1)).incClicker(?, ?)(?)
    }
  }
}
