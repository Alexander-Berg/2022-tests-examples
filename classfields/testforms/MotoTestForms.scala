package ru.yandex.vos2.autoru.utils.testforms

import ru.auto.api.ApiOfferModel._
import ru.auto.api.MotoModel.Moto.Engine
import ru.auto.api.MotoModel._
import ru.auto.api.{ApiOfferModel, MotoModel}
import ru.yandex.vos2.autoru.catalog.CommonCatalog
import ru.yandex.vos2.autoru.catalog.moto.model.MotoCard
import ru.yandex.vos2.autoru.components.AutoruCoreComponents
import ru.yandex.vos2.autoru.services.AutoruMotoCatalog
import ru.yandex.vos2.util.RandomUtil

import scala.jdk.CollectionConverters._
import scala.util.Random

/**
  * Created by andrey on 5/17/17.
  */
class MotoTestForms(components: AutoruCoreComponents) extends CommonTestForms[MotoFormInfo](components) {
  val category = Category.MOTO
  val catalog: CommonCatalog[MotoCard] = components.motoCatalog

  @scala.annotation.tailrec
  final override def randomCard: MotoCard = {
    val card = super.randomCard
    val markAutoruId = card.message.getMark.getAutoruId
    val modelAutoruId = card.message.getModel.getAutoruId
    if (nonEmptyNumber(markAutoruId) && nonEmptyNumber(modelAutoruId)) card
    else randomCard
  }

  @scala.annotation.tailrec
  final override def randomCard(formParams: TestFormParams[MotoFormInfo]): MotoCard = {
    val card = super.randomCard(formParams)
    val markAutoruId = card.message.getMark.getAutoruId
    val modelAutoruId = card.message.getModel.getAutoruId
    if (nonEmptyNumber(markAutoruId) && nonEmptyNumber(modelAutoruId)) card
    else randomCard(formParams)
  }

  protected def categorySpecificFormPart(formParams: TestFormParams[MotoFormInfo],
                                         builder: ApiOfferModel.Offer.Builder): Unit = {
    val card: MotoCard = formParams.optCard.getOrElse(randomCard(formParams))
    builder.setMotoInfo(generateMotoInfo(card))
  }

  override protected def categorySpecificUpdateForm(builder: Offer.Builder,
                                                    formParams: TestFormParams[MotoFormInfo]): Unit = {
    builder.getMotoInfoBuilder
      .clearEquipment()
      .putAllEquipment(randomEquipment(builder.getMotoInfo.getMotoCategory).asJava)
  }

  //scalastyle:off cyclomatic.complexity
  //scalastyle:off method.length
  private def generateMotoInfo(card: MotoCard): MotoModel.MotoInfo.Builder = {
    val b = MotoModel.MotoInfo.newBuilder()
    b.setMark(card.message.getMark.getCode)
    b.setModel(card.message.getModel.getCode)

    b.setDisplacement(RandomUtil.choose(Seq(20, 720, 900, 1200, 1300, 1370, 2200, 2400, 2500, 3000, 3500, 9000)))
    b.setHorsePower(RandomUtil.choose(Seq(0, 200, 210, 290, 470, 710, 980, 1370, 1500, 1570, 5530)))

    val motoCategory = card.motoCategory
    b.setMotoCategory(motoCategory)

    randomEngine(motoCategory).foreach(b.setEngine)
    randomTransmission(motoCategory).foreach(b.setTransmission)
    randomGear(motoCategory).foreach(b.setGear)

    motoCategory match {
      case MotoCategory.ATV =>
        b.setAtvType(randomAtvType)
      case MotoCategory.MOTORCYCLE =>
        b.setMotoType(randomMotoType)
      case MotoCategory.SNOWMOBILE =>
        b.setSnowmobileType(randomSnowmobileType)
      case _ =>
    }

    randomCylinderOrder(motoCategory).foreach(b.setCylinderOrder)
    randomCylinderAmount(motoCategory).foreach(b.setCylinderAmount)
    randomStrokesAmount(motoCategory).foreach(b.setStrokeAmount)

    b.putAllEquipment(randomEquipment(motoCategory).asJava)

    b
  }

