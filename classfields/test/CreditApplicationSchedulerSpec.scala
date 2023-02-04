package ru.yandex.vertis.shark.controller.scheduler

import com.softwaremill.quicklens._
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import ru.yandex.vertis.shark.model.AutoruCreditApplication
import ru.yandex.vertis.shark.proto.model.CreditApplication.State
import ru.yandex.vertis.shark.util.sampleAutoruCreditApplciation
import ru.yandex.vertis.zio_baker.util.DateTimeUtil.RichInstant
import zio.test.Assertion._
import zio.test._

object CreditApplicationSchedulerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CreditApplicationScheduler")(
      suite("scheduleAt should")(
        suite("set scheduleAt for the given CreditApplication")(
          testM("to updated time for active state") {
            val creditApplication = setAutoruCreditApplicationActiveState(sampleAutoruCreditApplciation())

            val testZ = for {
              scheduledCreditApplication <- scheduleAt(draftScheduleAfter)(creditApplication)
            } yield assert(scheduledCreditApplication.scheduledAt)(isSome(equalTo(scheduledCreditApplication.updated)))

            testZ.provideLayer(serviceToTestLayer)
          },
          testM("to updated time for cancelled state") {
            val creditApplication = setAutoruCreditApplicationCancelledState(sampleAutoruCreditApplciation())

            val testZ = for {
              scheduledCreditApplication <- scheduleAt(draftScheduleAfter)(creditApplication)
            } yield assert(scheduledCreditApplication.scheduledAt)(isSome(equalTo(scheduledCreditApplication.updated)))

            testZ.provideLayer(serviceToTestLayer)
          },
          testM("to updated time + draftScheduleAfter for draft state") {
            val creditApplication = setAutoruCreditApplicationDraftState(sampleAutoruCreditApplciation())

            val testZ =
              for {
                scheduledCreditApplication <- scheduleAt(draftScheduleAfter)(creditApplication)
              } yield assert(scheduledCreditApplication.scheduledAt)(
                isSome(equalTo(scheduledCreditApplication.updated.plusDuration(draftScheduleAfter)))
              )

            testZ.provideLayer(serviceToTestLayer)
          }
        ),
        testM("fail for unknown state") {
          val creditApplication = setAutoruCreditApplicationUnknownState(sampleAutoruCreditApplciation())

          val testZ =
            for {
              runResult <- scheduleAt(draftScheduleAfter)(creditApplication).run
            } yield assert(runResult)(fails(anything))

          testZ.provideLayer(serviceToTestLayer)
        }
      )
    )

  private val serviceToTestLayer = CreditApplicationScheduler.live
  private val draftScheduleAfter = 5.minutes

  private val modifyAutoruCreditApplicationState = modifyLens[AutoruCreditApplication](_.state)
  private val setAutoruCreditApplicationActiveState = modifyAutoruCreditApplicationState.setTo(State.ACTIVE)
  private val setAutoruCreditApplicationCancelledState = modifyAutoruCreditApplicationState.setTo(State.CANCELED)
  private val setAutoruCreditApplicationDraftState = modifyAutoruCreditApplicationState.setTo(State.DRAFT)
  private val setAutoruCreditApplicationUnknownState = modifyAutoruCreditApplicationState.setTo(State.UNKNOWN_STATE)
}
