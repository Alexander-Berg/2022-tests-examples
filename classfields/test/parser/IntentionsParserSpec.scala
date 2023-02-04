package ru.yandex.vertis.general.wizard.meta.parser

import general.bonsai.export_model.ExportedEntity
import general.wizard.synonyms.CatalogSynonymsMapping
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.meta.resources.IntentionPragmaticsSnapshot
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.meta.utils.TestUtils
import ru.yandex.vertis.general.wizard.model.{IntentionType, MetaWizardRequest, Pragmatic, RequestMatch}
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}

object IntentionsParserSpec extends DefaultRunnableSpec {

  private val brand = Pragmatic.Intention(IntentionType.Brand, "brand", Set("яндекс"))
  private val newIntention = Pragmatic.Intention(IntentionType.New, "new", Set("новый"))
  private val post = Pragmatic.Intention(IntentionType.Post, "new", Set("разместить"))

  private val snapshotSource: Seq[ExportedEntity] = Seq.empty
  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)
  private val intentions = Seq(brand, newIntention, post)
  private val catalogSynonymsMapping = CatalogSynonymsMapping.defaultInstance

  private val intentionSnapshot = IntentionPragmaticsSnapshot(intentions)

  private val dictionaryService =
    new LiveDictionaryService(
      intentionSnapshot,
      TestUtils.EmptyMetaPragmaticsSnapshot,
      bonsaiSnapshot,
      catalogSynonymsMapping
    )

  private val intentionsParser = new IntentionsParser(dictionaryService)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("IntentionsParser") {
      testM("parse intentions") {
        for {
          parsed <- intentionsParser.parse(
            PartialParsedQuery(
              MetaWizardRequest.empty("разместить объявление на новый сервис яндекс"),
              Set(
                RequestMatch.Intention.userInputIndices(Set(1), IntentionType.Neutral),
                RequestMatch.Intention.userInputIndices(Set(4), IntentionType.Neutral)
              )
            )
          )
        } yield assert(parsed.map(_.toSet).toSet)(
          equalTo(
            Set(
              Set(
                RequestMatch.Intention.userInputIndices(Set(0), IntentionType.Post),
                RequestMatch.Intention.userInputIndices(Set(3), IntentionType.New),
                RequestMatch.Intention.userInputIndices(Set(5), IntentionType.Brand)
              )
            )
          )
        )
      }
    }

}
