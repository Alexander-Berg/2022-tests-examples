package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.model.{AutoruCreditApplication, CreditProductId, SenderConverterContext}
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.scheduler.sender.TestCases.TestCaseContext
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.util.RichModel.RichCreditApplication
import ru.yandex.vertis.shark.{TestSharedConfig, TmsStaticSamples}
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion.isSome
import zio.test.TestAspect.{ignore, sequential}
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import scala.annotation.nowarn

trait TestCreditApplicationBankSender extends DefaultRunnableSpec with TestLayers with TestCases {

  protected def ignoreAll: Boolean = true

  protected def creditProduct: CreditProductId

  override lazy val config: TestSharedConfig = TestSharedConfig.local

  override def spec: ZSpec[TestEnvironment, Any] =
    suite(s"$creditProduct")(suiteSend) @@ (if (ignoreAll) ignore else sequential)

  protected def sampleCreditApplication: AutoruCreditApplication = TmsStaticSamples.sampleCreditApplication

  private def suiteSend: ZSpec[TestEnvironment, Any] = {
    val tests = testCases
      .filterNot(_.isIgnore)
      .map(TestCaseContext(_, sampleCreditApplication, creditProduct))
      .map(testSend)
    val label = s"$creditProduct.send"
    suite(label)(tests: _*)
  }

  def testSend(testContext: TestCaseContext): ZSpec[TestEnvironment, Any] =
    testM(testContext.test.name) {
      val res = for {
        decider <- ZIO.service[ConverterContextDecider.Service]
        sender <- ZIO.service[CreditApplicationBankSender.Service]
        clock <- ZIO.service[Clock.Service]
        time <- clock.instant
        ca = testContext.test.builder(testContext.ca)
        claim = ca.getClaimByCreditProductId(testContext.creditProduct).get
        converterContext <- decider.source(ca, time)
        context = SenderConverterContext.forTest(converterContext.origin)
        res <- sender.send(claim.id)(context)
      } yield {
        println(s"${testContext.test.name}: $res")
        res
      }
      testContext.test.assertion(res).provideLayer(testLayer)
    }

  // @todo not used in this version
  @nowarn("cat=unused-privates")
  private def testDecision(ca: AutoruCreditApplication): ZSpec[TestEnvironment, Any] =
    testM("decision") {
      val result = for {
        sender <- ZIO.service[CreditApplicationBankSender.Service]
        claim = ca.getClaimByCreditProductId(creditProduct).get
        res <- sender.check(ca, claim.id)
      } yield {
        println(s"decision res: $res")
        res
      }
      assertM(result)(isSome).provideLayer(testLayer)
    }
}
