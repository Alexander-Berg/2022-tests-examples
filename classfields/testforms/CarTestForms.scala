package ru.yandex.vos2.autoru.utils.testforms

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.{ApiOfferModel, CarsModel, CommonModel}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.catalog.cars.model.CarCard
import ru.yandex.vos2.autoru.catalog.cars.{CarsCatalog, Dictionaries}
import ru.yandex.vos2.autoru.components.AutoruCoreComponents
import ru.yandex.vos2.autoru.dao.proxy.FormWriteParams
import ru.yandex.vos2.autoru.model.AutoruSale
import ru.yandex.vos2.util.RandomUtil

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 2/20/17.
  */
//scalastyle:off number.of.methods
class CarTestForms(components: AutoruCoreComponents) extends CommonTestForms[CarFormInfo](components) {
  outer =>
  import CarTestForms._
  val catalog: CarsCatalog = components.carsCatalog

  val category = Category.CARS

  // LeftWheelSupport / RightWheelSupport
  val techParamIds: TechParamsHolder = TechParamsHolder(
    Seq(2307396, 2416343, 6143500, 8237771, 20153178, 20478437, 7345356),
    Seq(2416343, 20478437, 20474809)
  )

  override def randomCard: CarCard = {
    components.carsCatalog.getCardByTechParamId(techParamIds.all(RandomUtil.nextInt(techParamIds.all.length))).get
  }

  @tailrec
  final override def randomCard(formParams: TestFormParams[CarFormInfo]): CarCard = {
    val validTechParams = formParams.wheelLeft match {
      case Some(true) => techParamIds.leftWheeled
      case Some(false) => techParamIds.rightWheeled
      case None => (techParamIds.leftWheeled ++ techParamIds.rightWheeled).distinct
    }
    val techParamId = validTechParams(RandomUtil.nextInt(validTechParams.length))

    val card = components.carsCatalog.getCardByTechParamId(techParamId).get
    if (formParams.gearType.isEmpty || formParams.gearType == card.gearType) card
    else randomCard(formParams)
  }

  override protected def categorySpecificFormPart(formParams: TestFormParams[CarFormInfo],
                                                  builder: ApiOfferModel.Offer.Builder): Unit = {
    val card: CarCard = formParams.optCard.getOrElse(randomCard(formParams))
    builder.setCarInfo(generateCarInfo(formParams.wheelLeft, card))
  }

  override protected def categorySpecificUpdateForm(builder: ApiOfferModel.Offer.Builder,
                                                    formParams: TestFormParams[CarFormInfo]): Unit = {}

  private def generateCarInfo(wheelLeft: Option[Boolean], card: CarCard): CarsModel.CarInfo.Builder = {
    val isLeft = wheelLeft.getOrElse {
      RandomUtil.choose(card.steeringWheels) == CommonModel.SteeringWheel.LEFT
    }

    val b = CarsModel.CarInfo
      .newBuilder()
      .setArmored(false)
      .setBodyType(card.bodyType.getOrElse(""))
      .setEngineType(card.engineType)
      .setTransmission(card.transmission)
      .setDrive(card.gearType)
      .setMark(card.mark.code)
      .setModel(card.model.code)
      .setSuperGenId(card.supergen.id)
      .setConfigurationId(card.configuration.id)
      .setComplectationId(card.complectation.id)
      .setTechParamId(card.techParamId)
      .setWheelLeft(isLeft)
      .setSteeringWheel(if (isLeft) CommonModel.SteeringWheel.LEFT else CommonModel.SteeringWheel.RIGHT)
    if (card.complectation.availableOptions.nonEmpty) {
      b.putAllEquipment(createEquipment(card.complectation.availableOptions).map(_ -> Boolean.box(true)).toMap.asJava)
    }
    b
  }

  private def createEquipment(availableOptions: List[String]): Seq[String] = {
    val opts = availableOptions.filter(Dictionaries.equipments.byCode(_).nonEmpty)
    val currentOptions = RandomUtil.sample(opts, max = 5)
    val excludesKeys = currentOptions
      .flatMap { key =>
        components.equipmentHolder.getEquipment(key).map(res => res.excludes.map(exclude => exclude -> res.code))
      }
      .flatten
      .toMap

    currentOptions.filterNot(
      excludesKeys.contains
    )
  }

  protected val descriptionParts: List[String] = List(
    "Комплект зимних шин в подарок",
    "Комплект летних шин в подарок",
    "Своевременное обслуживание",
    "Пройдены все ТО",
    "Куплена не в кредит",
    "Непрокуренный салон",
    "Сервисная книжка",
    "Не участвовала в ДТП",
    "Фаркоп",
    "Машина огонь",
    "Не бита, не крашена",
    "Сел - поехал"
  )

  private val formAutoruSaleConverter = components.formAutoruSaleConverter
  private val formOfferConverter = components.formOfferConverter

  def toAutoruSale(formInfo: CarFormInfo,
                   now: DateTime = DateTime.now(),
                   formWriteParams: FormWriteParams = FormWriteParams.empty): AutoruSale = {
    val ad = components.offersReader.loadAdditionalData(formInfo.userRef, formInfo.form)(Traced.empty)
    formAutoruSaleConverter.convertNew(formInfo.form, ad, now, formWriteParams, None, 0)(Traced.empty)
  }

  def createAutoruSale(formParams: TestFormParams[CarFormInfo] = TestFormParams(),
                       now: DateTime = DateTime.now(),
                       formWriteParams: FormWriteParams = FormWriteParams.empty): AutoruSale = {
    val formInfo = createForm(formParams)
    toAutoruSale(formInfo, now, formWriteParams)
  }

  def toOffer(formInfo: CarFormInfo,
              now: DateTime = DateTime.now(),
              formWriteParams: FormWriteParams = FormWriteParams.empty): OfferModel.Offer = {
    val ad = components.offersReader.loadAdditionalData(formInfo.userRef, formInfo.form)(Traced.empty)
    formOfferConverter.convertNewOffer(
      formInfo.userRef,
      Category.CARS,
      formInfo.form,
      ad,
      now.getMillis,
      None,
      formWriteParams
    )(Traced.empty)
  }
}

object CarTestForms {

  case class TechParamsHolder(leftWheeled: Seq[Long], rightWheeled: Seq[Long]) {
    val all: Seq[Long] = (leftWheeled ++ rightWheeled).distinct
  }
}
