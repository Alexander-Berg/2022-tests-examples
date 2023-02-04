# -*- coding: utf-8 -*-

import httplib

import mock

from billing.dcs.dcs import api, constants
from billing.dcs.dcs.utils.common import Struct

from billing.dcs.tests.utils import MNCloseBaseTestCase


class ResolveGraphTestCase(MNCloseBaseTestCase):
    def setUp(self):
        super(ResolveGraphTestCase, self).setUp()

        self._mocks = []

        self.resolve_graph_task_issue_comment = 'task closed'
        self.close_unprocessed_issue_comment = 'count: {}'
        self._mock_patch(
            'billing.dcs.dcs.api.DB_CONFIGS',
            {
                'api_resolve_graph_task_issue_comment': self.resolve_graph_task_issue_comment,
                'api_close_unprocessed_issue_comment': self.close_unprocessed_issue_comment,
            }
        )

        self._mock_patch('billing.dcs.dcs.api.Startrek')

        self.current_issue_key = 'SSD-123456'
        self.current_uid = '123456'

        self.queue_team = [self.current_uid]
        api.Startrek().queues.get.side_effect = \
            lambda *args, **kwargs: \
                Struct(teamUsers=[Struct(uid=uid) for uid in self.queue_team])

        self.current_request = {
            'issue_key': self.current_issue_key,
            'uid': self.current_uid,
        }

        self.current_issue_params = {
            'runId': '1',
            'checkName': 'bua',
            'graphTask': self.task_name
        }
        api.Startrek().get_issue().__getitem__.side_effect = \
            lambda item: self.current_issue_params[item]

        self.current_run_type = constants.RunTypes.normal
        self._mock_patch('billing.dcs.dcs.api.TrackerActions._get_run_type',
                         side_effect=lambda *args: self.current_run_type)

        self.current_run_graph_task_id = self.task_name
        self._mock_patch('billing.dcs.dcs.api.TrackerActions._get_graph_task_id_from_run',
                         side_effect=lambda *args: self.current_run_graph_task_id)

        self._mock_patch('billing.dcs.dcs.api.TrackerActions._format_user_representation',
                         side_effect=lambda user_id: str(user_id))

        self.current_unprocessed_count = 666
        self._mock_patch('billing.dcs.dcs.api.TrackerActions._do_close_unprocessed',
                         side_effect=lambda *args: self.current_unprocessed_count)

    def tearDown(self):
        self._stop_mocks()
        super(ResolveGraphTestCase, self).tearDown()

    def _mock_patch(self, *args, **kwargs):
        mock_ = mock.patch(*args, **kwargs)
        mock_.start()
        self._mocks.append(mock_)

    def _stop_mocks(self):
        for mock_ in self._mocks:
            mock_.stop()

    @staticmethod
    def _format_team(uids):
        return map(lambda uid: Struct(uid=uid), uids)

    def test_incorrect_request(self):
        incorrect_requests = [
            {},
            {'uid': '123456'},
            {'issue_key': 'SSD-123456'},
        ]

        rgt = api.TrackerActions()
        for incorrect_request in incorrect_requests:
            status_code, _ = rgt._check_request(incorrect_request)
            self.assertEqual(status_code, httplib.BAD_REQUEST)

    def test_has_no_access(self):
        self.queue_team = ['1', '2']

        status_code, _ = api.TrackerActions()._check_request(self.current_request)
        self.assertEqual(status_code, httplib.FORBIDDEN)

    def test_incorrect_run_type(self):
        self.current_run_type = constants.RunTypes.test
        status_code, _ = api.TrackerActions()._check_request(self.current_request)
        self.assertEqual(status_code, httplib.BAD_REQUEST)

    def test_invalid_graph_task_id(self):
        self.current_run_graph_task_id = None
        self.current_issue_params['graphTask'] = None
        status_code, _ = api.TrackerActions().resolve_graph_task(self.current_request)
        self.assertEqual(status_code, httplib.BAD_REQUEST)

    def test_graph_task_cannot_be_closed(self):
        status_code, _ = api.TrackerActions().resolve_graph_task(self.current_request)
        self.assertEqual(status_code, httplib.BAD_REQUEST)

    def test_graph_task_resolved(self):
        task = self.mnclose.get_task(self.task_name)
        task.open()

        status_code, _ = api.TrackerActions().resolve_graph_task(self.current_request)
        self.assertEqual(status_code, httplib.OK)
        api.Startrek().issues[self.current_issue_key].comments. \
            create.assert_called_once_with(text=self.resolve_graph_task_issue_comment)

    def test_close_unprocessed_diffs(self):
        status_code, _ = api.TrackerActions().close_unprocessed_diffs(self.current_request)
        self.assertEqual(status_code, httplib.OK)

        issue_comment = self.close_unprocessed_issue_comment. \
            format(self.current_unprocessed_count)
        api.Startrek().issues[self.current_issue_key].comments. \
            create.assert_called_once_with(text=issue_comment)

# vim:ts=4:sts=4:sw=4:tw=79:et:
