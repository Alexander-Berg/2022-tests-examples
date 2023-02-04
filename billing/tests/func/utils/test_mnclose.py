# -*- coding: utf-8 -*-
import logging

import mock

from billing.dcs.dcs.utils.common import Struct
from billing.dcs.dcs.utils.mnclose import MNCloseTaskActions, MNCloseService, \
    NirvanaMnCloseSyncException

from billing.dcs.tests.utils import BaseTestCase, MNCloseBaseTestCase

log = logging.getLogger(__name__)


@mock.patch('billing.dcs.dcs.utils.mnclose.XML_CONFIG')
@mock.patch('billing.dcs.dcs.temporary.reports_utils.retrying.time.sleep')
@mock.patch('billing.dcs.dcs.temporary.reports_utils.mnclose.xmlrpc_client.ServerProxy')
class MNCloseServiceUnitTestCase(BaseTestCase):
    # noinspection PyUnusedLocal
    def test_get_task_retrying(self, server_proxy_mock, sleep_mock,
                               xml_config_mock):
        task_name = 'name'
        server_proxy_instance_mock = mock.Mock()
        total_expected_attempts = 3
        attempt = [0]  # container for attempt number

        # noinspection PyUnusedLocal
        def get_status(task_name, inst_dt):
            attempt[0] += 1
            if attempt[0] < total_expected_attempts:
                return {'error': 'some error'}
            else:
                return {'status': 'new_unopenable'}

        server_proxy_instance_mock.NirvanaMnCloseTasks.get_status = get_status
        server_proxy_mock.return_value = server_proxy_instance_mock

        task = MNCloseService().get_task(task_name)
        self.assertEqual(attempt[0], total_expected_attempts)
        self.assertEqual(task.name, task_name)

    # noinspection PyUnusedLocal
    def test_get_task_retrying_attempts_exceeded(self, server_proxy_mock,
                                                 sleep_mock, xml_config_mock):
        server_proxy_instance_mock = mock.Mock()
        server_proxy_instance_mock.tasks.monthindex = \
            lambda *args, **kwargs: {'error': 'some error'}
        server_proxy_mock.return_value = server_proxy_instance_mock

        with self.assertRaises(NirvanaMnCloseSyncException):
            MNCloseService().get_task('task name')


class MNCloseServiceTestCase(MNCloseBaseTestCase):
    def do_action(self, action, mnclose=None):
        mnclose = mnclose or self.mnclose
        mnclose.action(
            Struct({'instantiation_date': self.month,
                    'name': self.task_name}),
            action
        )

    def test_multiple_actions_in_a_row(self):
        """
        Используем один и тот же mnclose-сервис подряд
        """
        actions = [
            MNCloseTaskActions.open,
            MNCloseTaskActions.stall,
            MNCloseTaskActions.open,
            MNCloseTaskActions.resolve,
        ]

        mnclose = self.mnclose
        for action in actions:
            self.do_action(mnclose=mnclose, action=action)

# vim:ts=4:sts=4:sw=4:tw=79:et:
