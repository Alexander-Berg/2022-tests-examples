import csv
import unittest
import datetime
from io import StringIO
from unittest import mock
from unittest.mock import MagicMock, patch

from agency_rewards.rewards.utils.const import Email
from agency_rewards.rewards.common.notifications import send_stop_msg, send_email


class TestNotifications(unittest.TestCase):
    @patch(
        'agency_rewards.rewards.common.notifications.get_config_item',
        return_value='production',
    )
    @patch('agency_rewards.rewards.common.notifications.Config')
    @patch('agency_rewards.rewards.common.notifications.smtplib.SMTP')
    def test_send_stop_msg(self, smtp_mock, config_mock, get_config_item_mock):
        """
        Проверяем, что в сообщение об окончании расчета прикрепился логфайл
        """
        cfg = MagicMock()
        insert_dt = datetime.datetime.now()
        start_time = insert_dt + datetime.timedelta(minutes=5)
        finish_time = insert_dt + datetime.timedelta(minutes=10)

        config_mock.log_file_path = './test.log'
        open_mock = mock.mock_open(read_data=bytes('INFO: logmessage', encoding='utf-8'))

        with mock.patch('builtins.open', open_mock):
            send_stop_msg(cfg, insert_dt, start_time, finish_time, recipients=Email.Dev.value)

        msg = smtp_mock.mock_calls[2].kwargs['msg']
        self.assertIn('Content-Disposition: attachment; filename="test.log"', msg, msg)

    @patch('agency_rewards.rewards.common.notifications.smtplib.SMTP')
    def test_charset(self, smtp_mock):
        """
        Проверяем, что send_email не падает, если в csv_body передать нестандартные символы.
        С кодировкой windows-1251 в send_email этот тест падает.
        """
        data = [{'column1': "ÖÖÖ", 'column2': 'ÄÅÖ½'}]
        with StringIO() as f:
            writer = csv.DictWriter(f, fieldnames=['column1', 'column2'], delimiter=';', quoting=csv.QUOTE_ALL)
            writer.writeheader()
            for row in data:
                writer.writerow(row)
            csv_body = f.getvalue()
        send_email(
            recipients="test@test.test", subject="Hello", body="Testing charset", filename="test.csv", csv_body=csv_body
        )
