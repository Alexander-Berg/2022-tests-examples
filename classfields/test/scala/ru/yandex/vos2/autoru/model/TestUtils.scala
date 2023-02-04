package ru.yandex.vos2.autoru.model

import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.cert.CertModel.BrandCertStatus
import ru.auto.panoramas.PanoramasModel
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.{CarInfo, MotoInfo, TruckInfo, BrandCertInfo => ModelBrandCertInfo}
import ru.yandex.vos2.BasicsModel.{CompositeStatus, Photo}
import ru.yandex.vos2.OfferModel.Multiposting.Classified
import ru.yandex.vos2.OfferModel.Multiposting.Classified.ClassifiedName
import ru.yandex.vos2.OfferModel.{Multiposting, Offer, OfferService}
import ru.yandex.vos2.UserModel.{User, UserContacts, UserType}
import ru.yandex.vos2.getNow
import ru.yandex.vos2.util.RandomUtil
import ru.yandex.vos2.util.Dates._

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.util.Random

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 16.09.16
  */
object TestUtils {

  def createUser(): User.Builder = {
    User
      .newBuilder()
      .setUserRef("a_123")
      .setUserType(UserType.UT_OWNER)
  }

  def createDealer(): User.Builder = {
    User
      .newBuilder()
      .setUserRef("ac_321")
      .setUserType(UserType.UT_AUTO_SALON)
  }

  def createUserContacts(): UserContacts.Builder = {
    UserContacts.newBuilder()
  }

  def createCarInfo(): CarInfo.Builder = {
    CarInfo
      .newBuilder()
      .setMark("FORD")
      .setModel("FOCUS")
  }

  def createTruckInfo(): TruckInfo.Builder = {
    TruckInfo
      .newBuilder()
      .setMark("URAL")
      .setModel("4320_TRAKTOR")
  }

  def createMotoInfo(): MotoInfo.Builder = {
    MotoInfo
      .newBuilder()
      .setMark("BMW")
      .setModel("F_650_GS")
  }

  def createBrandCertInfo(isActive: Boolean): ModelBrandCertInfo.Builder = {
    ModelBrandCertInfo
      .newBuilder()
      .setCertStatus(
        if (isActive) BrandCertStatus.BRAND_CERT_ACTIVE
        else BrandCertStatus.BRAND_CERT_INACTIVE
      )
      .setProgramAlias("JaguarApproved")
      .setVin("SALCA2BGXFH531452")
      .setCreated(System.currentTimeMillis())
  }

  def createAutoruOffer(hash: String = "hash",
                        dealer: Boolean = false,
                        category: Category = Category.CARS,
                        withPhoto: Boolean = false,
                        withEquipment: Boolean = false,
                        withExternalPanorama: Boolean = false,
                        withInternalPanorama: Boolean = false): AutoruOffer.Builder = {
    val b = AutoruOffer
      .newBuilder()
      .setVersion(1)
      .setHashCode(hash)
      .setSection(Section.USED)
      .setSellerType(if (dealer) AutoruOffer.SellerType.COMMERCIAL else AutoruOffer.SellerType.PRIVATE)
    b.setCategory(category)
    if (dealer) {
      b.getSalonBuilder
        .setSalonId("salon_id")
        .setTitle("salon title")
    } else {
      b.getSellerBuilder.setUserRef("a_123")
    }
    category match {
      case Category.CARS =>
        b.setCarInfo(createCarInfo())
      case Category.TRUCKS =>
        b.setTruckInfo(createTruckInfo())
      case Category.MOTO =>
        b.setMotoInfo(createMotoInfo())
      case _ =>
    }
    if (withPhoto)
      b.addPhoto(
        Photo
          .newBuilder()
          .setCreated(getNow)
          .setIsMain(true)
          .setOrder(0)
          .setName(RandomUtil.randomSymbols(16, ('A', 'Z')))
      )
    if (withEquipment) b.setCarInfo(CarInfo.newBuilder().addAllEquipment(getRandomEquipmentList().asJava))
    if (withExternalPanorama) b.addExternalPanorama(createExternalPanorama.build())
    if (withInternalPanorama) b.addInteriorPanorama(createInteriorPanorama)
    b
  }

