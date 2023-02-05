package ru.yandex.market.toxin.report

import toxin.tools.verifiers.OverrideVerifier

class ToxinOverrideReportFormatter {

    fun format(errors: List<OverrideVerifier.IllegalOverrideError>): String {
        return buildString {
            for (error in errors) {
                appendLine("Error: " + error.message)
                appendLine()
            }
        }
    }
}