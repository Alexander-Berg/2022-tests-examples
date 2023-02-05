package ru.yandex.market.processor.testinstance.adapters

data class AdapterRecord(
    val adapter: InstanceAdapter,
    val shortcuts: Iterable<AdapterShortcut>
)