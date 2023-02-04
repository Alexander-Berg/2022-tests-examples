package ru.yandex.vos2.autoru.utils.testforms

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Offer.Builder
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CommonModel.Damage.{CarPart, DamageType}
import ru.auto.api.CommonModel.{Damage, DiscountOptions, PaidService, Photo}
import ru.auto.api.{ApiOfferModel, CommonModel}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.Currency
import ru.yandex.vos2.autoru.catalog.CommonCatalog
import ru.yandex.vos2.autoru.components.AutoruCoreComponents
import ru.yandex.vos2.autoru.model.SalonPoiContacts.SalonClient
import ru.yandex.vos2.autoru.model.{AutoruCommonLogic, AutoruUser, SalonPoi, UserPhone}
import ru.yandex.vos2.autoru.utils.ApiFormUtils.{RichPriceInfoBuilder, RichPriceInfoOrBuilder}
import ru.yandex.vos2.autoru.utils.geo.RegionTypes
import ru.yandex.vos2.autoru.utils.testforms.CommonTestForms._
import ru.yandex.vos2.autoru.utils.vin.VinUtils
import ru.yandex.vos2.autoru.utils.{Colors, PaidServiceUtils, StsUtils}
import ru.yandex.vos2.dao.utils.SimpleRowMapper
import ru.yandex.vos2.model.{UserRef, UserRefAutoru}
import ru.yandex.vos2.services.mds.AutoruAllNamespaceSettings
import ru.yandex.vos2.util.{CurrencyUtils, ExternalAutoruUserRef, RandomUtil}

import scala.jdk.CollectionConverters._

class TestFormsGenerator(components: AutoruCoreComponents) {
  lazy val carTestForms = new CarTestForms(components)
  lazy val truckTestForms = new TruckTestForms(components)
  lazy val motoTestForms = new MotoTestForms(components)

  def randomUser: AutoruUser = carTestForms.randomUser

  def randomSalon: SalonPoi = carTestForms.randomSalon

  def randomOwnerIds(isDealer: Boolean): (Long, String, UserRef) = {
    if (isDealer) {
      val client: SalonClient = randomSalon.client.get
      val clientId = client.id
      val extClientId = ExternalAutoruUserRef.salonRef(clientId)
      val userRef = client.userRef
      (clientId, extClientId, userRef)
    } else {
      val user: AutoruUser = randomUser
      val userId = user.id
      val extUserId = ExternalAutoruUserRef.privateRef(userId)
      val userRef = user.userRef
      (userId, extUserId, userRef)
    }
  }

  def createForm(category: String, formParams: TestFormParams[_]): FormInfo = {
    category match {
      case "cars" =>
        carTestForms.createForm(formParams.asInstanceOf[TestFormParams[CarFormInfo]])
      case "trucks" =>
        truckTestForms.createForm(formParams.asInstanceOf[TestFormParams[TruckFormInfo]])
      case "trucks_special" =>
        truckTestForms.createForm(formParams.asInstanceOf[TestFormParams[TruckFormInfo]].copy(specialTruck = true))
      case "moto" =>
        motoTestForms.createForm(formParams.asInstanceOf[TestFormParams[MotoFormInfo]])
      case _ => sys.error(s"unexpected category $category")
    }
  }

  def updateForm(formInfo: FormInfo, formParams: TestFormParams[_]): FormInfo = {
    formInfo match {
      case f: CarFormInfo =>
        carTestForms.updateForm(f, formParams.asInstanceOf[TestFormParams[CarFormInfo]])
      case f: TruckFormInfo =>
        truckTestForms.updateForm(f, formParams.asInstanceOf[TestFormParams[TruckFormInfo]])
      case f: MotoFormInfo =>
        motoTestForms.updateForm(f, formParams.asInstanceOf[TestFormParams[MotoFormInfo]])
    }
  }

