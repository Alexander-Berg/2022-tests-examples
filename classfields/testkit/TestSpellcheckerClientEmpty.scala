package ru.yandex.vertis.general.search.testkit

import common.clients.spellchecker.SpellcheckerClient
import common.clients.spellchecker.model.{SpellcheckerLanguageSetting, SpellcheckerOption, SpellcheckerResult}
import zio.{Has, Task, ULayer, ZLayer}

object TestSpellcheckerClientEmpty {

  val empty: ULayer[Has[SpellcheckerClient.Service]] = ZLayer.succeed(new SpellcheckerClient.Service {

    override def check(
        text: String,
        language: SpellcheckerLanguageSetting,
        options: Set[SpellcheckerOption]): Task[SpellcheckerResult] = ???
  })

}
