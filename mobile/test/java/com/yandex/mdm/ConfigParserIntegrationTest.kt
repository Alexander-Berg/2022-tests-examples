package com.yandex.mdm

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.os.Environment
import android.view.View
import androidx.test.rule.GrantPermissionRule
import com.google.gson.Gson
import com.yandex.entity.Metadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ConfigParserIntegrationTest {
    lateinit var activity: MainActivity
    lateinit var metadata: Metadata

    @get:Rule
    val runtimePermissionRule = GrantPermissionRule.grant(ACCESS_FINE_LOCATION, READ_EXTERNAL_STORAGE)

    fun prepareJsonAndStartActivity(fileName: String) {
        val configText = javaClass.getResource(fileName)!!.readText()

        val dir = File(Environment.getExternalStorageDirectory(), "mdm")
        dir.mkdir()
        val file = File(dir, "hypercube_config.json")
        file.createNewFile()
        file.writeText(configText)

        val gson = Gson()
        metadata = gson.fromJson(configText, Metadata::class.java)

        activity = Robolectric.buildActivity(MainActivity::class.java).create().resume().get()
    }

    @Test
    fun checkTextViews() {
        prepareJsonAndStartActivity("hypercube_config.json")

        assertEquals(activity.binding.textLocation.text, metadata.location)
        assertEquals(activity.binding.textSerial.text, metadata.serialNumber)
        assertEquals(activity.title, metadata.deviceName)

        val ownerFormat = "%s, %s" // todo: read format from resources?
        val owner = metadata.owner
        assert(activity.binding.textOwnerCurrent.text == ownerFormat.format(owner.name, owner.login))
        val previousOwner = metadata.previousOwner
        assert(activity.binding.textOwnerPrevious.text == ownerFormat.format(previousOwner?.name, previousOwner?.login))

        val bookedTo = metadata.bookedTo
        if (bookedTo != null) {
            assertEquals(activity.binding.textBookedTo.text, Date(bookedTo).toString())
        } else {
            assertTrue(activity.binding.textBookedTo.text.isNullOrEmpty())
        }
    }

    @Test
    fun checkObligatoryTextViews() {
        prepareJsonAndStartActivity("hypercube_config_only_obligatory.json")

        assertTrue(activity.binding.textLocation.text.isEmpty())
        assertEquals(activity.binding.textLocation.visibility, View.GONE)
        assertEquals(activity.binding.labelLocation.visibility, View.GONE)

        assertTrue(activity.binding.textSerial.text.isEmpty())
        assertEquals(activity.binding.textSerial.visibility, View.GONE)
        assertEquals(activity.binding.labelSerial.visibility, View.GONE)

        assertTrue(activity.binding.textBookedTo.text.isEmpty())
        assertEquals(activity.binding.textBookedTo.visibility, View.GONE)
        assertEquals(activity.binding.labelBookedTo.visibility, View.GONE)

        assertTrue(activity.binding.textOwnerPrevious.text.isEmpty())
        assertEquals(activity.binding.textOwnerPrevious.visibility, View.GONE)
        assertEquals(activity.binding.labelOwnerPrevious.visibility, View.GONE)

        assertEquals(activity.binding.textOwnerCurrent.text, metadata.owner.login)
        assertEquals(activity.binding.textOwnerCurrent.visibility, View.VISIBLE)
        assertEquals(activity.binding.labelOwnerCurrent.visibility, View.VISIBLE)

        assertNull(activity.title)
    }

    @Test
    fun checkEmptyTextViews() {
        //prepareJsonAndStartActivity("hypercube_config_nulls.json")

        //todo
        //depends on action after getting an empty owner
    }
}
