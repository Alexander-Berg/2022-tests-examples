package ru.auto.chatbot.utils

import java.io.{File, FileOutputStream}

import org.scalatest.FunSuite

class PdfGeneratorTest extends FunSuite {

  val json: String =
    """
      |{
      |  "offer": {
      |    "title": "LADA (ВАЗ) 2106  1999",
      |    "price": "40000",
      |    "description": "Проверка показала, что данные в объявлении <b>не совпали</b> с данными реальной машины.",
      |    "image": "http://avatars.mds.yandex.net/get-autoru-vos/1907522/059b03096abf0493490d61141cfde88b/1200x900",
      |    "link": "https://auto.ru/cars/used/sale/1089364540-6e8e4c81"
      |  },
      |  "questions": [
      |    {
      |      "group": "Документы и идентификационные номера",
      |      "items": [
      |        {
      |          "text": "Автомобиль продаёт не владелец",
      |          "status": "BAD"
      |        },
      |        {
      |          "text": "VIN на кузове совпадает с ПТС",
      |          "status": "OK"
      |        },
      |        {
      |          "text": "ПТС — оригинал",
      |          "status": "OK"
      |        }
      |      ]
      |    },
      |    {
      |      "group": "Проверка технического состояния",
      |      "items": [
      |        {
      |          "text": "Нет данных о потёках на трансмиссии",
      |          "status": "UNKNOWN"
      |        },
      |        {
      |          "text": "Не знаю, работает ли кондиционер",
      |          "status": "UNKNOWN"
      |        },
      |        {
      |          "text": "Уровень охлаждающей жидкости в норме",
      |          "status": "OK"
      |        },
      |        {
      |          "text": "Выхлоп не дымит",
      |          "status": "OK"
      |        },
      |        {
      |          "text": "Уровень масла в норме",
      |          "status": "OK"
      |        },
      |        {
      |          "text": "Нет странных звуков при работе мотора",
      |          "status": "OK"
      |        },
      |        {
      |          "text": "Есть потёки масла или охлаждающей жидкости",
      |          "status": "BAD"
      |        }
      |      ]
      |    },
      |    {
      |      "group": "Пробег",
      |      "items": [
      |        {
      |          "text": "Состояние педалей соответствует пробегу",
      |          "status": "OK"
      |        },
      |        {
      |          "text": "Состояние салона не соответствует пробегу",
      |          "status": "BAD"
      |        }
      |      ]
      |    },
      |    {
      |      "group": "Проверка состояния кузова, поиск следов аварий",
      |      "items": [
      |        {
      |          "text": "На кузове есть дефекты окраски",
      |          "status": "BAD"
      |        },
      |        {
      |          "text": "Нет данных о толщине ЛКП",
      |          "status": "UNKNOWN"
      |        },
      |        {
      |          "text": "Кузовные элементы одного оттенка",
      |          "status": "OK"
      |        },
      |        {
      |          "text": "Год выпуска фар и автомобиля совпадает",
      |          "status": "OK"
      |        },
      |        {
      |          "text": "Заметна коррозия кузова",
      |          "status": "BAD"
      |        },
      |        {
      |          "text": "Есть следы незаводской сварки/герметика",
      |          "status": "BAD"
      |        },
      |        {
      |          "text": "На кузове есть неровные зазоры",
      |          "status": "BAD"
      |        },
      |        {
      |          "text": "Есть признаки разбора салона",
      |          "status": "BAD"
      |        },
      |        {
      |          "text": "Есть следы демонтажа на болтах",
      |          "status": "BAD"
      |        }
      |      ]
      |    }
      |  ],
      |  "comment": "масло капает со всех щелей, тосол бежит из горловины заливной, парит. давление масла не показывает. уровень топлива не показывает. шкив помпы крутится неравномерно."
      |}
    """.stripMargin

  def saveToFile(bytes: Array[Byte], dist: File): Unit = {
    if (dist.exists()) {
      dist.delete()
    }
    val fos = new FileOutputStream(dist)
    fos.write(bytes)
    fos.close()
  }

  ignore("pdf generator test") {

    val generator = PdfGenerator.default

    val path = PdfGenerator.defaultBasePath

    val inputFile = new File(path + "/index.html")
    val outputPdfFile = new File(path + "/report.pdf")
    val outputHtmlFile = new File(path + "/report.html")

    val (pdf, html) = generator.createWithTemplate(inputFile, json)

    saveToFile(pdf, outputPdfFile)
    saveToFile(html, outputHtmlFile)

    println(s"сгенерированый pdf тут ${outputPdfFile.toURI.toURL.toExternalForm}")
    println(s"сгенерированый html тут ${outputHtmlFile.toURI.toURL.toExternalForm}")
  }

}
