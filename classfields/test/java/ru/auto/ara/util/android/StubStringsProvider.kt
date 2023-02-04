package ru.auto.ara.util.android

class StubStringsProvider: StringsProvider {
    override fun get(stringRes: Int): String = ""

    override fun get(stringRes: Int, vararg formatArgs: Any?): String = ""

    override fun plural(pluralRes: Int, count: Int): String = ""

    override fun plural(pluralRes: Int, count: Int, vararg formatArgs: Any?): String = ""

    override fun plural(pluralRes: Int, count: Int, zeroResource: Int): String = ""
}