  def categoryByString(category: String): Category = {
    category match {
      case "cars" => Category.CARS
      case "trucks" => Category.TRUCKS
      case "moto" => Category.MOTO
      case _ => sys.error(s"Unexpected category $category")
    }
  }

}

/**
  * Created by andrey on 3/31/17.
  */
//scalastyle:off number.of.methods
abstract class CommonTestForms[T <: FormInfo](components: AutoruCoreComponents) {
  def catalog: CommonCatalog[T#CardType]

  def category: ApiOfferModel.Category

  def randomCard: T#CardType = {
    catalog.randomCard
  }

  def randomCard(formParams: TestFormParams[T]): T#CardType = {
    catalog.randomCard
  }

  val defaultParams: TestFormParams[T] = TestFormParams[T]()
  val defaultPrivateParams: TestFormParams[T] = TestFormParams[T](isDealer = false)
  val defaultDealerParams: TestFormParams[T] = TestFormParams[T](isDealer = true)

  def createForm(formParams: TestFormParams[T] = defaultParams): T = {
    val formInfo: T = if (formParams.isDealer) {
      generateDealerForm(formParams)
    } else generatePrivateForm(formParams)
    if (!formParams.excludeUserRefs.contains(formInfo.form.getUserRef)) {
      formInfo
    } else createForm(formParams)
  }

  def updateForm(formInfo: T, formParams: TestFormParams[T]): T = {
    val form: Offer = formInfo.form
    val isDealer = formInfo.optSalon.nonEmpty
    val card = formInfo.card
    val formBuilder = updateCommonForm(form, isDealer, FormInfo.startYear(card), FormInfo.endYear(card), formParams)
    categorySpecificUpdateForm(formBuilder, formParams)
    formInfo.withForm(formBuilder.build()).asInstanceOf[T]
  }

  def generatePrivateForm(formParams: TestFormParams[T] = defaultPrivateParams): T = {
    import formParams._
    val card = optCard.getOrElse(randomCard(formParams))
    val formBuilder: Offer.Builder = ApiOfferModel.Offer.newBuilder
    formBuilder.setCategory(category)
    categorySpecificFormPart(formParams.copy(isDealer = false, optCard = Some(card)), formBuilder)
    commonFormPart(formParams.copy(isDealer = false), formBuilder, FormInfo.startYear(card), FormInfo.endYear(card))
    val user = {
      val (user, seller) = generateCommonSeller(optOwnerId, optGeobaseId)
      formBuilder
        .setSeller(seller)
        .setUserRef(ExternalAutoruUserRef.fromUserRef(user.userRef).getOrElse(""))
      user
    }
    optCreateDate.foreach(cd => {
      formBuilder.getAdditionalInfoBuilder.setCreationDate(cd.getMillis)
    })

    FormInfo.privateForm(user, formBuilder.build(), card).asInstanceOf[T]
  }

  def generateDealerForm(formParams: TestFormParams[T] = defaultDealerParams): T = {
    val card = formParams.optCard.getOrElse(randomCard(formParams))
    val (salon, salonBilder) = generateSalonSeller(formParams.optOwnerId)
    val userRef: String = salon.client.map(_.userRef.toPlain).getOrElse("")
    val formBuilder = ApiOfferModel.Offer.newBuilder
    formBuilder.setCategory(category)
    categorySpecificFormPart(formParams.copy(isDealer = true, optCard = Some(card)), formBuilder)
    commonFormPart(formParams.copy(isDealer = true), formBuilder, FormInfo.startYear(card), FormInfo.endYear(card))
    formBuilder
      .setUserRef(ExternalAutoruUserRef.fromUserRef(userRef).getOrElse(""))
      .setSalon(salonBilder)
    formParams.optCreateDate.foreach(cd => {
      formBuilder.getAdditionalInfoBuilder.setCreationDate(cd.getMillis)
    })
    if (formParams.customAddress) {
      formBuilder.getSellerBuilder.setCustomLocation(true)
      formBuilder.getSellerBuilder.getLocationBuilder.setAddress(randomAddress)
    }
    FormInfo.dealerForm(salon, formBuilder.build(), card).asInstanceOf[T]
  }

  def randomColorHex: String = {
    RandomUtil.choose(Colors.values).searcherColor
  }

  def randomPrice: Double = {
    RandomUtil.nextDouble(100000, 5000000).toLong.toDouble
  }

  protected def descriptionParts: List[String]

  def randomDescription: String = {
    RandomUtil.sample(descriptionParts, min = 1).mkString(". ")
  }

  private def updateCommonForm(readForm: ApiOfferModel.Offer,
                               isDealer: Boolean,
                               genStartYear: Int,
                               genEndYear: Option[Int],
                               formParams: TestFormParams[T]): Offer.Builder = {
    val formBuilder: Builder = readForm.toBuilder
    // AUTORUAPI-4698
    val optMinMileage = if (!isDealer) Option(readForm.getState.getMileage).filter(_ > 0) else None
    formBuilder
      .setColorHex(randomColorHex)
      .setPriceInfo(
        CommonModel.PriceInfo
          .newBuilder()
          .setPrice(readForm.getPriceInfo.selectPrice + 1000) // чтобы наверняка различалась
          .setCurrency(CurrencyUtils.fromCurrency(Currency.RUB))
      )
      .setSection(formParams.section)
      .setDescription(randomDescription)
      .setDocuments(
        randomDocuments(
          formBuilder,
          genStartYear,
          genEndYear,
          readForm.getDocuments.getSts,
          formParams.now,
          Some(readForm.getDocuments.getVin),
          Some(readForm.getDocuments.getLicensePlate)
        )
      )
      .setState(randomState(formParams.section, formParams.isYandexVideo, optMinMileage))
      .clearBadges()
      .addAllServices(randomServices.asJava)
      // при редактировании поле hidden не учитывается, объявление остается в том, статусе, в каком было
      // поэтому мы тут всегда меняем его, чтобы проверить этот факт
      .setAdditionalInfo(readForm.getAdditionalInfo.toBuilder.setHidden(!readForm.getAdditionalInfo.getHidden).build())
      .setDiscountOptions(randomDiscountOptions)
    if (formParams.generateBadges) formBuilder.addAllBadges(randomBadges.asJava)
    if (!isDealer) {
      val optUserId: Option[Long] = ExternalAutoruUserRef.fromExt(readForm.getUserRef).flatMap {
        case UserRefAutoru(id) => Some(id)
        case _ => None
      }
      val optGeoBaseId = if (formParams.sameGeobaseId) {
        Option(readForm.getSeller.getLocation.getGeobaseId)
          .filter(_ > 0)
          .orElse(Option(readForm.getPrivateSeller.getLocation.getGeobaseId).filter(_ > 0))
      } else formParams.optGeobaseId

      formBuilder.setSeller(generateCommonSeller(optUserId, optGeoBaseId)._2).clearPrivateSeller()

    } else {
      // для салонов не будем тут ничего указывать
      formBuilder.clearSeller().clearPrivateSeller()
    }
    formParams.optCreateDate.foreach(cd => {
      formBuilder.getAdditionalInfoBuilder.setCreationDate(cd.getMillis)
    })
    formBuilder
  }

  protected def commonFormPart(formParams: TestFormParams[T],
                               formBuilder: ApiOfferModel.Offer.Builder,
                               genStartYear: Int,
                               genEndYear: Option[Int]): Unit = {
    import formParams._
    formBuilder
      .setColorHex(randomColorHex)
      .setSection(section)
      .setAvailability(availability)
      .setPriceInfo(
        CommonModel.PriceInfo
          .newBuilder()
          .setPrice(randomPrice)
          .setCurrency(CurrencyUtils.fromCurrency(Currency.RUB))
      )
      .setDescription(randomDescription)
      .setDocuments(
        randomDocuments(formBuilder, genStartYear, genEndYear, now = now)
      )
      .setState(randomState(section, isYandexVideo))
      .setAdditionalInfo(
        ApiOfferModel.AdditionalInfo
          .newBuilder()
          .setHidden(hidden)
          .setNotDisturb(true)
          .setExchange(randomExchange)
          .setHaggle(randomHaggle)
      )
      .setDiscountOptions(randomDiscountOptions)
      .addAllServices(randomServices.asJava)
      .addAllTags(randomTags.asJava)
    if (generateBadges) formBuilder.addAllBadges(randomBadges.asJava)
  }

  protected def categorySpecificFormPart(formParams: TestFormParams[T], builder: ApiOfferModel.Offer.Builder): Unit

  protected def categorySpecificUpdateForm(builder: ApiOfferModel.Offer.Builder, formParams: TestFormParams[T]): Unit

  def randomDiscountOptions: DiscountOptions.Builder = {
    DiscountOptions
      .newBuilder()
      .setCredit(RandomUtil.nextInt(1000))
      .setInsurance(RandomUtil.nextInt(1000))
      .setTradein(RandomUtil.nextInt(1000))
      .setMaxDiscount(3000 + RandomUtil.nextInt(1000))
  }

  def randomDocuments(form: ApiOfferModel.OfferOrBuilder,
                      genStartYear: Int,
                      genEndYear: Option[Int],
                      sts: String = randomSts,
                      now: DateTime,
                      optVin: Option[String] = None,
                      optLicensePlate: Option[String] = None): ApiOfferModel.Documents.Builder = {
    val startYear: Int = genStartYear max 1890
    val endYear: Int = genEndYear.getOrElse(now.getYear) max startYear + 1
    val productionYear = randomYear(startYear, endYear)
    val productionDate: DateTime = new DateTime(productionYear, 1, 1, 0, 0, 0, 0)
    val purchaseDate = randomDate(productionDate, now)
    val warrantyExpire = randomDate(now.plusDays(2), now.plusYears(10))

    ApiOfferModel.Documents
      .newBuilder()
      .setOwnersNumber(2)
      .setPtsOriginal(true)
      .setPts(PtsStatus.ORIGINAL)
      .setCustomCleared(true)
      .setPurchaseDate(purchaseDate)
      .setYear(productionYear)
      .setSts(sts)
      .setLicensePlate(optLicensePlate.getOrElse(randomLicensePlate))
      .setWarranty(true)
      .setWarrantyExpire(warrantyExpire)
      .setVin(optVin.getOrElse(randomVin(form)))
  }

  private def randomYear(startYear: Int, endYear: Int): Int = {
    RandomUtil.nextInt(startYear, endYear + 1)
  }

  private def randomDate(dateStart: DateTime, dateEnd: DateTime): CommonModel.Date = {
    val randomDate = new DateTime(RandomUtil.nextLong(dateStart.getMillis, dateEnd.getMillis))
    CommonModel.Date
      .newBuilder()
      .setYear(randomDate.getYear)
      .setMonth(randomDate.getMonthOfYear)
      .setDay(randomDate.getDayOfMonth)
      .build()
  }

  def randomState(section: Section,
                  isYandexVideo: Boolean = RandomUtil.nextBool(0.2),
                  optMinMileage: Option[Int] = None): ApiOfferModel.State.Builder = {
    val mileage: Int = if (section == Section.USED) randomMileage(optMinMileage) else 0
    ApiOfferModel.State
      .newBuilder()
      .setMileage(mileage)
      .setStateNotBeaten(true)
      .setCondition(Condition.CONDITION_OK)
      .setVideo(randomVideo(isYandexVideo))
      .addAllDamages(randomDamages.asJava)
  }

  private def randomMileage(optMinMileage: Option[Int]): Int = {
    optMinMileage
      .map(mileage => RandomUtil.nextInt(mileage + 1, 1000000))
      .getOrElse(
        RandomUtil.nextInt(1, 1000000)
      )
  }

  private val imageNames: Seq[String] = components.oldSalesDatabase.master.jdbc
    .query(
      """(select name from all7.sales_images_10425 limit 5) union all
      |(select name from all7.sales_images_10431 limit 16) union all
      |(select name from all7.sales_images_10432 limit 9) union all
      |(select name from all7.sales_images_10433 limit 19) union all
      |(select name from all7.sales_images_10442 limit 6) union all
      |(select name from all7.sales_images_10443 limit 5) union all
      |(select name from all7.sales_images_10372 limit 9)""".stripMargin,
      SimpleRowMapper(rs => rs.getString(1))
    )
    .asScala
    .toSeq
    .map(name => s"autoru-all:$name")

  private def randomImages: Seq[Photo] = {
    RandomUtil.sample(imageNames, min = 1, max = 5).map { name =>
      Photo
        .newBuilder()
        .setName(name)
        .setNamespace(AutoruAllNamespaceSettings.namespace)
        .build()
    }
  }

  private val userIds = components.oldOfficeDatabase.master.jdbc
    .query(
      "select id from users.user where id in (select user_id from users.phone_numbers) limit 7",
      SimpleRowMapper(rs => Long.box(rs.getLong(1)))
    )
    .asScala
    .toSeq

  private val users: Map[Long, AutoruUser] = components.autoruUsersDao.getUsers(userIds.map(_.toLong))(Traced.empty)

  def generatePrivateSeller(optUserId: Option[Long],
                            optGeoBaseId: Option[Long] = None): (AutoruUser, ApiOfferModel.PrivateSeller.Builder) = {
    val user: AutoruUser = optUserId.flatMap(userId => users.get(userId)).getOrElse(randomUser)
    val builder: PrivateSeller.Builder = ApiOfferModel.PrivateSeller.newBuilder()
    builder
      .setName("Христофор")
      .addAllPhones(randomPhones(user.phones).asJava)
      .setRedirectPhones(true)
      .setLocation(
        ApiOfferModel.Location
          .newBuilder()
          .setAddress("Рублёвское шоссе")
          .setCoord(CommonModel.GeoPoint.newBuilder().setLatitude(43.905251).setLongitude(30.261402))
          .setGeobaseId(optGeoBaseId.getOrElse(randomGeobaseId))
      )
    (user, builder)
  }

  def generateCommonSeller(optUserId: Option[Long],
                           optGeoBaseId: Option[Long] = None): (AutoruUser, ApiOfferModel.Seller.Builder) = {
    val user: AutoruUser = optUserId.flatMap(userId => users.get(userId)).getOrElse(randomUser)
    val builder: Seller.Builder = ApiOfferModel.Seller.newBuilder()
    builder
      .setName("Христофор")
      .setUnconfirmedEmail("example@example.org")
      .addAllPhones(randomPhones(user.phones).asJava)
      .setRedirectPhones(true)
      .setChatsEnabled(true)
      .setLocation(
        ApiOfferModel.Location
          .newBuilder()
          .setAddress("Рублёвское шоссе")
          .setCoord(CommonModel.GeoPoint.newBuilder().setLatitude(43.905251).setLongitude(30.261402))
          .setGeobaseId(optGeoBaseId.getOrElse(randomGeobaseId))
      )
    (user, builder)
  }

  def randomUser: AutoruUser = {
    RandomUtil.choose(users.values.toSeq)
  }

  private def randomPhones(userPhones: Seq[UserPhone]): List[Phone] = {
    val phone = RandomUtil.choose(userPhones.filter(_.status == 1))
    val (callHourStart, callHourEnd) = randomCallHours
    List(
      ApiOfferModel.Phone
        .newBuilder()
        .setPhone(phone.phone.toString)
        .setCallHourStart(callHourStart)
        .setCallHourEnd(callHourEnd)
        .build()
    )
  }

  private val regions = components.regionTree.regions.toSeq.filter(r => {
    r.`type` == RegionTypes.City || r.`type` == RegionTypes.Village
  })

  def randomGeobaseId: Long = {
    RandomUtil.choose(regions).id
  }

  private val salonIds = components.oldOfficeDatabase.master.jdbc
    .query("select id from poi7.poi limit 6", SimpleRowMapper(rs => Long.box(rs.getLong(1))))
    .asScala
    .toSeq

  // clientId -> salon
  private val salons: Map[Long, SalonPoi] =
    components.autoruSalonsDao
      .getSalonPoi(salonIds.map(_.toLong))(Traced.empty)
      .values
      .flatMap(salon => {
        salon.client.map(client => (client.id, salon))
      })
      .toMap

  def generateSalonSeller(optClientId: Option[Long]): (SalonPoi, ApiOfferModel.Salon.Builder) = {
    // с вероятностью 0.9 не будем генерить айдишник салона, он должен быть вычислен сам в этом случае
    val salon: SalonPoi = optClientId.flatMap(clientId => salons.get(clientId)).getOrElse(randomSalon)
    val builder = ApiOfferModel.Salon.newBuilder()
    // с очень низкой вероятностью заполняем эти поля, потому что мы должны мочь обойтись и без них
    if (RandomUtil.nextBool(0.1)) {
      builder
        .setSalonId(salon.id)
        .setName(salon.properties.getOrElse("title", ""))
    }
    (salon, builder)
  }

  def randomSalon: SalonPoi = {
    RandomUtil.choose(salons.values.toSeq)
  }

  protected def nonEmptyNumber(str: String): Boolean = {
    str.nonEmpty && str.forall(_.isDigit)
  }
}

