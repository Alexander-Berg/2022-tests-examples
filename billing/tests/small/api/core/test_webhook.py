from django.urls import reverse
from rest_framework.status import HTTP_200_OK, HTTP_201_CREATED, HTTP_400_BAD_REQUEST

from billing.dcsaap.backend.tests.utils.models import create_webhook, mock_webhook_choices


class TestWebhookGet:
    """
    Тестирование GET запросов для вебхуков
    """

    def test_get_all_webhooks(self, tvm_api_client):
        for event_name in ('a', 'b', 'c'):
            create_webhook(event_name)

        response = tvm_api_client.get(reverse('webhook-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 3

    def test_filter_by_event_name(self, tvm_api_client):
        """
        Проверяем выборку по event_name
        """
        for event_name in ('a', 'a', 'b'):
            create_webhook(event_name)

        url_template = '{}?event_name={{}}'.format(reverse('webhook-list'))

        with mock_webhook_choices(['a']):
            response = tvm_api_client.get(url_template.format('a'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 2

        with mock_webhook_choices(['b']):
            response = tvm_api_client.get(url_template.format('b'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 1


class TestWebhookCreateUpdate:
    """
    Тестирование создания и изменения вебхуков
    """

    webhook_event = 'common.ping'
    webhook_url = 'http://a.b.c.d.yandex.ru/'
    webhook_tvm_id = 123456

    def test_create_ok(self, tvm_api_client):
        """
        Проверяем успешное заведение вебхука
        """
        response = tvm_api_client.get(reverse('webhook-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 0

        values = dict(
            event_name=self.webhook_event,
            destination_url=self.webhook_url,
            tvm_id=self.webhook_tvm_id,
        )

        with mock_webhook_choices([self.webhook_event]):
            response = tvm_api_client.post(reverse('webhook-list'), values, format='json')

        assert response.status_code == HTTP_201_CREATED
        assert response.data['event_name'] == self.webhook_event
        assert response.data['destination_url'] == self.webhook_url
        assert response.data['tvm_id'] == self.webhook_tvm_id

    def test_event_not_exists(self, tvm_api_client):
        """
        Проверяем корректный ответ на попытку создания вебхука по несуществующему событию
        """
        response = tvm_api_client.get(reverse('webhook-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 0

        values = dict(
            event_name='some.unknown-event',
            destination_url=self.webhook_url,
            tvm_id=self.webhook_tvm_id,
        )

        response = tvm_api_client.post(reverse('webhook-list'), values, format='json')
        assert response.status_code == HTTP_400_BAD_REQUEST
        assert 'event_name' in response.data

    def test_incorrect_url(self, tvm_api_client):
        """
        Проверяем корректный ответ на попытку создания вебхука с некорректным URL
        """
        response = tvm_api_client.get(reverse('webhook-list'))
        assert response.status_code == HTTP_200_OK
        assert response.data['count'] == 0

        values = dict(
            event_name=self.webhook_event,
            destination_url='some-invalid-url',
            tvm_id=self.webhook_tvm_id,
        )

        with mock_webhook_choices([self.webhook_event]):
            response = tvm_api_client.post(reverse('webhook-list'), values, format='json')

        assert response.status_code == HTTP_400_BAD_REQUEST
        assert 'destination_url' in response.data
