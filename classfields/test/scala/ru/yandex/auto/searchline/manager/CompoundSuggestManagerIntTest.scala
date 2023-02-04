package ru.yandex.auto.searchline.manager

import org.scalatest.{FreeSpecLike, Matchers}
import org.springframework.test.AbstractDependencyInjectionSpringContextTests
import ru.auto.api.ApiOfferModel.{Category, PtsStatus}
import ru.auto.api.CarsModel.Car
import ru.auto.api.CommonModel.SteeringWheel
import ru.auto.api.MotoModel.{Moto, MotoCategory}
import ru.auto.api.SearchlineModel
import ru.auto.api.TrucksModel.{HaggleType, TruckCategory}
import ru.auto.api.search.SearchModel.{CarsSearchRequestParameters, Currency, SearchRequestParameters}
import ru.auto.api.ui.UiModel._
import ru.yandex.auto.searchline.api.directive.DebugParams
import ru.yandex.auto.searchline.manager.SuggestMatchers.matchMarkModel
import ru.yandex.auto.searchline.model.{SearchQuery, Suggest}

import scala.collection.JavaConverters._

/**
  * @author pnaydenov
  */
class CompoundSuggestManagerIntTest extends AbstractDependencyInjectionSpringContextTests
  with FreeSpecLike with Matchers {
  private val ctx = loadContextLocations(Array("searchline-core-test.xml"))
  private val suggestManager = ctx.getBean("allCategorySuggestManager").asInstanceOf[CompoundSuggestManager]
  private val UNRECOGNIZED_FRAGMENT = SearchlineModel.SearchSuggestResponse.Token.Type.UNRECOGNIZED_FRAGMENT.getNumber

  private def testQuery(testName: String, category: Option[Category] = None)
                       (testFun: PartialFunction[List[Suggest], Unit]): Unit =
    registerTest(testName){
      val suggest = suggestManager.suggestFromQuery(SearchQuery(testName, testName.length, category,
        DebugParams.empty)).toList
      testFun.apply(suggest)
    }

  private def assertAllCategories(all: List[Suggest]): Unit = {
    all should have size (3)
    all.find(_.category == Category.CARS) shouldBe defined
    all.find(_.category == Category.TRUCKS) shouldBe defined
    all.find(_.category == Category.MOTO) shouldBe defined
    assert(all.find(_.category == Category.CARS).get.params.hasCarsParams)
    assert(all.find(_.category == Category.TRUCKS).get.params.hasTrucksParams)
    assert(all.find(_.category == Category.TRUCKS).get.params.getTrucksParams.hasTrucksCategory)
    assert(all.find(_.category == Category.MOTO).get.params.hasMotoParams)
    assert(all.find(_.category == Category.MOTO).get.params.getMotoParams.hasMotoCategory)
  }

  private def eachCategory(all: List[Suggest])(f: List[Suggest] ⇒ Unit): Unit = {
    val byCategory = all.groupBy(_.category)
    byCategory.keySet shouldEqual Set(Category.CARS, Category.TRUCKS, Category.MOTO)
    byCategory.foreach { case (category, suggest) ⇒ f(suggest) }
  }

  "Categorical" - {
    testQuery("Форд фокус") { case suggest :: Nil =>
      suggest.category shouldEqual Category.CARS
      suggest should matchMarkModel("FORD", "FOCUS")
    }

    testQuery("Форд фокус синий битый екат седан дизель механика праворукий с фото в наличии " +
              "до 500 тысяч рублей 5 литров 2017") { case suggest :: Nil =>
      suggest.category shouldEqual Category.CARS
      suggest should matchMarkModel("FORD", "FOCUS")
      suggest.params.getCurrency shouldEqual Currency.RUR
      assert(suggest.params.hasHasImage && suggest.params.getHasImage)
      suggest.params.getInStock shouldEqual Stock.IN_STOCK
      suggest.params.getColorList.asScala shouldEqual Seq("0000CC")
      suggest.params.getRidList.asScala shouldEqual Seq(54)
      suggest.params.getYearFrom shouldEqual 2017
      suggest.params.getYearTo shouldEqual 2017
      assert(!suggest.params.hasPriceFrom)
      suggest.params.getPriceTo shouldEqual 500000
      suggest.params.getDisplacementFrom shouldEqual 5000
      suggest.params.getDisplacementTo shouldEqual 5000
      suggest.params.getDamageGroup shouldEqual DamageGroup.BEATEN
      val car = suggest.params.getCarsParams
      car.getTransmissionList.asScala shouldEqual Seq(Car.Transmission.MECHANICAL)
      car.getSteeringWheel shouldEqual SteeringWheel.RIGHT
      car.getBodyTypeGroupList.asScala shouldEqual Seq(BodyTypeGroup.SEDAN)
      car.getEngineGroupList.asScala shouldEqual Seq(EngineGroup.DIESEL)
    }

    "category filter" in {
      val query = "мерс"
      val all = suggestManager.suggestFromQuery(SearchQuery(query, query.length, None, DebugParams.empty))
      val cars = suggestManager.suggestFromQuery(SearchQuery(query, query.length, Some(Category.CARS),
        DebugParams.empty))
      val trucks = suggestManager.suggestFromQuery(SearchQuery(query, query.length, Some(Category.TRUCKS),
        DebugParams.empty))
      val moto = suggestManager.suggestFromQuery(SearchQuery(query, query.length, Some(Category.MOTO),
        DebugParams.empty))

      assert(all.nonEmpty && cars.nonEmpty && trucks.nonEmpty && moto.nonEmpty)
      // all shouldEqual (cars ++ trucks ++ moto) // TODO: switch on after cyrillicName fix in trucks & moto
      cars.map(_.category).toSet shouldEqual Set(Category.CARS)
      trucks.map(_.category).toSet shouldEqual Set(Category.TRUCKS)
      moto.map(_.category).toSet shouldEqual Set(Category.MOTO)
    }

    testQuery("автомат") { case suggest =>
      suggest should have size (3)
      val truck = suggest.find(_.category == Category.TRUCKS).get
      val moto = suggest.find(_.category == Category.MOTO).get
      truck.params.getTrucksParams.getTrucksCategory shouldEqual TruckCategory.LCV
      moto.params.getMotoParams.getMotoCategory shouldEqual MotoCategory.MOTORCYCLE
    }

    testQuery("универсал") { case suggest :: Nil =>
      suggest.category shouldEqual Category.CARS
      suggest.params.getCarsParams.getBodyTypeGroupList.asScala.toSet shouldEqual Set(BodyTypeGroup.WAGON)
    }

    testQuery("внедорожник") { case suggest =>
      suggest should have size (2)
      val car = suggest.find(_.category == Category.CARS).get
      val moto = suggest.find(_.category == Category.MOTO).get
      car.params.getCarsParams.getBodyTypeGroupList.asScala.toSet shouldEqual Set(BodyTypeGroup.ALLROAD_5_DOORS)
      moto.params.getMotoParams.getMotoCategory shouldEqual MotoCategory.MOTORCYCLE
    }

    testQuery("ауди а4 2 литра") { case suggest :: Nil =>
      suggest should matchMarkModel("AUDI", "A4")
      suggest.tokens.filter(_.typeId == UNRECOGNIZED_FRAGMENT) shouldBe empty
      assert(suggest.params.getDisplacementFrom == 2000 && suggest.params.getDisplacementTo == 2000)
    }

    testQuery("ауди а4 2 тыс км") { case suggest :: Nil =>
      suggest should matchMarkModel("AUDI", "A4")
      suggest.tokens.filter(_.typeId == UNRECOGNIZED_FRAGMENT) shouldBe empty
      assert(suggest.params.getKmAgeFrom == 2000 && suggest.params.getKmAgeTo == 2000)
    }

    testQuery("ауди а4 2 выхухоля") { case suggest :: Nil =>
      suggest should matchMarkModel("AUDI", "A4", "3473225")
      suggest.tokens.filter(_.typeId == UNRECOGNIZED_FRAGMENT).toSet shouldEqual
        Set(Suggest.Token(10, 17, UNRECOGNIZED_FRAGMENT))
    }

    testQuery("ауди а4 2") { case suggest :: Nil =>
      suggest should matchMarkModel("AUDI", "A4", "3473225")
      suggest.tokens.filter(_.typeId == UNRECOGNIZED_FRAGMENT) shouldBe empty
    }

    testQuery("Ваз 2110 г. Чебоксары") { case suggest :: Nil =>
      suggest should matchMarkModel("VAZ", "2110")
      assert(!suggest.params.hasYearFrom)
      assert(!suggest.params.hasYearTo)
      suggest.params.getRidList.asScala.toSet shouldEqual Set(45)
    }

    testQuery("2114 2008 год") { case suggest :: Nil =>
      suggest should matchMarkModel("VAZ", "2114")
      suggest.params.getYearFrom shouldEqual 2008
      suggest.params.getYearTo shouldEqual 2008
    }

    testQuery("мини погрузчик") { case suggest =>
      val truck = suggest.find(_.category == Category.TRUCKS).get
      truck.params.getTrucksParams.getTrucksCategory shouldEqual TruckCategory.AUTOLOADER
    }

    testQuery("ауди 100") { case suggest :: Nil =>
      suggest.category shouldEqual Category.CARS
      suggest.params.getMarkModelNameplateList.asScala.toSet shouldEqual Set("AUDI#100")
    }

    testQuery("21100") { case suggest :: Nil =>
      suggest should matchMarkModel("VAZ", "2110")
    }

    testQuery("LADA (ВАЗ) 21099, 2109,21093,2108") { case suggest =>
      pending // TODO: don't have idea how to implement it and don't broke another tests
      suggest should have size (3)
      suggest.find(_.params.getMarkModelNameplateList.asScala.toSet == Set("VAZ#2109")) should be ('defined)
      suggest.find(_.params.getMarkModelNameplateList.asScala.toSet == Set("VAZ#21099")) should be ('defined)
      suggest.find(_.params.getMarkModelNameplateList.asScala.toSet == Set("VAZ#2108")) should be ('defined)
    }

    testQuery("2123 нива") { case suggest =>
      suggest.find(_.params.getMarkModelNameplateList.asScala.toSet == Set("VAZ#2123")) should be ('defined)
    }

    testQuery("нива 2123") { case suggest =>
      suggest.find(_.params.getMarkModelNameplateList.asScala.toSet == Set("VAZ#2123")) should be ('defined)
    }
  }

  "Colors" - {
    testQuery("синий") { case suggest :: _ =>
      suggest should matchMarkModel("")
      suggest.params.getColorCount shouldEqual 1
      suggest.params.getColor(0) shouldEqual "0000CC"
    }

    testQuery("ауди красный") { case suggest :: Nil =>
      suggest.params shouldEqual SearchRequestParameters.newBuilder().addColor("EE1D19")
        .setCarsParams(CarsSearchRequestParameters.newBuilder())
        .addMarkModelNameplate("AUDI").build()
    }
  }



  testQuery("Мазда 3 с кондиционером и люком", category = Some(Category.CARS)) { case suggest :: Nil =>
    suggest should matchMarkModel("MAZDA", "3")
    suggest.tokens.filter(_.typeId == UNRECOGNIZED_FRAGMENT) should have size (2)
    suggest.params.getCatalogEquipmentList.asScala.toList shouldEqual List("condition")
  }

  "Intervals" - {
    "Mileage" - {
      testQuery("от 5 до 100 тыс км.") { case all =>
        all.foreach { suggest =>
          suggest.params.getKmAgeFrom shouldEqual 5000
          suggest.params.getKmAgeTo shouldEqual 100000
        }
        assertAllCategories(all)
      }

      testQuery("от 5 тыс до 100 тыс км.") { case all =>
        all.foreach { suggest =>
          suggest.params.getKmAgeFrom shouldEqual 5000
          suggest.params.getKmAgeTo shouldEqual 100000
        }
        assertAllCategories(all)
      }

      testQuery("5 тыс км") { case all =>
        all.foreach { suggest =>
          suggest.params.getKmAgeFrom shouldEqual 5000
          suggest.params.getKmAgeTo shouldEqual 5000
        }
        assertAllCategories(all)
      }

      testQuery("от 5 тыс км") { case all =>
        all.foreach { suggest =>
          suggest.params.getKmAgeFrom shouldEqual 5000
          assert(!suggest.params.hasKmAgeTo)
        }
        assertAllCategories(all)
      }

      testQuery("до 5 тыс км") { case all =>
        all.foreach { suggest =>
          assert(!suggest.params.hasKmAgeFrom)
          suggest.params.getKmAgeTo shouldEqual 5000
        }
        assertAllCategories(all)
      }
    }

    "Power" - {
      testQuery("вмв 6 от 10 до 100 л.с.") { case suggest :: _ =>
        suggest.params.getPowerFrom shouldEqual 10
        suggest.params.getPowerTo shouldEqual 100
      }

      testQuery("мазда от 70 лошадок") { case suggest :: _ =>
        suggest.params.getPowerFrom shouldEqual 70
        assert(!suggest.params.hasPowerTo)
      }
    }

    "Price" - {
      testQuery("от 5 тысяч рублей непонятноеслово до 8 тысяч рублей") { case all =>
        all.foreach { suggest =>
          suggest.params.getPriceFrom shouldEqual 5000
          suggest.params.getPriceTo shouldEqual 8000
          suggest.params.getCurrency shouldEqual Currency.RUR
        }
        assertAllCategories(all)
      }

      testQuery("бмв от 3 тыс руб", category = Some(Category.CARS)) { case suggest :: Nil =>
        suggest should matchMarkModel("BMW")
        suggest.params.getPriceFrom shouldEqual 3000
        assert(!suggest.params.hasPriceTo)
        assert(!suggest.params.hasKmAgeFrom)
        assert(!suggest.params.hasKmAgeTo)
      }

      testQuery("бмв до 3 тыс руб", category = Some(Category.CARS)) { case suggest :: Nil =>
        suggest should matchMarkModel("BMW")
        assert(!suggest.params.hasPriceFrom)
        suggest.params.getPriceTo shouldEqual 3000
        assert(!suggest.params.hasKmAgeFrom)
        assert(!suggest.params.hasKmAgeTo)
      }

      testQuery("бмв 3 млн руб", category = Some(Category.CARS)) { case suggest :: Nil =>
        suggest should matchMarkModel("BMW")
        suggest.params.getPriceFrom shouldEqual 3000000
        suggest.params.getPriceTo shouldEqual 3000000
      }

      testQuery("бмв за 3 миллиона руб", category = Some(Category.CARS)) { case suggest :: Nil =>
        suggest should matchMarkModel("BMW")
        assert(!suggest.params.hasPriceFrom)
        suggest.params.getPriceTo shouldEqual 3000000
      }

      testQuery("бмв за 1 миллион руб", category = Some(Category.CARS)) { case suggest :: Nil =>
        suggest should matchMarkModel("BMW")
        assert(!suggest.params.hasPriceFrom)
        suggest.params.getPriceTo shouldEqual 1000000
      }

      testQuery("бмв за 5 миллионов руб", category = Some(Category.CARS)) { case suggest :: Nil =>
        suggest should matchMarkModel("BMW")
        assert(!suggest.params.hasPriceFrom)
        suggest.params.getPriceTo shouldEqual 5000000
      }

      testQuery("от 200000 до миллиона рублей") { case all =>
        all.foreach { suggest =>
          suggest.params.getPriceFrom shouldEqual 200000
          suggest.params.getPriceTo shouldEqual 1000000
        }
        assertAllCategories(all)
      }

      testQuery("от 200000 до 3 миллионов рублей") { case all =>
        all.foreach { suggest =>
          suggest.params.getPriceFrom shouldEqual 200000
          suggest.params.getPriceTo shouldEqual 3000000
        }
        assertAllCategories(all)
      }

      testQuery("туарег от 200000 миллионов рублей") { case suggest :: Nil =>
        assert(!suggest.params.hasPriceFrom)
        assert(!suggest.params.hasPriceTo)
      }
    }

    "Undefined" - {
      testQuery("от 5000 до 7000") { case all =>
        val priceSuggestAll = all.filter(_.params.hasPriceFrom)
        val runSuggestAll = all.filter(_.params.hasKmAgeFrom)
        assertAllCategories(priceSuggestAll)
        assertAllCategories(runSuggestAll)

        priceSuggestAll.foreach { priceSuggest =>
          priceSuggest.params.getPriceFrom shouldEqual 5000
          priceSuggest.params.getPriceTo shouldEqual 7000
          assert(!priceSuggest.params.hasKmAgeFrom)
          assert(!priceSuggest.params.hasKmAgeTo)
        }
        runSuggestAll.foreach { runSuggest =>
          runSuggest.params.getKmAgeFrom shouldEqual 5000
          runSuggest.params.getKmAgeTo shouldEqual 7000
          assert(!runSuggest.params.hasPriceFrom)
          assert(!runSuggest.params.hasPriceTo)
        }
      }

      testQuery("от 5 до 7 тысяч") { case all =>
        eachCategory(all) { suggest =>
          val priceSuggest = suggest.find(_.params.hasPriceFrom).get
          val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
          priceSuggest.params.getPriceFrom shouldEqual 5000
          priceSuggest.params.getPriceTo shouldEqual 7000
          assert(!priceSuggest.params.hasKmAgeFrom)
          assert(!priceSuggest.params.hasKmAgeTo)
          runSuggest.params.getKmAgeFrom shouldEqual 5000
          runSuggest.params.getKmAgeTo shouldEqual 7000
          assert(!runSuggest.params.hasPriceFrom)
          assert(!runSuggest.params.hasPriceTo)
        }
      }

      testQuery("от 200000 до миллиона") { case all =>
        eachCategory(all) { suggest =>
          val priceSuggest = suggest.find(_.params.hasPriceFrom).get
          val runSuggest = suggest.find(_.params.hasKmAgeFrom).get

          priceSuggest.params.getPriceFrom shouldEqual 200000
          priceSuggest.params.getPriceTo shouldEqual 1000000

          runSuggest.params.getKmAgeFrom shouldEqual 200000
          runSuggest.params.getKmAgeTo shouldEqual 1000000
        }
      }

      testQuery("от 500 тысяч") { case all =>
        eachCategory(all) { suggest =>
          val priceSuggest = suggest.find(_.params.hasPriceFrom).get
          val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
          priceSuggest.params.getPriceFrom shouldEqual 500000
          assert(!priceSuggest.params.hasPriceTo)
          runSuggest.params.getKmAgeFrom shouldEqual 500000
          assert(!runSuggest.params.hasKmAgeTo)
        }
      }

      testQuery("до 500 тысяч") { case all =>
        eachCategory(all) { suggest =>
          val priceSuggest = suggest.find(_.params.hasPriceTo).get
          val runSuggest = suggest.find(_.params.hasKmAgeTo).get
          assert(!priceSuggest.params.hasPriceFrom)
          priceSuggest.params.getPriceTo shouldEqual 500000
          assert(!runSuggest.params.hasKmAgeFrom)
          runSuggest.params.getKmAgeTo shouldEqual 500000
        }
      }

      testQuery("от 500") { case all =>
        eachCategory(all) { case suggest :: Nil =>
          assert(!suggest.params.hasPriceFrom)
          assert(!suggest.params.hasPriceTo)
          suggest.params.getKmAgeFrom shouldEqual 500
          assert(!suggest.params.hasKmAgeTo)
        }
      }

      testQuery("до 500") { case all =>
        eachCategory(all) { case suggest :: Nil =>
          assert(!suggest.params.hasPriceFrom)
          assert(!suggest.params.hasPriceTo)
          assert(!suggest.params.hasKmAgeFrom)
          suggest.params.getKmAgeTo shouldEqual 500
        }
      }

      testQuery("от 5000") { case all =>
        eachCategory(all) { suggest =>
          suggest should have size (2)
          val priceSuggest = suggest.find(_.params.hasPriceFrom).get
          val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
          priceSuggest.params.getPriceFrom shouldEqual 5000
          assert(!priceSuggest.params.hasPriceTo)
          runSuggest.params.getKmAgeFrom shouldEqual 5000
          assert(!runSuggest.params.hasKmAgeTo)
        }
      }

      testQuery("до 5000") { case all =>
        eachCategory(all) { suggest =>
          suggest should have size (2)
          val priceSuggest = suggest.find(_.params.hasPriceTo).get
          val runSuggest = suggest.find(_.params.hasKmAgeTo).get
          assert(!priceSuggest.params.hasPriceFrom)
          priceSuggest.params.getPriceTo shouldEqual 5000
          assert(!runSuggest.params.hasKmAgeFrom)
          runSuggest.params.getKmAgeTo shouldEqual 5000
        }
      }

      testQuery("500 тысяч") { case all =>
        eachCategory(all) { case suggest =>
          suggest should have size (2)
          val priceSuggest = suggest.find(_.params.hasPriceFrom).get
          val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
          priceSuggest.params.getPriceFrom shouldEqual 500000
          priceSuggest.params.getPriceTo shouldEqual 500000
          runSuggest.params.getKmAgeFrom shouldEqual 500000
          runSuggest.params.getKmAgeTo shouldEqual 500000
        }
      }

      testQuery("500 миллионов") { case all =>
        eachCategory(all) { case suggest :: Nil =>
          assert(!suggest.params.hasKmAgeFrom)
          assert(!suggest.params.hasKmAgeTo)
          suggest.params.getPriceFrom shouldEqual 500000000
          suggest.params.getPriceTo shouldEqual 500000000
        }
      }

      testQuery("200") { case all =>
        eachCategory(all) { suggest =>
          suggest.foreach { s =>
            assert(!s.params.hasKmAgeFrom)
            assert(!s.params.hasKmAgeTo)
            assert(!s.params.hasPriceFrom)
            assert(!s.params.hasPriceTo)
            s.params.getMarkModelNameplateList.asScala should not be empty
          }
        }
      }

      testQuery("бмв 3", category = Some(Category.CARS)) { case suggest :: Nil =>
        suggest should matchMarkModel("BMW", "3ER")
      }

      testQuery("бмв 3 тыс", category = Some(Category.CARS)) { case suggest @ _ :: _ :: Nil =>
        suggest.foreach(_ should matchMarkModel("BMW"))
        val priceSuggest = suggest.find(_.params.hasPriceFrom).get
        val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
        priceSuggest.params.getPriceFrom shouldEqual 3000
        priceSuggest.params.getPriceTo shouldEqual 3000
        runSuggest.params.getKmAgeFrom shouldEqual 3000
        runSuggest.params.getKmAgeTo shouldEqual 3000
      }

      testQuery("бмв от 3000", category = Some(Category.CARS)) { case suggest @ _ :: _ :: Nil =>
        suggest.foreach(_ should matchMarkModel("BMW"))
        val priceSuggest = suggest.find(_.params.hasPriceFrom).get
        val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
        priceSuggest.params.getPriceFrom shouldEqual 3000
        assert(!priceSuggest.params.hasPriceTo)
        runSuggest.params.getKmAgeFrom shouldEqual 3000
        assert(!runSuggest.params.hasKmAgeTo)
      }

      testQuery("бмв до 3000", category = Some(Category.CARS)) { case suggest @ _ :: _ :: Nil =>
        suggest.foreach(_ should matchMarkModel("BMW"))
        val priceSuggest = suggest.find(_.params.hasPriceTo).get
        val runSuggest = suggest.find(_.params.hasKmAgeTo).get
        assert(!priceSuggest.params.hasPriceFrom)
        priceSuggest.params.getPriceTo shouldEqual 3000
        assert(!runSuggest.params.hasKmAgeFrom)
        runSuggest.params.getKmAgeTo shouldEqual 3000
      }

      testQuery("бмв от 3000 до 6000", category = Some(Category.CARS)) { case suggest @ _ :: _ :: Nil =>
        suggest.foreach(_ should matchMarkModel("BMW"))
        val priceSuggest = suggest.find(_.params.hasPriceFrom).get
        val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
        priceSuggest.params.getPriceFrom shouldEqual 3000
        priceSuggest.params.getPriceTo shouldEqual 6000
        runSuggest.params.getKmAgeFrom shouldEqual 3000
        runSuggest.params.getKmAgeTo shouldEqual 6000
      }

      testQuery("бмв от 3 тыс", category = Some(Category.CARS)) { case suggest @ _ :: _ :: Nil =>
        suggest.foreach(_ should matchMarkModel("BMW"))
        val priceSuggest = suggest.find(_.params.hasPriceFrom).get
        val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
        priceSuggest.params.getPriceFrom shouldEqual 3000
        assert(!priceSuggest.params.hasPriceTo)
        runSuggest.params.getKmAgeFrom shouldEqual 3000
        assert(!runSuggest.params.hasKmAgeTo)
      }

      testQuery("бмв до 3 тыс", category = Some(Category.CARS)) { case suggest @ _ :: _ :: Nil =>
        suggest.foreach(_ should matchMarkModel("BMW"))
        val priceSuggest = suggest.find(_.params.hasPriceTo).get
        val runSuggest = suggest.find(_.params.hasKmAgeTo).get
        assert(!priceSuggest.params.hasPriceFrom)
        priceSuggest.params.getPriceTo shouldEqual 3000
        assert(!runSuggest.params.hasKmAgeFrom)
        runSuggest.params.getKmAgeTo shouldEqual 3000
      }

      testQuery("бмв от 3 до 6 тыс", category = Some(Category.CARS)) { case suggest @ _ :: _ :: Nil =>
        suggest.foreach(_ should matchMarkModel("BMW"))
        val priceSuggest = suggest.find(_.params.hasPriceFrom).get
        val runSuggest = suggest.find(_.params.hasKmAgeFrom).get
        priceSuggest.params.getPriceFrom shouldEqual 3000
        priceSuggest.params.getPriceTo shouldEqual 6000
        runSuggest.params.getKmAgeFrom shouldEqual 3000
        runSuggest.params.getKmAgeTo shouldEqual 6000
      }

      testQuery("автомат до 500 тысяч") { case variants if variants.nonEmpty =>
        variants.map(_.category) shouldEqual List(
          Category.CARS, Category.CARS,
          Category.TRUCKS, Category.TRUCKS,
          Category.MOTO, Category.MOTO)
        val cars = variants.filter(_.category == Category.CARS)
        cars should have size(2)
        val price = cars.find(_.params.hasPriceTo).get
        val run = cars.find(_.params.hasKmAgeTo).get
        price.params shouldEqual SearchRequestParameters.newBuilder().setPriceTo(500000)
          .setCarsParams(CarsSearchRequestParameters.newBuilder().addTransmission(Car.Transmission.AUTOMATIC)).build()
        run.params shouldEqual SearchRequestParameters.newBuilder().setKmAgeTo(500000)
          .setCarsParams(CarsSearchRequestParameters.newBuilder().addTransmission(Car.Transmission.AUTOMATIC)).build()
      }
    }

    "Year" - {
      testQuery("форд до 2047 года") { case suggest =>
        val currentYear = java.time.LocalDate.now().getYear
        assert(currentYear < 2047, "Hello future!")
        suggest.foreach(s => assert(!s.params.hasYearFrom && !s.params.hasYearTo))
      }

      testQuery("форд от 2009 до 2047 года") { case suggest =>
        val currentYear = java.time.LocalDate.now().getYear
        assert(currentYear < 2047, "Hello future!")
        suggest.foreach {
          s =>
            assert(s.params.hasYearFrom, 2009)
            assert(!s.params.hasYearTo)
            assert(!s.params.hasOwningTimeGroup, "Too many owning time years")
        }
      }

      testQuery("форд от 2047 до 2049 года") { case suggest =>
        val currentYear = java.time.LocalDate.now().getYear
        assert(currentYear < 2047, "Hello future!")
        suggest.foreach(s => assert(!s.params.hasYearFrom && !s.params.hasYearTo))
      }

      testQuery("форд от 2047 до 1953 года") { case suggest =>
        val currentYear = java.time.LocalDate.now().getYear
        assert(currentYear < 2047, "Hello future!")
        suggest.foreach(s => assert(!s.params.hasYearFrom && !s.params.hasYearTo))
      }

      testQuery("форд до 2017 года") { case suggest =>
        val currentYear = java.time.LocalDate.now().getYear
        suggest.foreach(s => assert(!s.params.hasYearFrom))
        suggest.map(_.params.getYearTo).toSet shouldEqual(Set(2017))
      }

      testQuery("форд от 1953 до 2017 года") { case suggest =>
        val currentYear = java.time.LocalDate.now().getYear
        suggest.map(_.params.getYearFrom).toSet shouldEqual(Set(1953))
        suggest.map(_.params.getYearTo).toSet shouldEqual(Set(2017))
      }

      testQuery("форд от 2017 до 2009 года") { case suggest =>
        val currentYear = java.time.LocalDate.now().getYear
        suggest.map(_.params.getYearFrom).toSet shouldEqual(Set(2009))
        suggest.map(_.params.getYearTo).toSet shouldEqual(Set(2017))
      }

      testQuery("Ваз 2109 2000-2010") { case suggest :: Nil =>
        suggest.params.getYearFrom shouldEqual(2000)
        suggest.params.getYearTo shouldEqual(2010)
      }

      testQuery("Ваз 2109 2000") { case suggest :: Nil =>
        suggest.params.getYearFrom shouldEqual(2000)
        suggest.params.getYearTo shouldEqual(2000)
      }

      testQuery("Ваз 2109 20000-20100") { case suggests =>
        suggests should have size(2)
        val price = suggests.find(_.params.hasPriceFrom).get
        val run = suggests.find(_.params.hasKmAgeFrom).get
        price.params.getPriceFrom shouldEqual 20000
        price.params.getPriceTo shouldEqual 20100
        assert(!price.params.hasYearFrom)
        assert(!price.params.hasYearTo)
        run.params.getKmAgeFrom shouldEqual 20000
        run.params.getKmAgeTo shouldEqual 20100
        assert(!run.params.hasYearFrom)
        assert(!run.params.hasYearTo)
      }
    }
  }

  "Other" - {
    testQuery("yamaha cvt") { case suggest :: _ =>
      suggest.params.getMotoParams.getTransmissionList.asScala shouldEqual Seq(Moto.Transmission.VARIATOR)
    }

    testQuery("задний") { case suggest :: _ =>
      suggest.params.getCarsParams.getGearTypeList.get(0) shouldEqual Car.GearType.REAR_DRIVE
      // TODO: dictionaries and support for trucks, moto
    }

    testQuery("один владелец") { case suggest :: _ =>
      suggest.params.getOwnersCountGroup shouldEqual OwnersCountGroup.ONE
    }

    testQuery("дубликат") { case suggest :: _ =>
      suggest.params.getPtsStatus shouldEqual PtsStatus.DUPLICATE.getNumber
    }

    testQuery("частник") { case suggest :: _ =>
      suggest.params.getSellerGroupList.asScala.toSet shouldEqual Set(SellerGroup.PRIVATE)
    }

    "don't fall in queries" in {
      val queries = scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("queries.txt"))
        .getLines().toList
      val responseCount = queries.par
        .map(q => suggestManager.suggestFromQuery(SearchQuery(q, q.length, None, DebugParams.empty)).length).sum

      assert(responseCount > 10000) // should be slightly greater than 9500 (queries size)
      assert(responseCount < 20000)
    }
  }

  "Options" - {
    testQuery("магнитола mp3", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getCatalogEquipmentList.asScala.toList shouldEqual List("audiosystem-cd")
    }
  }

  "Regions" - {
    testQuery("Montreal") { case suggest =>
      suggest.foreach(_.params.getRidList.asScala should be (empty)) // Ignore geo not in CIS
    }
  }

  "Dealers" - {
    testQuery("ATV центр ЛАХТА", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getDealerId shouldEqual "20867072"
      suggest.params.getMarkModelNameplateList.asScala should be (empty)
    }

    testQuery("акос газ", category = Some(Category.CARS)) { case suggest :: Nil =>
      pending
      suggest.params.getDealerId shouldEqual "21398461"
      suggest.params.getMarkModelNameplateList.asScala should be (empty)
    }

    testQuery("AFS-Taxi", category = Some(Category.CARS)) { case suggest =>
      pending  // TODO: can't handle aliases with "-" (in any dictionary type!)
      suggest should have size (1)
      suggest.head.params.getDealerId shouldEqual "21631032"
      suggest.head.params.getMarkModelNameplateList.asScala should be (empty)
    }

    testQuery("Crystal Motors", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getDealerNetId shouldEqual "20878830"
      suggest.params.getMarkModelNameplateList.asScala should be (empty)
    }
  }

  "Clearance" - {
    testQuery("лада от 150 мм", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getMarkModelNameplateList.asScala.toSet shouldEqual Set("VAZ")
      suggets.params.getClearanceFrom shouldEqual 150
      assert(!suggets.params.hasClearanceTo)
    }

    testQuery("внедорожник от 200 до 300 мм", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getCarsParams.getBodyTypeGroupList.asScala.toSet shouldEqual Set(BodyTypeGroup.ALLROAD_5_DOORS)
      suggets.params.getClearanceFrom shouldEqual 200
      suggets.params.getClearanceTo shouldEqual 300
    }

    testQuery("клиренс 220 мм", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getClearanceFrom shouldEqual 220
      suggets.params.getClearanceTo shouldEqual 220
    }
  }

  "Acceleration" - {
    testQuery("за 3 секунды", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getAccelerationTo shouldEqual 3
      assert(!suggets.params.hasAccelerationFrom)
    }

    testQuery("до 7 секунд", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getAccelerationTo shouldEqual 7
      assert(!suggets.params.hasAccelerationFrom)
    }

    testQuery("от 10 до 20 секунд", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getAccelerationFrom shouldEqual 10
      suggets.params.getAccelerationTo shouldEqual 20
    }
  }

  "Trunk volume" - {
    testQuery("от 200 до 400 литров", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getTrunkVolumeFrom shouldEqual 200
      suggets.params.getTrunkVolumeTo shouldEqual 400
    }

    testQuery("багажник от 200", category = Some(Category.CARS)) { case suggets =>
      pending // can't implement rule using current parser
      suggets should have size (1)
      suggets.head.params.getTrunkVolumeFrom shouldEqual 200
      assert(!suggets.head.params.hasTrunkVolumeTo)
    }

    testQuery("от 450 литров", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getTrunkVolumeFrom shouldEqual 450
      assert(!suggets.params.hasTrunkVolumeTo)
    }

    testQuery("от 5 литров", category = Some(Category.CARS)) { case suggets :: Nil =>
      // unacceptable value, at leat 100 l.
      assert(!suggets.params.hasTrunkVolumeFrom)
      assert(!suggets.params.hasTrunkVolumeTo)
    }
  }

  "Owning time" - {
    testQuery("до года", category = Some(Category.CARS)) { case suggets =>
      pending // can't implement rule using current parser
      suggets should have size (1)
      suggets.head.params.getOwningTimeGroup shouldEqual OwningTimeGroup.LESS_THAN_YEAR
    }

    testQuery("до 1 года", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getOwningTimeGroup shouldEqual OwningTimeGroup.LESS_THAN_YEAR
    }

    testQuery("до 2 лет", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getOwningTimeGroup shouldEqual OwningTimeGroup.FROM_1_TO_3
    }

    testQuery("2-3 года", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getOwningTimeGroup shouldEqual OwningTimeGroup.FROM_1_TO_3
    }

    testQuery("до 5 лет", category = Some(Category.CARS)) { case suggets :: Nil =>
      suggets.params.getOwningTimeGroup shouldEqual OwningTimeGroup.MORE_THAN_3
    }

    testQuery("до 1957 года", category = Some(Category.CARS)) { case suggets :: Nil =>
      assert(!suggets.params.hasOwningTimeGroup)
    }

    testQuery("от 2005 лет", category = Some(Category.CARS)) { case suggets :: Nil =>
      assert(!suggets.params.hasOwningTimeGroup)
    }
  }

  "Loading" - {
    testQuery("5 тонн") { case suggest :: Nil =>
        suggest.params.getTrucksParams.getLoadingFrom shouldEqual 5000
        assert(!suggest.params.getTrucksParams.hasLoadingTo)
    }

    testQuery("6.5 тонн") { case suggest :: Nil =>
      suggest.params.getTrucksParams.getLoadingFrom shouldEqual 6500
      assert(!suggest.params.getTrucksParams.hasLoadingTo)
    }

    testQuery("3,5 т") { case suggest :: Nil =>
      suggest.params.getTrucksParams.getLoadingFrom shouldEqual 3500
      assert(!suggest.params.getTrucksParams.hasLoadingTo)
    }

    testQuery("от 2 тонн") { case suggest :: Nil =>
      suggest.params.getTrucksParams.getLoadingFrom shouldEqual 2000
      assert(!suggest.params.getTrucksParams.hasLoadingTo)
    }

    testQuery("2-3 тонны") { case suggest :: Nil =>
      suggest.params.getTrucksParams.getLoadingFrom shouldEqual 2000
      suggest.params.getTrucksParams.getLoadingTo shouldEqual 3000
    }

    testQuery("от 600 кг") { case suggest :: Nil =>
      suggest.params.getTrucksParams.getLoadingFrom shouldEqual 600
      assert(!suggest.params.getTrucksParams.hasLoadingTo)
    }
  }

  "Seats" - {
    testQuery("на 4 места") { case suggest =>
      val cars = suggest.filter(_.category == Category.CARS)
      val trucks = suggest.filter(_.category == Category.TRUCKS)
      cars should have size(1)
      trucks should have size(1)
      cars.head.params.getCarsParams.getSeatsGroup shouldEqual SeatsGroup.SEATS_4_TO_5
      trucks.head.params.getTrucksParams.getSeatsFrom shouldEqual 4
      trucks.head.params.getTrucksParams.getSeatsTo shouldEqual 4
    }

    testQuery("от 5 мест") { case suggest =>
      val cars = suggest.filter(_.category == Category.CARS)
      val trucks = suggest.filter(_.category == Category.TRUCKS)
      cars should have size(1)
      trucks should have size(1)
      cars.head.params.getCarsParams.getSeatsGroup shouldEqual SeatsGroup.SEATS_4_TO_5
      trucks.head.params.getTrucksParams.getSeatsFrom shouldEqual 5
      assert(!trucks.head.params.getTrucksParams.hasSeatsTo)
    }

    testQuery("от 4 до 5 мест") { case suggest =>
      val cars = suggest.filter(_.category == Category.CARS)
      val trucks = suggest.filter(_.category == Category.TRUCKS)
      cars should have size(1)
      trucks should have size(1)
      cars.head.params.getCarsParams.getSeatsGroup shouldEqual SeatsGroup.SEATS_4_TO_5
      trucks.head.params.getTrucksParams.getSeatsFrom shouldEqual 4
      trucks.head.params.getTrucksParams.getSeatsTo shouldEqual 5
    }

    testQuery("8 мест") { case suggest =>
      val cars = suggest.filter(_.category == Category.CARS)
      val trucks = suggest.filter(_.category == Category.TRUCKS)
      cars should have size(1)
      trucks should have size(1)
      cars.head.params.getCarsParams.getSeatsGroup shouldEqual SeatsGroup.SEATS_6_TO_8
      trucks.head.params.getTrucksParams.getSeatsFrom shouldEqual 8
      trucks.head.params.getTrucksParams.getSeatsTo shouldEqual 8
    }
  }

  "Сylinders" - {
    testQuery("2 цилиндра") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto should have size(1)
      moto.head.params.getMotoParams.getCylindersList.asScala.toSet shouldEqual Set(Moto.Cylinders.CYLINDERS_2)
    }

    testQuery("2 циллиндра") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto should have size (1)
      moto.head.params.getMotoParams.getCylindersList.asScala.toSet shouldEqual Set(Moto.Cylinders.CYLINDERS_2)
    }

    testQuery("7 цилиндров") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto should have size (1)
      moto.head.params.getMotoParams.getCylindersList should be (empty)
    }

    testQuery("4-6 цилиндров") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto should have size (1)
      moto.head.params.getMotoParams.getCylindersList.asScala.toSet shouldEqual
        Set(Moto.Cylinders.CYLINDERS_4, Moto.Cylinders.CYLINDERS_5, Moto.Cylinders.CYLINDERS_6)
    }

    testQuery("от 6 цилиндров") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto should have size (1)
      moto.head.params.getMotoParams.getCylindersList.asScala.toSet shouldEqual
        Set(Moto.Cylinders.CYLINDERS_6, Moto.Cylinders.CYLINDERS_8, Moto.Cylinders.CYLINDERS_10)
    }
  }

  "Strokes" - {
    testQuery("2 такта") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto should have size (1)
      moto.head.params.getMotoParams.getStrokesList.asScala.toSet shouldEqual Set(Moto.Strokes.STROKES_2)
    }

    testQuery("4 такта") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto should have size (1)
      moto.head.params.getMotoParams.getStrokesList.asScala.toSet shouldEqual Set(Moto.Strokes.STROKES_4)
    }

    testQuery("3 такта") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto.foreach(_.params.getMotoParams.getStrokesList should be (empty))
    }

    testQuery("2-4 такта") { case suggest =>
      val moto = suggest.filter(_.category == Category.MOTO)
      moto.head.params.getMotoParams.getStrokesList.asScala.toSet shouldEqual
        Set(Moto.Strokes.STROKES_2, Moto.Strokes.STROKES_4)
    }
  }

  "Custom house" - {
    testQuery("растаможен", Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getCustomsStateGroup shouldEqual CustomsGroup.CLEARED
    }

    testQuery("не растаможен", Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getCustomsStateGroup shouldEqual CustomsGroup.NOT_CLEARED
    }
  }

  "Haggle" - {
    testQuery("торг", Some(Category.TRUCKS)) { case suggest :: Nil =>
      suggest.params.getTrucksParams.getHaggle shouldEqual HaggleType.HAGGLE_POSSIBLE
    }

    testQuery("торг не уместен", Some(Category.TRUCKS)) { case suggest :: Nil =>
      suggest.params.getTrucksParams.getHaggle shouldEqual HaggleType.HAGGLE_NOT_POSSIBLE
    }
  }

  "Exchange" - {
    testQuery("обмен", Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getExchangeGroup shouldEqual ExchangeGroup.POSSIBLE
    }

    testQuery("обмен не интересует", Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getExchangeGroup shouldEqual ExchangeGroup.NO_EXCHANGE
    }
  }

  "Delivery" - {
    testQuery("доставка", Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getWithDelivery shouldEqual TristateTumblerGroup.BOTH
    }

    testQuery("без доставки", Some(Category.CARS)) { case suggest :: Nil =>
      suggest.params.getWithDelivery shouldEqual TristateTumblerGroup.NONE
    }
  }

  "Warranty" - {
    testQuery("на гарантии", Some(Category.CARS)) { case suggest :: Nil =>
      assert(suggest.params.getWithWarranty)
    }
  }

  "Query auto corrector" - {
    testQuery("митсубиши", category = Some(Category.MOTO)) { case suggest =>
      suggest shouldNot be (empty)
      suggest.foreach(_.category shouldNot equal(Category.MOTO))
    }

    testQuery("иьц") { case suggest =>
      suggest shouldNot be (empty)
      suggest.foreach(_ should matchMarkModel("BMW"))
    }

    testQuery("ajkmrcdfuty") { case suggest =>
      suggest shouldNot be (empty)
      suggest.foreach(_ should matchMarkModel("VOLKSWAGEN"))
    }

    // "ajkmrcdfut" -> "фольксваге"
    testQuery("ajkmrcdfut") { case Nil =>
      // should not use autocomplete for layout changed queries
    }
  }

  "Category auto filter" - {
    testQuery("купить машину в кемерово") { case suggest =>
      suggest.filter(_.category == Category.CARS) should not be empty
      suggest.filter(_.category == Category.TRUCKS) should not be empty
      suggest.filter(_.category == Category.MOTO) should be (empty)
    }

    testQuery("купить грузовую машину в кемерово") { case suggest =>
      suggest.filter(_.category == Category.CARS) should be (empty)
      suggest.filter(_.category == Category.TRUCKS) should not be empty
      suggest.filter(_.category == Category.MOTO) should be (empty)
    }

    testQuery("купить легковую машину в кемерово") { case suggest =>
      suggest.filter(_.category == Category.CARS) should not be empty
      suggest.filter(_.category == Category.TRUCKS) should be (empty)
      suggest.filter(_.category == Category.MOTO) should be (empty)
    }

    testQuery("купить мотоцикл в кемерово") { case suggest =>
      suggest.filter(_.category == Category.CARS) should be (empty)
      suggest.filter(_.category == Category.TRUCKS) should be (empty)
      suggest.filter(_.category == Category.MOTO) should not be empty
    }
  }

  "Nameplates" - {
    testQuery("skoda 100serie 100 i", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest should matchMarkModel("SKODA", "100_SERIES", generation = "20343254")
    }

    testQuery("skoda 100serie 120", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest should matchMarkModel("SKODA", "100_SERIES", nameplate = "9263374")
    }

    testQuery("skoda 100serie 120 100 i", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest should matchMarkModel("SKODA", "100_SERIES", generation = "20343254", nameplate = "9263374")
    }

    testQuery("skoda 100serie 100 i 120", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest should matchMarkModel("SKODA", "100_SERIES", generation = "20343254", nameplate = "9263374")
    }

    testQuery("ваз 2104 самара", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest should matchMarkModel("VAZ", "2114", nameplate = "9264332")
      suggest.params.getRidList should be (empty)
    }

    testQuery("таврия самара", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest should matchMarkModel("ZAZ", "TAVRIA")
      suggest.params.getRidList should not be empty
    }

    testQuery("ваз 2104 в самаре", category = Some(Category.CARS)) { case suggest :: Nil =>
      suggest should matchMarkModel("VAZ", "2104")
      suggest.params.getRidList should not be empty
    }
  }

  "Vendors" - {
    testQuery("японские во владивостоке") { case suggest =>
      suggest.foreach { s =>
        s should matchMarkModel("VENDOR7")
        s.params.getRidList.asScala.map(_.toInt).toSet shouldEqual Set(75)
      }
    }

    testQuery("отечественные в волгограде") { case suggest =>
      suggest.foreach { s =>
        s should matchMarkModel("VENDOR1")
        s.params.getRidList.asScala.map(_.toInt).toSet shouldEqual Set(38)
      }
    }

    testQuery("японские китайские или бентли", category = Some(Category.CARS)) { case suggest =>
      suggest.map(_.params.getMarkModelNameplateList.get(0)).toSet shouldEqual Set("VENDOR7", "VENDOR10", "BENTLEY")
    }
  }
}
