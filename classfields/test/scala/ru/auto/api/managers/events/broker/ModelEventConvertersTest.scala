package ru.auto.api.managers.events.broker

import auto.events.common.Common
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.StatEvents.{ContextBlock, ContextPage}
import ru.auto.api.{AsyncTasksSupport, BaseSpec, DummyOperationalSupport}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Try

class ModelEventConvertersTest
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport
  with DummyOperationalSupport {

  "testConvertContextBlock" should {
    "convert from old enum" in {
      ModelEventConverters.convertContextBlock(ContextBlock.BLOCK_CARD) shouldBe Common.ContextBlock.BLOCK_CARD
      val conversions =
        ContextBlock.values().map(v => Try(ModelEventConverters.convertContextBlock(v)).isSuccess).distinct
      conversions shouldBe Seq(true)
    }
  }

  "testConvertContextPage" should {
    "convert from old enum" in {
      ModelEventConverters.convertContextPage(ContextPage.PAGE_CHAT) shouldBe Common.ContextPage.PAGE_CHAT
      val conversions =
        ContextPage.values().map(v => Try(ModelEventConverters.convertContextPage(v)).isSuccess).distinct
      conversions shouldBe Seq(true)
    }
  }

}
