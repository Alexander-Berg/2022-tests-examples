package ru.yandex.vos2.autoru.catalog

import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import ru.yandex.vos2.autoru.InitTestDbs

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 09.01.17
  */
@RunWith(classOf[JUnitRunner])
class YoctoCarsCatalogTest extends AnyFunSuite with OptionValues with InitTestDbs {

  //private val x = new DefaultAutoruCoreComponents
  //private val autoruCatalog = new AutoruCatalog(x.autoruCatalogDao)
  //private val autoruCatalogDao = x.autoruCatalogDao

  private val carsCatalog = components.carsCatalog

  test("Read yocto index from stream") {
    carsCatalog.size should be > 0
    carsCatalog.cards should not be empty
    /*carsCatalog.cards.map(_.feeding).toSet.toSeq.sorted.foreach(println)
    println("==========")
    carsCatalog.cards.map(_.engineFeeding).toSet.toSeq.sorted.foreach(println)*/
    /*val sameTech: List[CatalogCard] = carsCatalog.cards.filter(_.techParamId == 20681727).toList
    println(carsCatalog.getCard(20681727))
    println(sameTech.length)
    sameTech.foreach(println)*/
  }

  test("Get card by tech param id") {
    val card = carsCatalog.getCardByTechParamId(10373608L)
    card shouldBe 'defined
    //println(card)
  }

  /*private val driveMapping = Seq(
    Set("ALL_WHEEL_DRIVE", "ALL_FULL", "ALL", "ALL_PART"),
    Set("FRONT", "FORWARD_CONTROL"),
    Set("REAR", "REAR_DRIVE")).flatMap(d => d.map(x => x -> d)).toMap

  private val transmissionMapping = Seq(
    Set("AUTOMATIC", "ROBOT", "ROBOT_2CLUTCH", "ROBOT_1CLUTCH", "ROBOT_SEQ", "PP"),
    Set("VARIATOR"),
    Set("MECHANICAL")
  ).flatMap(d => d.map(x => x -> d)).toMap

  // тест проверяет, что то, что лежит в ёкте, похоже на то, что лежит в catalog7_yandex
  test("old db compare") {
    //val len = carsCatalog.cards.length
    log.info("start loading getVerbaToModificationMap")
    val oldDbModifications = autoruCatalogDao.getVerbaToModificationMap
    log.info("loading getVerbaToModificationMap done")
    oldDbModifications.toSeq.zipWithIndex.foreach { case ((techParamId, oldDbModification), idx) =>
      carsCatalog.getCard(techParamId).foreach { card =>
        //log.info(s"[${idx + 1}/$len] checking card from yocto for techParamId $techParamId")
        def makeCheck[A](checker: Modification => A, message: String): Unit = {
          check(checker, oldDbModification, card, message)
        }
        def makeCheck2[A](checker1: => A, checker2: => A, message: String): Unit = {
          check2(checker1, checker2, card, message)
        }
        def getModificationProperty(aliases: SettingAlias*): Option[String] = {
          aliases.flatMap(alias => oldDbModification.properties.get(alias.alias)).headOption
        }
        val cardModification = carsCatalog.autoruModification(card)
        assert(oldDbModification.id == cardModification.id,
          s"id not equal\n==========\n$oldDbModification\n==========\n$cardModification\n==========\n$card")
        assert(oldDbModification.markId == cardModification.markId,
          s"markId not equal\n==========\n$oldDbModification\n==========\n$cardModification\n$card")
        assert(oldDbModification.folderId == cardModification.folderId,
          s"folderId not equal\n==========\n$oldDbModification\n==========\n$cardModification\n$card")
        assert(oldDbModification.techParamId == cardModification.techParamId,
          s"techParamId not equal\n==========\n$oldDbModification\n==========\n$cardModification\n$card")
        assert(autoruCatalog.getMark(oldDbModification.markId).code == card.mark.code)
        assert(autoruCatalog.getModel(oldDbModification.folderId).code == card.model.code)
        assert(autoruCatalog.getModel(oldDbModification.folderId).id == card.model.folderId)

        //makeCheck(_.id, "id not equal")
        //makeCheck(_.markId, "markId not equal")
        //makeCheck(_.folderId, "folderId not equal")
        makeCheck(_.configurationId, "configurationId not equal")
        makeCheck(_.startYear, "startYear not equal")
        makeCheck(_.endYear, "endYear not equal")

        getModificationProperty(DRIVE).foreach(gearType => {
          makeCheck2(driveMapping(autoruCatalog.getGearTypes(gearType.toInt)), driveMapping(card.gearType),
            "gearType not equal")
        })
        makeCheck2(getModificationProperty(BODY_TYPE).flatMap(x => {
          autoruCatalog.getBodyTypes.get(x.toInt)
        }), card.bodyType, "bodyType not equal")
        getModificationProperty(GEARBOX, GEARBOX_TYPE).foreach(transmission => {
          makeCheck2(transmissionMapping(autoruCatalog.getTransmissionTypes(transmission.toInt)),
            transmissionMapping(card.transmission),
            "transmission not equal")
        })
        getModificationProperty(ENGINE_TYPE).foreach(engineType => {
          makeCheck2(autoruCatalog.getEngineTypes(engineType.toInt), card.engineType, "engineType not equal")
        })
        getModificationProperty(ENGINE_VOLUME).foreach(engineVolume => {
          makeCheck2(engineVolume.toInt, card.engineVolume, "engineVolume not equal")
        })
        getModificationProperty(ENGINE_POWER).foreach(enginePower => {
          makeCheck2(enginePower.toInt, card.enginePower, "enginePower not equal")
        })
        getModificationProperty(DOORS_COUNT).foreach(doorsCount => {
          makeCheck2(doorsCount.toInt, card.doorsCount, "doorsCount not equal")
        })
        getModificationProperty(POWER_SYSTEM).foreach(engineFeeding => {
          makeCheck2(autoruCatalog.getPowerSystems.get(engineFeeding.toInt), card.engineFeeding,
            "engineFeeding not equal")
        })
        getModificationProperty(TURBO_TYPE).foreach(feeding => {
          makeCheck2(autoruCatalog.getTurboTypes.get(feeding.toInt).filter(_ != "None"),
            card.feeding.filter(_ != "None"),
            "feeding not equal")
        })
      }
    }
  }

  private def check[A](checker: Modification => A, m1: Modification, card: CatalogCard, message: String): Unit = {
    val m2: Modification = carsCatalog.autoruModification(card)
    if (checker(m1) != checker(m2)) {
      println(s"$message: ${checker(m1)} != ${checker(m2)} for ${card.description}")
    }
  }

  private def check2[A](checker1: => A, checker2: => A, card: CatalogCard, message: String): Unit = {
    val res1 = checker1
    val res2 = checker2
    if (res1 != res2) {
      println(s"$message: $res1 != $res2 for ${card.description}")
    }
  }*/
}
