package com.yandex.maps.testapp

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.LayoutInflater
import android.widget.*
import androidx.core.util.Consumer

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.DialogFragment;
import java.lang.ref.WeakReference

class LocaleDialog : DialogFragment() {
    private var appliedLocale = ""
    private var onApplyWeak = WeakReference<Consumer<String>>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (onApplyWeak.get() == null) {
            dismiss()
            return null
        }
        dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)

        val contentView = inflater.inflate(R.layout.locale_dialog_layout, null)

        val localeEditText = contentView.findViewById<EditText>(R.id.locale_edit_text)
        localeEditText.setText(appliedLocale)

        val applyButton = contentView.findViewById<Button>(R.id.apply_button)
        applyButton.setOnClickListener{
            val newLocale = localeEditText.text.toString()
            onApplyWeak.get()?.accept(newLocale)
            dismiss()
        }

        val resetButton = contentView.findViewById<Button>(R.id.reset_button)
        resetButton.setOnClickListener {
            localeEditText.setText(appliedLocale)
        }

        contentView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dismiss()
        }

        contentView.findViewById<Button>(R.id.ru_locale_button).setOnClickListener {
            localeEditText.setText(getString(R.string.ru_locale))
            applyButton.performClick()
        }

        contentView.findViewById<Button>(R.id.en_locale_button).setOnClickListener {
            localeEditText.setText(getString(R.string.en_locale))
            applyButton.performClick()
        }

        localeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                val localeTextChanged = appliedLocale != localeEditText.text.toString()
                applyButton.isEnabled = localeTextChanged
                resetButton.isEnabled = localeTextChanged
            }
        })

        return contentView
    }

    companion object {
        fun show(activity: FragmentActivity, initLocale: String, onApply: Consumer<String>) {
            val dialog = LocaleDialog()
            dialog.appliedLocale = initLocale
            dialog.onApplyWeak = WeakReference(onApply)
            dialog.show(activity.supportFragmentManager, "locale_dialog")
        }
    }
}
