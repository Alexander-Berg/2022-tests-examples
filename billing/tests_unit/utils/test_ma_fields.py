# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import collections
from decimal import Decimal as D
import pytest
import io
import contextlib
import httpretty
import urllib
import hamcrest as hm
from http import client as http
from werkzeug import FileMultiDict
from marshmallow import ValidationError

from muzzle.captcha import URL as CAPTCHA_URL

from brest.core.typing import PassportId
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.utils import ma_fields, clean_dict
from yb_snout_api.utils.enum import Enum, IntEnum
from yb_snout_api.utils.ma_schemas.base import BaseSchema, fields
from yb_snout_api.utils.ma_schemas.date import DateBoundariesSchema
from yb_snout_api.utils.ma_schemas.auth import CaptchaSchema
from yb_snout_api.utils import deco


class A:
    def __init__(self, val):
        self.val = val


class TestCaseDecimalFields(TestCaseApiAppBase):
    @pytest.mark.parametrize(
        'decimals, normalize, in_val, out_val',
        [
            (6, True, D('0.001'), '0.001'),
            (6, False, D('0.001'), '0.001000'),
            (6, True, D('0.001000'), '0.001'),
            (6, True, D('01'), '1'),
            (6, False, D('01'), '1.000000'),
            (6, True, D('1.00'), '1'),
            (6, True, D('0.0000001'), '0'),
            (6, False, D('0.0000001'), '0.000000'),
            (6, True, D('0.000001'), '0.000001'),
            (6, True, D('1e-7'), '0'),
            (6, True, D('1e-6'), '0.000001'),
            (6, False, D('1e-6'), '0.000001'),
            (6, True, D('10e4'), '100000'),
            (6, False, D('10e4'), '100000.000000'),
            (6, True, D('55.5555555'), '55.555556'),
            (6, False, D('55.5555555'), '55.555556'),
            (6, True, D('55.5555545'), '55.555555'),
            (2, True, D('11.845'), '11.85'),
            (2, False, D('11.845'), '11.85'),
        ],
    )
    def test_decimal_serialize(
        self,
        decimals,
        normalize,
        in_val,
        out_val,
    ):
        field = ma_fields.DecimalField(decimals, normalize)
        hm.assert_that(
            field.serialize('val', A(in_val)),
            hm.equal_to(out_val),
        )

    @pytest.mark.parametrize(
        'in_val, out_val',
        [
            ('1.11', D('1.11')),
            ('1.5555', D('1.5555')),
            (1, D('1')),
            (1.5, D('1.5')),
            (b'1.5', D('1.5')),
        ],
    )
    def test_decimal_deserialize(self, in_val, out_val):
        """Независимо от объявленного количества знаков после запятой,
        всегда получает Decimal, который полностью соответсвует строке
        """
        field = ma_fields.DecimalField(2)
        assert field.deserialize(in_val) == out_val

    @pytest.mark.parametrize(
        'in_val, out_val',
        [
            ('1.11', D('1.11')),
            (' 1.11 ', D('1.11')),
            (' 1,11 ', D('1.11')),
            (b' 1,11 ', D('1.11')),
        ],
    )
    def test_decimal_different_splitters(self, in_val, out_val):
        field = ma_fields.DecimalField(2)
        assert field.deserialize(in_val) == out_val

    def test_invalid_number(self):
        field = ma_fields.DecimalField()
        with pytest.raises(ValidationError) as exc_info:
            field.deserialize('1.1a')
        assert exc_info.value.message == 'NUMBER_INVALID_FIELD_VALIDATION_ERROR'


class TestObjectIdField(TestCaseApiAppBase):
    field = ma_fields.ObjectIdField(PassportId)

    def test_serialize(self):
        val = self.field.serialize('val', A(123))
        assert val == 123

    def test_deserialize(self):
        val = self.field.deserialize('123')
        assert val == 123

    def test_validation(self):
        with pytest.raises(ValidationError) as exc_info:
            self.field.deserialize('-1')
        assert exc_info.value.message == 'POSITIVE_FIELD_VALIDATION_ERROR'


