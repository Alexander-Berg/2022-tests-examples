package com.yandex.mail.shadows

import androidx.fragment.app.FragmentTransaction
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject

/**
 * Remove this hack when https://github.com/robolectric/robolectric/issues/3852 is fixed
 */
@Implements(className = "androidx.core.app.BackStackRecord")
class ShadowBackStackRecord {

    @RealObject
    private val realTransaction: FragmentTransaction? = null

    @Implementation
    fun setTransition(transition: Int): FragmentTransaction? {
        return realTransaction
    }

    @Implementation
    fun setCustomAnimations(enter: Int, exit: Int, popEnter: Int, popExit: Int): FragmentTransaction? {
        return realTransaction
    }
}
