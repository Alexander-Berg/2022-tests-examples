package ru.auto.cabinet.test.gens

import org.scalacheck.Gen
import ru.auto.cabinet.test.gens.DateTimeGens._
import ru.auto.cabinet.test.gens.GeobaseGens._
import ru.auto.cabinet.model._

object ClientGens {

  val clientIdGen: Gen[ClientId] = Gen.posNum[ClientId]

  private val agencyIdGen: Gen[ClientId] = Gen.posNum[ClientId]

  private val originIdGen: Gen[OriginId] = Gen.alphaStr

  private val clientStatusGen: Gen[ClientStatus] =
    Gen.oneOf(ClientStatuses.values.toSeq)

  private val addressGen: Gen[String] = Gen.alphaStr

  private val websiteGen: Gen[Option[String]] = Gen.option(Gen.alphaStr)

  private val emailGen: Gen[String] = Gen.alphaStr

  private val clientPropertiesGen: Gen[ClientProperties] = for {
    regionId <- regionIdGen
    cityId <- cityIdGen
    originId <- originIdGen
    status <- clientStatusGen
    epoch <- offsetDateTimeGen
    address <- addressGen
    website <- websiteGen
    email <- emailGen
    managerEmail <- Gen.option(emailGen)
    firstModerationDate <- Gen.option(offsetDateTimeGen)
    createdDate <- Gen.option(offsetDateTimeGen)
    multipostingEnabled <- Gen.oneOf(true, false)
    firstModerated <- Gen.oneOf(true, false)
    isAgent <- Gen.oneOf(true, false)
  } yield ClientProperties(
    regionId,
    cityId,
    originId,
    status,
    epoch,
    address,
    website,
    email,
    managerEmail,
    firstModerationDate,
    createdDate,
    multipostingEnabled,
    firstModerated = firstModerated,
    isAgent = isAgent
  )

  def clientGen(clientIdGen: Gen[ClientId] = clientIdGen): Gen[Client] =
    for {
      clientId <- clientIdGen
      agencyId <- agencyIdGen
      properties <- clientPropertiesGen
    } yield Client(clientId, agencyId, properties)
}