class TestEnumField(TestCaseApiAppBase):

    class TestType(Enum):
        KEY1 = 'VAL_1'

    class TestIntType(IntEnum):
        KEY1 = 1

    str_field = ma_fields.EnumField(TestType)
    int_field = ma_fields.EnumField(TestIntType)

    @pytest.mark.parametrize(
        'field, res',
        [
            pytest.param(str_field, 'VAL_1', id='str_field'),
            pytest.param(int_field, 1, id='int_field'),
        ],
    )
    def test_deserialize(self, field, res):
        assert field.deserialize('KEY1') == res

    @pytest.mark.parametrize(
        'field',
        [
            pytest.param(str_field, id='str_field'),
            pytest.param(int_field, id='int_field'),
        ],
    )
    def test_invalid_deserialize(self, field):
        with pytest.raises(ValidationError) as exc_info:
            field.deserialize('INVALID')
        assert exc_info.value.message == 'ENUM_KEY_FIELD_VALIDATION_ERROR'

    @pytest.mark.parametrize(
        'field, res',
        [
            pytest.param(str_field, 'VAL_1', id='str_field'),
            pytest.param(int_field, 1, id='int_field'),
        ],
    )
    def test_make_upper_success(self, field, res):
        field.make_upper = True
        assert field.deserialize('key1') == res

    @pytest.mark.parametrize(
        'field',
        [
            pytest.param(str_field, id='str_field'),
            pytest.param(int_field, id='int_field'),
        ],
    )
    def test_make_error_success(self, field):
        field.make_upper = False
        with pytest.raises(ValidationError) as exc_info:
            field.deserialize('key1')
        assert exc_info.value.message == 'ENUM_KEY_FIELD_VALIDATION_ERROR'

    def test_none(self):
        field = ma_fields.EnumField(self.TestType, allow_none=True)
        res = field.deserialize(None)
        assert res is None

    def test_none_error(self):
        with pytest.raises(ValidationError) as exc_info:
            self.str_field.deserialize(None)
        assert exc_info.value.message == 'EMPTY_FIELD_VALIDATION_ERROR'

    @pytest.mark.parametrize(
        'req_val, res_val',
        [
            (None, None),
            (1, 'KEY1'),
            (2, 'UNKNOWN_VALUE'),
        ],
    )
    def test_serialize_int(self, req_val, res_val):
        field = ma_fields.EnumField(self.TestIntType, allow_none=True)
        assert field.serialize('val', A(req_val)) == res_val

    @pytest.mark.parametrize(
        'req_val, res_val',
        [
            (None, None),
            ('VAL_1', 'KEY1'),
            ('VAL_2', 'UNKNOWN_VALUE'),
        ],
    )
    def test_serialize_str(self, req_val, res_val):
        field = ma_fields.EnumField(self.TestType, allow_none=True)
        assert field.serialize('val', A(req_val)) == res_val


class TestDateBoundariesSchema(TestCaseApiAppBase):
    date_schema = DateBoundariesSchema()

    @pytest.mark.parametrize(
        'dt_from, dt_to',
        [
            ('2020-07-13T13:23:21', None),
            (None, '2020-07-13T13:34:51'),
            ('2020-07-13T13:23:21', '2020-07-13T13:34:51'),
        ],
    )
    def test_correct_validation(self, dt_from, dt_to):
        data = clean_dict({
            'dt_from': dt_from,
            'dt_to': dt_to,
        })
        res = self.date_schema.load(data)

        match_data = {}
        if dt_from:
            match_data['dt_from'] = datetime.datetime(2020, 7, 13, 13, 23, 21)
        if dt_to:
            match_data['dt_to'] = datetime.datetime(2020, 7, 13, 13, 34, 51)
        hm.assert_that(
            res,
            hm.has_properties({
                'data': hm.has_entries(date_boundary=hm.has_properties(match_data)),
                'errors': hm.empty(),
            }),
        )

    def test_failed(self):
        res, errors = self.date_schema.load({
            'dt_from': '2020-07-13T13:23:21',
            'dt_to': '2020-07-11T13:23:21',
        })
        hm.assert_that(
            errors,
            hm.has_entries({
                'dt_to': hm.contains('DATE_BOUNDARIES_FIELD_VALIDATION_ERROR'),
            }),
        )


