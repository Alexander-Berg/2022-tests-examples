package ru.auto.core_ui.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import ru.auto.ara.util.android.StringsProvider
import ru.auto.core_ui.R

class DateExtKtSpec : FreeSpec({
    "describe ${Long::secondsToTimeWords.name}" - {
        "it should format 0s" {
            0L.secondsToTimeWords(StubStringsProvider) shouldBe "0 с"
        }
        "it should format 1s" {
            1L.secondsToTimeWords(StubStringsProvider) shouldBe "1 с"
        }
        "it should format 33s" {
            33L.secondsToTimeWords(StubStringsProvider) shouldBe "33 с"
        }
        "it should format 60s" {
            60L.secondsToTimeWords(StubStringsProvider) shouldBe "1 м 0 с"
        }
        "it should format 153s" {
            153L.secondsToTimeWords(StubStringsProvider) shouldBe "2 м 33 с"
        }
        "it should format 693s" {
            693L.secondsToTimeWords(StubStringsProvider) shouldBe "11 м 33 с"
        }
        "it should format 3600s" {
            3600L.secondsToTimeWords(StubStringsProvider) shouldBe "1 ч 0 м 0 с"
        }
        "it should format 4293s" {
            4293L.secondsToTimeWords(StubStringsProvider) shouldBe "1 ч 11 м 33 с"
        }
        "it should format 90693s" {
            90693L.secondsToTimeWords(StubStringsProvider) shouldBe "25 ч 11 м 33 с"
        }
    }
})


@Suppress("NotImplementedDeclaration") // This is a stub class, not everything should be implemented
private object StubStringsProvider : StringsProvider {
    override fun plural(pluralRes: Int, count: Int): String {
        TODO("not implemented")
    }

    override fun plural(pluralRes: Int, count: Int, vararg formatArgs: Any?): String {
        TODO("not implemented")
    }

    override fun plural(pluralRes: Int, count: Int, zeroResource: Int): String {
        TODO("not implemented")
    }

    override fun get(stringRes: Int): String {
        TODO("not implemented")
    }

    override fun get(stringRes: Int, vararg formatArgs: Any?): String = when (stringRes) {
        R.string.hours -> "${formatArgs.first()} ч"
        R.string.minutes -> "${formatArgs.first()} м"
        R.string.seconds -> "${formatArgs.first()} с"
        else -> {
            TODO("not implemented")
        }
    }

}
