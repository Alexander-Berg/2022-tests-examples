package ru.yandex.yandexbus.experiments

import ru.yandex.yandexbus.inhouse.common.session.SessionInfoStorage

// copy-pasted from :common
class TestSessionInfoStorage : SessionInfoStorage {

    private var number: Long? = null

    override fun readAppSessionNumber() = number

    override fun writeAppSessionNumber(number: Long) {
        this.number = number
    }
}
