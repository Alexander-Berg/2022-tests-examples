# -*- coding: utf-8 -*-
import responses
import os

from django.conf import settings
from django.core.files.base import ContentFile
from django.test import TestCase
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.media.factories import ImageFactory
from events.media.models import Image
from events.arc_compat import read_asset


class TestAvatarsImageSave(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex()
        self.avatar_file_path = os.path.join(settings.FIXTURES_DIR, 'files', 'ava.jpeg')
        self.avatar_file = ContentFile(
            read_asset(self.avatar_file_path),
            name=self.avatar_file_path,
        )
        self.sizes = [
            f'{x}x{y or ""}'
            for (x, y) in settings.IMAGE_SIZES
        ]

    def register_uri(self, body=None, status=None):
        status = status or 200
        responses.add(
            responses.POST,
            'http://avatars-int.mdst.yandex.net:13000/put-forms/12345',
            json=body,
            status=status,
        )

    @responses.activate
    @patch('events.avatars_extra.storage.generate_code', return_value='12345')
    def test_save(self, generate_code):
        self.register_uri(body={
            'group-id': 69076,
        })

        response = self.client.post('/admin/api/v2/image/', data={
            'image': self.avatar_file,
        })

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['name'], 'ava.jpeg')
        self.assertEqual(len(response.data['links']), len(settings.IMAGE_SIZES))

        avatar_image = Image.objects.get(pk=response.data['id'])
        self.assertEqual(avatar_image.name, 'ava.jpeg')
        self.assertEqual(avatar_image.image.name, '69076/12345')
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    @patch('events.avatars_extra.storage.generate_code', return_value='12345')
    def test_save_fail(self, generate_code):
        self.register_uri(status=400)

        response = self.client.post('/admin/api/v2/image/', data={
            'image': self.avatar_file,
        })

        self.assertEqual(response.status_code, 400)
        self.assertListEqual(response.data, [
            'Произошла ошибка при загрузке файла, пожалуйста, попробуйте еще раз'
        ])
        self.assertEqual(len(responses.calls), 1)

    def test_get_list(self):
        image = ImageFactory()
        response = self.client.get('/admin/api/v2/image/')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 1)
        result = response.data['results'][0]

        self.assertDictEqual(result['links'], {
            size: f'https://avatars.mdst.yandex.net/get-forms/{image.image}/{size}'
            for size in self.sizes
        })

    def test_get_one(self):
        image = ImageFactory()
        response = self.client.get(f'/admin/api/v2/image/{image.pk}/')
        self.assertEqual(response.status_code, 200)

        self.assertDictEqual(response.data['links'], {
            size: f'https://avatars.mdst.yandex.net/get-forms/{image.image}/{size}'
            for size in self.sizes
        })


class TestImageView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.system_image = ImageFactory(is_system=True)
        self.non_system_image = ImageFactory()

    def test_should_return_all_images(self):
        response = self.client.get('/admin/api/v2/image/')
        self.assertEqual(response.status_code, 200)

        results = response.data['results']
        self.assertEqual(len(results), 2)

    def test_should_return_non_system_images(self):
        response = self.client.get('/admin/api/v2/image/?is_system=False')
        self.assertEqual(response.status_code, 200)

        results = response.data['results']
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]['id'], self.non_system_image.pk)

    def test_should_return_system_images(self):
        response = self.client.get('/admin/api/v2/image/?is_system=True')
        self.assertEqual(response.status_code, 200)

        results = response.data['results']
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]['id'], self.system_image.pk)
