package ru.yandex.vertis.general.feed.transformer.integration_test

case class TestCase(originalFile: String, transformedResult: String, description: String)

object TestCases {

  val TestCases = List(
    TestCase("invalid-format.csv", "invalid-format.csv-04e28cfd9854750c38361577de520ee0", "Фид с неизвестным форматом"),
    TestCase(
      "invalid-general.xml",
      "invalid-general.xml-c1518edb18cead68c54760862ed3a624",
      "Фид Я.Объявлений с ошибками"
    ),
    TestCase("valid-avito.xml", "valid-avito.xml-172bfb046321646d4922142b6efe9570", "Фид Авито без ошибок"),
    TestCase("valid-general.xml", "valid-general.xml-db39a4ec4d511703ec51bcda98763a8b", "Фид Я.Объявлений без ошибок")
  )
}
