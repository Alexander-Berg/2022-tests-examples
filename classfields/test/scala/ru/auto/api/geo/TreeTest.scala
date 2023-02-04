package ru.auto.api.geo

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 16.02.17
  */
class TreeTest extends AnyFunSuite with OptionValues {
  private val Earth = Region(10000L, 0L, RegionTypes.Other, "Земля", "", "", "", "", "", 0d, 0d, 0, None)

  private val Europe =
    Region(111L, Earth.id, RegionTypes.Continent, "Европа", "", "", "", "", "", 57.767961d, 40.926858d, 0, None)

  private val Russia =
    Region(225L, Europe.id, RegionTypes.Country, "Россия", "", "", "", "", "", 61.698653d, 99.505405d, 0, None)

  private val MoscowAndRegion = Region(
    1L,
    Russia.id,
    RegionTypes.FederalSubject,
    "Москва и МО",
    "",
    "",
    "",
    "",
    "",
    55.73143963963309d,
    37.62798440624998d,
    0,
    None
  )

  private val Moscow =
    Region(213L, MoscowAndRegion.id, RegionTypes.City, "Москва", "", "", "", "", "", 55.75396d, 37.620393d, 0, None)

  private val tree = new Tree(Seq(Earth, Europe, Russia, MoscowAndRegion, Moscow))

  test("empty Tree") {
    val tree = new Tree(Seq.empty)
    tree.size shouldBe 0
  }

  test("nonEmpty tree") {
    tree.size shouldBe tree.regions.size
  }

  test("get region by id") {
    tree.region(225L).value shouldBe Russia
    tree.region(663L) shouldBe None
  }

  test("get root") {
    tree.root.value shouldBe Earth
  }

  test("get country") {
    tree.country(213L).value shouldBe Russia
    tree.country(Moscow).value shouldBe Russia
    tree.country(Europe) shouldBe None
  }

  test("get federal subject") {
    tree.federalSubject(213L).value shouldBe MoscowAndRegion
    tree.federalSubject(Moscow).value shouldBe MoscowAndRegion
    tree.federalSubject(Europe) shouldBe None
  }

  test("get city") {
    tree.city(213L).value shouldBe Moscow
    tree.city(Moscow).value shouldBe Moscow
    tree.city(MoscowAndRegion) shouldBe None
    tree.city(Europe) shouldBe None
  }

  test("get parent region") {
    tree.parent(213L).value shouldBe MoscowAndRegion
    tree.parent(Moscow).value shouldBe MoscowAndRegion
    tree.parent(Russia).value shouldBe Europe
    tree.parent(Earth) shouldBe None
  }

  test("get path to root") {
    tree.pathToRoot(225L) shouldBe Seq(Russia, Europe, Earth)
    tree.pathToRoot(Russia) shouldBe Seq(Russia, Europe, Earth)
  }

  test("get all children") {
    tree.allChildren(Russia) shouldBe Set(Russia, MoscowAndRegion, Moscow)
  }
}
