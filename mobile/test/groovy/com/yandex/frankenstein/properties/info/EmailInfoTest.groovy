package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class EmailInfoTest {

    final String host = "/some/email/smtp/host"
    final String port = "123"
    final String user = "app_metrica_user"
    final String password = "app_metrica_password"
    final String from = "from@email.ya"
    final String to = "to@email.ya"

    @Test
    void testExtractInfo() {
        final EmailInfo emailInfo = new EmailInfo(host, port, user, password, from, to)
        assertThat(emailInfo.smtpHost).isEqualTo(host)
        assertThat(emailInfo.port.toString()).isEqualTo(port)
        assertThat(emailInfo.user).isEqualTo(user)
        assertThat(emailInfo.password).isEqualTo(password)
        assertThat(emailInfo.from).isEqualTo(from)
        assertThat(emailInfo.to).isEqualTo(to)
    }

    @Test
    void testExtractInfoWithoutHost() {
        final EmailInfo emailInfo = new EmailInfo("", port, user, password, from, to)
        assertThat(emailInfo.smtpHost).isEqualTo("smtp.yandex-team.ru")
        assertThat(emailInfo.port.toString()).isEqualTo(port)
        assertThat(emailInfo.user).isEqualTo(user)
        assertThat(emailInfo.password).isEqualTo(password)
        assertThat(emailInfo.from).isEqualTo(from)
        assertThat(emailInfo.to).isEqualTo(to)
    }

    @Test
    void testToString() {
        final EmailInfo info = new EmailInfo(host, port, user, password, from, to)
        final String expected = """
EmailInfo {
    smtpHost='$host',
    port='$port',
    user='$user',
    password='$password',
    from='$from',
    to='$to'
}
"""
        assertThat(info.toString()).isEqualTo(expected)
    }
}
