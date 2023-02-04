package ru.yandex.realty.model.tags

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{CategoryType, Offer, OfferType}
import ru.yandex.realty.model.tags.extractors.DescriptionContainsExtractor

/**
  *
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class DescriptionContainsExtractorTest extends SpecBase {

  "DescriptionContainsAlgorithm" should {
    "Return tag for check if offer description contains at least one value from the input list for empty restrictions " in {
      val offer = new Offer()
      offer.setDescription("Это описание оффера должно содержать подстроку 'в стиле лофт'")
      val values = Seq("в стиле лофт", "странный стиль", "подстрока")
      val tag = Tag(1L, "name", 1, Seq.empty, CategoryAndTypeRestriction(Set.empty, Set.empty))
      val algorithm = DescriptionContainsExtractor(tag, values)

      algorithm.extract(offer) should be(Set(tag))
    }

    "Do not return Tag for check if offer description does not contain any value from the input list for empty restrictions" in {
      val offer = new Offer()
      offer.setDescription("Это описание оффера не должно содержать входные значения для алгоритма")
      val values = Seq("в стиле лофт", "странный стиль", "подстрока")
      val tag = Tag(2L, "name", 1, Seq.empty, CategoryAndTypeRestriction(Set.empty, Set.empty))
      val algorithm = DescriptionContainsExtractor(tag, values)

      algorithm.extract(offer) should be(Set.empty)
    }

    "Return tag for check if offer description contains at least one value from the input list and is applicable for tag restrictions" in {
      val offer = new Offer()
      offer.setDescription("Это описание оффера должно содержать подстроку 'в стиле лофт'")
      offer.setCategoryType(CategoryType.APARTMENT)
      offer.setOfferType(OfferType.RENT)
      val values = Seq("в стиле лофт", "странный стиль", "подстрока")
      val tag = Tag(
        3L,
        "name",
        1,
        Seq.empty,
        CategoryAndTypeRestriction(Set(CategoryType.APARTMENT, CategoryType.ROOMS), Set(OfferType.RENT))
      )
      val algorithm = DescriptionContainsExtractor(tag, values)

      algorithm.extract(offer) should be(Set(tag))
    }

    "Do not return Tag for check if offer description contains value from the input list and is NOT applicable for tag restrictions" in {
      val offer = new Offer()
      offer.setDescription("Это описание оффера должно содержать подстроку 'в стиле лофт'")
      offer.setCategoryType(CategoryType.COMMERCIAL)
      offer.setOfferType(OfferType.RENT)
      val values = Seq("в стиле лофт", "странный стиль", "подстрока")
      val tag = Tag(
        4L,
        "name",
        1,
        Seq.empty,
        CategoryAndTypeRestriction(Set(CategoryType.APARTMENT, CategoryType.ROOMS), Set(OfferType.RENT))
      )
      val algorithm = DescriptionContainsExtractor(tag, values)

      algorithm.extract(offer) should be(Set.empty)
    }
  }

}
