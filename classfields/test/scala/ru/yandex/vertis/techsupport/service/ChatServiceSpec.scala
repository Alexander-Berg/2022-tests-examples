package ru.yandex.vertis.vsquality.techsupport.service

import cats.syntax.applicative._
import org.scalacheck.{Arbitrary, Prop}
import org.scalatestplus.scalacheck.Checkers
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.vsquality.techsupport.clients.WispClient
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpVertisChatClient
import ru.yandex.vertis.vsquality.techsupport.model.{ChatProvider, Domain}
import ru.yandex.vertis.vsquality.techsupport.service.impl.ChatServiceImpl
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._

/**
  * @author devreggs
  */
class ChatServiceSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  private val autoruVertisChatClient: HttpVertisChatClient[F] = mock[HttpVertisChatClient[F]]
  private val realtyVertisChatClient: HttpVertisChatClient[F] = mock[HttpVertisChatClient[F]]
  private val wispClient: WispClient[F] = mock[WispClient[F]]
  private val pollFeature = Feature("", _ => "")

  private val chatServiceImpl =
    new ChatServiceImpl[F](autoruVertisChatClient, realtyVertisChatClient, wispClient, pollFeature)

  stub(autoruVertisChatClient.request(_, _, _)) { case (_, _, _) => ().pure[F] }
  stub(realtyVertisChatClient.request(_, _, _)) { case (_, _, _) => ().pure[F] }
  stub(wispClient.request(_)) { case _ => ().pure[F] }

  "VertisChatService" should {
    "sends requests" in {
      Checkers.check(
        Prop.forAll(
          gen[ChatService.Envelope].suchThat(x =>
            x.destination.provider == ChatProvider.VertisChats &&
              (x.clientId.domain == Domain.Autoru || x.clientId.domain == Domain.Realty)
          )
        ) { envelope: ChatService.Envelope =>
          chatServiceImpl.send(envelope).await
          true
        }
      )
    }
  }
}
