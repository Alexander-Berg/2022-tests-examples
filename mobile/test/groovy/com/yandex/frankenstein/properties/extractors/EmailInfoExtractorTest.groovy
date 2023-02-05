package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.EmailInfo
import org.gradle.api.Project
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class EmailInfoExtractorTest {

    final String host = "some_email_smtp_host"
    final int port = 123
    final String user = "app_metrica_user"
    final String password = "app_metrica_password"
    final String from = "from@email.ya"
    final String to = "to@email.ya"

    final EmailInfoExtractor mEmailInfoExtractor = new EmailInfoExtractor()

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                        "email.smtp.host": host,
                        "email.smtp.port": port,
                        "email.user": user,
                        "email.password": password,
                        "email.from": from,
                        "email.to": to
                ]}
        ] as Project
        final EmailInfo emailInfo = mEmailInfoExtractor.extractInfo(project)
        assertThat(emailInfo.smtpHost).isEqualTo(host)
        assertThat(emailInfo.port).isEqualTo(port)
        assertThat(emailInfo.user).isEqualTo(user)
        assertThat(emailInfo.password).isEqualTo(password)
        assertThat(emailInfo.from).isEqualTo(from)
        assertThat(emailInfo.to).isEqualTo(to)
    }
}
