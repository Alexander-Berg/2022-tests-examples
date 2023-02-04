# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import http.client as http
import hamcrest as hm

from balance import exc
from tests.tutils import has_exact_entries

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.resource import URL, build_custom_resource_cxt

FAKE_INVOICE_ID = -123


class PlainException(exc.EXCEPTION):
    code = 123
    _format = 'Test msg %(object_id)s'

    def __init__(self, id):
        self.object_id = id
        super(PlainException, self).__init__()


class TankerException(PlainException):
    _tanker_fields = ['object_id']


class FirstLevelException(PlainException):
    pass


class SecondLevelException(FirstLevelException):
    pass


class Mixin(exc.EXCEPTION):
    pass


class ComplexException(FirstLevelException, Mixin):
    pass


class BaseException(Exception):
    pass


class LevelBaseException(Exception):
    pass


class StrCodeException(exc.EXCEPTION):
    code = 'abc'


class TestCaseErrorHandler(TestCaseApiAppBase):
    BASE_API = URL

    def test_not_found(self):
        response = self.test_client.get(u'/v1/invoice?invoice_id={}'.format(FAKE_INVOICE_ID))

        expected_data = {
            'description': u'Invoice with ID {} not found in DB'.format(FAKE_INVOICE_ID),
            'error': 'INVOICE_NOT_FOUND',
        }

        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND), 'response code must be NOT_FOUND')
        hm.assert_that(response.get_json(), hm.has_entries(expected_data))

    def test_bad_request(self):
        response = self.test_client.get('/v1/invoice')

        expected_data = {
            'description': u'Can\'t parse args.'
                           u'\nId счета Missing required parameter in the JSON body'
                           u' or the post body or the query string',
            'error': u'SnoutParseArgsException',
        }

        hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST), 'response code must be BAD_REQUEST')
        hm.assert_that(response.get_json(), hm.has_entries(expected_data))

    @pytest.mark.parametrize(
        'w_tanker',
        [True, False],
    )
    def test_tanker_context(self, w_tanker):
        def func():
            raise TankerException(666) if w_tanker else PlainException(666)

        with build_custom_resource_cxt(return_func=func):
            response = self.test_client.get(self.BASE_API)

        parents = ['EXCEPTION']
        if w_tanker:
            parents = ['PlainException'] + parents

        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        required_res = {
            'code': 123,
            'error': 'TankerException' if w_tanker else 'PlainException',
            'description': 'Test msg 666',
            'label': hm.starts_with('ERR-'),
            'parents': hm.contains(*parents),
        }
        if w_tanker:
            required_res['tanker_context'] = has_exact_entries({'object-id': 666})
        hm.assert_that(
            response.get_json(),
            has_exact_entries(required_res),
        )

    @pytest.mark.parametrize(
        'exc_cls, parents',
        [
            pytest.param(exc.EXCEPTION, [], id='base'),
            pytest.param(PlainException, ['EXCEPTION'], id='plain'),
            pytest.param(FirstLevelException, ['PlainException', 'EXCEPTION'], id='first_level'),
            pytest.param(SecondLevelException, ['FirstLevelException', 'PlainException', 'EXCEPTION'], id='second_level'),
            pytest.param(ComplexException, ['FirstLevelException', 'PlainException', 'Mixin', 'EXCEPTION'], id='complex'),
            pytest.param(LevelBaseException, [], id='python_base'),
        ],
    )
    def test_parents(self, exc_cls, parents):
        def func():
            raise exc_cls(666)

        with build_custom_resource_cxt(return_func=func):
            response = self.test_client.get(self.BASE_API)

        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        required_res = {
            'error': exc_cls.__name__,
            'description': 'Test msg 666' if exc_cls is not exc.EXCEPTION else 'Unknown error occurred in Balance engine',
            'label': hm.starts_with('ERR-'),
        }
        if exc_cls is exc.EXCEPTION:
            required_res['description'] = 'Unknown error occurred in Balance engine'
            required_res['parents'] = hm.empty()
        elif issubclass(exc_cls, exc.EXCEPTION):
            required_res['description'] = 'Test msg 666'
            required_res['code'] = 123
            required_res['parents'] = hm.contains(*parents)
        else:
            required_res['description'] = 'Unknown error happened'

        hm.assert_that(
            response.get_json(),
            has_exact_entries(required_res),
        )

    @mock.patch('yb_snout_api.utils.ma_schemas.exception.LABEL_PREFIX', 'SNOUT3V')
    def test_label(self):
        def func():
            raise PlainException(666)

        with build_custom_resource_cxt(return_func=func):
            response = self.test_client.get(
                self.BASE_API,
                headers={'X-Request-Id': 666},
            )

        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        required_res = {
            'error': 'PlainException',
            'description': 'Test msg 666',
            'label': hm.starts_with('ERR-SNOUT3V-666-'),
        }
        hm.assert_that(
            response.get_json(),
            hm.has_entries(required_res),
        )

    def test_str_code(self):
        def func():
            raise StrCodeException()

        with build_custom_resource_cxt(return_func=func):
            response = self.test_client.get(self.BASE_API)

        hm.assert_that(response.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'code': 'abc',
                'error': 'StrCodeException',
            }),
        )
