package ru.yandex.yandexnavi.annotations.client_testapp

import android.content.Context
import ru.yandex.yandexnavi.annotations.AnnotationSdkComponent
import ru.yandex.yandexnavi.annotations.client_testapp.repo.AnnotationRepo
import ru.yandex.yandexnavi.annotations.client_testapp.ui.AnnotationToastView
import ru.yandex.yandexnavi.annotations.client_testapp.usecase.ActionToIconMapper

class Di(
    private val context: Context
) {
    private val annotationGateway = AnnotationSdkComponent.annotationGateway(context)
    private val annotationRepo = AnnotationRepo(annotationGateway)

    fun annotationRepo(): AnnotationRepo = annotationRepo
    fun actionToIconMapper(): ActionToIconMapper = ActionToIconMapper()
    fun annotationToastView(): AnnotationToastView = AnnotationToastView(context, annotationRepo)
}
