package ru.yandex.vertis.shark.client.saturn.converter

import cats.implicits.catsSyntaxOptionId
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.saturn.SaturnClient
import ru.yandex.vertis.shark.client.saturn.SaturnClient.SearchRequest._
import ru.yandex.vertis.test_utils.assertions.Assertions
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.LocalDate

object SearchRequestBuilderSpec extends DefaultRunnableSpec with StaticSamples with Assertions.DiffSupport {

  private val requestId: String = "some-request-id"
  private val formulaId: String = "some-formula"

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("SearchRequestBuilder")(
      testM("build") {
        val expected = SaturnClient.SearchRequest(
          requestId = requestId,
          service = "auto_ru",
          formulaId = formulaId.some,
          autoid = 17830914,
          phone = "79267010001".some,
          email = "vasyivanov@yandex.ru".some,
          fio = "Иванов Василий".some,
          numChildren = NumChildren.One.some,
          passportNum = "1234506789".some,
          passportIssueDate = LocalDate.of(2005, 2, 2).some,
          dateOfBirth = LocalDate.of(1982, 11, 25).some,
          placeOfBirth = "Россия Самарская обл г Тольятти".some,
          isSurnameChange = true.some,
          prevSurname = "Петров".some,
          addressRegister = "Самарская обл, г Тольятти, Обводное шоссе, д 3".some,
          addressLiving = "г Москва, пр-кт Вернадского, д 99, корп 1".some,
          jobType = JobType.Employee.some,
          jobPositionType = JobPositionType.ItSpecialist.some,
          workExperience = WorkExperience.FiveToSevenYears.some,
          monthlyIncome = 100000.some,
          incomeConfirmation = IncomeConfirmation.By2Ndfl.some,
          additionalPhone = "79267010002".some,
          additionalPhoneType = None,
          education = Education.IncompleteHigher.some,
          maritalStatus = MaritalStatus.Divorced.some,
          rentalType = RentalType.PayRent.some
        )
        val actual =
          SearchRequestBuilder.build(sampleCreditApplication, requestId = requestId.some, formulaId = formulaId.some)
        assertM(actual)(noDiff(expected))
      }
    )
}
