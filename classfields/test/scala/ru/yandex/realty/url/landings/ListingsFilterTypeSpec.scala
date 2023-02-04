package ru.yandex.realty.url.landings

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.canonical.base.params.ParameterType
import ru.yandex.realty.urls.landings.ListingFilterType
import ru.yandex.realty.urls.landings.ListingFilterType.WithIncludeTagFilter

class ListingsFilterTypeSpec extends WordSpec with Matchers {

  "ListingsFilterType" should {

    "for include tag mixin" should {

      "has mixin when have include tag param" in {
        val target = ListingFilterType.values.filterNot(_.isInstanceOf[WithIncludeTagFilter])
        for {
          t <- target
        } withClue(s"${t.entryName} filter") {
          t.asParams.map(_.`type`) should not contain ParameterType.IncludeTags
        }

      }

      "has not mixin when has not include tag param" in {
        val target = ListingFilterType.values.filter(_.isInstanceOf[WithIncludeTagFilter])
        for {
          t <- target
        } withClue(s"${t.entryName} filter") {
          t.asParams.map(_.`type`) should contain(ParameterType.IncludeTags)
        }

      }

    }
  }

}
