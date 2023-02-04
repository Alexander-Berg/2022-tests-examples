package ru.yandex.vos2.autoru.utils.testforms

import ru.auto.api.ApiOfferModel._
import ru.auto.api.TrucksModel.TruckCategory._
import ru.auto.api.TrucksModel._
import ru.auto.api.{ApiOfferModel, CommonModel, TrucksModel}
import ru.yandex.vos2.autoru.catalog.CommonCatalog
import ru.yandex.vos2.autoru.catalog.trucks.model.TruckCard
import ru.yandex.vos2.autoru.components.AutoruCoreComponents
import ru.yandex.vos2.autoru.model.AutoruCommonLogic._
import ru.yandex.vos2.autoru.services.AutoruTrucksCatalog
import ru.yandex.vos2.util.RandomUtil

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 3/31/17.
  */
class TruckTestForms(components: AutoruCoreComponents) extends CommonTestForms[TruckFormInfo](components) {

  val catalog: CommonCatalog[TruckCard] = components.trucksCatalog
  val category = Category.TRUCKS

  @scala.annotation.tailrec
  final override def randomCard: TruckCard = {
    val card = super.randomCard
    val markAutoruId = card.message.getMark.getAutoruId
    val modelAutoruId = card.message.getModel.getAutoruId
    if (nonEmptyNumber(markAutoruId) && nonEmptyNumber(modelAutoruId)) card
    else randomCard
  }

  @scala.annotation.tailrec
  final override def randomCard(formParams: TestFormParams[TruckFormInfo]): TruckCard = {
    val card = super.randomCard(formParams)
    val markAutoruId = card.message.getMark.getAutoruId
    val modelAutoruId = card.message.getModel.getAutoruId
    if (nonEmptyNumber(markAutoruId) && nonEmptyNumber(modelAutoruId)) {
      if (formParams.specialTruck) {
        if (card.isSpecial) {
          if (formParams.excludeTruckCategories.nonEmpty) {
            if (!formParams.excludeTruckCategories.get.contains(card.truckCategory)) card
            else randomCard(formParams)
          } else card
        } else randomCard(formParams)
      } else {
        if (!card.isSpecial) {
          if (formParams.excludeTruckCategories.nonEmpty) {
            if (!formParams.excludeTruckCategories.get.contains(card.truckCategory)) card
            else randomCard(formParams)
          } else card
        } else randomCard(formParams)
      }
    } else randomCard(formParams)
  }

  override protected def categorySpecificFormPart(formParams: TestFormParams[TruckFormInfo],
                                                  builder: ApiOfferModel.Offer.Builder): Unit = {
    val card: TruckCard = formParams.optCard.getOrElse(randomCard(formParams))
    builder.setTruckInfo(generateTruckInfo(formParams.wheelLeft, card))
  }

  override protected def categorySpecificUpdateForm(builder: Offer.Builder,
                                                    formParams: TestFormParams[TruckFormInfo]): Unit = {
    builder.getTruckInfoBuilder
      .clearEquipment()
      .putAllEquipment(randomEquipment(builder.getTruckInfo.getTruckCategory).asJava)
  }

