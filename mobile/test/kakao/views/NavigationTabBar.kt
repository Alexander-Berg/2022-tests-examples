package ru.yandex.market.test.kakao.views

import android.widget.ImageView
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.ImageViewAssertions
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R
import ru.yandex.market.test.kakao.util.isNotVisible
import ru.yandex.market.test.kakao.util.withPosition
import ru.yandex.market.ui.view.bottomnavigation.NavigationTabBarItemView

class KNavigationTabBar(
    private val tabCount: Int = DEFAULT_TAB_COUNT,
    private val function: ViewBuilder.() -> Unit
) : KBaseView<KNavigationTabBar>(function) {

    init {
        require(tabCount > 0) {
            "Positive tab count required, but actual value is $tabCount!"
        }
    }

    val tabs = (0 until tabCount).map {
        KNavigationTab {
            isDescendantOfA(this@KNavigationTabBar.function)
            isInstanceOf(NavigationTabBarItemView::class.java)
            withPosition(it)
        }
    }.toTypedArray()

    val mainTab = tabs[MAIN_TAB_INDEX]

    val catalogTab = tabs[CATALOG_TAB_INDEX]

    val discountTab = tabs[DISCOUNTS_TAB_INDEX]

    val expressTab = tabs[EXPRESS_TAB_INDEX]

    val productsTab = tabs[PRODUCTS_TAB_INDEX]

    val cartTab = tabs[CART_TAB_INDEX]

    val profileTab = tabs[PROFILE_TAB_INDEX]

    fun isSelected(index: Int) {
        tabs.forEachIndexed { i, tab ->
            run {
                if (i == index) {
                    tab.isSelected()
                } else {
                    tab.isNotSelected()
                }
            }
        }
    }

    fun isMainTabSelected() = isSelected(MAIN_TAB_INDEX)

    fun isCatalogTabSelected() = isSelected(CATALOG_TAB_INDEX)

    fun isDiscountsTabSelected() = isSelected(DISCOUNTS_TAB_INDEX)

    fun isExpressTabSelected() = isSelected(EXPRESS_TAB_INDEX)

    fun isProductsTabSelected() = isSelected(PRODUCTS_TAB_INDEX)

    fun isCartTabSelected() = isSelected(CART_TAB_INDEX)

    fun isProfileTabSelected() = isSelected(PROFILE_TAB_INDEX)

    class KNavigationTab(private val function: ViewBuilder.() -> Unit) :
        KBaseView<KNavigationTab>(function),
        ImageViewAssertions {

        private val icon = KImageView {
            isDescendantOfA(this@KNavigationTab.function)
            isInstanceOf(ImageView::class.java)
        }

        private val badge = KTextView {
            isDescendantOfA(this@KNavigationTab.function)
            withId(R.id.badge)
        }

        fun checkBadgeText(badgeText: String) {
            badge.hasText(badgeText)
        }

        fun checkBadgeVisible(visible: Boolean) {
            badge {
                if (visible) {
                    isVisible()
                } else {
                    isNotVisible()
                }
            }
        }
    }

    companion object {
        private const val MAIN_TAB_INDEX = 0
        private const val CATALOG_TAB_INDEX = 1
        private const val DISCOUNTS_TAB_INDEX = 2
        private const val EXPRESS_TAB_INDEX = 3
        private const val PRODUCTS_TAB_INDEX = 4
        private const val CART_TAB_INDEX = 5
        private const val PROFILE_TAB_INDEX = 6
        private const val DEFAULT_TAB_COUNT = 7
    }
}
