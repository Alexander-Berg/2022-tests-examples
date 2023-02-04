package ru.yandex.realty.cadastr.util

import org.scalatest.WordSpecLike
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.cadastr.backend.ExcerptStringHelper
import ru.yandex.realty.cadastr.proto.registry.Person

@RunWith(classOf[JUnitRunner])
class ExcerptStringHelperSpec extends WordSpecLike {

  "ExcerptStringHelper" should {

    "disguise person name" in {
      testPersonName("И***** И.И.", "Иванов Иван Иванович", "Иванов", "Иван", "Иванович")
      testPersonName("С****** Д.В.", "Сидоров Денис Васильевич", "Сидоров", "Денис", "Васильевич")
      testPersonName("Б***** Н.", "Басков Николай", "Басков", "Николай")
    }

    "disguise cadastral number" in {
      testCadastralNumber("50:45:0000000:*****", "50:45:0000000:62178")
      testCadastralNumber("50:25:0130202:**", "50:25:0130202:40")
      testCadastralNumber("78:42:0018304:****", "78:42:0018304:3023")
    }

    "disguise reg number" in {
      testRegNumber(
        "50:45:0000000:*****-50/001/2019-3",
        "50:45:0000000:62178-50/001/2019-3",
        "50:45:0000000:62178"
      )
      testRegNumber(
        "77-77/009-77/009/288/2015-13***",
        "77-77/009-77/009/288/2015-136/4",
        "77:09:0001014:7421"
      )

      testRegNumber(
        "12345***",
        "12345678",
        "50:45:0000000:62178"
      )
      testRegNumber(
        "**",
        "12",
        "77:09:0001014:7421"
      )
    }

    "check valid cadastral number" in {
      testCadastralNumberValidation("50:45:0000000:62178", isValid = true)
      testCadastralNumberValidation("77:09:0001014:7421", isValid = true)
      testCadastralNumberValidation("50:25:0130202:40", isValid = true)
      testCadastralNumberValidation("47:13:1201004:209", isValid = true)
      testCadastralNumberValidation("75:24:240201:76", isValid = true)

      testCadastralNumberValidation("50:45:0000000", isValid = false)
      testCadastralNumberValidation("0000000:62178", isValid = false)
      testCadastralNumberValidation("1", isValid = false)
      testCadastralNumberValidation("-", isValid = false)
      testCadastralNumberValidation("symbols", isValid = false)
      testCadastralNumberValidation("27-27-01/100/2011-141", isValid = false)
      testCadastralNumberValidation("52:18:0070254:0:18/22", isValid = false)
      testCadastralNumberValidation("50:45:0000000:6217812", isValid = false)
    }
  }

  private def testPersonName(
    expectedMaskedName: String,
    expectedHiddenName: String,
    surname: String = "",
    firstName: String = "",
    patronymic: String = ""
  ): Unit = {
    val person = buildPerson(surname, firstName, patronymic)
    val resultNames = ExcerptStringHelper.disguisePersonName(person)

    assertEquals(expectedMaskedName, resultNames.maskedName)
    assertEquals(expectedHiddenName, resultNames.hiddenName)
  }

  private def buildPerson(surname: String, firstName: String, patronymic: String): Person =
    Person
      .newBuilder()
      .setFamilyName(surname)
      .setFirstName(firstName)
      .setPatronymic(patronymic)
      .build()

  private def testCadastralNumber(expectedResult: String, cadastralNumber: String) {
    val disguisedCadastralNumber = ExcerptStringHelper.disguiseCadastralNumber(cadastralNumber)
    assertEquals(expectedResult, disguisedCadastralNumber)
  }

  private def testRegNumber(expectedResult: String, regNumber: String, cadastralNumber: String) {
    val disguisedRegNumber = ExcerptStringHelper.disguiseRegNumber(regNumber, cadastralNumber)
    assertEquals(expectedResult, disguisedRegNumber)
  }

  private def testCadastralNumberValidation(string: String, isValid: Boolean) {
    val result = ExcerptStringHelper.isValidCadastralNumber(string)
    assertEquals(isValid, result)
  }
}
