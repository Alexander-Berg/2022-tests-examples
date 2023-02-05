package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R
import ru.yandex.market.feature.starrating.StarsLayout
import ru.yandex.market.ui.view.ReviewGradeLayout
import ru.yandex.market.utils.children

class KReviewGrade(function: ViewBuilder.() -> Unit) : KBaseView<KReviewGrade>(function) {

    private val errorText = KTextView {
        withId(R.id.textReviewGradeError)
        isDescendantOfA(function)
    }

    fun checkGrade(grade: Int) {
        view.check { view, notFoundException ->
            if (view is ReviewGradeLayout) {
                if (view.selectedGrade != grade) {
                    throw AssertionError("Wrong grade, expected $grade got ${view.selectedGrade}")
                }
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }

    fun setGrade(grade: Int) {
        view.check { view, notFoundException ->
            if (view is ReviewGradeLayout) {
                val childIndex = grade - 1
                view.children
                    .filterIsInstance<StarsLayout>()
                    .firstOrNull()
                    ?.getChildAt(childIndex)
                    ?.performClick()
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }

    fun isErrorVisible() {
        errorText.isVisible()
    }
}