package ru.yandex.market.test.kakao.views

import com.agoda.kakao.common.builders.ViewBuilder
import com.agoda.kakao.common.views.KBaseView
import com.agoda.kakao.image.ImageViewAssertions

class KNavigationBar(
    private val function: ViewBuilder.() -> Unit
) : KBaseView<KNavigationBar>(function),
    ImageViewAssertions {
    val feedTab = KNavigationTab {
        isDescendantOfA(this@KNavigationBar.function)
        withResourceName("feedTabFragment")
    }

    class KNavigationTab(private val function: ViewBuilder.() -> Unit) :
        KBaseView<KNavigationTab>(function)
}