package ru.yandex.yandexnavi.lists.items

import ru.yandex.yandexnavi.lists.api.BaseListPresenter
import ru.yandex.yandexnavi.lists.api.ItemPresenter

class SimpleListPresenter(
    private val items: List<ItemPresenter>
) : BaseListPresenter() {
    override fun items(): List<ItemPresenter> = items
}
