import json
from mock import Mock, patch

from django.test import TestCase, RequestFactory
from django.core.urlresolvers import reverse

from staff.emission.django.emission_master import urls, views
from staff.emission.django.emission_master.controller import controller


class ViewsTestCase(TestCase):
    urls = urls.urlpatterns

    def setUp(self) -> None:
        self.factory = RequestFactory()

    def test_get_slice_no_params(self):
        request = self.factory.get(reverse('emission-slice'))
        request.auth_mechanism = None
        response = views.log_slice(request)

        self.assertEqual(response.status_code, 400)

    def test_get_slice(self):
        controller.get_slice = Mock(return_value=[
            {'id': i, 'data': 'test data %d' % i}
            for i in range(1, 4)
        ])

        request = self.factory.get(reverse('emission-slice'), data={'start': 1, 'stop': 3})
        request.auth_mechanism = None
        response = views.log_slice(request)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/json; charset=utf-8')

        messages = json.loads(response.content)

        self.assertEqual(len(messages), 3)

    def test_get_slice_no_stop(self):
        controller.get_slice = Mock(return_value=[
            {'id': i, 'data': 'test data %d' % i}
            for i in range(1, 4)
        ])

        request = self.factory.get(reverse('emission-slice'), data={'start': 1})
        request.auth_mechanism = None
        response = views.log_slice(request)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/json; charset=utf-8')

        messages = json.loads(response.content)

        self.assertEqual(len(messages), 3)

    def test_get_slice_empty(self):
        controller.get_slice = Mock(return_value=[])
        request = self.factory.get(reverse('emission-slice'), data={'start': 1})
        request.auth_mechanism = None
        response = views.log_slice(request)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/json; charset=utf-8')
        messages = json.loads(response.content)

        self.assertEqual(len(messages), 0)

    def test_get_unsent(self):
        with patch.object(controller, 'get_unsent') as mock:
            mock.return_value = [{'some_key': 1}, {'some_key': 2}, {'some_key': 3}]
            request = self.factory.get(reverse('get-unsent'), data={'count': 3})
            request.auth_mechanism = None
            response = views.get_unsent(request)
            messages = json.loads(response.content)

            self.assertEqual(len(messages), 3)

    def test_mark_sent(self):
        with patch.object(controller, 'mark_sent'):
            request = self.factory.post(reverse('mark-sent'), data={'message_ids': [1, 2]})
            request.auth_mechanism = None
            response = views.mark_sent(request)
            self.assertEqual(response.status_code, 204)
