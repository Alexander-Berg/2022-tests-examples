package ru.yandex.auto.clone.unifier.modifier

import java.util

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.model.UnifiedCarInfo
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.message.AutoUtilsSchema
import ru.yandex.auto.message.AutoUtilsSchema.{ResolutionPart, Status}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class AdvantagesModifierOwnerSpec extends WordSpecLike with Matchers {

  private val advantagesModifier = new AdvantagesModifier
  private val color =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_COLOR).setStatus(Status.OK)
  private val oneOwners =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_OWNERS).setStatus(Status.OK)

  "Get ONE_OWNER tag if RP_OWNERS=OK and setOwnersCount(1)" in {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    carInfo.setOwnersCount(1)
    carInfo.setState(State.GOOD.toString)

    val resolution = AutoUtilsSchema.VinIndexResolution
      .newBuilder()
      .addEntries(oneOwners)
      .addEntries(color)
    carInfo.setVinIndexResolution(resolution.build())
    advantagesModifier.appendOwnersCount(carInfo, tags)

    tags shouldEqual List(SearchTag.ONE_OWNER.getValue).asJava
  }

  "Get NO ONE_OWNER tag if state is not USED" in {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    carInfo.setOwnersCount(1)
    carInfo.setState(State.BAD.toString)

    val resolution = AutoUtilsSchema.VinIndexResolution
      .newBuilder()
      .addEntries(oneOwners)
      .addEntries(color)
    carInfo.setVinIndexResolution(resolution.build())
    advantagesModifier.appendOwnersCount(carInfo, tags)

    tags shouldEqual List().asJava
  }

  "Get NO ONE_OWNER tag if RP_OWNERS=OK and setOwnersCount(2)" in {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    carInfo.setOwnersCount(2)
    carInfo.setState(State.EXCELLENT.toString)

    val resolution = AutoUtilsSchema.VinIndexResolution
      .newBuilder()
      .addEntries(oneOwners)
      .addEntries(color)
    carInfo.setVinIndexResolution(resolution.build())
    advantagesModifier.appendOwnersCount(carInfo, tags)

    tags shouldEqual List().asJava
  }

  "Get NO ONE_OWNER tag if only setOwnersCount(1)" in {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    carInfo.setOwnersCount(1)
    carInfo.setState(State.GOOD.toString)
    advantagesModifier.appendOwnersCount(carInfo, tags)
    tags shouldEqual List().asJava
  }

  "Get NO ONE_OWNER tag if only RP_OWNERS=OK" in {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    carInfo.setState(State.GOOD.toString)
    val resolution = AutoUtilsSchema.VinIndexResolution
      .newBuilder()
      .addEntries(oneOwners)
    carInfo.setVinIndexResolution(resolution.build())
    advantagesModifier.appendOwnersCount(carInfo, tags)
    tags shouldEqual List().asJava
  }

  "Get NO ONE_OWNER tag by default" in {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    carInfo.setState(State.MIDDLING.toString)
    advantagesModifier.appendOwnersCount(carInfo, tags)
    tags shouldEqual List().asJava
  }

}
