package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.uikit.text.InternalTextView
import ru.yandex.market.utils.children

class CharacteristicsMandatoryItemsMatcher(
    private val items: List<String>
) : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("check mandatory items in characteristics")
    }

    override fun matchesSafely(view: View?): Boolean {
        return (view as? TableLayout?)?.let {
            checkMandatoryItems(it)
        } ?: false
    }

    private fun checkMandatoryItems(view: TableLayout): Boolean {
        val characteristicsItems = view.children.toList().map {
            ((it as TableRow).children.first() as InternalTextView).text.toString()
        }
        return characteristicsItems.containsAll(items)
    }
}