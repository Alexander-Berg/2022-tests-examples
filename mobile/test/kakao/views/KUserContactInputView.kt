package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.clean.presentation.view.UserContactInputView

class KUserContactInputView : KBaseCompoundView<KUserContactInputView> {

    constructor(function: ViewBuilder.() -> Unit) : super(UserContactInputView::class, function)

    constructor(
        parent: Matcher<View>,
        function: ViewBuilder.() -> Unit
    ) : super(UserContactInputView::class, parent, function)

    private val fullNameModernInputView = KModernInputView(parentMatcher) {
        withId(R.id.editProfileFullName)
    }

    private val emailModernInputView = KModernInputView(parentMatcher) {
        withId(R.id.editProfileEmail)
    }

    private val phoneModernInputView = KModernInputView(parentMatcher) {
        withId(R.id.editProfilePhone)
    }

    fun replaceFullName(fullName: String) {
        fullNameModernInputView.replaceText(fullName)
    }

    fun replaceEmail(email: String) {
        emailModernInputView.replaceText(email)
    }

    fun replacePhone(phone: String) {
        phoneModernInputView.replaceText(phone)
    }

}