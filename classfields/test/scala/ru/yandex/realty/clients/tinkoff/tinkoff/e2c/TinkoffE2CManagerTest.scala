package ru.yandex.realty.clients.tinkoff.tinkoff.e2c

import org.junit.runner.RunWith
import org.scalatest.AsyncFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.tinkoff.e2c.enums.CardStatus
import ru.yandex.realty.clients.tinkoff.e2c.response.Card
import ru.yandex.realty.clients.tinkoff.e2c.{TinkoffE2CClient, TinkoffE2CManager}
import ru.yandex.realty.rent.proto.api.user.OwnerCardsStatusNamespace.OwnerCardsStatus
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class TinkoffE2CManagerTest extends AsyncFunSuite with MockitoSupport {
  implicit val traced: Traced = Traced.empty
  val tinkoffE2CClient: TinkoffE2CClient = mock[TinkoffE2CClient]
  val tinkoffE2CManager = new TinkoffE2CManager(tinkoffE2CClient)

  test("getOwnerCardsStatus") {
    when(tinkoffE2CClient.getCardList(?)(?)).thenReturn(
      Future.successful(
        List(
          Card("", "", "", CardStatus.Active, 7, None),
          Card("", "", "", CardStatus.Expired, 7, None),
          Card("", "", "", CardStatus.Inactive, 7, None),
          Card("", "", "", CardStatus.Deleted, 7, None)
        )
      )
    )

    tinkoffE2CManager.getOwnerCardsStatus("", hasActualRequests = false)(Traced.empty).map { status =>
      assert(status == OwnerCardsStatus.SEVERAL_CARDS_BOUND)
    }
  }
}
