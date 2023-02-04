import pytest
import requests
from django.conf.urls import url
from django.http import Http404
from rest_framework.exceptions import NotAuthenticated
from rest_framework.response import Response
from rest_framework.status import (
    HTTP_401_UNAUTHORIZED,
    HTTP_404_NOT_FOUND,
    HTTP_500_INTERNAL_SERVER_ERROR,
)
from rest_framework.views import APIView
from kelvin.urls import urlpatterns


class SimpleView(APIView):

    def get(self, request):
        return Response('test')


urlpatterns += [
    url(r'^integration-test-error-codes/$', SimpleView.as_view())
]


@pytest.mark.skip()
@pytest.mark.parametrize(
    'exception, status_code, errors',
    (
        (
            Exception('Hello!'),
            HTTP_500_INTERNAL_SERVER_ERROR,
            [
                {
                    'source': '__response__',
                    'code': 'error',
                    'message': u'Произошла ошибка сервера.',
                }
            ]
        ),
        (
            Http404('Url not found'),
            HTTP_404_NOT_FOUND,
            [
                {
                    'source': '__response__',
                    'code': 'not_found',
                    'message': u'Url not found',
                }
            ]
        ),
        (
            NotAuthenticated(
                u'Пожалуйста зарегистрируйтесь', code='login_first'
            ),
            HTTP_401_UNAUTHORIZED,
            [
                {
                    'source': '__response__',
                    'code': 'login_first',
                    'message': u'Пожалуйста зарегистрируйтесь',
                }
            ]
        ),
    )
)
@pytest.mark.usefixtures('disable_tvm_middleware')
def test_handler_xxx_response(exception, status_code,
                              errors, mocker, live_server):
    """
    Проверяем на запущенном сервисе формат ответов в случае ошибок.
    """
    mocker.patch.object(SimpleView, 'get', side_effect=exception)

    with requests.Session() as session:
        # выключаем использование прокси в контейнере
        # иначе запрос к локальному сервису будет идти через прокси,
        # т.к. она прописана в переменных окружения
        session.trust_env = False
        response = session.get(
            live_server.url + '/integration-test-error-codes/'
        )

    assert response.status_code == status_code
    response_json = response.json()

    assert set(response_json) == {'errors'}
    assert response_json['errors'] == errors
