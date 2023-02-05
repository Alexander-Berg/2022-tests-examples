package ru.yandex.market.test.kakao.views

import android.widget.LinearLayout
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R
import ru.yandex.market.screen.item.AddReviewPhotoItem
import ru.yandex.market.screen.item.SelectReviewPhotoItem

class KAddProductPhotoView(function: ViewBuilder.() -> Unit) : KBaseView<LinearLayout>(function) {

    private val addReviewPhoto = KRecyclerView(builder = {
        isDescendantOfA(function)
        withId(R.id.recyclerAddPhoto)
    }, itemTypeBuilder = {
        itemType(::AddReviewPhotoItem)
        itemType(::SelectReviewPhotoItem)
    })

    private val photoCount = KTextView {
        withId(R.id.textAddPhotoCount)
        isDescendantOfA(function)
    }

    fun clickAddPhoto() {
        addReviewPhoto.scrollTo()
        addReviewPhoto.lastChild<SelectReviewPhotoItem> { click() }
    }

    fun checkPhotoAdded(position: Int) {
        addReviewPhoto.childAt<AddReviewPhotoItem>(position) {
            isVisible()
        }
    }

    fun checkPhotoCountText(text: String) {
        photoCount.hasText(text)
    }

    fun clickPhotoRemove(position: Int) {
        addReviewPhoto.childAt<AddReviewPhotoItem>(position) {
            clickRemove()
        }
    }
}