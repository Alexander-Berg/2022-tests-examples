package com.yandex.maps.testapp.search.test

import com.yandex.maps.testapp.search.toJson

private val replacements = mapOf(
    // Prettify field names
    Regex("""([^\\])"([^"\n]+)":""") to "$1$2 →",
    // Unescape quotes
    Regex("""\\"""") to "\"",
    // Ellipsize long strings
    Regex(""""([^"\n]{40})[^"\n]+"""") to "\"$1…\"",
    // Remove unneeded opening/closing braces, first level array braces and end-of-line commas
    Regex("""(^\{\n|\n\}|,\s*(?=\n))|^\[\n|\n\]""") to "",
    // Remove first indent level
    Regex("""(^|\n)  """) to "$1"
)

private fun applyReplacements(string: String): String {
    var json = string
    replacements.forEach { (regex, replacement) ->
        json = regex.replace(json, replacement)
    }
    return json
}

fun <T> toPrettyJson(obj: T): String {
    return applyReplacements(toJson(obj))
}
