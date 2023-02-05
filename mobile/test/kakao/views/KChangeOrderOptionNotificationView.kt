package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class KChangeOrderOptionNotificationView(private val findParentViewAction: ViewBuilder.() -> Unit) :
    KBaseView<KModernInputView>(findParentViewAction) {

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : this({
        isDescendantOfA { withMatcher(parent) }
        function(this)
    })

    val notificationIconImageView = KImageView {
        isDescendantOfA(this@KChangeOrderOptionNotificationView.findParentViewAction)
        withId(R.id.notificationIcon)
    }

    val closeButton = KImageView {
        isDescendantOfA(this@KChangeOrderOptionNotificationView.findParentViewAction)
        withId(R.id.closeButton)
    }

    val messageTextView = KTextView {
        isDescendantOfA(this@KChangeOrderOptionNotificationView.findParentViewAction)
        withId(R.id.messageView)
    }

}