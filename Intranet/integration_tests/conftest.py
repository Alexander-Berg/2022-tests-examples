from past.builtins import basestring
import json
import pytest
import types

from django.conf import settings
from django.conf.urls import include, url
from django.contrib import admin
from django.contrib.auth import get_user_model
from django.http.response import HttpResponseRedirectBase

from rest_framework.test import APIClient

from kelvin.urls import urlpatterns


# Рассказываем pytest где поискать наши фикстуры
pytest_plugins = [
    'integration_tests.fixtures.base',
    'integration_tests.fixtures.accounts',
    'integration_tests.fixtures.courses',
    'integration_tests.fixtures.journals',
    'integration_tests.fixtures.lessons',
    'integration_tests.fixtures.news',
    'integration_tests.fixtures.problems',
    'integration_tests.fixtures.projects',
    'integration_tests.fixtures.settings',
    'integration_tests.fixtures.staff_notifications',
    'integration_tests.fixtures.subjects',
    'integration_tests.fixtures.tvm',
]

User = get_user_model()


def pytest_configure():
    """
    Заменяет асинхронное выполнение селери-тасков на синхронное,
    авторизующую мидлварь на тестовую, проставляем неавторизованного
    пользователя.
    Тестирует без репликации БД
    """
    settings.CELERY_ALWAYS_EAGER = True
    settings.MIDDLEWARE = tuple(
        'django_yauth.middleware.YandexAuthTestMiddleware'
        if middleware in ('django_yauth.middleware.YandexAuthMiddleware',
                          'django_sirius_auth.middleware.SiriusAuthMiddleware')
        else middleware
        for middleware in settings.MIDDLEWARE
        if middleware != 'django_replicated.middleware.ReplicationMiddleware'
    )
    settings.YAUTH_TEST_USER = False

    settings.DEBUG_TVM_SERVICE_ID = settings.TVM_FRONTEND

    # тесты запускаются с `DEBUG=False` и это фича, поэтому надо добавить
    # урлы админки
    urlpatterns.append(url(r'^admin/', include(admin.site.urls)))


class JSONClient(APIClient):
    """
    Тестовый клиент для json-запросов
    """
    def __init__(self, *args, **kwargs):
        """
        Создает суперпользователя
        """
        super(JSONClient, self).__init__(*args, **kwargs)
        self.super_user = User.objects.create(
            username='super_user',
            email='super@user.com',
            is_staff=True,
            is_superuser=True,
        )
        self.user = User.objects.create(
            username='yauth-test-user',
            email='some@unknown.email'
        )

    def login(self, is_superuser=False, user=None,
              user_info='yauth-test-user'):
        """
        Проставляет в настройки тестовые данные пользователя

        :param is_superuser: залогиниться как суперюзер
        :param user: экземпляр пользователя, которого надо авторизовать
        :param user_info: строка или словарь, строка - это логин пользователя,
            словарь - для любых полей пользователя
        """
        if is_superuser:
            settings.YAUTH_TEST_USER = self.super_user.username
            self.user = self.super_user
        elif user is not None:
            settings.YAUTH_TEST_USER = user.username
            self.user = User.objects.get(username=user.username)
        else:
            settings.YAUTH_TEST_USER = user_info
            if user_info:
                self.user = User.objects.get(
                    username=(user_info if isinstance(user_info, basestring)
                              else user_info.get('login', 'yauth-test-user'))
                )

    def logout(self):
        """
        Проставляет в настройки неавторизованного тестового пользователя
        """
        settings.YAUTH_TEST_USER = False

    def _request_json(self, method_name, url, data, **kwargs):
        """
        Посылает запрос указанного типа, преобразуя data в json
        и проставляя нужный content_type
        """
        assert method_name in ('post', 'put', 'patch')
        params = {'content_type': 'application/json'}
        if kwargs:
            params.update(**kwargs)
        return getattr(self, method_name)(url, json.dumps(data), **params)

    def post_json(self, url, data, **kwargs):
        """
        Посылает post-запрос, преобразуя data в json
        и проставляя нужный content_type
        """
        return self._request_json('post', url, data, **kwargs)

    def put_json(self, url, data, **kwargs):
        """
        Посылает put-запрос, преобразуя data в json
        и проставляя нужный content_type
        """
        return self._request_json('put', url, data, **kwargs)

    def patch_json(self, url, data, **kwargs):
        """
        Посылает patch-запрос, преобразуя data в json
        и проставляя нужный content_type
        """
        return self._request_json('patch', url, data, **kwargs)

    def request(self, **request):
        """
        Добавляет метод `json` для получения ответа
        """
        request.update({
            'HTTP_X_TVM_TICKET': (
                '2:B7:1965048799:7F:CD0:ILvub8xEN8cgA9T4FzraiY1HYZdZGNN214lfho'
                'B4pjaL875VIc5_4nn7Rdg2RYvJE70BB7mv8j1sCKP6z7Ukcq3AsA1t8rqszKL'
                'ouC4aoUCu1t8vd2_Uaxy7TfdNU-2ComwVzGN3vBbPLvdRmSHEBr3kB1ZWDQSO'
                '2HG7zAQCCpc'
            ),
        })
        response = super(JSONClient, self).request(**request)

        # добавляем метод `json` ответам с Content-Type: application/json
        if (not isinstance(response, HttpResponseRedirectBase) and
                hasattr(response, 'accepted_media_type') and
                response.accepted_media_type.startswith(u'application/json')):
            response.json = types.MethodType(
                lambda self: json.loads(self.content), response
            )

        return response


@pytest.fixture()
def yclient():
    """
    Фикстура с тестовым "json-клиентом" с возможностью логиниться
    """
    return JSONClient()


class TVMClient(APIClient):
    def __init__(self, tvm_id, *args, **kwargs):
        self._tvm_id = tvm_id
        super().__init__(*args, **kwargs)

        self.super_user = User.objects.create(
            username='super_user',
            email='super@example.com',
            is_staff=True,
            is_superuser=True,
        )
        self.user = User.objects.create(
            username='yauth-test-user',
            email='some@example.com'
        )

    def login(self, is_superuser=False, user=None):
        if not user:
            user = self.user

        if is_superuser:
            user = self.super_user

        return self.force_authenticate(user=user)

    def request(self, **kwargs):
        kwargs.update(dict(
            HTTP_DEBUG_SERVICE_ID=self._tvm_id,
            format="json",
        ))
        return super().request(**kwargs)


@pytest.fixture()
def jclient() -> TVMClient:
    return TVMClient(settings.TVM_FRONTEND)
