from functools import partial

import json

from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.models import AnonymousUser
from django.test import RequestFactory
from django.test.client import JSON_CONTENT_TYPE_RE
from django.urls import resolve
from rest_framework.test import APIClient as DRFAPIClient


User = get_user_model()


class APIClient(DRFAPIClient):
    """
    NOTE: в django.test.client.APIClient при конвертации
    response.content в json, делается decode без указания кодировки.
    В результате, получаем ошибку при вызове response.json(),
    если в ответе есть non-ascii символы.
    """
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._tvm_client_id = None

    def _parse_json(self, response, **extra):
        if not hasattr(response, '_json'):
            if not JSON_CONTENT_TYPE_RE.match(response.get('Content-Type')):
                raise ValueError(
                    'Content-Type header is "{0}", not "application/json"'
                    .format(response.get('Content-Type'))
                )
            response._json = json.loads(response.content.decode('utf-8'), **extra)
        return response._json

    def login(self, login, mechanism_name='cookie', tvm_client_id=None, **credentials):
        try:
            User.objects.get(username=login)
        except User.DoesNotExist:
            return False
        settings.AUTH_TEST_USER = login
        settings.AUTH_TEST_MECHANISM = mechanism_name
        settings.AUTH_TEST_TVM_CLIENT_ID = tvm_client_id
        self._tvm_client_id = tvm_client_id
        return True

    def logout(self):
        settings.AUTH_TEST_USER = None
        settings.AUTH_TEST_MECHANISM = None
        settings.AUTH_TEST_TVM_CLIENT_ID = None
        self._tvm_client_id = None


class DjangoSimpleClient:
    """
    Клиент для тестирования вьюх чистого Django.

    Для работы в вьюхами django-rest-framework использовать APIClient.
    """
    _available_methods = (
        'get',
        'post',
        'put',
        'patch',
        'delete',
    )

    def __init__(self, user=None):
        self.request_factory = RequestFactory()
        self.user = user or AnonymousUser()

    def _get_request(self, method_name, path, *args, **kwargs):
        method = getattr(self.request_factory, method_name)
        request = method(path, *args, **kwargs)
        request.user = self.user
        request.META['HTTP_HOST'] = settings.FEMIDA_HOST
        return request

    def _request(self, method_name, path, *args, **kwargs):
        request = self._get_request(method_name, path, *args, **kwargs)
        resolver_match = resolve(path)
        view_function = resolver_match.func
        return view_function(
            request,
            *resolver_match.args,
            **resolver_match.kwargs
        )

    def authenticate(self, user):
        self.user = user

    def __getattr__(self, item):
        if item in self._available_methods:
            return partial(self._request, item)
        raise AttributeError("'%s' object has no attribute '%s'" % (self.__class__.__name__, item))
