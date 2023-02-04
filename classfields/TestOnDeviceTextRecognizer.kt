package ru.auto.feature.recognizer.textrecognizer

import android.app.Activity
import android.graphics.Rect
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import ru.auto.core_ui.util.runOnUiDelayed
import java.util.concurrent.Executor

class TestOnDeviceTextRecognizer : IOnDeviceTextRecognizer {

    private val listTextBlock: List<Text.TextBlock> = listOf(
        Text.TextBlock(
            "",
            Rect(),
            emptyList(),
            "",
            listOf(
                createLine2("а108аа195"),
                createLine2("б108бб195"),
                createLine2("(VIN)"),
                createLine2("JTJHK31U802038156")
            )
        )
    )

    private val processingResult = Text("", listTextBlock)

    private fun createLine2(p0: String) = Text.Line("", Rect(), emptyList(), "", listOf(createElement2(p0)))

    @Suppress("MagicNumber")
    private fun createElement2(p0: String) = Text.Element(p0, Rect(338, 909, 687, 953), emptyList(), "")

    override fun processImage(image: InputImage): Task<Text> {
        return object : Task<Text>() {
            override fun isComplete() = true
            override fun getException(): Exception? = null
            override fun addOnFailureListener(p0: OnFailureListener) = this
            override fun addOnFailureListener(p0: Executor, p1: OnFailureListener) = this
            override fun addOnFailureListener(p0: Activity, p1: OnFailureListener) = this
            override fun addOnSuccessListener(p0: OnSuccessListener<in Text>) = also {
                runOnUiDelayed(RECOGNITION_DELAY) { p0.onSuccess(processingResult) }
            }

            override fun addOnSuccessListener(p0: Executor, p1: OnSuccessListener<in Text>) = also {
                runOnUiDelayed(RECOGNITION_DELAY) { p1.onSuccess(processingResult) }
            }

            override fun addOnSuccessListener(p0: Activity, p1: OnSuccessListener<in Text>) = also {
                runOnUiDelayed(RECOGNITION_DELAY) { p1.onSuccess(processingResult) }
            }

            override fun isSuccessful() = true
            override fun isCanceled() = false
            override fun getResult() = processingResult
            override fun <X : Throwable?> getResult(p0: Class<X>) = processingResult
        }
    }

    companion object {
        private const val RECOGNITION_DELAY = 5000L
    }
}
