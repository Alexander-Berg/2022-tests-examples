# -*- coding: utf-8 -*-
import unittest.mock
from unittest.mock import (
    patch,
    ANY,
)
from hamcrest import (
    calling,
    assert_that,
    raises,
    is_not,
)
from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.core.sms.utils import send_sms_to_admins
from intranet.yandex_directory.src.yandex_directory.core.sms.tasks import (
    SendSmsTask,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.exceptions import DuplicatedTask


class TestSendSmsToAdmins(TestCase):
    def test_send_sms_to_admins(self):
        # отправляем смс всем админам организации
        # внешним и внутренним

        sms_template = 'text {tld}'
        tld = 'en'
        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.sms.utils.lang_for_notification', return_value='ru'), \
             patch('intranet.yandex_directory.src.yandex_directory.core.sms.utils._get_sms_template', return_value=sms_template), \
             patch.object(SendSmsTask, 'place_into_the_queue') as place_into_the_queue:

            send_sms_to_admins(
                self.meta_connection,
                self.main_connection,
                org_id,
                'organization_domain',
                tld=tld,
            )

            expected_text = sms_template.format(tld=tld)
            place_into_the_queue.assert_has_calls(
                [
                    unittest.mock.call(
                        ANY,
                        None,
                        text=expected_text,
                        uid=self.admin_uid,
                        org_id=org_id,
                    ),
                    unittest.mock.call(
                        ANY,
                        None,
                        text=expected_text,
                        uid=self.outer_admin['id'],
                        org_id=org_id,
                    )
                ],
                any_order=True,
            )

    def test_duplicate_sms_to_admins(self):
        # проверяем что не выбрасывается исключение при повторяющемся task

        sms_template = 'text {tld}'
        tld = 'en'
        with patch('intranet.yandex_directory.src.yandex_directory.core.sms.utils.lang_for_notification', return_value='ru'), \
             patch('intranet.yandex_directory.src.yandex_directory.core.sms.utils._get_sms_template', return_value=sms_template), \
             patch.object(SendSmsTask, 'delay') as delay:
            delay.side_effect = DuplicatedTask

            assert_that(
                calling(send_sms_to_admins).with_args(
                    self.meta_connection,
                    self.main_connection,
                    self.organization['id'],
                    'organization_domain',
                    tld=tld
                ),
                is_not(raises(DuplicatedTask))
            )