  private def randomAtvType: Atv.Type = {
    RandomUtil.chooseEnum(MotoModel.Atv.Type.values())
  }

  private def randomMotoType: MotoModel.Moto.Type = {
    RandomUtil.chooseEnum(MotoModel.Moto.Type.values())
  }

  private def randomSnowmobileType: MotoModel.Snowmobile.Type = {
    RandomUtil.chooseEnum(MotoModel.Snowmobile.Type.values())
  }

  private def randomEngine(category: MotoCategory): Option[Moto.Engine] = {
    val engines = AutoruMotoCatalog.engineBindings
      .collect {
        case (cat, engine) if cat == category => Moto.Engine.forNumber(engine.getNumber)
      }
    Random.shuffle(engines).headOption
  }

  private def randomTransmission(category: MotoCategory): Option[Moto.Transmission] = {
    val transmissions = AutoruMotoCatalog.transmissionBindings._5
      .filter(_._2 == category)
      .map(x => Moto.Transmission.forNumber(x._3.getNumber))
    if (transmissions.isEmpty) None
    else Some(RandomUtil.choose(transmissions))
  }

  private def randomGear(category: MotoCategory): Option[Moto.GearType] = {
    val gearTypes = AutoruMotoCatalog.driveBindings._5
      .filter(_._2 == category)
      .map(x => Moto.GearType.forNumber(x._3.getNumber))
    if (gearTypes.isEmpty) None
    else Some(RandomUtil.choose(gearTypes))
  }

  private def randomCylinderOrder(category: MotoCategory): Option[Moto.CylinderOrder] = {
    val cylinderTypes = AutoruMotoCatalog.cylTypeBindings._5
      .filter(_._2 == category)
      .map(x => Moto.CylinderOrder.forNumber(x._3.getNumber))
    if (cylinderTypes.isEmpty) None
    else Some(RandomUtil.choose(cylinderTypes))
  }

  private def randomCylinderAmount(category: MotoCategory): Option[Moto.Cylinders] = {
    val cylinders = AutoruMotoCatalog.cylBindings._5
      .filter(_._2 == category)
      .map(x => Moto.Cylinders.forNumber(x._3.getNumber))
    if (cylinders.isEmpty) None
    else Some(RandomUtil.choose(cylinders))
  }

  private def randomStrokesAmount(category: MotoCategory): Option[Moto.Strokes] = {
    val strokes = AutoruMotoCatalog.strokesBindings._5
      .filter(_._2 == category)
      .map(x => Moto.Strokes.forNumber(x._3.getNumber))
    if (strokes.isEmpty) None
    else Some(RandomUtil.choose(strokes))
  }

  private def randomEquipment(category: MotoCategory): Map[String, java.lang.Boolean] = {
    val equipmentElemsSeq: Seq[String] = AutoruMotoCatalog.equipmentBindings._5
      .filter(_._2 == category)
      .map(x => x._3.name().stripPrefix("EQUIP_").toLowerCase.replaceAll("_", "-"))
    if (equipmentElemsSeq.isEmpty) Map.empty
    else {
      RandomUtil
        .sample(equipmentElemsSeq, min = 1, max = 5)
        .map(_ -> Boolean.box(true))
        .toMap
    }
  }

  protected val descriptionParts: List[String] = List(
    "Мотоцикл привезён под заказ из Японии",
    "Мотоцикл куплен у официального дилера в Москве",
    "Дисковые гидравлические тормоза",
    "Оригинальный ПТС",
    "Универсальный питбайк подходящий практически для всего!",
    "Средний размер и в меру мощный двигатель позволят эффективно тренироваться или выступать в гонках",
    "Крепкая стальная рама и самый популярный и беспроблемный двигатель YX 125cc",
    "На байк установлен хороший карбюратор mikuni VM22 с оптимальным набором настроек",
    "Та самая золотая середина",
    "Мотоцикл в идеальном техническом и визуальном состоянии",
    "Полная система выхлопа Termignoni",
    "Защиты рук Ducati Performance",
    "Зеркала с индикаторами поворота Ducati Perfomance",
    "Прижимная плита  и крышка сцепления Ducati Perfomance",
    "Карбоновые накладки"
  )
}
