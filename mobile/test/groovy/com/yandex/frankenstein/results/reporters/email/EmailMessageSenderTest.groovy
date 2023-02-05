package com.yandex.frankenstein.results.reporters.email

import com.yandex.frankenstein.properties.info.EmailInfo
import org.gradle.api.logging.Logger
import org.junit.Test

import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

import static org.assertj.core.api.Assertions.assertThat

class EmailMessageSenderTest {

    final Logger dummyLogger = [
            info: {}
    ] as Logger

    final EmailInfo emailInfo = new EmailInfo("smtpHost", "1234", "user", "password", "from", "to")

    final EmailMessageSender emailMessageSender = new EmailMessageSender(dummyLogger, emailInfo)

    @Test
    void testGetEmailProperties() {
        final Properties properties = emailMessageSender.getEmailProperties()
        final Properties expected = new Properties()
        expected.putAll([
                "mail.smtp.user": emailInfo.user,
                "mail.smtp.host": emailInfo.smtpHost,
                "mail.smtp.port": emailInfo.port,
                "mail.smtp.socketFactory.port": emailInfo.port,
                "mail.smtp.starttls.enable": "true",
                "mail.smtp.auth": "true",
                "mail.smtp.socketFactory.class": "javax.net.ssl.SSLSocketFactory",
                "mail.smtp.socketFactory.fallback": "false",
        ])

        assertThat(properties).isEqualTo(expected)
    }

    @Test
    void testCreateMimeMessage() {
        final MimeMultipart mimeMultipart = new MimeMultipart()
        final String subject = "subject"
        final MimeMessage message = emailMessageSender.createMimeMessage(mimeMultipart, subject)

        assertThat(message.getContent()).isSameAs(mimeMultipart)
        assertThat(message.subject).isEqualTo(subject)
        assertThat(message.from.toList()).containsOnly(new InternetAddress(emailInfo.from))
        assertThat(message.allRecipients.toList()).containsOnly(new InternetAddress(emailInfo.to))
    }
}
