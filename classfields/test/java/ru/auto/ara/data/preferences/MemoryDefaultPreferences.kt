package ru.auto.ara.data.preferences


class MemoryDefaultPreferences : IDefaultPreferences {

    private var frontlog: Boolean = false
    private var sessionTimestamp: Long = 0L

    override fun setFrontlogImmediate(isEnabled: Boolean) {
        frontlog = isEnabled
    }

    override fun isFrontlogImmediate(): Boolean = frontlog

    override fun setSessionTimestamp(sessionTimestamp: Long) {
        this.sessionTimestamp = sessionTimestamp
    }

    override fun getSessionTimestamp(): Long = sessionTimestamp

}
