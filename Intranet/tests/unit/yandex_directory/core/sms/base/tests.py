# coding: utf-8

from hamcrest import (
    assert_that,
    equal_to,
    calling,
    raises
)
from unittest.mock import (
    patch,
    Mock,
)

from testutils import (
    TestCase,
    override_settings,
)

from intranet.yandex_directory.src.yandex_directory.core.sms.exceptions import SmsError
from intranet.yandex_directory.src.yandex_directory.core.sms.sms import send_sms


class TestSendSms(TestCase):

    @override_settings(YASMS_BASE_URL='http://some-send-sms-usl.yandex.ru', SMS_SEND_FUNCTION='intranet.yandex_directory.src.yandex_directory.core.sms.sms.send')
    def test_raise_exc_on_error(self):
        # в случае ответа с кодом ошибки бросаем исключение

        uid = 123
        text = 'test sms тест СМС'
        with patch('intranet.yandex_directory.src.yandex_directory.app.requests') as mock_requests_session:
            response = Mock()
            response.text = """
                <?xml version="1.0" encoding="windows-1251"?>
                <doc>
                    <error>User does not have an active phone to recieve messages</error>
                    <errorcode>NOCURRENT</errorcode>
                </doc>
            """.strip()
            response.status_code = 200
            mock_requests_session.post.return_value = response

            assert_that(
                calling(send_sms).with_args(uid, text),
                raises(SmsError)
            )

    @override_settings(YASMS_BASE_URL='http://some-send-sms-usl.yandex.ru', SMS_SEND_FUNCTION='intranet.yandex_directory.src.yandex_directory.core.sms.sms.send')
    def test_return_smsid(self):
        # в случае нормального ответа вернем идентификатор смс сообщения

        uid = 123
        text = 'test sms тест СМС'
        with patch('intranet.yandex_directory.src.yandex_directory.app.requests') as mock_requests_session:
            response = Mock()
            response.text = """
                    <?xml version="1.0" encoding="windows-1251"?>
                    <doc>
                        <message-sent id="12345" />
                    </doc>
                """.strip()
            response.status_code = 200
            mock_requests_session.post.return_value = response

            assert_that(
                send_sms(uid, text),
                equal_to('12345')
            )
