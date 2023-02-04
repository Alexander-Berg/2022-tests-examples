# coding: utf-8

from settings_common import *  # noqa

# Используется в письмах мониторингов
STARTREK_UI_URL = 'https://st.test.yandex-team.ru'
YT_ENV_SUBDIRECTORY = 'test'

FAKE_SERVICE_URL = 'http://greed-dev3h.paysys.yandex.net:8666/'
STATIC_FILES_URL = FAKE_SERVICE_URL + 'static/'

mailer = {
    'SenderName': 'Яндекс.Баланс',
    'Sender': 'info@balance.yandex.ru',
    'RecipientName': 'Balance DCS Test',
    'Recipient': ['balance-dcs-test@yandex-team.ru', ],
    'Encoding': 'utf-8',
    'SendMail': ' /usr/sbin/sendmail',
    'HostType': 'test'
}
