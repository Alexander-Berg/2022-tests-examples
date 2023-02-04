package ru.yandex.vertis.moisha.impl.autoru

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import ru.yandex.vertis.moisha.model.gens.Producer
import ru.yandex.vertis.moisha.impl.autoru.gens._
import ru.yandex.vertis.moisha.impl.autoru.view.AutoRuRequestView

import scala.collection.JavaConverters._

/**
  * Generates ammo for yandextank
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
object AmmoGenerator extends App {

  def generateForPython: Unit = {
    val RequestsCount = 20000
    val OutFile = "python_request.txt"

    val lines = RequestGen
      .next(RequestsCount)
      .map(request => {
        val json = AutoRuRequestView.jsonFormat.write(AutoRuRequestView(request)).compactPrint

        s"POST||/api/1.x/service/autoru/price||good||$json"
      })

    Files.write(Paths.get(OutFile), lines.asJava, StandardCharsets.UTF_8)
  }

  generateForPython
}
