package ru.yandex.vertis.feedprocessor.autoru.scheduler.converter

import org.apache.commons.io.IOUtils
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Section, SellerType}
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.TrucksModel.TruckCategory
import ru.yandex.vertis.feedprocessor.autoru.model.{Generators, ServiceInfo, Task, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.scheduler.parser.car.CarParser
import ru.yandex.vertis.feedprocessor.autoru.scheduler.parser.truck.TruckParser
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators.truckBusInfoGen
import AutoruExternalOfferXmlConverterSpec._
import ru.yandex.vertis.feedprocessor.{PropSpecBase, WordSpecBase}
import ru.yandex.vertis.feedprocessor.util.YoutubeUrlParser

import java.nio.charset.StandardCharsets.UTF_8

class AutoruExternalOfferXmlConverterSpec extends WordSpecBase with PropSpecBase {

  "AutoruExternalOfferXmlConverter" should {
    "convert external car offer to xml [unknown classified]" in {

      val carOfferGenerator = AutoruGenerators.carExternalOfferGen(Generators.newTasksGen)

      forAll(
        carOfferGenerator.map(
          _.copy(
            mark = mark,
            model = model,
            classifieds = classifieds,
            vin = vin,
            sts = sts,
            description = description,
            images = images,
            avitoImages = avitoImages,
            dromImages = dromImages,
            avitoSaleServices = avitoSaleServices,
            dromSaleServices = dromSaleServices,
            avitoDescription = avitoDescription,
            dromDescription = dromDescription
          )
        )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified = None).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val carParser = new CarParser(feed)(taskContext)

        val results = carParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        offer.video.map(YoutubeUrlParser.createFullUrl) shouldEqual Some("https://www.youtube.com/watch?v=MJIC9MWhrrs")

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.complectation shouldEqual offer.complectation
        parsedOffer.bodyType shouldEqual offer.bodyType
        parsedOffer.wheel shouldEqual offer.wheel
        parsedOffer.color shouldEqual offer.color
        parsedOffer.metallic shouldEqual offer.metallic
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.ownersNumber shouldEqual offer.ownersNumber
        parsedOffer.run shouldEqual offer.run.filter(_ > 0)
        parsedOffer.year shouldEqual offer.year
        parsedOffer.registryYear shouldEqual offer.registryYear
        parsedOffer.doorsCount shouldEqual offer.doorsCount
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.creditDiscount shouldEqual offer.creditDiscount
        parsedOffer.insuranceDiscount shouldEqual offer.insuranceDiscount
        parsedOffer.tradeinDiscount shouldEqual offer.tradeinDiscount
        parsedOffer.maxDiscount shouldEqual offer.maxDiscount
        parsedOffer.description shouldEqual offer.description
        parsedOffer.extras shouldEqual offer.extras.map(_.toList.map(_.toLowerCase))
        parsedOffer.images shouldEqual offer.images
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.avitoDescription shouldEqual offer.avitoDescription
        parsedOffer.dromDescription shouldEqual offer.dromDescription
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.warrantyExpire shouldEqual offer.warrantyExpire
        parsedOffer.sts shouldEqual offer.sts
        parsedOffer.armored shouldEqual offer.armored
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.acceptedAutoruExclusive shouldEqual offer.acceptedAutoruExclusive
        parsedOffer.onlineViewAvailable shouldEqual offer.onlineViewAvailable
        parsedOffer.bookingAllowed shouldEqual offer.bookingAllowed

        // if initial offer has unification info then parsed will have EngineInfo instead of ModificationString
        /* @TODO: FIX flacky test - https://st.yandex-team.ru/VSDEALERS-1876
        offer.unification match {
          case None => parsedOffer.modification shouldEqual offer.modification
          case Some(value) =>
            parsedOffer.modification shouldEqual EngineInfo(
              engineVolume = value.displacement,
              enginePower = value.horsePower.getOrElse(0),
              engineType = value.engineType.getOrElse(""),
              gearbox = value.gearType.getOrElse(""),
              drive = value.transmission.getOrElse(""),
              nameplate = None
            )
        }
         */

        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
        parsedOffer.exteriorPanoramaState shouldEqual offer.exteriorPanoramaState
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
      }
    }

    "convert external car offer to xml [AVITO classified]" in {
      val carOfferGenerator = AutoruGenerators.carExternalOfferGen(Generators.newTasksGen)
      val classified = Some(ClassifiedName.AVITO)

      forAll(
        carOfferGenerator.map(
          _.copy(
            mark = mark,
            model = model,
            classifieds = classifieds,
            vin = vin,
            sts = sts,
            description = description,
            images = images,
            avitoImages = avitoImages,
            dromImages = dromImages,
            avitoSaleServices = avitoSaleServices,
            dromSaleServices = dromSaleServices,
            avitoDescription = avitoDescription,
            dromDescription = dromDescription
          )
        )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val carParser = new CarParser(feed)(taskContext)

        val results = carParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        offer.video.map(YoutubeUrlParser.createFullUrl) shouldEqual Some("https://www.youtube.com/watch?v=MJIC9MWhrrs")

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.complectation shouldEqual offer.complectation
        parsedOffer.bodyType shouldEqual offer.bodyType
        parsedOffer.wheel shouldEqual offer.wheel
        parsedOffer.color shouldEqual offer.color
        parsedOffer.metallic shouldEqual offer.metallic
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.ownersNumber shouldEqual offer.ownersNumber
        parsedOffer.run shouldEqual offer.run.filter(_ > 0)
        parsedOffer.year shouldEqual offer.year
        parsedOffer.registryYear shouldEqual offer.registryYear
        parsedOffer.doorsCount shouldEqual offer.doorsCount
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.creditDiscount shouldEqual offer.creditDiscount
        parsedOffer.insuranceDiscount shouldEqual offer.insuranceDiscount
        parsedOffer.tradeinDiscount shouldEqual offer.tradeinDiscount
        parsedOffer.maxDiscount shouldEqual offer.maxDiscount
        parsedOffer.description shouldEqual offer.avitoDescription
        parsedOffer.extras shouldEqual offer.extras.map(_.toList.map(_.toLowerCase))
        parsedOffer.images shouldEqual offer.avitoImages
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.avitoDescription shouldEqual offer.avitoDescription
        parsedOffer.dromDescription shouldEqual offer.dromDescription
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.warrantyExpire shouldEqual offer.warrantyExpire
        parsedOffer.sts shouldEqual offer.sts
        parsedOffer.armored shouldEqual offer.armored
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.acceptedAutoruExclusive shouldEqual offer.acceptedAutoruExclusive
        parsedOffer.onlineViewAvailable shouldEqual offer.onlineViewAvailable
        parsedOffer.bookingAllowed shouldEqual offer.bookingAllowed

        // if initial offer has unification info then parsed will have EngineInfo instead of ModificationString
        /* @TODO: FIX flacky test - https://st.yandex-team.ru/VSDEALERS-1876
        offer.unification match {
          case None => parsedOffer.modification shouldEqual offer.modification
          case Some(value) =>
            parsedOffer.modification shouldEqual EngineInfo(
              engineVolume = value.displacement,
              enginePower = value.horsePower.getOrElse(0),
              engineType = value.engineType.getOrElse(""),
              gearbox = value.gearType.getOrElse(""),
              drive = value.transmission.getOrElse(""),
              nameplate = None
            )
        }
         */

        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
        parsedOffer.exteriorPanoramaState shouldEqual offer.exteriorPanoramaState
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
      }
    }

    "convert external car offer to xml [AVITO classified, fallback to global]" in {
      val carOfferGenerator = AutoruGenerators.carExternalOfferGen(Generators.newTasksGen)
      val classified = Some(ClassifiedName.AVITO)

      forAll(
        carOfferGenerator.map(
          _.copy(
            mark = mark,
            model = model,
            classifieds = classifieds,
            vin = vin,
            sts = sts,
            description = description,
            images = images,
            avitoImages = Seq.empty,
            dromImages = dromImages,
            avitoSaleServices = avitoSaleServices,
            dromSaleServices = dromSaleServices,
            avitoDescription = None,
            dromDescription = dromDescription
          )
        )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val carParser = new CarParser(feed)(taskContext)

        val results = carParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        offer.video.map(YoutubeUrlParser.createFullUrl) shouldEqual Some("https://www.youtube.com/watch?v=MJIC9MWhrrs")

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.complectation shouldEqual offer.complectation
        parsedOffer.bodyType shouldEqual offer.bodyType
        parsedOffer.wheel shouldEqual offer.wheel
        parsedOffer.color shouldEqual offer.color
        parsedOffer.metallic shouldEqual offer.metallic
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.ownersNumber shouldEqual offer.ownersNumber
        parsedOffer.run shouldEqual offer.run.filter(_ > 0)
        parsedOffer.year shouldEqual offer.year
        parsedOffer.registryYear shouldEqual offer.registryYear
        parsedOffer.doorsCount shouldEqual offer.doorsCount
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.creditDiscount shouldEqual offer.creditDiscount
        parsedOffer.insuranceDiscount shouldEqual offer.insuranceDiscount
        parsedOffer.tradeinDiscount shouldEqual offer.tradeinDiscount
        parsedOffer.maxDiscount shouldEqual offer.maxDiscount
        parsedOffer.description shouldEqual offer.description
        parsedOffer.extras shouldEqual offer.extras.map(_.toList.map(_.toLowerCase))
        parsedOffer.images shouldEqual offer.images
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.avitoDescription shouldEqual offer.avitoDescription
        parsedOffer.dromDescription shouldEqual offer.dromDescription
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.warrantyExpire shouldEqual offer.warrantyExpire
        parsedOffer.sts shouldEqual offer.sts
        parsedOffer.armored shouldEqual offer.armored
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.acceptedAutoruExclusive shouldEqual offer.acceptedAutoruExclusive
        parsedOffer.onlineViewAvailable shouldEqual offer.onlineViewAvailable
        parsedOffer.bookingAllowed shouldEqual offer.bookingAllowed

        // if initial offer has unification info then parsed will have EngineInfo instead of ModificationString
        /* @TODO: FIX flacky test - https://st.yandex-team.ru/VSDEALERS-1876
        offer.unification match {
          case None => parsedOffer.modification shouldEqual offer.modification
          case Some(value) =>
            parsedOffer.modification shouldEqual EngineInfo(
              engineVolume = value.displacement,
              enginePower = value.horsePower.getOrElse(0),
              engineType = value.engineType.getOrElse(""),
              gearbox = value.gearType.getOrElse(""),
              drive = value.transmission.getOrElse(""),
              nameplate = None
            )
        }
         */

        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
        parsedOffer.exteriorPanoramaState shouldEqual offer.exteriorPanoramaState
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
      }
    }

    "convert external car offer to xml [DROM classified]" in {
      val carOfferGenerator = AutoruGenerators.carExternalOfferGen(Generators.newTasksGen)
      val classified = Some(ClassifiedName.DROM)

      forAll(
        carOfferGenerator.map(
          _.copy(
            mark = mark,
            model = model,
            classifieds = classifieds,
            vin = vin,
            sts = sts,
            description = description,
            images = images,
            avitoImages = avitoImages,
            dromImages = dromImages,
            avitoSaleServices = avitoSaleServices,
            dromSaleServices = dromSaleServices,
            avitoDescription = avitoDescription,
            dromDescription = dromDescription
          )
        )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val carParser = new CarParser(feed)(taskContext)

        val results = carParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        offer.video.map(YoutubeUrlParser.createFullUrl) shouldEqual Some("https://www.youtube.com/watch?v=MJIC9MWhrrs")

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.complectation shouldEqual offer.complectation
        parsedOffer.bodyType shouldEqual offer.bodyType
        parsedOffer.wheel shouldEqual offer.wheel
        parsedOffer.color shouldEqual offer.color
        parsedOffer.metallic shouldEqual offer.metallic
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.ownersNumber shouldEqual offer.ownersNumber
        parsedOffer.run shouldEqual offer.run.filter(_ > 0)
        parsedOffer.year shouldEqual offer.year
        parsedOffer.registryYear shouldEqual offer.registryYear
        parsedOffer.doorsCount shouldEqual offer.doorsCount
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.creditDiscount shouldEqual offer.creditDiscount
        parsedOffer.insuranceDiscount shouldEqual offer.insuranceDiscount
        parsedOffer.tradeinDiscount shouldEqual offer.tradeinDiscount
        parsedOffer.maxDiscount shouldEqual offer.maxDiscount
        parsedOffer.description shouldEqual offer.dromDescription
        parsedOffer.extras shouldEqual offer.extras.map(_.toList.map(_.toLowerCase))
        parsedOffer.images shouldEqual offer.dromImages
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.avitoDescription shouldEqual offer.avitoDescription
        parsedOffer.dromDescription shouldEqual offer.dromDescription
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.warrantyExpire shouldEqual offer.warrantyExpire
        parsedOffer.sts shouldEqual offer.sts
        parsedOffer.armored shouldEqual offer.armored
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.acceptedAutoruExclusive shouldEqual offer.acceptedAutoruExclusive
        parsedOffer.onlineViewAvailable shouldEqual offer.onlineViewAvailable
        parsedOffer.bookingAllowed shouldEqual offer.bookingAllowed

        // if initial offer has unification info then parsed will have EngineInfo instead of ModificationString
        /* @TODO: FIX flacky test - https://st.yandex-team.ru/VSDEALERS-1876
        offer.unification match {
          case None => parsedOffer.modification shouldEqual offer.modification
          case Some(value) =>
            parsedOffer.modification shouldEqual EngineInfo(
              engineVolume = value.displacement,
              enginePower = value.horsePower.getOrElse(0),
              engineType = value.engineType.getOrElse(""),
              gearbox = value.gearType.getOrElse(""),
              drive = value.transmission.getOrElse(""),
              nameplate = None
            )
        }
         */

        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
        parsedOffer.exteriorPanoramaState shouldEqual offer.exteriorPanoramaState
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
      }
    }

    "convert external car offer to xml [DROM classified, fallback to global]" in {
      val carOfferGenerator = AutoruGenerators.carExternalOfferGen(Generators.newTasksGen)
      val classified = Some(ClassifiedName.DROM)

      forAll(
        carOfferGenerator.map(
          _.copy(
            mark = mark,
            model = model,
            classifieds = classifieds,
            vin = vin,
            sts = sts,
            description = description,
            images = images,
            avitoImages = avitoImages,
            dromImages = Seq.empty,
            avitoSaleServices = avitoSaleServices,
            dromSaleServices = dromSaleServices,
            avitoDescription = avitoDescription,
            dromDescription = None
          )
        )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val carParser = new CarParser(feed)(taskContext)

        val results = carParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        offer.video.map(YoutubeUrlParser.createFullUrl) shouldEqual Some("https://www.youtube.com/watch?v=MJIC9MWhrrs")

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.complectation shouldEqual offer.complectation
        parsedOffer.bodyType shouldEqual offer.bodyType
        parsedOffer.wheel shouldEqual offer.wheel
        parsedOffer.color shouldEqual offer.color
        parsedOffer.metallic shouldEqual offer.metallic
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.ownersNumber shouldEqual offer.ownersNumber
        parsedOffer.run shouldEqual offer.run.filter(_ > 0)
        parsedOffer.year shouldEqual offer.year
        parsedOffer.registryYear shouldEqual offer.registryYear
        parsedOffer.doorsCount shouldEqual offer.doorsCount
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.creditDiscount shouldEqual offer.creditDiscount
        parsedOffer.insuranceDiscount shouldEqual offer.insuranceDiscount
        parsedOffer.tradeinDiscount shouldEqual offer.tradeinDiscount
        parsedOffer.maxDiscount shouldEqual offer.maxDiscount
        parsedOffer.description shouldEqual offer.description
        parsedOffer.extras shouldEqual offer.extras.map(_.toList.map(_.toLowerCase))
        parsedOffer.images shouldEqual offer.images
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.avitoDescription shouldEqual offer.avitoDescription
        parsedOffer.dromDescription shouldEqual offer.dromDescription
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.warrantyExpire shouldEqual offer.warrantyExpire
        parsedOffer.sts shouldEqual offer.sts
        parsedOffer.armored shouldEqual offer.armored
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.acceptedAutoruExclusive shouldEqual offer.acceptedAutoruExclusive
        parsedOffer.onlineViewAvailable shouldEqual offer.onlineViewAvailable
        parsedOffer.bookingAllowed shouldEqual offer.bookingAllowed

        // if initial offer has unification info then parsed will have EngineInfo instead of ModificationString
        /* @TODO: FIX flacky test - https://st.yandex-team.ru/VSDEALERS-1876
        offer.unification match {
          case None => parsedOffer.modification shouldEqual offer.modification
          case Some(value) =>
            parsedOffer.modification shouldEqual EngineInfo(
              engineVolume = value.displacement,
              enginePower = value.horsePower.getOrElse(0),
              engineType = value.engineType.getOrElse(""),
              gearbox = value.gearType.getOrElse(""),
              drive = value.transmission.getOrElse(""),
              nameplate = None
            )
        }
         */

        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
        parsedOffer.exteriorPanoramaState shouldEqual offer.exteriorPanoramaState
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
      }
    }

    "convert external truck offer to xml [unknown classified]" in {
      val truckOfferGenerator = AutoruGenerators.truckExternalOfferGen(newBusTasksGen, truckBusInfoGen())

      forAll(
        truckOfferGenerator
          .map(
            _.copy(
              mark = mark,
              model = model,
              classifieds = classifieds,
              vin = vin,
              description = description,
              images = images,
              avitoImages = avitoImages,
              dromImages = dromImages,
              avitoSaleServices = avitoSaleServices,
              dromSaleServices = dromSaleServices,
              avitoDescription = avitoDescription,
              dromDescription = dromDescription,
              modification = modification
            )
          )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified = None).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val truckParser = new TruckParser(feed)(busTaskContext)

        val results = truckParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.modification shouldEqual offer.modification
        parsedOffer.color shouldEqual offer.color
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.haggle shouldEqual offer.haggle
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.run shouldEqual offer.run
        parsedOffer.year shouldEqual offer.year
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.description shouldEqual offer.description
        parsedOffer.extras shouldEqual offer.extras
        parsedOffer.images shouldEqual offer.images
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.info shouldEqual offer.info
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
      }
    }

    "convert external truck offer to xml [AVITO classified]" in {
      val truckOfferGenerator = AutoruGenerators.truckExternalOfferGen(newBusTasksGen, truckBusInfoGen())
      val classified = Some(ClassifiedName.AVITO)

      forAll(
        truckOfferGenerator
          .map(
            _.copy(
              mark = mark,
              model = model,
              classifieds = classifieds,
              vin = vin,
              description = description,
              images = images,
              avitoImages = avitoImages,
              dromImages = dromImages,
              avitoSaleServices = avitoSaleServices,
              dromSaleServices = dromSaleServices,
              avitoDescription = avitoDescription,
              dromDescription = dromDescription,
              modification = modification
            )
          )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val truckParser = new TruckParser(feed)(busTaskContext)

        val results = truckParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.modification shouldEqual offer.modification
        parsedOffer.color shouldEqual offer.color
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.haggle shouldEqual offer.haggle
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.run shouldEqual offer.run
        parsedOffer.year shouldEqual offer.year
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.description shouldEqual offer.avitoDescription
        parsedOffer.extras shouldEqual offer.extras
        parsedOffer.images shouldEqual offer.avitoImages
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.info shouldEqual offer.info
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
      }
    }

    "convert external truck offer to xml [AVITO classified, fallback to global]" in {
      val truckOfferGenerator = AutoruGenerators.truckExternalOfferGen(newBusTasksGen, truckBusInfoGen())
      val classified = Some(ClassifiedName.AVITO)

      forAll(
        truckOfferGenerator
          .map(
            _.copy(
              mark = mark,
              model = model,
              classifieds = classifieds,
              vin = vin,
              description = description,
              images = images,
              avitoImages = Seq.empty,
              dromImages = dromImages,
              avitoSaleServices = avitoSaleServices,
              dromSaleServices = dromSaleServices,
              avitoDescription = None,
              dromDescription = dromDescription,
              modification = modification
            )
          )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val truckParser = new TruckParser(feed)(busTaskContext)

        val results = truckParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.modification shouldEqual offer.modification
        parsedOffer.color shouldEqual offer.color
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.haggle shouldEqual offer.haggle
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.run shouldEqual offer.run
        parsedOffer.year shouldEqual offer.year
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.description shouldEqual offer.description
        parsedOffer.extras shouldEqual offer.extras
        parsedOffer.images shouldEqual offer.images
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.info shouldEqual offer.info
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
      }
    }

    "convert external truck offer to xml [DROM classified]" in {
      val truckOfferGenerator = AutoruGenerators.truckExternalOfferGen(newBusTasksGen, truckBusInfoGen())
      val classified = Some(ClassifiedName.DROM)

      forAll(
        truckOfferGenerator
          .map(
            _.copy(
              mark = mark,
              model = model,
              classifieds = classifieds,
              vin = vin,
              description = description,
              images = images,
              avitoImages = avitoImages,
              dromImages = dromImages,
              avitoSaleServices = avitoSaleServices,
              dromSaleServices = dromSaleServices,
              avitoDescription = avitoDescription,
              dromDescription = dromDescription,
              modification = modification
            )
          )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val truckParser = new TruckParser(feed)(busTaskContext)

        val results = truckParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.modification shouldEqual offer.modification
        parsedOffer.color shouldEqual offer.color
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.haggle shouldEqual offer.haggle
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.run shouldEqual offer.run
        parsedOffer.year shouldEqual offer.year
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.description shouldEqual offer.dromDescription
        parsedOffer.extras shouldEqual offer.extras
        parsedOffer.images shouldEqual offer.dromImages
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.info shouldEqual offer.info
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
      }
    }

    "convert external truck offer to xml [DROM classified, fallback to global]" in {
      val truckOfferGenerator = AutoruGenerators.truckExternalOfferGen(newBusTasksGen, truckBusInfoGen())
      val classified = Some(ClassifiedName.DROM)

      forAll(
        truckOfferGenerator
          .map(
            _.copy(
              mark = mark,
              model = model,
              classifieds = classifieds,
              vin = vin,
              description = description,
              images = images,
              avitoImages = avitoImages,
              dromImages = Seq.empty,
              avitoSaleServices = avitoSaleServices,
              dromSaleServices = dromSaleServices,
              avitoDescription = avitoDescription,
              dromDescription = None,
              modification = modification
            )
          )
      ) { offer =>
        val xml = AutoruExternalOfferXmlConverter.externalOffersToXml(Seq(offer), classified).toString()
        val feed = IOUtils.toInputStream(xml, UTF_8)
        val truckParser = new TruckParser(feed)(busTaskContext)

        val results = truckParser.toList
        val errors = results.collect { case Left(error) => error.error.getMessage }
        val offers = results.collect { case Right(o) => o }

        errors should have size 0
        offers should have size 1

        val parsedOffer = offers.head

        parsedOffer.mark shouldEqual offer.mark
        parsedOffer.model shouldEqual offer.model
        parsedOffer.modification shouldEqual offer.modification
        parsedOffer.color shouldEqual offer.color
        parsedOffer.availability shouldEqual offer.availability
        parsedOffer.custom shouldEqual offer.custom
        parsedOffer.state shouldEqual offer.state
        parsedOffer.haggle shouldEqual offer.haggle
        parsedOffer.exchange shouldEqual offer.exchange
        parsedOffer.run shouldEqual offer.run
        parsedOffer.year shouldEqual offer.year
        parsedOffer.currency.currency shouldEqual offer.currency.currency
        parsedOffer.vin shouldEqual offer.vin
        parsedOffer.price shouldEqual offer.price
        parsedOffer.description shouldEqual offer.description
        parsedOffer.extras shouldEqual offer.extras
        parsedOffer.images shouldEqual offer.images
        parsedOffer.saleServices shouldEqual offer.saleServices
        parsedOffer.avitoSaleServices shouldEqual offer.avitoSaleServices
        parsedOffer.dromSaleServices shouldEqual offer.dromSaleServices
        parsedOffer.classifieds shouldEqual offer.classifieds
        parsedOffer.uniqueId shouldEqual offer.uniqueId
        parsedOffer.action shouldEqual offer.action
        parsedOffer.poiId shouldEqual offer.poiId
        parsedOffer.panoramas shouldEqual offer.panoramas
        parsedOffer.badges shouldEqual offer.badges
        parsedOffer.contactInfo shouldEqual offer.contactInfo
        parsedOffer.info shouldEqual offer.info
        parsedOffer.deliveryInfo shouldEqual offer.deliveryInfo
        parsedOffer.serviceAutoApply shouldEqual offer.serviceAutoApply
      }
    }
  }
}

