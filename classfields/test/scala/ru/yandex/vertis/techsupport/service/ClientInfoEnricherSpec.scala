package ru.yandex.vertis.vsquality.techsupport.service

import cats.instances.try_._
import com.softwaremill.tagging._
import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.techsupport.model.{ClientInfo, Tags, UserId}
import ru.yandex.vertis.vsquality.techsupport.service.enricher.CompositeClientInfoEnricher

import scala.util.{Success, Try}

/**
  * @author devreggs
  */
class ClientInfoEnricherSpec extends SpecBase {

  private val user = UserId.Client.Autoru.PrivatePerson(47549791L.taggedWith[Tags.AutoruPrivatePersonId])

  private val clientInfo = generate[ClientInfo]()

  private val nameEnricher: ClientInfoEnricher[Try] =
    (clientInfo: ClientInfo) => Success(clientInfo.copy(userName = Some("name1")))

  private val nameEnricher2: ClientInfoEnricher[Try] =
    (clientInfo: ClientInfo) => Success(clientInfo.copy(userName = Some("name2")))

  private val enricher = new CompositeClientInfoEnricher[Try](Seq(nameEnricher, nameEnricher2))

  "CompositeClientInfoEnricher" should {
    "enrich sequentually" in {
      enricher.enrich(ClientInfo.empty(user)) shouldBe Success(ClientInfo.empty(user).copy(userName = Some("name2")))
    }

    "empty enrich in empty enricher" in {
      ClientInfoEnricher.empty(catsStdInstancesForTry).enrich(clientInfo) shouldBe Success(clientInfo)
    }
  }

}
