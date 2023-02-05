package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.toolbar.ToolbarViewAssertions
import ru.beru.android.R

class KSearchAppBarLayout(function: ViewBuilder.() -> Unit) : KBaseView<KSearchAppBarLayout>(function),
    ToolbarViewAssertions {

    private val backButton = KImageView { withId(R.id.viewSearchAppBarLayoutBackIcon) }
    private val searchInput = KEditText { withId(R.id.viewSearchAppBarLayoutInput) }
    private val searchIcon = KImageView { withId(R.id.viewSearchAppBarLayoutSearchIcon) }

    fun isBackButtonVisible() = backButton.isVisible()

    fun isSearchInputVisible() = searchInput.isVisible()

    fun isSearchInputFocused() = searchInput.isFocused()

    fun searchInputHasEmptyText() = searchInput.hasEmptyText()

    fun searchInputHasHint(hint: String) = searchInput.hasHint(hint)

    fun searchByRequest(requestText: String) {
        searchInput.replaceText(requestText)
        searchIcon.click()
    }
}
