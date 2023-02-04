package auto.dealers.application.api.test

import ru.auto.api.api_offer_model.{Category, Section}
import auto.dealers.application.api.{ApplicationService, ApplicationServiceLive}
import ru.auto.application.proto.model.Application
import auto.dealers.application.scheduler.test.kafka.CreditApplicationDictionaryServiceTest
import zio.test.Assertion._
import zio.test._

object ApplicationServiceLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("ApplicationServiceLive")(
      testM("save credit application") {
        assertM(ApplicationService.saveApplication(application).run)(succeeds(anything))
          .provideCustomLayer(
            CreditApplicationDictionaryServiceTest.layer >>> ApplicationServiceLive.live
          )
      },
      testM("not fail on duplicate key on insert") {
        val result = for {
          _ <- ApplicationService.saveApplication(application)
          _ <- ApplicationService.saveApplication(application)
        } yield ()

        assertM(result.run)(succeeds(anything))
          .provideCustomLayer(
            CreditApplicationDictionaryServiceTest.layer >>> ApplicationServiceLive.live
          )
      }
    )

  }

  val application = Application(
    123,
    20101,
    "dealer",
    "bmw",
    "x6",
    0,
    2020,
    "1234-567",
    "auto.ru/cars/new/1234-567",
    category = Category.CARS,
    section = Section.NEW
  ).withCredit(
    Application.ApplicationCredit(
      1000000,
      12,
      100000,
      100000,
      "Test Testov",
      "test@auto.ru",
      "8-800-555-35-35"
    )
  )
}
