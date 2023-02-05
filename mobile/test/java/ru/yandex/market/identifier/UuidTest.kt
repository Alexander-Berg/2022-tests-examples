package ru.yandex.market.identifier

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.domain.auth.model.Uuid

class UuidTest {

    @Test
    fun `Method 'isStub' returns true for stub`() {
        val stubUuid = Uuid.stub()
        assertThat(stubUuid.isStub()).isTrue
    }

    @Test
    fun `Method 'isStub' returns false for non-stub`() {
        val nonStubUuid = Uuid("")
        assertThat(nonStubUuid.isStub()).isFalse
    }

    @Test
    fun `Method 'isEmpty' returns true for null uuid reference`() {
        assertThat(Uuid.isEmpty(null)).isTrue
    }

    @Test
    fun `Method 'isEmpty' returns true for uuid with empty value`() {
        val uuidWithEmptyValue = Uuid("")
        assertThat(Uuid.isEmpty(uuidWithEmptyValue)).isTrue
    }

    @Test
    fun `Method 'isEmpty' returns false for uuid with non-empty value`() {
        val uuid = Uuid("non-empty-value")
        assertThat(Uuid.isEmpty(uuid)).isFalse
    }

    @Test
    fun `Method 'equals' returns true for uuids with same values`() {
        val uuid1 = Uuid("uuid")
        val uuid2 = Uuid("uuid")
        assertThat(uuid1 == uuid2).isTrue
    }

    @Test
    fun `Method 'equals' returns false for uuids with different values`() {
        val uuid1 = Uuid("uuid1")
        val uuid2 = Uuid("uuid2")
        assertThat(uuid1 == uuid2).isFalse
    }
}