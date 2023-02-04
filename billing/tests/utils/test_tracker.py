from unittest import mock

import pytest

from . import project_utils as pu


class TestTracker:
    @pytest.mark.parametrize(
        'url, expected_url, expected_version',
        (
            ('', 'https://st-api.yandex-team.ru', 'v2'),
            (pu.Tracker.Environment.PRODUCTION.url, 'https://st-api.yandex-team.ru', 'v2'),
            (pu.Tracker.Environment.SANDBOX.url, 'https://st-api.test.yandex-team.ru', 'v2'),
            ('https://st-api.yandex-team.ru', 'https://st-api.yandex-team.ru', None),
            ('https://st-api.yandex-team.ru:6666/v3', 'https://st-api.yandex-team.ru:6666', 'v3'),
            ('https://st-api.yandex-team.ru:6666/v1?q=1#help=1', 'https://st-api.yandex-team.ru:6666', 'v1'),
        ),
        ids=(
            'default-production-env',
            'production-env',
            'sandbox-env',
            'no-version',
            'custom-env',
            'custom-env-with-query',
        ),
    )
    def test_split_version(self, tracker_mock: mock.MagicMock, url: str, expected_url: str, expected_version: str):
        pu.Tracker(token='token', url=url)

        expected = dict(
            token='token',
            useragent=pu.Tracker.USER_AGENT,
            base_url=expected_url,
        )
        if expected_version:
            expected['api_version'] = expected_version

        tracker_mock.assert_called_once_with(**expected)

    def test_add_comment(self, tracker_mock: mock.MagicMock):
        issue = 'ISSUE-1'
        text = 'comment-text'
        summonees = ('login1', 'login2')
        attachments = ('file1.xlsx', 'file2.png')

        pu.Tracker('token').add_comment(issue_key=issue, text=text, summonees=summonees, attachments=attachments)

        client: mock.MagicMock = tracker_mock()
        client.issues[issue].comments.create.assert_called_once_with(
            text=text, summonees=summonees, attachments=attachments
        )
