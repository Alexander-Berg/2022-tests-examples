package ru.yandex.vertis.general.search.logic.test.extractors

import ru.yandex.vertis.general.search.logic.extractors.MultiTokenExtractor
import ru.yandex.vertis.general.search.model.Misspell
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object MultiTokenExtractorTest extends DefaultRunnableSpec {

  private def runTest(initialText: String, expectedTokens: Seq[String]) =
    for {
      tokens <- MultiTokenExtractor.extract(initialText)
    } yield assert(tokens)(hasSameElements(expectedTokens))

  private def runStickTokensTest(initialText: String, expectedMisspell: Option[String]) =
    for {
      misspellOpt <- MultiTokenExtractor.stickTokens(initialText)
    } yield assert(misspellOpt)(equalTo(expectedMisspell.map(Misspell(initialText, _))))

  private def combine(first: ZIO[Any, Nothing, TestResult], rest: ZIO[Any, Nothing, TestResult]*) = {
    rest.fold(first) { (a, b) =>
      a.zip(b).map(result => result._1 && result._2)
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("MultiTokenExtractor")(
      testM("Цифры, затем английская буква") {
        combine(
          runTest(
            initialText = "Honor 7a pro",
            expectedTokens = Seq("7а")
          ),
          runTest(
            initialText = "Xiaomi mi 10t",
            expectedTokens = Seq("10т")
          )
        )
      },
      testM("Цифры, затем русская буква") {
        combine(
          runTest(
            initialText = "Honor 7а про",
            expectedTokens = Seq("7a")
          ),
          runTest(
            initialText = "Xiaomi ми 10т",
            expectedTokens = Seq("10t")
          )
        )
      },
      testM("Английская буква, затем цифры") {
        combine(
          runTest(
            initialText = "Samsung Galaxy A52",
            expectedTokens = Seq("а52")
          ),
          runTest(
            initialText = "Щётка Xiaomi T300",
            expectedTokens = Seq("т300")
          )
        )
      },
      testM("Русская буква, затем цифры") {
        combine(
          runTest(
            initialText = "Samsung Galaxy А52",
            expectedTokens = Seq("a52")
          ),
          runTest(
            initialText = "Щётка Xiaomi Т300",
            expectedTokens = Seq("t300")
          )
        )
      },
      testM("2 буквы подряд") {
        combine(
          runTest(
            initialText = "BenQ-Siemens EL71",
            expectedTokens = Seq("ел71")
          ),
          runTest(
            initialText = "айфон 128гб",
            expectedTokens = Seq("128gb")
          )
        )
      },
      testM("Буквы, цифры, буквы>") {
        combine(
          runTest(
            initialText = "Sony Ericsson W710k",
            expectedTokens = Seq("в710к")
          ),
          runTest(
            initialText = "Сони Эриксон К750и",
            expectedTokens = Seq("k750i")
          )
        )
      },
      testM("Цифры, буквы, цифры") {
        combine(
          runTest(
            initialText = "Hitachi Deskstar 7K3000",
            expectedTokens = Seq("7к3000")
          ),
          runTest(
            initialText = "DAYCO 8ПК1675",
            expectedTokens = Seq("8pk1675")
          )
        )
      },
      testM("Транслитерация пары букв") {
        combine(
          runTest(
            initialText = "sho4",
            expectedTokens = Seq("шо4")
          ),
          runTest(
            initialText = "ч10",
            expectedTokens = Seq("ch10")
          )
        )
      },
      testM("Многозначный транслит") {
        combine(
          runTest(
            initialText = "gws850ce bosch",
            expectedTokens = Seq("гвс850се", "гвс850це", "жвс850се", "жвс850це")
          ),
          runTest(
            initialText = "Турбина для Audi Q5",
            expectedTokens = Seq("ку5", "кью5")
          ),
          runTest(
            initialText = "BMW X7",
            expectedTokens = Seq("х7", "икс7")
          ),
          runTest(
            initialText = "Наушники i7s TWS",
            expectedTokens = Seq("и7с", "ай7с")
          ),
          runTest(
            initialText = "Айфон 4с",
            expectedTokens = Seq("4c", "4s")
          ),
          runTest(
            initialText = "Крыша БМВ х5",
            expectedTokens = Seq("h5", "x5")
          ),
          runTest(
            initialText = "ВД40 200мл",
            expectedTokens = Seq("vd40", "wd40", "200ml")
          ),
          runTest(
            initialText = "Блок питания CJA1300H",
            expectedTokens = Seq("сйа1300х", "цйа1300х")
          )
        )
      },
      testM("Дубли схлопываются") {
        runTest(
          initialText = "Чехол хуавей p20, p20 lite, p20 pro",
          expectedTokens = Seq("п20")
        )
      },
      testM("Несколько мультитокенов в одной строке") {
        runTest(
          initialText = "Дисплей для Huawei Honor 30S 40S Nova 7SE",
          expectedTokens = Seq("30с", "40с", "7се")
        )
      },
      testM("Учитываются последовательности до 5 цифр и 3 букв") {
        combine(
          runTest(
            initialText = "Пылесос Bosch BWD41740",
            expectedTokens = Seq("бвд41740")
          ),
          runTest(
            initialText = "Артикул: 244105898R",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "AirPods2",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "Отель 7seas",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "Груз 9тонн",
            expectedTokens = Seq()
          )
        )
      },
      testM("Максимальная длина токена - 10 символов") {
        combine(
          runTest(
            initialText = "8cd6bp85aq4",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "EZ-IPC-B1B20P-0280B",
            expectedTokens = Seq()
          )
        )
      },
      testM("Токены с дефисами в середине слова") {
        combine(
          runTest(
            initialText = "Астромеханический дроид Р2-Д2",
            expectedTokens = Seq("r2-d2")
          ),
          runTest(
            initialText = "A57-310pro",
            expectedTokens = Seq("а57-310про")
          ),
          runTest(
            initialText = "Xiaomi DEM-LD200",
            expectedTokens = Seq("дем-лд200")
          )
        )
      },
      testM("Дефисы не должны находиться в начале, в конце слова и несколько подряд") {
        combine(
          runTest(
            initialText = "-g4",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "k8-",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "7gt--9t",
            expectedTokens = Seq()
          )
        )
      },
      testM("Токены с пробелами пропускаются") {
        combine(
          runTest(
            initialText = "32 GB",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "ВД 40",
            expectedTokens = Seq()
          )
        )
      },
      testM("Все части токена должны быть на одном языке") {
        combine(
          runTest(
            initialText = "t20л",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "W750ай",
            expectedTokens = Seq()
          )
        )
      },
      testM("Буквы должны присутствовать") {
        combine(
          runTest(
            initialText = "501253",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "1-1",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "50-12-53",
            expectedTokens = Seq()
          )
        )
      },
      testM("Пустые строки и спецсимволы не обрабатываются") {
        combine(
          runTest(
            initialText = "",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "2344!@# $%^61 &*()123-=+ 30\\32 50[{1 2}] 60'`",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = "p20, p30+",
            expectedTokens = Seq()
          ),
          runTest(
            initialText = ".40r",
            expectedTokens = Seq()
          )
        )
      },
      testM("Склеиваются части мультитокена") {
        runStickTokensTest(
          initialText = "Хиамо ми 9 т",
          expectedMisspell = Some("Хиамо ми9т")
        )
      },
      testM("Группы одинакового типа не приклеиваются") {
        runStickTokensTest(
          initialText = "Дисплей для blade ZTE a - 5 2020",
          expectedMisspell = Some("Дисплей для blade ZTE a-5 2020")
        )
      },
      testM("В пустом тексте ничего не склеивается") {
        runStickTokensTest(
          initialText = "",
          expectedMisspell = None
        )
      },
      testM("К существующим мультитокенам ничего не доклеивается") {
        runStickTokensTest(
          initialText = "осб 9мм",
          expectedMisspell = None
        )
      }
    )
  }
}
