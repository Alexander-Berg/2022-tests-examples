package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher
import ru.yandex.market.test.kakao.util.asViewMatcher
import ru.yandex.market.test.kakao.util.concatWith
import kotlin.reflect.KClass

open class KBaseCompoundView<KV>(
    viewClass: KClass<*>,
    function: ViewBuilder.() -> Unit
) : KBaseView<KV>(function.concatWith { isInstanceOf(viewClass.java) }) {

    constructor(viewClass: KClass<*>, parent: Matcher<View>, function: ViewBuilder.() -> Unit) : this(
        viewClass = viewClass,
        function = {
            isDescendantOfA { withMatcher(parent) }
            function()
        }
    )

    protected val parentMatcher = function.concatWith { isInstanceOf(viewClass.java) }.asViewMatcher()
}