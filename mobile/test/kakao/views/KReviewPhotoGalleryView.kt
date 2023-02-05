package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R
import ru.yandex.market.screen.reviews.ReviewPhotoRecyclerItem

class KReviewPhotoGalleryView(private val function: ViewBuilder.() -> Unit) :
    KBaseView<KReviewPhotoGalleryView>(function) {

    private val showAllText = KTextView {
        isDescendantOfA(this@KReviewPhotoGalleryView.function)
        withId(R.id.textReviewPhotoGalleryShowAll)
    }

    private val recycler = KRecyclerView(builder = {
        isDescendantOfA(this@KReviewPhotoGalleryView.function)
        withId(R.id.recyclerReviewPhotoGallery)
    }) {
        itemType(::ReviewPhotoRecyclerItem)
    }

    fun clickShowAll() {
        showAllText.click()
    }

    fun clickItem(position: Int) {
        recycler.childAt<ReviewPhotoRecyclerItem>(position) {
            click()
        }
    }

    fun checkItem(position: Int) {
        recycler.childAt<ReviewPhotoRecyclerItem>(position) {
            isVisible()
        }
    }

}