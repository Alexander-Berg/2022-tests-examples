package ru.yandex.yandexnavi.annotations.client_testapp.repo

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import ru.yandex.yandexnavi.annotations.Annotation
import ru.yandex.yandexnavi.annotations.gateway.AnnotationGateway
import ru.yandex.yandexnavi.common.utils.Optional

class AnnotationRepo(
    private val annotationGateway: AnnotationGateway
) {
    private val annotationProcessor = BehaviorProcessor.create<Optional<Annotation>>()

    init {
        annotationGateway.addListener(object : AnnotationGateway.AnnotationListener {
            override fun onAnnotationChanged() {
                val annotation = Optional.of(annotationGateway.annotation())
                annotationProcessor.onNext(annotation)
                logAnnotation(annotation)
            }
        })
    }

    fun annotations(): Flowable<Optional<Annotation>> {
        return annotationProcessor
            .onBackpressureLatest()
    }

    private fun logAnnotation(annotationOptional: Optional<Annotation>) {
        Log.d(
            "ANNOTATION_TEST",
            "Updated annotation" +
                ", action=${annotationOptional.get()?.action}" +
                ", distance=${annotationOptional.get()?.distance}"
        )
    }
}