object CommonTestForms {

  def randomSts: String = {
    // ^$|^[0-9]{2}[А-ЯЁ]{2}[0-9]{6}$|^[0-9]{10}$
    val part1 = RandomUtil.nextDigits(2)
    val part2 = RandomUtil.randomSymbols(2, ('А', 'Я'), ('Ё', 'Ё'))
    val part3 = RandomUtil.nextDigits(6)
    val ans = s"$part1$part2$part3"
    if (StsUtils.checkSts(ans)) ans
    else {
      println(s"wrong sts: $ans")
      randomSts
    }
  }

  def randomVin(form: ApiOfferModel.OfferOrBuilder): String = {
    val wheelLeft = AutoruCommonLogic.isWheelLeft(form)
    val year = form.getDocuments.getYear
    val isSpecial = AutoruCommonLogic.isSpecial(form.getTruckInfo.getTruckCategory)
    randomVin(wheelLeft, year, isSpecial)
  }

  def randomVin(wheelLeft: Boolean = true, year: Int = 1998, isSpecial: Boolean = false): String = {
    val ans = if (wheelLeft) {
      // ^[A-Za-z0-9]{13}[0-9]{4}$ - except I, O, Q letters
      val part1 = RandomUtil.randomSymbols(13, ('A', 'H'), ('J', 'N'), ('P', 'P'), ('R', 'Z'), ('0', '9'))
      val part2 = RandomUtil.nextDigits(4)
      s"$part1$part2"
    } else {
      val len = RandomUtil.nextInt(8, 31)
      RandomUtil.randomSymbols(len, ('A', 'H'), ('J', 'N'), ('P', 'P'), ('R', 'Z'), ('0', '9'), ('-', '-'))
    }
    if (VinUtils.checkVin(ans, wheelLeft, year, isSpecial, None).valid) ans
    else {
      println(s"wrong vin: $ans")
      randomVin(wheelLeft, year, isSpecial)
    }
  }

