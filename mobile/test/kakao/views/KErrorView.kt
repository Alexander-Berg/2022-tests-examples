package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R

class KErrorView(function: ViewBuilder.() -> Unit) : KBaseView<KErrorView>(function) {

    val image = KImageView {
        withId(R.id.common_error_image)
    }

    val title = KTextView {
        withId(R.id.common_error_title)
    }

    val subtitle = KTextView {
        withId(R.id.common_error_message)
    }

    val positiveButton = KButton {
        withId(R.id.positiveButton)
    }

    val negativeButton = KButton {
        withId(R.id.negativeButton)
    }

    fun checkSomethingGoesWrongVisible() {
        image.isVisible()

        title.hasText("Что-то пошло не так")
        title.isVisible()

        positiveButton.isVisible()
        positiveButton.hasText("Попробовать ещё раз")

        negativeButton.isGone()
    }

    fun checkServiceErrorVisible() {
        image.isVisible()

        title.hasText("Произошла ошибка\nсервиса")
        title.isVisible()

        positiveButton.isVisible()
        positiveButton.hasText("Обновить")

        negativeButton.isGone()
    }
}