package com.yandex.mobile.realty.test.notes

import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.success

/**
 * @author misha-kozlov on 14.01.2021
 */
abstract class NotesTest {

    fun DispatcherRegistry.registerNoteSaving(offerId: String, note: String) {
        register(
            request {
                method("PUT")
                path("2.0/user/me/personalization/offer/$offerId/note")
                body("""{"note": "$note"}""")
            },
            success()
        )
    }

    fun DispatcherRegistry.registerNoteRemoving(offerId: String) {
        register(
            request {
                method("DELETE")
                path("2.0/user/me/personalization/offer/$offerId/note")
            },
            success()
        )
    }

    companion object {

        const val OFFER_ID = "0"
        const val UID = "1"
        const val TEXT = "some text"
        const val PRESET = "Отличный вариант"
    }
}
