# coding: utf-8
import json

from mock import Mock

from django.test import TestCase
from django.core.urlresolvers import reverse

from emission.django.emission_master import urls
from emission.django.emission_master.controller import controller


class ViewsTestCase(TestCase):
    urls = urls.urlpatterns

    def test_get_slice_no_params(self):
        response = self.client.get(reverse('emission-slice'))

        self.assertEqual(response.status_code, 400)

    def test_get_slice(self):
        controller.get_slice = Mock(return_value=[{'id': i,
                                                   'data': 'test data %d' % i}
                                                            for i in range(1, 4)])

        response = self.client.get(reverse('emission-slice'), data={'start': 1, 'stop': 3})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/json; charset=utf-8')

        messages = json.loads(response.content)

        self.assertEqual(len(messages), 3)

    def test_get_slice_no_stop(self):
        controller.get_slice = Mock(return_value=[{'id': i,
                                                   'data': 'test data %d' % i}
                                                            for i in range(1, 4)])

        response = self.client.get(reverse('emission-slice'), data={'start': 1})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/json; charset=utf-8')

        messages = json.loads(response.content)

        self.assertEqual(len(messages), 3)

    def test_get_slice_empty(self):
        controller.get_slice = Mock(return_value=[])
        response = self.client.get(reverse('emission-slice'), data={'start': 1})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/json; charset=utf-8')
        messages = json.loads(response.content)

        self.assertEqual(len(messages), 0)
