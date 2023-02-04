package ru.auto.salesman.service.impl.user

import org.joda.time.DateTime
import ru.auto.salesman.client.BunkerClient
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.cached.HardcodedProductDescriptions
import ru.auto.salesman.service.impl.ProductDescriptionServiceImpl
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.test.model.gens.user.BunkerModelGenerator
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

import scala.concurrent.duration.DurationInt

class ProductDescriptionServiceImplSpec extends BaseSpec with BunkerModelGenerator {

  private val bunkerClient = mock[BunkerClient]
  implicit val rc: RequestContext = AutomatedContext("test")
  private val cacheOutdatedAfter = 31.minutes

  private def outdatedTime = DateTime.now().plus(cacheOutdatedAfter.toMillis)

  private def createService() =
    new ProductDescriptionServiceImpl(bunkerClient)

  "AutoRuBunkerServiceImpl" should {
    "do http request on creation" in {
      forAll(DescriptionsGenerator) { descriptions =>
        (bunkerClient.getDescriptions _)
          .expects()
          .returningZ(descriptions)

        val bunkerServiceNew = createService()
        bunkerServiceNew.get().success
      }
    }

    "not do http request if last request was less than cache expired period" in {
      forAll(DescriptionsGenerator) { descriptions =>
        (bunkerClient.getDescriptions _)
          .expects()
          .once()
          .returningZ(descriptions)

        val bunkerService = createService()

        bunkerService.get().success
        bunkerService.get().success
      }
    }

    "do http request if cache expired" in {
      forAll(DescriptionsGenerator) { descriptions =>
        (bunkerClient.getDescriptions _)
          .expects()
          .twice()
          .returningZ(descriptions)

        val bunkerService = createService()

        bunkerService.get().success
        bunkerService.get().provideConstantClock(outdatedTime).success
      }
    }

    "fallback to hardcoded if http request fails" in {
      (bunkerClient.getDescriptions _)
        .expects()
        .once()
        .throwingZ(new TestException)

      val bunkerService = createService()

      val (hardcoded, result) = (for {
        hardcoded <- HardcodedProductDescriptions.descriptions()
        res <- bunkerService.get()
      } yield (hardcoded, res)).success.value

      result shouldBe hardcoded
    }

    "init fallback on first request and use it after cache expired and bunker error" in {
      forAll(DescriptionsGenerator) { descriptions =>
        (bunkerClient.getDescriptions _)
          .expects()
          .once()
          .returningZ(descriptions)

        val bunkerService = createService()

        (bunkerClient.getDescriptions _)
          .expects()
          .once()
          .throwingZ(new TestException)

        bunkerService
          .get()
          .provideConstantClock(outdatedTime)
          .success
          .value shouldBe descriptions
      }
    }

    "return previous value if cache expired, ask bunker for new and returns it on next request" in {
      val descriptions = DescriptionsGenerator.next

      (bunkerClient.getDescriptions _)
        .expects()
        .once()
        .returningZ(descriptions)

      val bunkerService = createService()

      bunkerService.get().success.value shouldBe descriptions

      val newDescriptions = DescriptionsGenerator.next
      (bunkerClient.getDescriptions _)
        .expects()
        .once()
        .returningZ(newDescriptions)

      bunkerService
        .get()
        .provideConstantClock(outdatedTime)
        .success
        .value shouldBe descriptions

      bunkerService
        .get()
        .success
        .value shouldBe newDescriptions
    }
  }

  implicit override lazy val domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
