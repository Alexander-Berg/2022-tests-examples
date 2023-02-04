# coding: utf8

import mock
import pytest

from startrek_client.exceptions import BadRequest

from billing.dcs.dcs.core import util


class TestIdListComment(object):
    @pytest.fixture
    def time_sleep_mock(self):
        with mock.patch('time.sleep') as m:
            yield m

    @pytest.fixture(autouse=True)
    def db_configs_mock(self):
        with mock.patch.object(util, 'DB_CONFIGS') as m:
            yield m

    @property
    def response_mock(self):
        return mock.MagicMock()

    @property
    def report_issue_mock(self):
        issue_mock = mock.MagicMock()
        return issue_mock

    def test_retry(self, time_sleep_mock):
        report_issue = self.report_issue_mock
        report_issue.comments.create.side_effect = BadRequest(self.response_mock)

        with pytest.raises(BadRequest):
            util.id_list_comment(
                report_issue,
                [[1], [2], [3]],
                u'Описание',
            )

        assert 10 == report_issue.comments.create.call_count