  //scalastyle:off cyclomatic.complexity
  //scalastyle:off method.length
  private def generateTruckInfo(wheelLeft: Option[Boolean], card: TruckCard): TrucksModel.TruckInfo.Builder = {
    val b = TrucksModel.TruckInfo.newBuilder()
    val truckCategory: TruckCategory =
      if (!card.isSpecial) card.truckCategory
      else randomSpecialCategory

    b.setTruckCategory(truckCategory)

    b.setDisplacement(RandomUtil.nextInt(1, 100000))

    val left = wheelLeft.getOrElse(RandomUtil.nextBool(0.8))
    b.setSteeringWheel(if (left) CommonModel.SteeringWheel.LEFT else CommonModel.SteeringWheel.RIGHT)

    randomCabinType(truckCategory).foreach(b.setCabin)
    b.setLoading(RandomUtil.nextInt(0, 3000 + 1))
    b.setSeats(RandomUtil.nextInt(0, 16 + 1))

    b.setAxis(RandomUtil.nextInt(0, 7))
    //b.setHangerKey(???)
    b.setSaddleHeight(RandomUtil.chooseEnum(SaddleHeight.values()))

    b.setChassisSuspension(RandomUtil.chooseEnum(Suspension.ChassisType.values()))
    b.setCabinSuspension(RandomUtil.chooseEnum(Suspension.CabinType.values()))
    b.setSuspension(RandomUtil.chooseEnum(Suspension.Type.values()))

    b.setBrakes(RandomUtil.chooseEnum(BrakeType.values()))

    randomWheelDrive(truckCategory).foreach(b.setWheelDrive)
    b.setEuroClass(RandomUtil.chooseEnumWithUnknown(EuroClass.values()))
    setBodyTypeByCategory(b, truckCategory)

    //b.setStateKey(???)
    //b.setAutoType(???)
    randomDriveType(truckCategory).foreach(b.setGear)
    b.setMark(card.message.getMark.getCode)
    b.setModel(card.message.getModel.getCode)
    //b.setGeneration(???)
    b.setHorsePower(RandomUtil.nextInt(0, 2301))
    randomEngineType(truckCategory).foreach(b.setEngine)
    randomTransmission(truckCategory).foreach(b.setTransmission)
    b.putAllEquipment(randomEquipment(truckCategory).asJava)

    if (card.isSpecial) {
      b.setOperatingHours(RandomUtil.nextInt(0, 30000))
      if (haveLoadHeight(truckCategory)) {
        if (truckCategory == CRANE) b.setLoadHeight(RandomUtil.nextInt(0, 180))
        else if (truckCategory == AUTOLOADER) b.setLoadHeight(RandomUtil.nextInt(0, 10))
      }
      if (haveCraneRadius(truckCategory)) b.setCraneRadius(RandomUtil.nextInt(0, 180))
      if (haveBucketVolume(truckCategory)) b.setBucketVolume(RandomUtil.nextInt(0, 40))
      if (haveTractionClass(truckCategory)) b.setTractionClass(RandomUtil.chooseEnum(TractionClass.values()))
    }

    b
  }

  def randomSpecialCategory: TruckCategory = {
    RandomUtil.choose(
      Seq(AGRICULTURAL, CONSTRUCTION, AUTOLOADER, CRANE, DREDGE, BULLDOZERS, CRANE_HYDRAULICS, MUNICIPAL)
    )
  }

  private def randomCabinType(category: TruckCategory): Option[CabinType] = {
    val cabinTypes = AutoruTrucksCatalog.cabinBindings._5
      .filter(_._2 == category)
      .map(x => CabinType.forNumber(x._3.getNumber))
    if (cabinTypes.isEmpty) None
    else Some(RandomUtil.choose(cabinTypes))
  }

  private def randomEngineType(category: TruckCategory): Option[Engine] = {
    val engineTypes = AutoruTrucksCatalog.engineBindings._5
      .filter(_._2 == category)
      .map(x => Engine.forNumber(x._3.getNumber))
    if (engineTypes.isEmpty) None
    else Some(RandomUtil.choose(engineTypes))
  }

  private def randomWheelDrive(category: TruckCategory): Option[WheelDrive] = {
    val wheelDrives = AutoruTrucksCatalog.wheelDriveBindings._5
      .filter(_._2 == category)
      .map(x => WheelDrive.forNumber(x._3.getNumber))
    if (wheelDrives.isEmpty) None
    else Some(RandomUtil.choose(wheelDrives))
  }

  private def randomDriveType(category: TruckCategory): Option[GearType] = {
    val driveTypes = AutoruTrucksCatalog.driveBindings._5
      .filter(_._2 == category)
      .map(x => GearType.forNumber(x._3.getNumber))
    if (driveTypes.isEmpty) None
    else Some(RandomUtil.choose(driveTypes))
  }

