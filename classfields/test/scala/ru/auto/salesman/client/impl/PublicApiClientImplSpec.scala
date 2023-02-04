package ru.auto.salesman.client.impl

import ru.auto.salesman.client.PublicApiClient.GetMatchApplicationQuery
import ru.auto.salesman.client.proto.ProtoExecutor
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest.MatchApplicationId
import ru.auto.salesman.model.token.PublicApiToken
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

import java.util.UUID

class PublicApiClientImplSpec
    extends BaseSpec
    with ProtobufSupport
    with ServiceModelGenerators {

  "PublicApiClient" should {

    "pass token to public-api with Vertis prefix" in {
      val executor = mock[ProtoExecutor]
      val testToken = PublicApiToken("test_token")
      val publicApiClient = new PublicApiClientImpl(executor, testToken)
      val anyQuery =
        GetMatchApplicationQuery(
          MatchApplicationId(UUID.randomUUID()),
          clientId = 1
        )
      publicApiClient.resolver
        .headers(anyQuery)
        .get("x-authorization")
        .value shouldBe "Vertis test_token"
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
