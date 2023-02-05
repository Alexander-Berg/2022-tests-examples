package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import org.hamcrest.Matcher
import ru.beru.android.R

class KPhotoSnippetBlock(parent: Matcher<View>, function: ViewBuilder.() -> Unit) :
    KBaseView<KPhotoSnippetBlock>(parent, function) {

    private val wishLike = KImageView(parent) {
        withId(R.id.productOfferFavoriteButtonOnPhoto)
    }

    fun checkWishLikeVisible() {
        wishLike.isVisible()
    }
}
