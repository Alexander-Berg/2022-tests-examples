package com.yandex.launcher.statistics

import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(StoryManager::class)
class StoryManagerShadow {

    val events = ArrayList<EventShadow>()

    fun __constructor__() { /* Required by shadow's contract */ }

    @Implementation
    protected fun processEvent(type: Int, param0: Int, param1: Any?) {
        events.add(0, EventShadow(type, param0, param1))
    }

    class EventShadow(val type: Int? = null, val param0: Int? = null, val param1: Any? = null)
}