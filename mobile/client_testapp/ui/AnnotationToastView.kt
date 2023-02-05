package ru.yandex.yandexnavi.annotations.client_testapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import ru.yandex.yandexnavi.annotations.Annotation
import ru.yandex.yandexnavi.annotations.client_testapp.repo.AnnotationRepo
import ru.yandex.yandexnavi.common.utils.Optional
import java.util.concurrent.TimeUnit

class AnnotationToastView(
    private val context: Context,
    private val annotationRepo: AnnotationRepo
) {
    init {
        subscribeToAnnotations()
    }

    @SuppressLint("CheckResult")
    private fun subscribeToAnnotations() {
        annotationRepo.annotations()
            .throttleLatest(2, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                showAnnotation(it)
            }
    }

    private fun showAnnotation(annotationOptional: Optional<Annotation>) {
        val annotation = annotationOptional.get()
        val text = "Route annotation updated\nAction=${annotation?.action}\nDistance=${annotation?.distance}"
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}