object AutoruExternalOfferXmlConverterSpec {

  val newBusServiceInfoGen: Gen[ServiceInfo] =
    serviceInfoGen(sectionGen = Gen.const(Section.NEW.getNumber), categoryGen = Gen.const(TruckCategory.BUS_VALUE))
  val newBusTasksGen: Gen[Task] = tasksGen(Gen.const(Task.Status.New), newBusServiceInfoGen)
  val taskContext: TaskContext = TaskContext(newTasksGen.next)
  val busTaskContext: TaskContext = TaskContext(newBusTasksGen.next)

  val classifieds: Option[Seq[String]] = Some(Seq("autoru", "avito", "drom"))
  val mark: String = "Mark"
  val model: String = "Model"
  val description: Option[String] = Some("description")
  val vin: Option[String] = Some("VIN")
  val sts: Option[String] = Some("STS")
  val modification: Option[String] = Some("modification")
  val images: Seq[String] = Seq("image1", "image2")
  val avitoImages: Seq[String] = Seq("avito_image1", "avito_image2", "avito_image3")
  val dromImages: Seq[String] = Seq("drom_image1")
  val avitoSaleServices: Seq[String] = Seq("service1", "service2", "service3")
  val dromSaleServices: Seq[String] = Seq("service1", "service2", "service3")
  val avitoDescription: Option[String] = Some("avito_description")
  val dromDescription: Option[String] = Some("drom_description")
  val sellerType: SellerType = SellerType.PRIVATE
}
