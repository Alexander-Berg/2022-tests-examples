package ru.yandex.auto.clone.unifier.modifier

import java.util

import org.junit.runner.RunWith
import org.scalatest.{Assertion, Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.model.UnifiedCarInfo
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.message.AutoUtilsSchema
import ru.yandex.auto.message.AutoUtilsSchema.{ResolutionEntry, ResolutionPart, Status}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class AdvantagesModifierAccidentsSpec extends WordSpecLike with Matchers {

  private val advantagesModifier = new AdvantagesModifier
  private val color =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_COLOR).setStatus(Status.OK)
  private val noAccidents =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_ACCIDENTS).setStatus(Status.OK)
  private val noBeaten =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_BEATEN).setStatus(Status.OK)

  "Get tag if plausible report is provided" in {
    check(Seq(noAccidents, noBeaten, color), State.EXCELLENT.toString, List(SearchTag.NO_ACCIDENTS.getValue))
  }

  "Wrong code" in {
    check(Seq(noAccidents, noBeaten, color), "BEATEN", List())
  }

  "Get nothing if no relevant info about the accidents" in {
    check(Seq(color), State.GOOD.toString, List())
  }

  "Get nothing for BEATEN cars" in {
    check(Seq(noAccidents, noBeaten), State.BEATEN.toString, List())
  }

  "Get nothing if no info about the accidents" in {
    check(Seq(), State.GOOD.toString, List())
  }

  "Get nothing if BEATEN report is missing" in {
    check(Seq(noAccidents, color), State.GOOD.toString, List())
  }

  private[this] def check(
      reports: Seq[ResolutionEntry.Builder],
      stateCode: String,
      result: List[String]
  ): Assertion = {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    carInfo.setState(stateCode)
    val resolution = AutoUtilsSchema.VinIndexResolution.newBuilder()
    reports.foreach(r => resolution.addEntries(r))
    carInfo.setVinIndexResolution(resolution.build())
    advantagesModifier.appendNoAccidents(carInfo, tags)

    tags shouldEqual result.asJava
  }
}
