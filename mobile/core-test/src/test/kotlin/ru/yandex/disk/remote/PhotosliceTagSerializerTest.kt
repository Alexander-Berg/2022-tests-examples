@file:Suppress("IllegalIdentifier")

package ru.yandex.disk.remote

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

private const val ID = "test_id"
private const val REVISION = "test_revision"
private const val EXPECTED_TAG = "$ID:$REVISION"

class PhotosliceTagSerializerTest {

    @Test
    fun `should serialize tag right`() {
        val tag = PhotosliceTag(ID, REVISION)
        val serialized = PhotosliceTag.Serializer.serialise(tag)
        assertThat(serialized, equalTo(EXPECTED_TAG))
    }


    @Test
    fun `should deserialize tag right`() {
        val deserialised = PhotosliceTag.Serializer.deserialise(EXPECTED_TAG)

        assertThat(deserialised.id, equalTo(ID))
        assertThat(deserialised.revision, equalTo(REVISION))
    }
}