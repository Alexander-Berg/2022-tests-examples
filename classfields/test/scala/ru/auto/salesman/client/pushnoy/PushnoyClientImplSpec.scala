package ru.auto.salesman.client.pushnoy

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{headerValueByName, _}
import ru.auto.salesman.client.json.JsonExecutorImpl
import ru.auto.salesman.client.pushnoy.PushBodyConverter._
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

class PushnoyClientImplSpec
    extends BaseSpec
    with UserModelGenerators
    with PushnoyModelGenerator {
  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext("test")

  "Pushnoy client" should {
    "pushToUser should use right route with delivery in params" in {
      forAll(
        AutoruUserGen,
        pushTemplateV1Gen,
        ToPushDeliveryGeneratorWithDelivery,
        PushResponseGen
      ) { (user, templateGenerated, paramsGenerated, response) =>
        val serverAddress = runServer {
          (post & headerValueByName("Accept") & pathPrefix(
            "api" / "v1" / "auto" / "user" / user.toString / "push"
          ) & parameter("target") & pathPrefix(
            paramsGenerated.delivery.get.entryName
          ) & pathEnd &
            entity(as[ToSendTemplate])) { (acceptHeader, param, ent) =>
            acceptHeader shouldBe "application/json"
            param shouldBe "devices"

            ent.payload.action shouldBe "deeplink"

            complete(response)
          } ~ {
            complete(throw new Exception("Didn't match route"))
          }
        }

        val executor = new JsonExecutorImpl(serverAddress.toString)

        val pushnoyClient = new PushnoyClientImpl(executor)

        val params = paramsGenerated.copy(userId = user.toString)

        pushnoyClient
          .pushToUser(templateGenerated, params)
          .success
          .value shouldBe response.count
      }
    }

    "pushToUser should use right route without delivery in params" in {
      forAll(
        AutoruUserGen,
        pushTemplateV1Gen,
        ToPushDeliveryGeneratorWithoutDelivery,
        PushResponseGen
      ) { (user, templateGenerated, paramsGenerated, response) =>
        val serverAddress = runServer {
          (post & headerValueByName("Accept") & pathPrefix(
            "api" / "v1" / "auto" / "user" / user.toString / "push"
          ) & parameter("target") & pathEnd &
            entity(as[ToSendTemplate])) { (acceptHeader, param, ent) =>
            acceptHeader shouldBe "application/json"
            param shouldBe "devices"

            ent.payload.action shouldBe "deeplink"

            complete(response)
          } ~ {
            complete(throw new Exception("Didn't match route"))
          }
        }

        val executor = new JsonExecutorImpl(serverAddress.toString)

        val pushnoyClient = new PushnoyClientImpl(executor)

        val params = paramsGenerated.copy(userId = user.toString)

        pushnoyClient
          .pushToUser(templateGenerated, params)
          .success
          .value shouldBe response.count
      }
    }
  }

}
