package ru.yandex.vertis.general.wizard.api.services

import common.palma.Palma
import general.wizard.palma.{IntentionPragmatic, Synonym, SynonymGroup}
import ru.yandex.vertis.general.wizard.meta.resources.IntentionPragmaticsSnapshot

object TestIntentionPragmatics {

  private val ya = IntentionPragmatic(
    code = "Brand",
    synonymGroups = Seq(SynonymGroup("", Seq(Synonym("яндекс"), Synonym("яндекс.объявления"))))
  )

  private val newIntention =
    IntentionPragmatic(code = "New", synonymGroups = Seq(SynonymGroup("", Seq(Synonym("новый")))))

  private val addIntention =
    IntentionPragmatic(code = "Add", synonymGroups = Seq(SynonymGroup("", Seq(Synonym("продать")))))

  private val commercialIntention =
    IntentionPragmatic(code = "Commercial", synonymGroups = Seq(SynonymGroup("", Seq(Synonym("купить")))))
  private val input = Seq(newIntention, ya, addIntention, commercialIntention)

  val dictionarySnapshot: IntentionPragmaticsSnapshot = IntentionPragmaticsSnapshot.build(input)
}
