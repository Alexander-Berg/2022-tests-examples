package ru.yandex.market.util

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.base.presentation.core.utils.ArgumentProperty
import ru.yandex.market.base.presentation.core.utils.booleanArg
import ru.yandex.market.base.presentation.core.utils.optionalParcelableArg

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class FragmentExtensionsTest {

    @Test
    fun `Optional parcelable argument properly returns null`() {
        val fragment = TestFragment()

        assertThat(fragment.optionalParcelableArg).isNull()
    }

    @Test
    fun `Optional parcelable argument properly returns present value`() {
        val uri = Uri.parse("123")
        val fragment = TestFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_PARCELABLE, uri)
            }
        }

        assertThat(fragment.optionalParcelableArg).isEqualTo(uri)
    }

    @Test
    fun `Boolean argument returns passed value`() {
        val fragment = TestFragment().apply {
            arguments = Bundle().apply {
                putBoolean(KEY_BOOLEAN, true)
            }
        }

        assertThat(fragment.booleanArg).isEqualTo(true)
    }

    @Suppress("UNUSED_VARIABLE")
    @Test
    fun `Argument property calls getter only once and then use cached value`() {

        class TestPropertyGetter : (Fragment) -> Boolean {

            var numberOfInvocations = 0

            override fun invoke(p1: Fragment): Boolean {
                numberOfInvocations++
                return true
            }
        }

        val getter = TestPropertyGetter()

        class ArgumentTestFragment : Fragment() {
            val property by ArgumentProperty(getter)
        }

        val fragment = ArgumentTestFragment()

        val first = fragment.property
        val second = fragment.property

        assertThat(getter.numberOfInvocations).isEqualTo(1)
    }
}

private const val KEY_BOOLEAN = "Boolean"
private const val KEY_PARCELABLE = "Parcelable"

private class TestFragment : Fragment() {
    val booleanArg by booleanArg(KEY_BOOLEAN)
    val optionalParcelableArg by optionalParcelableArg<Uri>(KEY_PARCELABLE)
}