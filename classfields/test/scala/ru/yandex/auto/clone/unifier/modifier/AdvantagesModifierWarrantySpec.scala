package ru.yandex.auto.clone.unifier.modifier

import java.util

import org.junit.runner.RunWith
import org.scalatest.{Assertion, Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.model.UnifiedCarInfo
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.message.AutoUtilsSchema
import ru.yandex.auto.message.AutoUtilsSchema.{ResolutionPart, Status}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class AdvantagesModifierWarrantySpec extends WordSpecLike with Matchers {

  private val advantagesModifier = new AdvantagesModifier
  private def beatenResolution(status: Status) =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_BEATEN).setStatus(status)

  private val emptyTags = List()
  private val warrantyTag = List(SearchTag.WARRANTY.getValue)

  "Get tag if all is OK" in {
    check(warrantyTag)
  }

  "Get nothing if sate is not USED" in {
    check(emptyTags, state = State.BAD)
  }

  "Get nothing wrong warranty" in {
    check(emptyTags, warranty = "2")
  }

  "Get nothing if beaten status is not OK" in {
    check(emptyTags, beatenStatus = Status.ERROR)
  }

  private[this] def check(
      result: List[String],
      state: State = State.GOOD,
      warranty: String = "1",
      beatenStatus: Status = Status.OK
  ): Assertion = {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    carInfo.setState(state.toString)
    carInfo.setWarranty(warranty)

    val resolution = AutoUtilsSchema.VinIndexResolution.newBuilder()
    resolution.addEntries(beatenResolution(beatenStatus))
    carInfo.setVinIndexResolution(resolution.build())

    advantagesModifier.appendWarranty(carInfo, tags)

    tags shouldEqual result.asJava
  }
}
