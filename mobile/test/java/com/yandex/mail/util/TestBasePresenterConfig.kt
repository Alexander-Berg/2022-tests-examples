package com.yandex.mail.util

import com.yandex.mail.ui.presenters.configs.BasePresenterConfig
import io.reactivex.schedulers.Schedulers.trampoline

class TestBasePresenterConfig {
    val config = BasePresenterConfig(trampoline(), trampoline())
}
