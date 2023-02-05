package ru.yandex.market.identifier

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.domain.auth.model.MarketUid

class MarketUidTest {

    @Test
    fun `Method 'isEmpty' returns true for null market uid reference`() {
        assertThat(MarketUid.isEmpty(null)).isEqualTo(true)
    }

    @Test
    fun `Method 'isEmpty' returns true for market uid with empty value`() {
        val muidWithEmptyValue = MarketUid("")
        assertThat(MarketUid.isEmpty(muidWithEmptyValue)).isEqualTo(true)
    }

    @Test
    fun `Method 'isEmpty' returns false for market uid with non-empty value`() {
        val muid = MarketUid("non-empty-value")
        assertThat(MarketUid.isEmpty(muid)).isEqualTo(false)
    }

    @Test
    fun `Method 'equals' returns true for market uids with same values`() {
        val muid1 = MarketUid("muid")
        val muid2 = MarketUid("muid")
        assertThat(muid1 == muid2).isEqualTo(true)
    }

    @Test
    fun `Method 'equals' returns false for market uids with different values`() {
        val muid1 = MarketUid("muid1")
        val muid2 = MarketUid("muid2")
        assertThat(muid1 == muid2).isEqualTo(false)
    }

}