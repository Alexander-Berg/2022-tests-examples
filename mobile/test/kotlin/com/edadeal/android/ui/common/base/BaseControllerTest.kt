package com.edadeal.android.ui.common.base

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import com.edadeal.android.model.navigation.RouterStackEntry
import com.edadeal.android.util.Utils
import com.nhaarman.mockito_kotlin.mock
import kotlinx.parcelize.Parcelize
import okio.ByteString
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class BaseControllerTest {

    @Test
    fun `delegated properties should return default values when they not set`() {
        val bundle = Bundle(javaClass.classLoader)
        val unitController = UnitController(bundle)

        assertEquals("", unitController.search)
        assertNull(unitController.deepLinkAdContext)
        assertEquals(false, unitController.isDeepLink)
        assertNull(unitController.clickedAd)

        assertEquals(0, unitController.int)
        assertTrue(Utils.isEquals(unitController.float, 0f))
        assertEquals(false, unitController.bool)
        assertEquals(setOf(""), unitController.stringSet)
        assertEquals(TestParcelable(), unitController.parcelable)
        assertNull(unitController.nullableParcelable)
        assertEquals(setOf(ByteString.EMPTY), unitController.byteStringSet)
        assertEquals(TestEnum.A, unitController.enum)
        assertEquals(TestSerializable(), unitController.serializable)
        assertNull(unitController.nullableEnum)
        assertNull(unitController.nullableSerializable)
        assertNull(unitController.nonParcelable)
    }

    @Test
    fun `delegated properties should return correct values and put them to bundle`() {
        val bundle = Bundle(javaClass.classLoader)
        val unitController = UnitController(bundle)
        val int = 1
        val float = 1f
        val bool = true
        val stringSet = setOf("a", "b")
        val parcelable = TestParcelable(128)
        val nullableParcelable = TestParcelable(256)
        val byteStringSet = setOf(ByteString.of(0), ByteString.of(1))
        val enum = TestEnum.B
        val nullableEnum = TestEnum.A
        val serializable = TestSerializable().apply { v = 512 }
        val nullableSerializable = TestSerializable().apply { v = 1024 }
        val nonParcelable = TestNonParcelable(2048)

        unitController.int = int
        unitController.float = float
        unitController.bool = bool
        unitController.stringSet = stringSet
        unitController.parcelable = parcelable
        unitController.nullableParcelable = nullableParcelable
        unitController.byteStringSet = byteStringSet
        unitController.enum = enum
        unitController.nullableEnum = nullableEnum
        unitController.serializable = serializable
        unitController.nullableSerializable = nullableSerializable
        unitController.nonParcelable = nonParcelable

        assertEquals(int, unitController.int)
        assertTrue(Utils.isEquals(unitController.float, float))
        assertEquals(bool, unitController.bool)
        assertEquals(stringSet, unitController.stringSet)
        assertEquals(parcelable, unitController.parcelable)
        assertEquals(nullableParcelable, unitController.nullableParcelable)
        assertEquals(byteStringSet, unitController.byteStringSet)
        assertEquals(enum, unitController.enum)
        assertEquals(nullableEnum, unitController.nullableEnum)
        assertEquals(serializable, unitController.serializable)
        assertEquals(nullableSerializable, unitController.nullableSerializable)
        assertEquals(nonParcelable, unitController.nonParcelable)
        assertEquals(12, bundle.keySet().size)
    }

    @Test
    fun `nullable delegated properties should remove values from bundle when set to null`() {
        val bundle = Bundle(javaClass.classLoader)
        val unitController = UnitController(bundle)
        val nullableParcelable = TestParcelable(128)
        val nullableEnum = TestEnum.A
        val nullableSerializable = TestSerializable().apply { v = 256 }
        val nonParcelable = TestNonParcelable(512)

        unitController.nullableParcelable = nullableParcelable
        unitController.nullableEnum = nullableEnum
        unitController.nullableSerializable = nullableSerializable
        unitController.nonParcelable = nonParcelable
        assertEquals(4, bundle.keySet().size)

        unitController.nullableParcelable = null
        unitController.nullableEnum = null
        unitController.nullableSerializable = null
        unitController.nonParcelable = null
        assertEquals(0, bundle.keySet().size)
    }

    @Test
    fun `delegated properties should be available when initializing instance`() {
        val bundle = Bundle(javaClass.classLoader)
        val firstController = TestInitController(bundle)
        firstController.search = "text"
        val secondController = TestInitController(bundle)

        assertEquals(firstController.search, secondController.searchInitValue)
    }

    @Test
    fun `delegated properties should be preserved when rebinding on new bundle`() {
        val unitController = UnitController(Bundle(javaClass.classLoader))
        val int = 128
        unitController.int = int

        assertEquals(int, unitController.int)
        unitController.rebind(Bundle(javaClass.classLoader))
        assertEquals(int, unitController.int)
    }

    class UnitController @JvmOverloads constructor(bundle: Bundle = Bundle()) : BaseController(bundle) {
        override val uiClass = ChildUi::class.java

        var int by intBundle(0)
        var float by floatBundle(0f)
        var bool by booleanBundle(false)
        var string by stringBundle("")
        var stringSet by stringSetBundle(setOf(""))
        var parcelable by parcelableBundle(TestParcelable())
        var nullableParcelable by nullableParcelableBundle<TestParcelable>()
        var byteStringSet by byteStringSetBundle(setOf(ByteString.EMPTY))
        var enum by enumBundle(TestEnum.A)
        var nullableEnum by nullableEnumBundle<TestEnum>()
        var serializable by serializableBundle(TestSerializable())
        var nullableSerializable by nullableSerializableBundle<TestSerializable>()
        var nonParcelable: TestNonParcelable? by parcelableProxyBundle(
            { p: TestParcelable -> TestNonParcelable(p.v) },
            { TestParcelable(it.v) }
        )

        override fun createUi(
            parentUi: ParentUi,
            inflater: LayoutInflater,
            stackEntry: RouterStackEntry,
            navigationResult: Any?
        ) = mock<ChildUi>()
    }

    class TestInitController @JvmOverloads constructor(bundle: Bundle = Bundle()) : BaseController(bundle) {
        override val uiClass = ChildUi::class.java
        val searchInitValue = search

        override fun createUi(
            parentUi: ParentUi,
            inflater: LayoutInflater,
            stackEntry: RouterStackEntry,
            navigationResult: Any?
        ) = mock<ChildUi>()
    }

    enum class TestEnum { A, B }

    @Parcelize
    data class TestParcelable(val v: Int = 0) : Parcelable

    class TestSerializable : Serializable {

        companion object {
            const val serialVersionUID: Long = 1L
        }

        var v = 0

        override fun equals(other: Any?) = other === this || (other is TestSerializable && v == other.v)
        override fun hashCode() = v.hashCode()
    }

    data class TestNonParcelable(val v: Int = 0)
}
