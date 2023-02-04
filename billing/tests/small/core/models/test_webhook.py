from django.test import override_settings

from billing.dcsaap.backend.tests.utils.models import create_webhook


class TestWebhook:
    """
    Тестирование логики модели Webhook
    """

    webhook_event = 'common.ping'

    def test_send(self, requests_mock):
        """
        Проверяем, что вебхук отправляется через метод send
        """

        webhook = create_webhook(self.webhook_event)
        requests_mock.register_uri('POST', webhook.url, text='OK')

        with override_settings(WEBHOOK_EVENTS=[self.webhook_event]):
            webhook.send()

        assert requests_mock.called
        assert requests_mock.call_count == 1

        request = requests_mock.last_request
        assert request.url == webhook.url
        assert request.headers.get('X-Ya-Service-Ticket')
        assert request.json() == {'event_name': webhook.event_name, 'payload': None}

    def test_send_with_payload(self, requests_mock):
        """
        Проверяем, что вебхук отправляется с дополнительным содержимым
        """

        webhook = create_webhook(self.webhook_event)
        requests_mock.register_uri('POST', webhook.url, text='OK')

        webhook_payload = {
            'field': 'value',
            'another': 123456,
        }
        expected_json = {'event_name': webhook.event_name, 'payload': webhook_payload}

        with override_settings(WEBHOOK_EVENTS=[self.webhook_event]):
            webhook.send(payload=webhook_payload)

        assert requests_mock.called
        assert requests_mock.call_count == 1

        request = requests_mock.last_request
        assert request.url == webhook.url
        assert request.headers.get('Content-Type') == 'application/json'
        assert request.headers.get('X-Ya-Service-Ticket')
        assert request.json() == expected_json
