# -*- coding: utf-8 -*-

from contextlib import contextmanager
from decimal import Decimal as D

import pytest
from yt.wrapper.mappings import FrozenDict

import balance.exc as exc
from balance.utils.partner_processing.jsonschema_logic import JSONSchemaParamsMixin


@contextmanager
def no_exception_ctx(*args, **kwargs):
    yield


def pytest_raises_ctx(*args, **kwargs):
    def wrapper():
        return pytest.raises(*args, **kwargs)
    return wrapper


@pytest.mark.parametrize(
    'params, expected_params, exception_context',

    [
        ########### Все параметры переданы ##############
        (
            {
                'prop_str__not_required__default__not_nullable': u'ёёё',
                'prop_number__not_required__not_default__not_nullable': 31.337,
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': 1
            },
            {
                'prop_str__not_required__default__not_nullable': u'ёёё',
                'prop_number__not_required__not_default__not_nullable': 31.337,
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': 1
            },
            None
        ),
        ################# Не передан парамет с дефолтным значением ################
        (
            {
                # 'prop_str__not_required__default__not_nullable': u'ёёё',
                'prop_number__not_required__not_default__not_nullable': 31.337,
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': None
            },
            {
                'prop_str__not_required__default__not_nullable': u'ёёё',
                'prop_number__not_required__not_default__not_nullable': 31.337,
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': None
            },
            None
        ),
        ################# Передан параметр None для которого None не разрешен ################
        (
            {
                'prop_str__not_required__default__not_nullable': None,
                'prop_number__not_required__not_default__not_nullable': 31.337,
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': None
            },
            None,
            pytest_raises_ctx(exc.COMMON_JSONSCHEMA_VALIDATION_EXCEPTION, match="Error during json schema validation: "
                                                                                "None is not of type 'string'")
        ),
        ########### Не передан необязательный параметр без дефолта ##############
        (
            {
                'prop_str__not_required__default__not_nullable': u'ёёё',
                # 'prop_number__not_required__not_default__not_nullable': 31.337,
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': 1
            },
            {
                'prop_str__not_required__default__not_nullable': u'ёёё',
                # 'prop_number__not_required__not_default__not_nullable': 31.337,
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': 1
            },
            None
        ),
        ########### Decimal как number ##############
        (
            {
                'prop_str__not_required__default__not_nullable': u'ёёё',
                'prop_number__not_required__not_default__not_nullable': D('31.337'),
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': 2
            },
            {
                'prop_str__not_required__default__not_nullable': u'ёёё',
                'prop_number__not_required__not_default__not_nullable': D('31.337'),
                'prop_str__required__no_default__not_nullable': 'ёёё',
                'prop_int__required__no_default__nullable': 2
            },
            None
        ),
    ]
)
def test_json_params(params, expected_params, exception_context):
    params_schema = {
        'type': 'object',
        'properties': {
            'prop_str__not_required__default__not_nullable': {
                'type': 'string',
                'default': u'ёёё',
            },
            'prop_number__not_required__not_default__not_nullable': {
                'type': 'number',
            },
            'prop_str__required__no_default__not_nullable': {
                'type': 'string',
            },
            'prop_int__required__no_default__nullable': {
                'anyOf': [
                    {'type': 'integer'},
                    {'type': 'null'}
                ],
            }
        },
        'required': [
            'prop_str__required__no_default__not_nullable',
            'prop_int__required__no_default__nullable'
        ]
    }
    if exception_context is None:
        exception_context = no_exception_ctx
    with exception_context():
        JSONSchemaParamsMixin.validate_and_set_default_params_by_schema(params_schema, params)
    if exception_context is not no_exception_ctx:
        return
    assert FrozenDict(params) == FrozenDict(expected_params)
