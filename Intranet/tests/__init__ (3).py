# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.test import TestCase
from django.test import modify_settings
from django.test import override_settings


@override_settings(YAUTH_TEST_USER='1')
@modify_settings(MIDDLEWARE_CLASSES={
    'remove': 'django_yauth.middleware.YandexAuthMiddleware',
    'append': 'django_yauth.middleware.YandexAuthTestMiddleware',
})
class YauthTestCase(TestCase):
    pass
