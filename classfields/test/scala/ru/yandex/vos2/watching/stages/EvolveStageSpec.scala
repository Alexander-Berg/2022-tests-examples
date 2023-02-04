package ru.yandex.vos2.watching.stages

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.realty.components.TestRealtyCoreComponents
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.watching.ProcessingState

/**
  * Created by Vsevolod Levin on 19.02.2018.
  */
@RunWith(classOf[JUnitRunner])
class EvolveStageSpec extends WordSpec with Matchers {

  "EvolveStage" should {
    s"send set SchemaVer to value ${EvolveStage.SchemaVersion}" in {
      val offer = TestUtils.createOffer().build()
      val processedOffer =
        new EvolveStage(TestRealtyCoreComponents.features).process(ProcessingState(offer, offer)).offer
      processedOffer.getSchemaVer should be(EvolveStage.SchemaVersion)
    }
  }
}