  def randomLicensePlate: String = {
    val p1 = RandomUtil.choose("АВЕКМНОРСТУХ").toString
    val p2 = RandomUtil.chooseN(3, "0123456789").mkString
    val p3 = RandomUtil.chooseN(2, "АВЕКМНОРСТУХ").mkString
    val p4 = RandomUtil.chooseN(2, "0123456789").mkString
    s"$p1$p2$p3$p4"
  }

  private val yandexVideoIds = Seq("m-63774-156a07376d0-87d395248cef988d", "m-63774-156a07376d0-87d395248cef988d")

  private val youtubeIds = Seq(
    "MJIC9MWhrrs",
    "_HVhaVB8olU",
    "fRr0ubZqXXM",
    "UWYjvpfLuVs",
    "aqXlrMMT5qM",
    "joFv-TjTNC8",
    "0FwTBdZCMBQ",
    "fYPz68fMoAw",
    "AGhfnXZiFSs",
    "4ZDLoBp8gK8",
    "AmtJ5cry0xQ"
  )

  def randomVideo(isYandexVideo: Boolean = RandomUtil.nextBool(0.2)): CommonModel.Video.Builder = {
    if (isYandexVideo) {
      val yandexVideoId = RandomUtil.choose(yandexVideoIds)
      CommonModel.Video.newBuilder().setYandexId(yandexVideoId)
    } else {
      val youtubeId = RandomUtil.choose(youtubeIds)
      CommonModel.Video.newBuilder().setYoutubeUrl(s"https://youtube.com/watch?v=$youtubeId").setYoutubeId(youtubeId)
    }
  }

