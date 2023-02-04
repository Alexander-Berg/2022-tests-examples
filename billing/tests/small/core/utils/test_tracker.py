"""
Проверяем утилиты `core.utils.tracker`
"""
import pytest
from django.conf import settings
from django.test import override_settings

from billing.dcsaap.backend.core.utils import tracker


class TestCreateIssue:
    """
    Тестируем логику создания задач в трекере.
    """

    @pytest.mark.usefixtures('tracker_mock')
    def test_disabled(self):
        """
        Тестируем, что с выключенным трекером задачи не создаются.
        """
        with override_settings(TRACKER_ENABLED=False):
            issue_key = tracker.create_issue('SSD', 'Hello, world', 'Hello, beautiful world')
        assert issue_key is None

    @pytest.mark.usefixtures('tracker_mock')
    def test_success(self, requests_mock):
        """
        Проверяем успешное создание задачи.
        """
        issue_queue = 'SSD'
        issue_summary = 'Hello, world'
        issue_description = 'Hello, beautiful world'

        issue_key = tracker.create_issue(issue_queue, issue_summary, issue_description)
        assert issue_key == f'{issue_queue}-1'

        # POST /issues + GET /fields
        assert requests_mock.call_count == 2
        issue_body = requests_mock.request_history[0].json()
        assert issue_body['queue'] == issue_queue
        assert issue_body['summary'] == issue_summary
        assert issue_body['description'] == issue_description

    def test_fail(self, requests_mock):
        """
        Проверяем неуспешное создание задачи.
        """
        requests_mock.register_uri(
            'POST',
            f'{settings.TRACKER_API_HOST}/{tracker.VERSION}/issues/',
            status_code=500,
            text='tracker is down now',
        )

        with override_settings(TRACKER_ENABLED=True):
            issue_key = tracker.create_issue('SSD', 'Hello, world', 'Hello, beautiful world')
        assert issue_key is None
