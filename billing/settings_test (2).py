# -*- coding: utf-8 -*-

import statface_client

app_cfg_path = '/etc/yandex/balance-reports/reports-test.cfg.xml'

#mail sender
mailer = {
    "SenderName"    :   "Яндекс.Баланс",
    "Sender"        :   "info-noreply@support.yandex.com",
    "Encoding"      :   "utf-8",
    "SendMail"      :   " /usr/sbin/sendmail",
}

mnclose = {
    'url': 'http://robot:xxx_robot_666@balance-:5002/be/',
    'user': 'robot',
}

stat = {
    'user': 'robot_lightrevan',
    'host': statface_client.STATFACE_BETA
}

report = {
    'mode': 'test',
}

dev_emails = ['balance-support-dev@yandex-team.ru']
bcc_emails = []
