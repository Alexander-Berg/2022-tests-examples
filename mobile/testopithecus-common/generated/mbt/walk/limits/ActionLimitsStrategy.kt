// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/limits/action-limits-strategy.ts >>>

package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*

public interface ActionLimitsStrategy {
    fun check(actions: Stack<MBTAction>): Boolean
}

