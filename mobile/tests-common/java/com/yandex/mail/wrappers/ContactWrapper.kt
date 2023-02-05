package com.yandex.mail.wrappers

import com.yandex.mail.network.response.AbookJson
import com.yandex.mail.network.response.AbookSuggestJson.SuggestContact

data class ContactWrapper(
    val cid: String,
    val email: String,
    val first: String,
    val last: String
) {

    fun generateContact() = AbookJson.Contact(
        AbookJson.Contact.ContactName("full", last, "middle", first), cid, email, 0L, 0L
    )

    fun generateSuggestContact() = SuggestContact("cid", email, "$first $last")

    class ContactWrapperBuilder internal constructor() {

        private var cid: String? = null

        private var email: String? = null

        private var first: String? = null

        private var last: String? = null

        fun cid(cid: String) = apply { this.cid = cid }

        fun email(email: String) = apply { this.email = email }

        fun first(first: String) = apply { this.first = first }

        fun last(last: String) = apply { this.last = last }

        fun build(): ContactWrapper {
            return ContactWrapper(cid!!, email!!, first!!, last!!)
        }
    }

    companion object {

        @JvmStatic
        fun builder(): ContactWrapperBuilder {
            return ContactWrapperBuilder()
        }
    }
}
