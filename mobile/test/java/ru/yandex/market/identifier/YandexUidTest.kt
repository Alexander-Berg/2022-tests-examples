package ru.yandex.market.identifier

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.domain.auth.model.YandexUid

class YandexUidTest {

    @Test
    fun `Method 'isEmpty' returns true for null yandex uid reference`() {
        assertThat(YandexUid.isEmpty(null)).isTrue
    }

    @Test
    fun `Method 'isEmpty' returns true for yandex uid with empty value`() {
        val yuidWithEmptyValue = YandexUid("")
        assertThat(YandexUid.isEmpty(yuidWithEmptyValue)).isTrue
    }

    @Test
    fun `Method 'isEmpty' returns false for yandex uid with non-empty value`() {
        val yuid = YandexUid("non-empty-value")
        assertThat(YandexUid.isEmpty(yuid)).isFalse
    }

    @Test
    fun `Method 'equals' returns true for yandex uids with same values`() {
        val yuid1 = YandexUid("yuid")
        val yuid2 = YandexUid("yuid")
        assertThat(yuid1 == yuid2).isTrue
    }

    @Test
    fun `Method 'equals' returns false for yandex uids with different values`() {
        val yuid1 = YandexUid("yuid1")
        val yuid2 = YandexUid("yuid2")
        assertThat(yuid1 == yuid2).isFalse
    }

}
