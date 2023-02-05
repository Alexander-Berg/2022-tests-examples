package ru.yandex.market.test.util

import ru.yandex.market.base.network.fapi.contract.AbstractFapiContract

class NetworkCallInAndroidTestException(vararg contracts: AbstractFapiContract<*>) :
    Throwable("Found calls in ui test: ${contracts.joinToString { it.resolverName }}")