class TestFileField(TestCaseApiAppBase):
    @staticmethod
    def _get_file(filename='test.jpg'):
        d = FileMultiDict()
        d.add_file('file', io.BytesIO(b"abcdef"), filename)
        return d.get('file')

    @pytest.mark.parametrize(
        'filename, mimetype',
        [
            ('test.jpg', 'image/jpeg'),
            ('test.png', 'image/png'),
            ('test.xml', 'application/xml'),
            ('test.xsl', 'application/xslt+xml'),
            ('test.pdf', 'application/pdf'),
            ('test.doc', 'application/msword'),
            ('test.xls', 'application/vnd.ms-excel'),
            ('test.txt', 'text/plain'),
            ('test', 'application/octet-stream'),
            ('test.exe', 'application/x-msdos-program'),
            ('test.sh', 'text/x-sh'),
        ],
    )
    def test_file_deserialize(self, filename, mimetype):
        field = ma_fields.FileField()
        res = field.deserialize(self._get_file(filename))
        hm.assert_that(
            res,
            hm.contains(
                filename,
                mimetype,
                b"abcdef",
            ),
        )

    def test_invalid_type(self):
        field = ma_fields.FileField()
        with pytest.raises(ValidationError) as exc_info:
            field.deserialize('INVALID')
        assert exc_info.value.message == 'FILE_INVALID_FIELD_VALIDATION_ERROR'

    @pytest.mark.parametrize(
        'filename, res_filename, mimetype',
        [
            pytest.param('../../../balance/scripts/remove_db.sh', 'balance_scripts_remove_db.sh', 'text/x-sh', id='sh'),
            pytest.param('<script>alert("Panic!");</script>', 'scriptalertPanic_script', 'application/octet-stream', id='js'),
        ],
    )
    def test_insecure_filename(self, filename, res_filename, mimetype):
        field = ma_fields.FileField()
        res = field.deserialize(self._get_file(filename))
        hm.assert_that(
            res,
            hm.contains(
                res_filename,
                mimetype,
                b"abcdef",
            ),
        )

    @pytest.mark.parametrize(
        'filename, ok',
        [
            ('test.txt', True),
            ('test.doc', True),
            ('test', False),
            ('test.jpg', False),
        ],
    )
    def test_allowed_extensions(self, filename, ok):
        field = ma_fields.FileField(allowed_extensions=['txt', 'doc'], description='Morning!')
        field.description = 'Morning!\nAllowed extensions: txt, doc'

        if ok:
            field.deserialize(self._get_file(filename))

        else:
            with pytest.raises(ValidationError) as exc_info:
                field.deserialize(self._get_file(filename))
            assert exc_info.value.message == 'FILE_EXTENSION_FIELD_VALIDATION_ERROR'


