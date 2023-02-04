package ru.auto.salesman.test.model.gens

import java.util.UUID

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest.Statuses.New
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest.{
  MatchApplicationId,
  Status
}
import ru.auto.salesman.model.{AutoruUser, ClientId, Funds}
import ru.yandex.vertis.generators.{
  BasicGenerators,
  DateTimeGenerators,
  ProtobufGenerators
}

trait MatchApplicationGenerators
    extends BasicGenerators
    with ProtobufGenerators
    with DateTimeGenerators
    with OfferModelGenerators
    with BasicSalesmanGenerators {

  def MatchApplicationIdGen: Gen[MatchApplicationId] =
    MatchApplicationId(UUID.randomUUID())

  val MatchApplicationCreateRequestGen: Gen[MatchApplicationCreateRequest] =
    for {
      clientId <- ClientIdGen
      userId <- UserIdGen
      matchApplicationId <- MatchApplicationIdGen
      clientOfferId <- AutoruOfferIdGen
      clientOfferCategory <- OfferCategoryKnownGen
      clientOfferSection <- OfferSectionKnownGen

    } yield
      MatchApplicationCreateRequest(
        clientId,
        AutoruUser(userId),
        matchApplicationId,
        clientOfferId,
        clientOfferCategory,
        clientOfferSection,
        billingStatus = New,
        createDate = now(),
        isRead = false
      )

}

object MatchApplicationGenerators extends MatchApplicationGenerators {

  implicit class RichMatchApplicationCreateRequestGen(
      private val gen: Gen[MatchApplicationCreateRequest]
  ) extends AnyVal {

    def withClientId(clientId: ClientId): Gen[MatchApplicationCreateRequest] =
      gen.map(_.copy(clientId = clientId))

    def withUserId(userId: AutoruUser): Gen[MatchApplicationCreateRequest] =
      gen.map(_.copy(userId = userId))

    def withMatchApplicationId(
        id: MatchApplicationId
    ): Gen[MatchApplicationCreateRequest] =
      gen.map(_.copy(matchApplicationId = id))

    def withCategoryAndSection(
        category: Category,
        section: Section
    ): Gen[MatchApplicationCreateRequest] =
      gen.map(
        _.copy(clientOfferCategory = category, clientOfferSection = section)
      )

    def withBillingStatus(
        status: Status,
        cost: Funds = 0
    ): Gen[MatchApplicationCreateRequest] =
      gen.map(_.copy(billingStatus = status, billingPrice = cost))

    def withCreateDate(
        createDate: DateTime
    ): Gen[MatchApplicationCreateRequest] =
      gen.map(_.copy(createDate = createDate))

    def withIsRead(isRead: Boolean): Gen[MatchApplicationCreateRequest] =
      gen.map(_.copy(isRead = isRead))
  }

}
