import pytest
from django.test import override_settings

from billing.dcsaap.backend.core.signals import webhook_event_triggered

from billing.dcsaap.backend.tests.utils.models import create_webhook

WEBHOOK_EVENT = 'common.ping'


@pytest.fixture(scope="function", autouse=True)
def webhook(db, requests_mock):
    w = create_webhook(WEBHOOK_EVENT)
    requests_mock.register_uri('POST', w.url, text='OK')
    return w


def test_dispatch(requests_mock, webhook):
    """
    Проверяем отправку через сигнал
    """
    with override_settings(WEBHOOK_EVENTS=[WEBHOOK_EVENT]):
        webhook_event_triggered.send(test_dispatch, event_name=WEBHOOK_EVENT)

    assert requests_mock.called
    assert requests_mock.call_count == 1

    request = requests_mock.last_request
    assert request.method == 'POST'
    assert request.url == webhook.url
    assert request.headers.get('X-Ya-Service-Ticket')
    assert request.json() == {'event_name': webhook.event_name, 'payload': None}


def test_dispatch_with_payload(requests_mock, webhook):
    """
    Проверяем отправку вебхука с дополнительным содержимым
    """

    webhook_payload = {
        'field': 'value',
        'int_field': 123456,
    }
    expected_json = {
        'event_name': webhook.event_name,
        'payload': webhook_payload,
    }

    with override_settings(WEBHOOK_EVENTS=[WEBHOOK_EVENT]):
        webhook_event_triggered.send(test_dispatch, event_name=WEBHOOK_EVENT, payload=webhook_payload)

    assert requests_mock.called
    assert requests_mock.call_count == 1

    request = requests_mock.last_request
    assert request.url == webhook.url
    assert request.headers.get('X-Ya-Service-Ticket')
    assert request.json() == expected_json


def test_dispatch_unknown_event(requests_mock):
    """
    Проверяем, что неизвестный сигнал не отправляется
    """

    webhook_event_triggered.send(test_dispatch_unknown_event, event_name='any-non-existent-event')
    assert not requests_mock.called
