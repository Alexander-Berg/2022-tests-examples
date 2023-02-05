package ru.yandex.market.processor.testinstance.adapters

import javax.lang.model.type.TypeMirror

interface InstanceAdaptersRegistry {

    fun getProvider(type: TypeMirror): InstanceAdapter?
}