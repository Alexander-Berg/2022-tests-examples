package com.edadeal.android.data

import com.edadeal.android.data.files.FileStorage
import com.edadeal.android.util.IOUtils
import okio.ByteString
import okio.source
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull

class FileStorageTest {
    private val rootDir = TemporaryFolder().apply { create() }
    private val fileStorage = FileStorage(rootDir.newFolder(), rootDir.newFolder())

    @Test
    fun testOkioSource() {
        assertNotNull(rootDir.newFile().source())
        assertFails { File(rootDir.root, "nope").source() }
    }

    @Test
    fun `loadExperimentsJson() should return content, that was saved by saveExperiments()`() {
        val json = "{\"a\":\"b\"}"
        fileStorage.saveExperiments(json)
        assertEquals(json, fileStorage.loadExperimentsJson())
    }

    @Test
    fun testLoadSaveCart() {
        val cartItems = listOf(1L, 2L, 3L).map { CartItemDto(id = ByteString.of(it.toByte())) }
        IOUtils.writeObjectToJsonFile(fileStorage.fileCart, cartItems)
        assertEquals(cartItems.map { it.id }, fileStorage.loadCart().map { it.id })
    }
}
