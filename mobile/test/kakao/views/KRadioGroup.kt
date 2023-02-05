package ru.yandex.market.test.kakao.views

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher

class KRadioGroup : KBaseView<KRadioGroup> {
    constructor(function: ViewBuilder.() -> Unit) : super(function)
    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun setCheckedButtonWithId(id: Int) {
        view.check { view, notFoundException ->
            if (view is RadioGroup) {
                view.findViewById<RadioButton>(id)?.let {
                    view.check(id)
                } ?: throw NullPointerException("There's no radio button with id: $id")
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }
}