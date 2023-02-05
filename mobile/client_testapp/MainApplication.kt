package ru.yandex.yandexnavi.annotations.client_testapp

import android.app.Application
import android.content.Context
import io.reactivex.processors.BehaviorProcessor
import ru.yandex.yandexnavi.annotations.client_testapp.ui.AnnotationToastView

class MainApplication : Application() {

    private val diProcessor = BehaviorProcessor.create<Di>()
    lateinit var di: Di

    private lateinit var annotationToastView: AnnotationToastView

    override fun onCreate() {
        super.onCreate()
        di = Di(this)
        annotationToastView = di.annotationToastView()
    }
}

fun Context.di(): Di {
    return (this.applicationContext as MainApplication).di
}
