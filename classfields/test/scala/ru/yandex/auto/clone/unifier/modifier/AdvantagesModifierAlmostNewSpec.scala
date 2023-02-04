package ru.yandex.auto.clone.unifier.modifier

import java.time.{Instant, ZoneOffset}
import java.util

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.model.UnifiedCarInfo
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.message.AutoUtilsSchema
import ru.yandex.auto.message.AutoUtilsSchema.{ResolutionEntry, ResolutionPart, Status}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class AdvantagesModifierAlmostNewSpec extends WordSpecLike with Matchers {

  private val advantagesModifier = new AdvantagesModifier
  private val noAccidents =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_SERIOUS_ACCIDENTS).setStatus(Status.OK)

  private def beaten(status: Status): ResolutionEntry.Builder =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_BEATEN).setStatus(status)

  private def carsharing(status: Status): ResolutionEntry.Builder =
    AutoUtilsSchema.ResolutionEntry.newBuilder().setPart(ResolutionPart.RP_CARSHARING).setStatus(status)

  "Should work with all conditions" in {
    val notEmpty = List(SearchTag.ALMOST_NEW.getValue)
    infoWith(2, 10000, notEmpty)
    infoWith(1, 16000, notEmpty)
    infoWith(1, 16000, notEmpty)
    infoWith(2, 18000, notEmpty)
    infoWith(3, 50000, notEmpty)

    infoWith(1, 10000, List.empty, carsharingStatus = Status.ERROR)
    infoWith(1, 10000, List.empty, beatenStatus = Status.ERROR)
    infoWith(4, 10000, List.empty)
    infoWith(2, 52000, List.empty)
    infoWith(3, 53000, List.empty)
  }

  private def infoWith(
      yearsAgo: Int,
      mileage: Int,
      target: List[String],
      carsharingStatus: Status = Status.OK,
      beatenStatus: Status = Status.OK
  ) = {
    val tags = new util.ArrayList[String]()
    val carInfo = new UnifiedCarInfo("1")
    val yearNow = Instant.now().atZone(ZoneOffset.UTC).getYear

    carInfo.setYear(yearNow - yearsAgo)
    carInfo.setMileage(mileage)
    carInfo.setState(State.GOOD.toString)

    val resolution = AutoUtilsSchema.VinIndexResolution
      .newBuilder()
      .addEntries(carsharing(carsharingStatus))
      .addEntries(beaten(beatenStatus))
      .addEntries(noAccidents)
    carInfo.setVinIndexResolution(resolution.build())

    advantagesModifier.appendAlmostNew(carInfo, tags)
    tags shouldEqual target.asJava
  }

}