  private def randomTransmission(category: TruckCategory): Option[Transmission] = {
    val transmissions = AutoruTrucksCatalog.transmissionBindings._5
      .filter(_._2 == category)
      .map(x => Transmission.forNumber(x._3.getNumber))
    if (transmissions.isEmpty) None
    else Some(RandomUtil.choose(transmissions))
  }

  private def randomEquipment(category: TruckCategory): Map[String, java.lang.Boolean] = {
    val equipmentElemsSeq: Seq[String] = AutoruTrucksCatalog.equipmentBindings._5
      .filter(_._2 == category)
      .map(x => x._3.name().stripPrefix("TRUCK_EQUIP_").toLowerCase.replaceAll("_", "-"))
    if (equipmentElemsSeq.isEmpty) Map.empty
    else {
      RandomUtil
        .sample(equipmentElemsSeq, min = 1, max = 5)
        .map(_ -> Boolean.box(true))
        .toMap
    }
  }

  def setBodyTypeByCategory(b: TruckInfo.Builder, category: TruckCategory): TruckInfo.Builder = {
    category match {
      case BUS =>
        b.setBusType(RandomUtil.chooseEnum(Bus.Type.values()))
      case LCV =>
        b.setLightTruckType(RandomUtil.chooseEnum(LightTruck.BodyType.values()))
      case SWAP_BODY =>
        b.setSwapBodyType(RandomUtil.chooseEnum(SwapBody.Type.values()))
      case TRAILER =>
        b.setTrailerType(RandomUtil.chooseEnum(Trailer.Type.values()))
      case TRUCK =>
        val values: Seq[Truck.BodyType] = Truck.BodyType.values().collect {
          case Truck.BodyType.THERMOBODY => Truck.BodyType.ISOTHERMAL_BODY
          case v => v
        }
        b.setTruckType(RandomUtil.chooseEnum(values))
      case AGRICULTURAL =>
        b.setAgriculturalType(RandomUtil.chooseEnum(Agricultural.Type.values()))
      case CONSTRUCTION =>
        val values: Seq[Construction.Type] = Construction.Type.values().collect {
          case Construction.Type.MIXER => Construction.Type.MIXER_TRUCK
          case v => v
        }
        b.setConstructionType(RandomUtil.chooseEnum(values))
      case AUTOLOADER =>
        b.setAutoloaderType(RandomUtil.chooseEnum(Autoloader.Type.values()))
      case DREDGE =>
        b.setDredgeType(RandomUtil.chooseEnum(Dredge.Type.values()))
      case BULLDOZERS =>
        b.setBulldozerType(RandomUtil.chooseEnum(Bulldozer.Type.values()))
      case MUNICIPAL =>
        val values: Seq[Municipal.Type] = Municipal.Type.values().collect {
          case Municipal.Type.OOZE_CLEANING_MACHINE => Municipal.Type.VACUUM_MACHINE
          case Municipal.Type.PRESSER_GARBAGE => Municipal.Type.GARBAGE_TRUCK
          case v => v
        }
        b.setMunicipalType(RandomUtil.chooseEnum(values))
      case _ =>
      //nothing to set
    }
    b
  }

  protected val descriptionParts: List[String] = List(
    "ПТС оригинал",
    "Прекрасный внешний вид",
    "Чистый ухоженный салон нареканий нет",
    "Все ТО пройдены своевременно",
    "Один хозяин по ПТС",
    "Свободен от банковских залогов и прочих обременений",
    "Комфортабельный и надежный автомобиль в своем классе",
    "Произведена предпродажная диагностика двигателя и ходовой",
    "Бережная и безаварийная эксплуатация",
    "Рама целая",
    "Не перевозили тяжести",
    "Цельнометаллический фургон"
  )
}