@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestCaptchaSchema(TestCaseApiAppBase):
    ENDPOINT = 'test_snout'
    ROUTE_URL = '/test_url'
    URL = '/v1' + ROUTE_URL

    @staticmethod
    def _register_captcha_checking(status_code=http.OK, body_res='ok'):
        httpretty.register_uri(
            httpretty.GET,
            CAPTCHA_URL + '/check',
            '''<?xml version="1.0"?><image_check>%s</image_check>''' % body_res,
            status=status_code,
        )

    @contextlib.contextmanager
    def build_custom_resource_cxt(self, for_admin_ui=False, for_client_ui=False):
        from yb_snout_api.servant import flask_app, resource_groups
        from yb_snout_api.core.resource import Resource, ClientAccessMixin

        v1_api = resource_groups['v1']

        class SnoutTestResource(ClientAccessMixin, Resource):
            @deco.add_schema_doc(
                v1_api,
                CaptchaSchema(
                    strict=True,
                    for_admin_ui=for_admin_ui,
                    for_client_ui=for_client_ui,
                ),
            )
            def get(self):
                return 'Success'

        v1_api.add_resource(SnoutTestResource, self.ROUTE_URL, endpoint=self.ENDPOINT)

        try:
            yield SnoutTestResource

        finally:
            del flask_app.view_functions['.'.join([v1_api.app.name, self.ENDPOINT])]

    def test_assert_error(self):
        with pytest.raises(AssertionError):
            with self.build_custom_resource_cxt():
                self.test_client.get(self.URL)

    @pytest.mark.parametrize('is_admin', [True, False])
    @pytest.mark.parametrize('field', ['_captcha_key', '_captcha_rep'])
    def test_lack_fields(self, is_admin, field):
        builder_params = {'for_admin_ui' if is_admin else 'for_client_ui': True}
        params = {'_captcha_key': 'key', '_captcha_rep': 'rep'}
        del params[field]

        with self.build_custom_resource_cxt(**builder_params):
            res = self.test_client.get(self.URL, params, is_admin=is_admin)
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        data = res.get_json()
        hm.assert_that(
            data,
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    field: hm.contains(hm.has_entries({
                        'error': u'REQUIRED_FIELD_VALIDATION_ERROR',
                        'description': 'Missing data for required field.',
                    })),
                }),
            }),
        )

    @pytest.mark.parametrize('is_admin', [True, False])
    def test_skip_checking_captcha(self, is_admin):
        builder_params = {'for_admin_ui' if is_admin else 'for_client_ui': True}

        with self.build_custom_resource_cxt(**builder_params):
            res = self.test_client.get(self.URL, is_admin=not is_admin)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(res.get_json()['data'], hm.equal_to('Success'))

    @pytest.mark.parametrize('is_admin', [True, False])
    def test_success(self, is_admin):
        builder_params = {'for_admin_ui' if is_admin else 'for_client_ui': True}
        params = {'_captcha_key': 'key', '_captcha_rep': 'rep'}
        self._register_captcha_checking()

        with self.build_custom_resource_cxt(**builder_params):
            res = self.test_client.get(self.URL, params, is_admin=is_admin)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(res.get_json()['data'], hm.equal_to('Success'))
        last_request = httpretty.last_request()
        assert last_request.method == 'GET'
        path, qs = urllib.splitquery(last_request.path)
        assert path == '/check'
        hm.assert_that(
            qs.split('&'),
            hm.contains_inanyorder(
                'rep=rep',
                'key=key',
                'type=std',
            ),
        )

    def test_resource_anavailable(self):
        params = {'_captcha_key': 'key', '_captcha_rep': 'rep'}
        self._register_captcha_checking(http.BAD_GATEWAY)

        with self.build_custom_resource_cxt(for_admin_ui=True):
            res = self.test_client.get(self.URL, params)
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'CAPTCHA_API_UNAVAILABLE',
                'description': 'Captcha API is unavailable',
            }),
        )

    @pytest.mark.parametrize(
        'rep, res_rep',
        [
            pytest.param('rep', 'rep', id='utf8'),
            pytest.param('😀', '%F0%9F%98%80', id='uni'),
        ],
    )
    def test_invalid_captcha(self, rep, res_rep):
        params = {'_captcha_key': 'key', '_captcha_rep': rep}
        self._register_captcha_checking(body_res='failed')

        with self.build_custom_resource_cxt(for_admin_ui=True):
            res = self.test_client.get(self.URL, params)
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'INVALID_CAPTCHA',
                'description': 'Captcha is invalid',
            }),
        )
        last_request = httpretty.last_request()
        assert last_request.method == 'GET'
        path, qs = urllib.splitquery(last_request.path)
        assert path == '/check'
        hm.assert_that(
            qs.split('&'),
            hm.contains_inanyorder(
                'rep=%s' % res_rep,
                'key=key',
                'type=std',
            ),
        )


class CustomSchema(BaseSchema):
    name = fields.String()
    age = fields.Integer()


class TestRecursiveDictField(TestCaseApiAppBase):
    dict_field = ma_fields.RecursiveDict(fields.Nested(CustomSchema()))
    default_dict_field = fields.Dict()
    client_schema = collections.namedtuple('Client', ['name', 'age'])

    def test_ok(self):
        client1 = self.client_schema('Fry', 1000000)
        client2 = self.client_schema('Leela', 19)
        res = self.dict_field.serialize('val', A({'client1': client1, 'client2': client2}))
        hm.assert_that(
            res,
            hm.has_entries({
                'client1': hm.has_entries({'name': 'Fry', 'age': 1000000}),
                'client2': hm.has_entries({'name': 'Leela', 'age': 19}),
            }),
        )

    def test_invalid(self):
        client = self.client_schema('Fray', 'invalid int')
        with pytest.raises(ValidationError) as exc_info:
            self.dict_field.serialize('val', A({'client': client}))
        hm.assert_that(
            exc_info.value.message,
            hm.has_entries({
                'age': hm.contains('INTEGER_INVALID_FIELD_VALIDATION_ERROR'),
            }),
        )

    def test_recursive_ok(self):
        client1 = self.client_schema('Fry', 1000000)
        client2 = self.client_schema('Leela', 19)
        res = self.dict_field.serialize('val', A({'client1': [client1], 'client2': [{'0': [client1, client2]}]}))
        hm.assert_that(
            res,
            hm.has_entries({
                'client1': hm.contains(hm.has_entries({'name': 'Fry', 'age': 1000000})),
                'client2': hm.contains(
                    hm.has_entries({
                        '0': hm.contains(
                            hm.has_entries({'name': 'Fry', 'age': 1000000}),
                            hm.has_entries({'name': 'Leela', 'age': 19}),
                        ),
                    }),
                ),
            }),
        )