  def getRandomEquipmentList(desiredOptionsNumber: Int = 5) = {
    val equipmentList = List(
      "airbag-side",
      "electro-trunk",
      "tinted-glass",
      "automatic-lighting-control",
      "auto-park",
      "front-seats-heat",
      "tyre-pressure",
      "audiosystem",
      "front-seats-heat-vent",
      "servo",
      "start-stop-function",
      "20-inch-wheels",
      "door-sill-panel",
      "leather-gear-stick",
      "volume-sensor"
    )
    val optionsNumber = if (desiredOptionsNumber < equipmentList.size) desiredOptionsNumber else equipmentList.size
    Random
      .shuffle(equipmentList)
      .take(optionsNumber)
      .map(AutoruOffer.Equipment.newBuilder.setEquipped(true).setName(_).build)
  }

  def createExternalPanorama: AutoruOffer.ExternalPanorama.Builder = {
    AutoruOffer.ExternalPanorama
      .newBuilder()
      .setId(RandomUtil.randomSymbols(16, ('A', 'Z')))
      .setStatus(AutoruOffer.ExternalPanorama.Status.COMPLETED)
      .setPublished(true)
  }

  def createInteriorPanorama: AutoruOffer.InteriorPanorama.Builder = {
    val builder = AutoruOffer.InteriorPanorama
      .newBuilder()
    val interiorBuilder = builder.getPanoramaBuilder
    val now = instantToTimestamp(Instant.now())
    interiorBuilder
      .setId(RandomUtil.randomSymbols(16, ('A', 'Z')))
      .setStatus(PanoramasModel.Status.COMPLETED)
      .setPublished(true)
    builder.setPoiCount(1)
    builder.setPublishedAt(now)
    builder.setUpdateAt(now)
    builder.setPanorama(interiorBuilder)
    builder.setPublished(true)
  }

  def createOffer(now: Long = System.currentTimeMillis(),
                  dealer: Boolean = false,
                  category: Category = Category.CARS,
                  withPhoto: Boolean = false,
                  withEquipment: Boolean = false,
                  withDescription: Boolean = false,
                  withExternalPanorama: Boolean = false,
                  withInternalPanorama: Boolean = false,
                  withMultiposting: Boolean = false): Offer.Builder = {
    val user = if (dealer) createDealer() else createUser()
    val offerId = AutoruOfferID.generateID(user.getUserRef)
    val id = offerId.id
    val b = Offer.newBuilder()
    b.setOfferID(offerId.toPlain)
      .setOfferIRef(id)
      .setUser(user)
      .setUserRef(user.getUserRef)
      .setOfferService(OfferService.OFFER_AUTO)
      .setTimestampCreate(now)
      .setTimestampUpdate(now)
      .setTimestampWillExpire(AutoruCommonLogic.expireDate(now).getMillis)
      .setOfferAutoru(
        createAutoruOffer(
          offerId.hash.get,
          dealer,
          category,
          withPhoto,
          withEquipment,
          withExternalPanorama,
          withInternalPanorama
        )
      )
    if (withDescription) b.setDescription(RandomUtil.randomSymbols(32, ('A', 'Z')))
    if (withMultiposting) b.setMultiposting(Multiposting.newBuilder().setStatus(CompositeStatus.CS_ACTIVE))
    b.clearFlag()
  }

  def createPhoto(name: String, main: Boolean = true, order: Int = 0): Photo.Builder = {
    createPhotoExt(name, name, angle = 0, blur = false, main, order)
  }

  def createPhotoExt(name: String,
                     origName: String,
                     angle: Int = 0,
                     blur: Boolean = false,
                     main: Boolean = true,
                     order: Int = 0): Photo.Builder = {
    val photoBuilder = Photo
      .newBuilder()
      .setIsMain(main)
      .setOrder(order)
      .setName(name)
      .setOrigName(origName)
      .setCreated(getNow)
    photoBuilder.getCurrentTransformBuilder.setAngle(angle).setBlur(blur)
    photoBuilder
  }

  def createMultiposting(status: CompositeStatus = CompositeStatus.CS_ACTIVE): Multiposting.Builder =
    Multiposting.newBuilder().setStatus(status)

  def createClassified(name: ClassifiedName = ClassifiedName.AUTORU,
                       status: CompositeStatus = CompositeStatus.CS_ACTIVE,
                       enabled: Boolean = true): Classified.Builder = {
    Classified
      .newBuilder()
      .setStatus(status)
      .setName(name)
      .setEnabled(enabled)
  }
}