  private val carParts: Array[Damage.CarPart] = CarPart
    .values()
    .filter(c => {
      c != CarPart.UNRECOGNIZED && c != CarPart.CAR_PART_UNKNOWN
    })

  def randomDamages: Seq[Damage] = {
    RandomUtil
      .sample(carParts, min = 1, max = 3)
      .map(carPart => {
        CommonModel.Damage
          .newBuilder()
          .setCarPart(carPart)
          .addAllType(List(randomDamageType).asJava)
          .setDescription(":(")
          .build()
      })
  }

  private val damageTypes: Array[DamageType] = DamageType
    .values()
    .filter(d => {
      d != DamageType.UNRECOGNIZED && d != DamageType.DAMAGE_TYPE_UNKNOWN
    })

  def randomDamageType: DamageType = {
    RandomUtil.choose(damageTypes)
  }

  def randomAddress: String = {
    RandomUtil.randomSymbols(10, ('a', 'z'))
  }

  def randomCallHours: (Int, Int) = {
    val callHourStart = RandomUtil.nextInt(0, 23)
    val callHourEnd = RandomUtil.nextInt(callHourStart, 23)
    (callHourStart, callHourEnd)
  }

  private val popularBadges = Seq(
    "Два комплекта резины",
    "Тонировка",
    "Кондиционер",
    "Камера заднего вида",
    "Климат-контроль",
    "Кожаный салон",
    "Парктроник",
    "Автозапуск",
    "Коврики в подарок",
    "Webasto"
  )

  def randomBadges: Seq[String] = {
    RandomUtil.sample(popularBadges, min = 1, max = 3)
  }

  private val serviceTypes = PaidServiceUtils.services.filter(_ != "all_sale_badge").toSeq

  def randomServices: Seq[PaidService] = {
    RandomUtil.sample(serviceTypes, min = 1, max = 3).map(x => PaidService.newBuilder().setService(x).build())
  }

  def randomWheelLeft: Boolean = {
    RandomUtil.nextBool(0.8)
  }

  def randomExchange: Boolean = {
    RandomUtil.nextBool(0.3)
  }

  def randomHaggle: Boolean = {
    RandomUtil.nextBool(0.3)
  }

  private val tags = (1 to 10).map(i => s"tag$i")

  def randomTags: Seq[String] = RandomUtil.sample(tags, max = 3)
}
