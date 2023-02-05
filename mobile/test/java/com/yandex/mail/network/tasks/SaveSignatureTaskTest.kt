package com.yandex.mail.network.tasks

import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.settings.MailSettings
import com.yandex.mail.tools.Accounts
import com.yandex.mail.util.AccountNotInDBException
import com.yandex.mail.util.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(IntegrationTestRunner::class)
class SaveSignatureTaskTest : BaseIntegrationTest() {

    @Before
    @Throws(Exception::class)
    fun setUp() {
        init(Accounts.testLoginData)
    }

    @Test
    @Throws(AccountNotInDBException::class)
    fun `on success should set_useDefault_to_false`() {
        accountSettings.edit().setSignature(app.getString(MailSettings.DEFAULT_SIGNATURE_RES_ID)).setUseDefaultSignature(true).apply()
        val test_signature = "test signature"
        val saveSignatureTask = SaveSignatureTask(app, test_signature, user.uid)
        saveSignatureTask.onSuccess(app)
        assertThat(accountSettings.signature()).isEqualTo(test_signature)
        assertThat(accountSettings.useDefaultSignature()).isFalse()
    }
}
