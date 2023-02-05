package ru.yandex.yandexnavi.annotations.client_testapp.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.view_annotation_simple.view.*
import ru.yandex.yandexnavi.annotations.Annotation
import ru.yandex.yandexnavi.annotations.client_testapp.R
import ru.yandex.yandexnavi.annotations.client_testapp.repo.AnnotationRepo
import ru.yandex.yandexnavi.annotations.client_testapp.usecase.ActionToIconMapper
import ru.yandex.yandexnavi.common.utils.Optional
import java.text.SimpleDateFormat
import java.util.*

class AnnotationSimpleView : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private lateinit var disposable: CompositeDisposable
    private lateinit var annotationRepo: AnnotationRepo
    private lateinit var actionToIconMapper: ActionToIconMapper

    init {
        View.inflate(context, R.layout.view_annotation_simple, this)
    }

    fun bind(
        annotationRepo: AnnotationRepo,
        actionToIconMapper: ActionToIconMapper
    ) {
        this.annotationRepo = annotationRepo
        this.actionToIconMapper = actionToIconMapper
        onInit()
    }

    private fun onInit() {
        if (::disposable.isInitialized && !disposable.isDisposed) {
            val annotationDisposable = annotationRepo.annotations()
                .subscribe(::updateAnnotation)
            disposable.add(annotationDisposable)
        }
    }

    private fun updateAnnotation(annotationOptional: Optional<Annotation>) {
        val currentTime = SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(Date())
        view_updated_at.text = "Updated at: $currentTime"
        val annotation = annotationOptional.get()
        view_action.text = "Action: ${annotation?.action}"
        view_distance.text = "Distance: ${annotation?.distance}"

        val icon = if (annotation != null) {
            actionToIconMapper.execute(annotation.action)
        } else {
            R.drawable.notification_exit
        }
        view_icon.setImageResource(icon)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        disposable = CompositeDisposable()
        onInit()
    }

    override fun onDetachedFromWindow() {
        disposable.dispose()
        super.onDetachedFromWindow()
    }
}
