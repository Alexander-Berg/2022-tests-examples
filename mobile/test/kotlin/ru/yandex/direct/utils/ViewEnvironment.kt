// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.content.res.Resources
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import io.reactivex.schedulers.TestScheduler
import ru.yandex.direct.newui.base.AdapterProvider
import ru.yandex.direct.newui.base.BaseAdapter
import ru.yandex.direct.newui.base.BaseView

open class ViewEnvironment {
    val resources = mock<Resources>()
    val context = mock<Context> {
        on { resources } doReturn resources
    }
    val lifecycle = mock<Lifecycle>()
    val scheduler = TestScheduler()

    fun <T> T.stubViewMethods(): T
        where T : BaseView {
        stub {
            on { context } doReturn this@ViewEnvironment.context
            on { resources } doReturn this@ViewEnvironment.resources
            if (it is LifecycleOwner) {
                on { it.lifecycle } doReturn this@ViewEnvironment.lifecycle
            }
        }
        return this
    }

    fun <T, R> T.stubAdapterViewMethods(baseAdapter: BaseAdapter<R>): T
        where T : BaseView,
              T : AdapterProvider<R> {
        stubViewMethods().stub {
            on { adapter } doReturn baseAdapter
        }
        return this
    }
}