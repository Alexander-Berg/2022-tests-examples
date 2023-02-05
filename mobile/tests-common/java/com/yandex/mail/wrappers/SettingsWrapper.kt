package com.yandex.mail.wrappers

import com.yandex.mail.LoginData
import com.yandex.mail.network.response.SettingsJson
import com.yandex.mail.settings.MailSettings
import com.yandex.mail.tools.MockServerTools

data class SettingsWrapper(
    val login: String,
    val name: String,
    var signature: String, // todo: make immutable?
    val uid: String,
    val addresses: Array<SettingsJson.Emails.Address>,
    val threaded: Boolean
) { // todo: check nullability

    private fun generateAccountInformation(): SettingsJson.AccountInformation {
        val emails = SettingsJson.Emails(ArrayList(addresses.toList()))
        return SettingsJson.AccountInformation(COMPOSE_CHECK_STRING, emails, "suid_$login", uid, "regCountry")
    }

    private fun generateAccountInformationWrapper() =
        SettingsJson.AccountInformationWrapper(generateAccountInformation())

    private fun generateSettingsSetup(): SettingsJson.SettingsSetup {
        val settings = SettingsJson.Body(
            "colorScheme",
            addresses[0].asAddress(),
            this.name,
            this.threaded,
            this.signature,
            false,
            MailSettings.SignaturePlace.AFTER_REPLY,
            SettingsJson.Body.ReplyTo(
                emptyList()
            )
        )
        return SettingsJson.SettingsSetup(settings)
    }

    private fun generateUserParameters(areTabsEnabled: Boolean): SettingsJson.UserParametersWrapper {
        val wr = SettingsJson.UserParametersWrapper(SettingsJson.UserParametersWrapper.UserParameters(null, showFolderTabs = areTabsEnabled))
        return wr
    }

    fun generateSettingsResponse(areTabsEnabled: Boolean) = SettingsJson(
        MockServerTools.createOkStatus(),
        generateAccountInformationWrapper(),
        generateSettingsSetup(),
        generateUserParameters(areTabsEnabled)
    )

    class SettingsWrapperBuilder internal constructor() { // todo: not sure about `internal constructor()`

        private var login: String? = null

        private var name: String? = null

        private var signature: String? = null

        private var uid: String? = null

        private var addresses: Array<SettingsJson.Emails.Address>? = null

        private var threaded: Boolean = false

        fun login(login: String) = apply {
            this.login = login
        }

        fun name(name: String) = apply {
            this.name = name
        }

        fun signature(signature: String) = apply {
            this.signature = signature
        }

        fun uid(uid: String) = apply {
            this.uid = uid
        }

        fun addresses(addresses: Array<SettingsJson.Emails.Address>) = apply {
            this.addresses = addresses
        }

        fun threaded(threaded: Boolean) = apply {
            this.threaded = threaded
        }

        fun build(): SettingsWrapper {
            return SettingsWrapper(login!!, name!!, signature!!, uid!!, addresses!!, threaded)
        }
    }

    companion object {

        private val COMPOSE_CHECK_STRING = "ce1cbc5f81f8e6e1a5bc2c225f84d8f0"

        private val DEFAULT_DOMAINS = listOf("ya.ru", "yandex.ru", "yandex.ua")

        @JvmStatic
        fun defaultSettings(loginData: LoginData): SettingsWrapper.SettingsWrapperBuilder {
            val login = loginData.name
            val addresses = DEFAULT_DOMAINS.map { domain -> makeAddress(login, domain) }
            return builder()
                .login(login)
                .name("Name of " + login)
                .signature("Signature of " + login)
                .uid(loginData.uid.toString()) // TODO string uid
                .addresses(addresses.toTypedArray())
                .threaded(true) // TODO maybe, off?
        }

        @JvmStatic
        fun makeAddress(login: String, domain: String): SettingsJson.Emails.Address {
            val split = login.split("@")
            val address = if (split.size == 2) {
                SettingsJson.Emails.Address(split[0], split[1])
            } else {
                SettingsJson.Emails.Address(login, domain)
            }
            return address
        }

        fun builder(): SettingsWrapperBuilder {
            return SettingsWrapperBuilder()
        }
    }
}
