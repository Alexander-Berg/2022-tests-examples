package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.list.KAdapterItemTypeBuilder
import io.github.kakaocup.kakao.spinner.KSpinner
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R

class KMarketSpinner(
    private val findParentViewAction: ViewBuilder.() -> Unit,
    itemTypeBuilder: KAdapterItemTypeBuilder.() -> Unit
) : KBaseView<KSpinner>(findParentViewAction) {

    val dialog = KSpinner(
        builder = { withId(R.id.dateSelectorView) },
        itemTypeBuilder = itemTypeBuilder
    )

    init {
        findParentViewAction
    }

    val icExpandImageView = KImageView {
        isDescendantOfA(this@KMarketSpinner.findParentViewAction)
        withId(R.id.icon)
    }

    val hintTextView = KTextView {
        isDescendantOfA(this@KMarketSpinner.findParentViewAction)
        withId(R.id.hint)
    }

    val valueTextView = KTextView {
        isDescendantOfA(this@KMarketSpinner.findParentViewAction)
        withId(R.id.text)
    }

}