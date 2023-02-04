package ru.auto.ara.ui.auth.controller

import android.net.Uri
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.RobolectricTest
import ru.auto.test.runner.AllureRobolectricRunner
import java.net.URL

/**
 * @author aleien on 29.09.17.
 */
@RunWith(AllureRobolectricRunner::class) class MailRuUriParserTest : RobolectricTest() {

    private val accessToken = "b6442ed12223a7d0b459916b8ea03ce5"
    private val exampleAuthSuccess = "http://connect.mail.ru/oauth/success.html#" +
            "refresh_token=$accessToken&" +
            "access_token=b6442ed12223a7d0b459916b8ea03ce5&" +
            "token_type=bearer"
    private val exampleAuthFailure = "http://connect.mail.ru/oauth/failure"

    private val successUri = parseUrl(exampleAuthSuccess)
    private val failureUri = parseUrl(exampleAuthFailure)

    private val tested = MailRuUriParser()

    private fun parseUrl(string: String): Uri {
        val url = URL(string)
        return Uri.parse(url.toURI().toString())
    }

    @Test
    fun `when url contains access_token return it`() {
        assertThat(tested.getAccessToken(successUri)).isEqualToIgnoringCase(accessToken)
    }

    @Test
    fun `when url contains no access_token return null`() {
        assertThat(tested.getAccessToken(failureUri)).isNull()
    }


}
