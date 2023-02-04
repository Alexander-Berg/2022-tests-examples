package ru.yandex.vertis.moderation.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.ExternalId

/**
  * Specs for [[ExternalId]] conversions
  *
  * @author sunlight
  */
@RunWith(classOf[JUnitRunner])
class ExternalIdConversionSpec extends SpecBase {

  "conversion" should {
    "successfully convert external id" in {
      val externalId = CoreGenerators.ExternalIdGen.next
      val convExternalId = ExternalId(externalId.id)
      externalId should be(convExternalId)
    }
  }

}
