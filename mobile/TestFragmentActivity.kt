package com.yandex.mail.tools

import android.os.Bundle
import android.widget.LinearLayout
import androidx.annotation.IdRes
import com.yandex.mail.ClearTitleCallback
import com.yandex.mail.R
import com.yandex.mail.fragment.ActionBarActivityWithFragments

open class TestFragmentActivity : ActionBarActivityWithFragments(), ClearTitleCallback {

    val neverAskAgainPermissions = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LinearLayout(this)
        view.id = ROBOLECTRIC_DEFAULT_FRAGMENT_ID
        setContentView(view)
    }

    override fun getLightThemeRes() = R.style.YaTheme_MailList_Light

    override fun getDarkThemeRes() = R.style.YaTheme_MailList_Dark

    override fun shouldShowRequestPermissionRationale(permission: String) = !neverAskAgainPermissions.contains(permission)

    companion object {

        @JvmField
        @IdRes
        val ROBOLECTRIC_DEFAULT_FRAGMENT_ID = 1
    }

    override fun clearTitle() {
    }
}
