package ru.yandex.partner.core.entity.block.formats

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


internal class MediaFormatTest {

    @Test
    fun test() {
        val mediaFormat = setOf(
            "1000x120", "160x600", "240x400", "240x600", "300x250", "300x300",
            "300x500", "300x600", "336x280", "728x90", "970x250", "970x90"
        )
            .map { MediaFormat.Companion.buildMediaFormat(it)!! }
            .maxByOrNull { it.square.or(it.height) }
        Assertions.assertThat(mediaFormat?.square).isEqualTo(242500)

        val withPercent = MediaFormat.Companion.buildMediaFormat("550%x1000")
        Assertions.assertThat(withPercent?.square).isEqualTo(1000)

    }

}


