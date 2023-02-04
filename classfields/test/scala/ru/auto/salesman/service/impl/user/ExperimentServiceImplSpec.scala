package ru.auto.salesman.service.impl.user

import org.scalatest.BeforeAndAfter
import ru.auto.salesman.Task
import ru.auto.salesman.client.uaas.UaasClient
import ru.auto.salesman.model._
import ru.auto.salesman.model.user.{Experiment, Experiments}
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.CryptoFunctions
import ru.yandex.vertis.scalatest.BetterTryValues

class ExperimentServiceImplSpec
    extends DeprecatedMockitoBaseSpec
    with UserModelGenerators
    with BetterTryValues
    with BeforeAndAfter {

  import ExperimentServiceImplSpec._

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  private val uaasClient = mock[UaasClient]

  private val service = new ExperimentServiceImpl(
    uaasClient
  )

  "ExperimentServiceImpl" should {
    "return success value from UaasClient " in {
      val result = Some(
        Experiments(
          testBoxes,
          List[Experiment](
            Experiment(
              "experiment_id",
              geoIds = None,
              experimentProducts = None
            )
          )
        )
      )
      when(uaasClient.getExperiments(testUserHash))
        .thenReturnZ(result)

      service.getExperimentsForUser(testUser).success.value shouldBe result

    }

    "handle error from uaasClient" in {
      when(uaasClient.getExperiments(testUserHash))
        .thenReturn(Task.fail(new Exception("test exception")))
      service
        .getExperimentsForUser(testUser)
        .success
        .value shouldBe None
    }

  }
}

object ExperimentServiceImplSpec {
  private val testUser = AutoruUser(42)

  private val testUserHash = CryptoFunctions.md5(testUser.id.toString)

  private val testBoxes = "testBoxes"
}
