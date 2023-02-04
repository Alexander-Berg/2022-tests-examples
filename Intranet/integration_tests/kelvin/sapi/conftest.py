import json
import types

import pytest
from django.conf import settings
from django.conf.urls import url, include
from django.contrib import admin
from django.contrib.auth import get_user_model
from django.http.response import HttpResponseRedirectBase
from rest_framework.test import APIClient

from integration_tests.conftest import JSONClient
from kelvin.urls import urlpatterns

User = get_user_model()


def pytest_configure():
    """
    Заменяем миддлвар авторизации на тестовый
    Добавляем урлы админки
    """
    settings.CELERY_ALWAYS_EAGER = True

    settings.MIDDLEWARE = tuple(
        'django_sirius_auth.middleware.SiriusAuthTestMiddleware'
        if mw == 'django_sirius_auth.middleware.SiriusAuthMiddleware'
        else mw
        for mw in settings.MIDDLEWARE
        if mw != 'django_replicated.middleware.ReplicationMiddleware'
    )

    settings.SIRIUS_TEST_USER = False

    urlpatterns.append(url(r'^admin/', include(admin.site.urls)))
    

class TestJsonClient(JSONClient):
    """
    Пропатченный API-клиент для тестов
    """

    def __init__(self, *args, **kwargs):
        """

        """
        super(TestJsonClient, self).__init__(*args, **kwargs)

        self.user = User.objects.create(
            username='sirius-auth-test-user',
            email='sirius-auth-test-user@unknown.unknown',
        )

    def login(self, is_superuser=False, user=None,
              user_info='sirius-auth-test-user'):
        """

        """
        if user is not None:
            settings.SIRIUS_TEST_USER = user.username
            self.user = User.objects.get(username=user.username)

        else:
            settings.SIRIUS_TEST_USER = user_info
            if user_info:
                self.user = User.objects.get(
                    username=user_info
                )

    def logout(self):
        """
        Сбрасывает пользователя
        """
        settings.SIRIUS_TEST_USER = False

    def request(self, **kwargs):
        """

        """
        kwargs.update({
            'HTTP_HOST': 'api.sochisirius.online'
        })

        return super(TestJsonClient, self).request(**kwargs)


@pytest.fixture()
def sclient():
    """
    Фикстура HTTP-клиента
    """
    return TestJsonClient()
