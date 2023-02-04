package ru.yandex.vertis.general.favorites.model.testkit

import general.search.model.SearchContextEnum.SearchContext
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.favorites.model.searches.{SavedSearchInverted, SavedSearchInvertedListItem}
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia.DeriveGen

object SavedSearchInvertedListItemGen {

  implicit private val unknownFieldSetGen: DeriveGen[_root_.scalapb.UnknownFieldSet] =
    DeriveGen.instance(Gen.const(_root_.scalapb.UnknownFieldSet.empty))

  val itemGen: Gen[Random with Sized, SavedSearchInvertedListItem] = for {
    savedSearchInverted <- DeriveGen[SavedSearchInverted].noShrink
    savedSearchInvertedFixed = savedSearchInverted.copy(searchRequest =
      savedSearchInverted.searchRequest.copy(
        searchContext = SearchContext.FAVORITES_INTERNAL,
        lockedFields = savedSearchInverted.searchRequest.lockedFields.filterNot(f => f.isUnrecognized || f.isUnknown)
      )
    )
    ownerId <- OwnerIdGen.anyUserId.noShrink
  } yield SavedSearchInvertedListItem(ownerId = ownerId, savedSearchInverted = savedSearchInvertedFixed)

  def items(count: Int): Gen[Random with Sized, List[SavedSearchInvertedListItem]] =
    Gen.listOfN(count)(itemGen.noShrink).noShrink

}
