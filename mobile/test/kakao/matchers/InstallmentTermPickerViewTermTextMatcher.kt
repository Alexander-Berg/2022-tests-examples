package ru.yandex.market.test.kakao.matchers

import android.view.View
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.feature.termPicker.view.TermPickerView

class InstallmentTermPickerViewTermTextMatcher(
    val text: String
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("InstallmentTermPickerView contains selected term with text \"$text\"")
    }

    override fun matchesSafely(view: View): Boolean {
        return (view is TermPickerView) && view.getSelectedTerm() == text
    }
}
