package ru.yandex.realty.newbuilding.gen

import java.lang.{Boolean => JBoolean, Float => JFloat}
import java.util.{List => JList}

import org.scalacheck.Gen
import ru.yandex.realty.model.offer.{BalconyType, BathroomUnitType, DealStatus, FlatType, Renovation}
import ru.yandex.realty.model.sites.{CommissioningDate, Decoration}
import ru.yandex.realty.util.boxed.{floatAsOption, intAsOption}

import scala.collection.JavaConverters._

trait NewbuildingOfferGenerators {

  val emptyDateGen: Gen[CommissioningDate] = Gen.const(CommissioningDate.EMPTY)

  val finishedDateGen: Gen[CommissioningDate] = Gen.const(CommissioningDate.FINISHED)

  val futureDateGen: Gen[CommissioningDate] = for {
    year <- Gen.choose(2034, 2049)
    quarter <- Gen.choose(1, 4)
  } yield new CommissioningDate(year, quarter, false)

  val commissioningDateGen: Gen[Option[CommissioningDate]] =
    Gen.option(Gen.oneOf(emptyDateGen, finishedDateGen, futureDateGen))

  val roomsValueIntegerGen: Gen[Integer] = Gen.oneOf(Gen.choose(-2, -1).map(Int.box), Gen.choose(1, 4).map(Int.box))
  val roomsOptionGen: Gen[Option[Integer]] = Gen.option(roomsValueIntegerGen)

  val areaValueGen: Gen[JFloat] = Gen.choose[Float](23f, 31f).map(Float.box)

  def kitchenSpaceGen(areaValue: JFloat): Gen[JFloat] =
    Gen.choose[Float](areaValue.floatValue() - 17f, areaValue.floatValue() - 14f).map(Float.box)

  def livingSpaceOptionGen(area: Option[JFloat], kitchenSpace: Option[JFloat]): Gen[Option[JFloat]] = Gen.option(
    Gen
      .const(
        (Seq(0f) ++ floatAsOption(area.orNull).toSeq ++ floatAsOption(kitchenSpace.orNull).map(v => -v).toSeq).sum
      )
      .map(Float.box)
  )

  val priceGen: Gen[Option[JFloat]] = Gen.option(Gen.choose[Float](2399999f, 5199999f).map(Float.box))

  def priceSqMGen(area: Option[JFloat], price: Option[JFloat]): Option[JFloat] =
    floatAsOption(area.orNull).flatMap(av => price.map(pv => 1.0f * pv / av)).map(Float.box)

  def isFinishedGen(commissioningDateOption: Option[CommissioningDate]): Option[JBoolean] =
    commissioningDateOption.map(_.isFinished).map(Boolean.box)

  val floorsTotalValueGen: Gen[Integer] = Gen.choose(5, 28).map(Int.box)
  val floorsTotalOptionGen: Gen[Option[Integer]] = Gen.option(floorsTotalValueGen)

  def floorOptionGen(floorsTotalOption: Option[Integer]): Gen[Option[Integer]] = {
    val maxFloor = floorsTotalOption.flatMap(intAsOption)

    maxFloor match {
      case Some(maxFloorValue) => Gen.option(Gen.choose(1, maxFloorValue).map(Int.box))
      case _ => Gen.const(None)
    }
  }

  val bathroomUnitGen: Gen[BathroomUnitType] = Gen.oneOf(BathroomUnitType.values())
  val bathroomUnitOptionGen: Gen[Option[BathroomUnitType]] = Gen.option(bathroomUnitGen)
  val balconyGen: Gen[BalconyType] = Gen.oneOf(BalconyType.values())
  val balconyOptionGen: Gen[Option[BalconyType]] = Gen.option(balconyGen)

  val dealStatusOptionGen: Gen[Option[DealStatus]] = Gen.option(Gen.oneOf(DealStatus.values()))

  val roomSpaceGen: Gen[Option[JList[JFloat]]] =
    Gen.option(Gen.listOf(Gen.choose(0f, 29f).map(Float.box)).map(_.asJava))

  private val randomJBooleanOption: Gen[Option[JBoolean]] = Gen.option(Gen.oneOf(true, false).map(Boolean.box))
  val internalGen: Gen[Option[JBoolean]] = randomJBooleanOption
  val apartmentsGen: Gen[Option[JBoolean]] = randomJBooleanOption

  val ceilingHeightGen: Gen[Option[JFloat]] = Gen.option(Gen.choose(2.45f, 3.9f).map(Float.box))
  val renovationGen: Gen[Option[Renovation]] = Gen.option(Gen.oneOf(Renovation.values()))
  val decorationGen: Gen[Option[Decoration]] = Gen.option(Gen.oneOf(Decoration.values()))
  val flatTypeGen: Gen[Option[FlatType]] = Gen.option(Gen.oneOf(FlatType.values()))

}
