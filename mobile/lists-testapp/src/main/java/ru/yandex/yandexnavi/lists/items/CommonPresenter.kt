package ru.yandex.yandexnavi.lists.items

import ru.yandex.yandexnavi.lists.api.ItemPresenter
import ru.yandex.yandexnavi.lists.items.analytics.LifecycleRegister

open class CommonPresenter(
    private val lifecycleRegister: LifecycleRegister
) : ItemPresenter() {
    val presenterId: Int = lifecycleRegister.createId()
    private var isAttached = false
    private var attachedCounter = 0
    var bindCounter = 0

    open fun onAttach() {
        require(!isAttached)
        attachedCounter = maxOf(0, attachedCounter + 1)
        isAttached = true
        lifecycleRegister.registerAttach()
    }

    open fun onDetach() {
        require(isAttached)
        isAttached = false
        lifecycleRegister.registerDetach()
    }

    open fun bind() {
        bindCounter = maxOf(0, bindCounter + 1)
        lifecycleRegister.registerBind()
    }
}
