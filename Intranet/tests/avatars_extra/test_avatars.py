# -*- coding: utf-8 -*-
import os

from django.conf import settings
from django.db import models
from django.test import TestCase, override_settings
from unittest.mock import Mock
from urllib.parse import urlparse

from events.avatars_extra.models import AvatarAdminFileWidget, AvatarImageField
from events.avatars_extra.storage import AvatarsStorage
from events.common_app.utils import requests_session


class UserModel(models.Model):
    name = models.CharField(max_length=255)
    avatar = models.ImageField(storage=AvatarsStorage(), upload_to='/')

    class Meta:
        app_label = 'avatars_extra'


class UserModel2(models.Model):
    name = models.CharField(max_length=255)
    avatar = AvatarImageField()

    class Meta:
        app_label = 'avatars_extra'


class TestUrlAssertsMixin(TestCase):
    def assert_page_exists(self, url, msg=''):
        response = requests_session.get(
            url,
            timeout=settings.DEFAULT_TIMEOUT,
            verify=settings.YANDEX_ROOT_CERTIFICATE,
        )
        self.assertNotEqual(response.status_code, 404, msg=msg)


class TestAvatarsStorage(TestCase):
    def setUp(self):
        self.storage = AvatarsStorage()
        self.file_name = 'some-file-name'

    @override_settings(AVATARS_HOST=None)
    def test_validate_params_without_host(self):
        self.assertRaises(AttributeError, AvatarsStorage)

    @override_settings(AVATARS_HOST_FOR_WRITE=None)
    def test_validate_params_without_host_for_write(self):
        self.assertRaises(AttributeError, AvatarsStorage)

    @override_settings(AVATARS_NAMESPACE=None)
    def test_validate_params_without_avatars_namespace(self):
        self.assertRaises(AttributeError, AvatarsStorage)

    def test_init_should_use__host_for_write__from_params_if_specified(self):
        storage = AvatarsStorage(host_for_write='google.com')
        self.assertEqual(storage.host_for_write, 'google.com')

    @override_settings(AVATARS_HOST_FOR_WRITE='yandex.com')
    def test_init_should_use__host_for_write__from_settings__if_it_is_not_specified_in_params(self):
        storage = AvatarsStorage()
        self.assertEqual(storage.host_for_write, 'yandex.com')

    def test_get_clean_hostname(self):
        experiments = [
            {
                'host': 'http://google.com/hello/',
                'expected': 'google.com'
            },
            {
                'host': 'google.com/hello/',
                'expected': ''
            }
        ]

        for exp in experiments:
            with override_settings(AVATARS_HOST=exp['host']):
                storage = AvatarsStorage()
                self.assertEqual(storage.get_clean_hostname(), exp['expected'], msg=exp)

    def test_url(self):
        expected = os.path.join(self.storage._get_base_url_for_method(name=self.file_name, method='get'), 'orig')
        self.assertEqual(self.storage.url(self.file_name), expected, 'неправильно сформирован url')

    def test_clean_name(self):
        experiments = [
            {
                'value':    '//0!#$1kjvvsa;l',
                'expected': '01kjvvsal'
            }
        ]

        for exp in experiments:
            response = self.storage._clean_name(exp['value'])
            self.assertEqual(response, exp['expected'], 'метод _clean_name() должно оставлять только буквы и цифры')

    def test__get_base_url_for_method__should_use_host_from___get_host_for_method(self):
        self.storage._get_host_for_method = Mock(return_value='http://some_host.ru')
        response = self.storage._get_base_url_for_method(name='some_file', method='patch')
        response_host = urlparse(response).netloc
        expected = 'some_host.ru'
        msg = 'метод _get_base_url_for_method должен использовать в качестве хоста значение из метода _get_host_for_method'
        self.assertEqual(response_host, expected, msg=msg)

    def test__get_base_url_for_method__should_return_join_of__host__method_and_namespace__file_name(self):
        method = 'some_http_method'
        host = 'http://yandex.ru'
        namespace = 'forms-gena'
        self.storage._get_host_for_method = Mock(return_value=host)
        self.storage.namespace = namespace

        response = self.storage._get_base_url_for_method(name=self.file_name, method=method)
        method_and_namespace = '%s-%s' % (method, namespace)
        expected = os.path.join(host, method_and_namespace, self.file_name)
        self.assertEqual(response, expected)

    def test_get_host_for_method_should_return__host_for_write__for_write_methods(self):
        self.storage.host_for_write = 'http://google.com'
        self.storage.host = 'http://yandex.ru'

        self.storage.read_methods = ['read_method_1', 'read_method_2']
        response = self.storage._get_host_for_method('write_method_1')
        self.assertEqual(response, self.storage.host_for_write)

    def test_get_host_for_method_should_return__host__for_read_methods(self):
        self.storage.host_for_write = 'http://google.com'
        self.storage.host = 'http://yandex.ru'

        self.storage.read_methods = ['read_method_1', 'read_method_2']
        response = self.storage._get_host_for_method('read_method_1')
        self.assertEqual(response, self.storage.host)


class TestAvatarImageField(TestCase):
    def setUp(self):
        # создаем пользователя без аватара
        self.gena = UserModel2.objects.create(name='gena')
        # записываем напрямую в базу имя файла
        UserModel2.objects.filter(pk=self.gena.pk).update(avatar='some-file')
        self.gena = UserModel2.objects.get(pk=self.gena.pk)

        self.base_url_for_get = os.path.join(settings.AVATARS_HOST,
                                             'get-%s' % settings.AVATARS_NAMESPACE,
                                             self.gena.avatar.name)

    def test_get_thumb_url(self):
        experiments = [
            {
                'width':        None,
                'height':       None,
                'expected_url': os.path.join(self.base_url_for_get, 'orig'),
                'msg':          'если не указан width и height, должен быть выведен thumbnail с алиасом orig'
            },
            {
                'width':        None,
                'height':       10,
                'expected_url': os.path.join(self.base_url_for_get, 'orig'),
                'msg':          'если не указан width или height, должен быть выведен thumbnail с алиасом orig'
            },
            {
                'width':        10,
                'height':       None,
                'expected_url': os.path.join(self.base_url_for_get, 'orig'),
                'msg':          'если не указан width или height, должен быть выведен thumbnail с алиасом orig'
            },
            {
                'width':        10,
                'height':       10,
                'expected_url': os.path.join(self.base_url_for_get, '10x10'),
                'msg':          'если указан width и height, должен быть выведен thumbnail размером width на height'
            }
        ]

        for exp in experiments:
            response = self.gena.avatar.get_thumb_url(width=exp['width'], height=exp['height'])
            self.assertEqual(response, exp['expected_url'], msg=exp['msg'])

    def test_get_thumb_url_with_sizes_in_positional_params(self):
        response = self.gena.avatar.get_thumb_url(10, 10)
        expected = os.path.join(self.base_url_for_get, '10x10')
        self.assertEqual(response, expected)

    def test_widget(self):
        widget_class = type(self.gena.avatar.field.formfield().widget)
        self.assertEqual(widget_class, AvatarAdminFileWidget, 'у поля AvatarImage должен быть свой виджет')
