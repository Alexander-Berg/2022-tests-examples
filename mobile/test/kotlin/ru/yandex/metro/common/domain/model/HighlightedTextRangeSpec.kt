package ru.yandex.metro.common.domain.model

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEmpty
import ru.yandex.metro.ClassSpek

class HighlightedTextRangeSpec : ClassSpek(HighlightedTextRange::class.java, {
    context("case sensitive") {
        val source = "Park Kultury"
        val match = "Park"

        it("should return list with exactly one highlight") {
            val result = source.getHighlightedTextRanges(match)
            result.count() shouldBe 1
            result[0].begin shouldBe 0
            result[0].length shouldBe 4
        }
    }

    context("case insensitive") {
        val source = "İkitelli Sanayi"
        val match = "ikiTe"

        it("should return list with exactly one highlight") {
            val result = source.getHighlightedTextRanges(match)
            result.count() shouldBe 1
            result[0].begin shouldBe 0
            result[0].length shouldBe 5
        }
    }

    context("substring fully matches source") {
        val source = "Park Kultury"
        val match = "Park Kultury"

        it("should return list with exactly one highlight") {
            val result = source.getHighlightedTextRanges(match)
            result.count() shouldBe 1
            result[0].begin shouldBe 0
            result[0].length shouldBe 12
        }
    }

    context("source contains substring more than once") {
        val source = "Бульвар Рокоссовского"
        val match = "ко"

        it("should return list with 2 highlights") {
            val result = source.getHighlightedTextRanges(match)
            result.count() shouldBe 2

            result[0].begin shouldBe 10
            result[0].length shouldBe 2

            result[1].begin shouldBe 17
            result[1].length shouldBe 2
        }
    }

    context("source does Not contain substring") {
        val source = "Kropotkinskaya"
        val match = "Biblioteka"

        it("should return empty list") {
            val result = source.getHighlightedTextRanges(match)
            result.shouldBeEmpty()
        }
    }

    context("match is empty") {
        val source = "Park Kultury"
        val match = ""

        it("should return empty list") {
            val result = source.getHighlightedTextRanges(match)
            result.shouldBeEmpty()
        }
    }

    context("source is shorter than substring") {
        val source = "Belomorskaya"
        val match = "Belomorskaya ulica"

        it("should return empty list") {
            val result = source.getHighlightedTextRanges(match)
            result.shouldBeEmpty()
        }
    }
})
