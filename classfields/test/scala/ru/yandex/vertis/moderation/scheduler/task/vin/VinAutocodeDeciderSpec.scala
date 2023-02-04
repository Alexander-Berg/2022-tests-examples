package ru.yandex.vertis.moderation.scheduler.task.vin

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.auto.external.autocode.AutocodeVinInfo
import ru.auto.external.autocode.AutocodeVinInfo._
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.httpclient.vin.{Vin, Year}
import ru.yandex.vertis.moderation.model.autoru.{Mark, Model, VinResolution}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{AutoruEssentialsGen, InstanceGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Autoru
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials._
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.scheduler.task.vin.VinAutocodeDecider._
import ru.yandex.vertis.moderation.util.DateTimeUtil._

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class VinAutocodeDeciderSpec extends SpecBase {

  val decider: VinAutocodeDecider = VinAutocodeDecider.forService(Service.AUTORU)

  import VinAutocodeDecider.Source

  private def instance(mark: Mark = "land",
                       model: Model = "cruiser",
                       horsePower: Int = 200,
                       engineVolume: Int = 2500,
                       vin: Option[Vin] = Some("test-vin"),
                       year: Year = 1990,
                       steeringWheel: SteeringWheel = SteeringWheel.LEFT,
                       category: Category = Category.CARS,
                       colorName: String = "white",
                       vinUpdated: DateTime = now()
                      ) = {
    val autoru = AutoruEssentialsGen.next
    val instance = InstanceGen.next

    instance.copy(essentials =
      autoru.copy(
        source = Autoru.AutoruEssentials.Source.AUTO_RU,
        vin = vin,
        year = Some(year),
        steeringWheel = Some(steeringWheel),
        category = Some(category),
        model = Some(model),
        mark = Some(mark),
        horsePower = Some(horsePower),
        engineVolume = Some(engineVolume),
        colorName = Some(colorName),
        vinResolution = Some(VinResolution(Some(vinUpdated)))
      )
    )
  }
  private val veryOld: DateTime = now().minusDays(300)
  private val freshReg: DateTime = now().minusDays(38)
  private val oldReg: DateTime = now().minusDays(70)

  private def createHistory(vin: String = "test-vin",
                            year: Int = 1990,
                            mark: Mark = "land",
                            model: Model = "cruiser",
                            horsePower: Int = 200,
                            engineVolume: Int = 2500,
                            steeringWheel: String = "LEFT",
                            inPledge: Boolean = false,
                            hasConstraints: Boolean = false,
                            wanted: Boolean = false,
                            hasAccidents: Boolean = false,
                            lastRegStarts: DateTime = oldReg,
                            lastRegExpired: Option[DateTime] = None,
                            hasPeriods: Boolean = true,
                            color: String = "white",
                            parsedColors: Iterable[String] = Iterable("ffffff")
                           ): History = {
    val reg = Registration.newBuilder()
    if (hasPeriods) {
      val periodOld = RegistrationPeriod.newBuilder()
      periodOld.setFrom(veryOld.getMillis)
      periodOld.setTo(lastRegStarts.getMillis)
      val periodLast = RegistrationPeriod.newBuilder()
      periodLast.setFrom(lastRegStarts.getMillis)
      lastRegExpired.map(_.getMillis).foreach(periodLast.setTo)
      reg.addPeriods(periodOld)
      reg.addPeriods(periodLast)
    }
    reg.setYear(year)
    reg.setPowerHp(horsePower)
    reg.setWheel(steeringWheel)
    reg.setModel(model)
    reg.setMark(mark)
    reg.setDisplacement(engineVolume)
    reg.setColor(color)
    parsedColors.foreach(reg.addParsedColors)
    val item = Item.newBuilder()
    item.setVin(vin)
    item.setRegistration(reg)
    val history = History.newBuilder()
    history.setRegistration(item)
    if (inPledge) {
      val pledge = Pledge.newBuilder()
      val pledges = Item.newBuilder()
      pledges.addPledges(pledge)
      history.setPledges(pledges)
    }
    if (hasConstraints) {
      val constraint = Constraint.newBuilder()
      val constraints = Item.newBuilder()
      constraints.addConstraints(constraint)
      history.setConstraints(constraints)
    }
    if (wanted) {
      val wanted = AutocodeVinInfo.Wanted.newBuilder()
      val wanteds = Item.newBuilder()
      wanteds.addWanted(wanted)
      history.setWanted(wanteds)
    }
    if (hasAccidents) {
      val accident = Accident.newBuilder()
      val accidents = Item.newBuilder()
      accidents.addAccidents(accident)
      history.setAccidents(accidents)
    }
    history.build()
  }

  private val noneVin = instance(vin = None)
  private val emptyVin = instance(vin = Some(""))
  private val anotherVin = instance(vin = Some("another-vin"))
  private val rightWheel = instance(steeringWheel = SteeringWheel.RIGHT)
  private val moto = instance(category = Category.MOTORCYCLE)

  val NotFitSources =
    Seq(
      Source(rightWheel, createHistory(hasPeriods = false)),
      Source(moto, createHistory())
    )

  val MismatchedSources =
    Seq(
      Source(instance(year = 2000), createHistory(year = 2012, inPledge = true)) ->
        Set(Pledges, WrongYear(2000, 2012)),
      Source(instance(mark = "lada"), createHistory(hasConstraints = true, parsedColors = Iterable.empty)) ->
        Set(Constraints, WrongMarkModel("lada", "cruiser", "land", "cruiser")),
      Source(instance(horsePower = 100), createHistory(wanted = true, steeringWheel = "WRONG")) ->
        Set(VinAutocodeDecider.Wanted, WrongHorsePower(100, 200)),
      Source(instance(engineVolume = 1951, horsePower = 100), createHistory(engineVolume = 1849, horsePower = 111)) ->
        Set(WrongEngineVolume(1951, 1849), WrongHorsePower(100, 111)),
      Source(
        instance(engineVolume = 2000),
        createHistory(hasAccidents = true, steeringWheel = "RIGHT", color = "black", parsedColors = Iterable("000000"))
      ) ->
        Set(
          Accidents,
          WrongSteeringWheel(SteeringWheel.LEFT, SteeringWheel.RIGHT),
          WrongEngineVolume(2000, 2500),
          WrongColor("white", Seq("black"))
        ),
      Source(instance(), createHistory(lastRegStarts = freshReg, parsedColors = Iterable("000000", "ffffff"))) ->
        Set(NewRegistration(freshReg)),
      Source(instance(), createHistory(lastRegExpired = Some(freshReg))) ->
        Set(NoRegistration(Some(freshReg))),
      Source(instance(), createHistory(hasPeriods = false)) ->
        Set.empty,
      Source(
        instance(vinUpdated = now().minusDays(VinAutocodeDecider.AutocodeOutdatedDays + 1)),
        createHistory(lastRegExpired = Some(freshReg))
      ) ->
        Set.empty
    )

  val MatchedSources =
    Seq(
      Source(instance(), createHistory()),
      Source(instance(engineVolume = 1951), createHistory(engineVolume = 2149)),
      Source(instance(engineVolume = 1951, horsePower = 100), createHistory(engineVolume = 2049, horsePower = 91)),
      Source(instance(engineVolume = 1951, horsePower = 100), createHistory(engineVolume = 1851, horsePower = 110)),
      Source(instance(mark = "LAND"), createHistory())
    )

  "fail source with non equals VINs" in {
    intercept[IllegalArgumentException] {
      Source(noneVin, createHistory())
    }
    intercept[IllegalArgumentException] {
      Source(emptyVin, createHistory())
    }
    intercept[IllegalArgumentException] {
      Source(anotherVin, createHistory())
    }
    intercept[IllegalArgumentException] {
      Source(anotherVin, createHistory(vin = ""))
    }
  }

  NotFitSources.foreach { source =>
    s"return None for $source" in {
      decider(source) shouldBe Set.empty
    }
  }

  MismatchedSources.foreach { case (source, mismatch) =>
    s"return $mismatch for $source" in {
      decider(source) shouldBe mismatch
    }
  }

  MatchedSources.foreach { source =>
    s"return AutocodeMatched for $source" in {
      decider(source) shouldBe Set.empty
    }
  }

}
