package ru.yandex.vertis.vsquality.techsupport.service

import cats.Monad
import cats.data.OptionT
import cats.syntax.applicative._
import ru.yandex.vertis.vsquality.techsupport.clients.BunkerClient
import ru.yandex.vertis.vsquality.techsupport.model.Domain
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import com.softwaremill.tagging._
import ru.yandex.vertis.vsquality.techsupport.service.impl.BunkerBanReasonsService
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import io.circe.parser._
import org.mockito.stubbing.OngoingStubbing

/**
  * @author devreggs
  */
class BanReasonsServiceSpec extends SpecBase {

  private val content = parse("{\t\"offer_editable\": true}").toOption
  private val client: BunkerClient[F] = mock[BunkerClient[F]]
  when(client.getContent(?)).thenReturn(Monad[F].pure(content))

  when(client.list(?, ?, ?))
    .thenReturn(Seq(BunkerClient.NodeInfo("node".taggedWith[BunkerClient.BunkerNodeTag], isDeleted = false)).pure)
  private val banReasonsService = new BunkerBanReasonsService[F](client)

  "BanReasonsService" should {
    "returns autoru reasons if not deleted" in {
      banReasonsService.getReasons(Domain.Autoru).await.size should be > 0
    }
  }
}
