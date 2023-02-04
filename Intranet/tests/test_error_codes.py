"""
Тестируем новый формат сообщений об ошибках.
"""
import json
from builtins import str

import pytest

from django.conf.urls import url
from django.core.exceptions import PermissionDenied as DjangoPermissionDenied
from django.core.exceptions import ValidationError as DjangoValidationError
from django.http.response import Http404
from django.utils.translation import ugettext_lazy as _

from rest_framework import fields, serializers
from rest_framework.exceptions import APIException, ErrorDetail, NotFound, PermissionDenied, ValidationError
from rest_framework.response import Response
from rest_framework.settings import api_settings
from rest_framework.status import (
    HTTP_400_BAD_REQUEST, HTTP_403_FORBIDDEN, HTTP_404_NOT_FOUND, HTTP_500_INTERNAL_SERVER_ERROR,
)
from rest_framework.views import APIView

from kelvin.common.error_responses import ErrorsComposer
from kelvin.common.utils_for_tests import assert_items_equal
from kelvin.urls import urlpatterns


@pytest.fixture
def view_response(request, mocker, client):
    """
    Возвращает код и тело ответа от вью при различных исключениях внутри.
    """
    assert hasattr(request, 'param'), 'This fixture must be parametrized'

    mocker.patch.object(SimpleView, 'get', side_effect=request.param)

    response = client.get('/test-error-codes/')
    return response.status_code, json.loads(response.rendered_content)


class SimpleView(APIView):

    def get(self, request):
        return Response('test')


urlpatterns += [
    url(r'^test-error-codes/$', SimpleView.as_view())
]


class SimpleSerializer(serializers.Serializer):
    name = fields.CharField(max_length=10)
    age = fields.IntegerField(min_value=18, required=True)
    password = fields.CharField(required=True)

    default_error_messages = {
        'incorrect_password': _(u'Вы неправильно ввели пароль'),
    }

    def validate(self, attrs):
        password = attrs.get('password')
        if password != 'admin':
            self.fail('incorrect_password')
        return super(SimpleSerializer, self).validate(attrs)


def _get_default_message(field_class, code, value=None):
    """
    Достает сообщение по умолчанию для поля по коду.
    """
    message = field_class().error_messages[code]
    if value is not None:
        message = message.format(**{code: value})
    return str(message)


# Tests
@pytest.mark.parametrize(
    'data, errors_representation',
    (
        (
            {'name': 'Ivan Invanov', 'password': 'Qwerty'},
            [
                {
                    'source': 'name',
                    'code': 'max_length',
                    'message': _get_default_message(
                        fields.CharField, 'max_length', 10
                    ),
                },
                {
                    'source': 'age',
                    'code': 'required',
                    'message': _get_default_message(
                        fields.IntegerField, 'required'
                    ),
                }
            ]
        ),
        (
            {'name': 'Ivan', 'age': 18, 'password': 'Qwerty'},
            [
                {
                    'source': api_settings.NON_FIELD_ERRORS_KEY,
                    'code': 'incorrect_password',
                    'message': _get_default_message(
                        SimpleSerializer, 'incorrect_password'
                    )
                },
            ]
        )
    )
)
def test_error_codes_from_simple_serializer(data, errors_representation):
    """
    Проверка ошибок при валидации сериализатора.
    """
    serializer = SimpleSerializer(data=data)

    with pytest.raises(ValidationError) as exc_info:
        serializer.is_valid(raise_exception=True)

    composer = ErrorsComposer(exception=exc_info.value)
    print(composer.get_representation())
    actual = sorted(composer.get_representation(), key=lambda x: x.get('source'))
    expected = sorted(errors_representation, key=lambda x: x.get('source'))

    assert actual == expected


@pytest.mark.parametrize(
    'view_response,'
    'expected_status_code,'
    'expected_errors',
    (
        (
            APIException('Generic API Error'),
            HTTP_500_INTERNAL_SERVER_ERROR,
            [
                {
                    'source': '__response__',
                    'code': 'error',
                    'message': 'Generic API Error',
                }
            ]
        ),
        (
            ValidationError(_(u'Беда!')),
            HTTP_400_BAD_REQUEST,
            [
                {
                    'source': '__response__',
                    'code': 'invalid',
                    'message': u'Беда!',
                }
            ]
        ),
        (
            ValidationError(
                {
                    'password': _(u'Пароль неверен.'),
                    'age': ErrorDetail(_(u'Вы слишком молоды'), 'too_young'),
                }
            ),
            HTTP_400_BAD_REQUEST,
            [
                {
                    'source': 'age',
                    'code': 'too_young',
                    'message': u'Вы слишком молоды',
                },
                {
                    'source': 'password',
                    'code': 'invalid',
                    'message': u'Пароль неверен.',
                }
            ]
        ),
        (
            NotFound('Not Found'),
            HTTP_404_NOT_FOUND,
            [
                {
                    'source': '__response__',
                    'code': 'not_found',
                    'message': 'Not Found',
                }
            ]
        ),
        (
            PermissionDenied('Permission denied', code='wrong_permission'),
            HTTP_403_FORBIDDEN,
            [
                {
                    'source': '__response__',
                    'code': 'wrong_permission',
                    'message': 'Permission denied',
                }
            ]
        ),
        (
            DjangoValidationError('Simple validation exception'),
            HTTP_400_BAD_REQUEST,
            [
                {
                    'source': '__response__',
                    'code': 'invalid',
                    'message': 'Simple validation exception',
                }
            ]
        ),
        (
            DjangoValidationError(_(u'Плохой запрос'), code='simple message'),
            HTTP_400_BAD_REQUEST,
            [
                {
                    'source': '__response__',
                    'code': 'simple_message',
                    'message': u'Плохой запрос',
                }
            ]
        ),
        (
            DjangoPermissionDenied(_(u'Доступ запрещён!')),
            HTTP_403_FORBIDDEN,
            [
                {
                    'source': '__response__',
                    'code': 'permission_denied',
                    'message': u'Доступ запрещён!',
                }
            ]
        ),
        (
            Http404(),
            HTTP_404_NOT_FOUND,
            [
                {
                    'source': '__response__',
                    'code': 'not_found',
                    'message': NotFound.default_detail,
                }
            ]
        ),
    ),
    indirect=['view_response']
)
@pytest.mark.usefixtures('disable_tvm_middleware')
@pytest.mark.xfail
def test_different_exceptions_in_view(view_response,
                                      expected_status_code,
                                      expected_errors):
    """
    Проверяет, что ответ от API в случаях каких-либо
    исключений соответствует новому формату.
    """
    status_code, response_body = view_response
    assert status_code == expected_status_code
    assert set(response_body) == {'errors'}
    assert_items_equal(response_body['errors'], expected_errors)
