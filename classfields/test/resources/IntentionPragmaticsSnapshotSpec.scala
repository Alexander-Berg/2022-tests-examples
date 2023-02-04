package ru.yandex.vertis.general.wizard.meta.resources

import general.wizard.palma.{IntentionPragmatic, Synonym, SynonymGroup}
import ru.yandex.vertis.general.wizard.model.{IntentionType, Pragmatic}
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, test, DefaultRunnableSpec, ZSpec}

object IntentionPragmaticsSnapshotSpec extends DefaultRunnableSpec {

  private val ya = IntentionPragmatic(code = "Brand", synonymGroups = Seq(SynonymGroup("", Seq(Synonym("яндекс")))))

  private val newIntention =
    IntentionPragmatic(code = "New", synonymGroups = Seq(SynonymGroup("", Seq(Synonym("новый")))))
  private val input = Seq(ya, newIntention)

  private val output = Seq(
    Pragmatic.Intention(IntentionType.Brand, "", Set("яндекс")),
    Pragmatic.Intention(IntentionType.New, "", Set("новый"))
  )
  private val dictionarySnapshot = IntentionPragmaticsSnapshot.build(input)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("IntentionPragmatics")(
      test("load resource") {
        assert(dictionarySnapshot.pragmatics)(equalTo(output))
      }
    )
}
