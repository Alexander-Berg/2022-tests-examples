import json
import logging
from base64 import b64encode

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_items, has_properties

from pay.lib.utils.uaas import UaasParser

API_UAAS_HANDLER = 'test_pay_handler'


@pytest.fixture
def uaas_headers():
    pay_testitem1 = [
        {
            'HANDLER': API_UAAS_HANDLER,
            'CONTEXT': {
                'MAIN': {
                    API_UAAS_HANDLER: {
                        'yandex_pay_plus.cashback_category': '0.15',
                    }
                }
            },
            'TESTID': ['1', '2'],
        },
    ]
    pay_testitem2 = [
        {
            'HANDLER': API_UAAS_HANDLER,
            'CONTEXT': {
                'MAIN': {
                    API_UAAS_HANDLER: {
                        'yandex_pay_plus.something_else': r'¯\_(ツ)_/¯',
                    }
                }
            },
            'TESTID': ['3', '2'],
        },
    ]
    other_testitem = [
        {
            'HANDLER': 'OTHER',
            'CONTEXT': {
                'MAIN': {
                    API_UAAS_HANDLER: {'setting': 'should_not_be_seen'},
                }
            },
        }
    ]
    flags = ','.join(
        b64encode(json.dumps(each).encode()).decode() for each in (other_testitem, pay_testitem1, pay_testitem2)
    )
    return {
        'X-Yandex-ExpFlags': flags,
        'X-Yandex-ExpBoxes': '398290,0,-1;398773,0,-1',
        'X-Yandex-ExpBoxes-Crypted': '7Dra0n87zDsekbtTguWFqIVGOOV-gAYf',
        'X-Yandex-ExpConfigVersion': '16209',
        'X-Yandex-ExpSplitParams': 'fake',
        'X-Yandex-LogstatUID': '123',
    }


@pytest.mark.asyncio
async def test_exp_flags(uaas_headers, dummy_logger):
    exp_flags = UaasParser(uaas_headers, API_UAAS_HANDLER, dummy_logger).exp_flags

    assert_that(
        exp_flags,
        equal_to(
            {
                'yandex_pay_plus.cashback_category': '0.15',
                'yandex_pay_plus.something_else': r'¯\_(ツ)_/¯',
            }
        ),
    )


@pytest.mark.asyncio
async def test_exp_flags_parsing_logged(uaas_headers, caplog, dummy_logger):
    caplog.set_level(logging.INFO)

    UaasParser(uaas_headers, API_UAAS_HANDLER, dummy_logger).exp_flags

    assert_that(
        caplog.records,
        has_items(
            has_properties(
                message='UAAS headers',
                name='dummy_logger',
                levelno=logging.INFO,
                _context=has_entries(uaas=uaas_headers),
            ),
            has_properties(
                message='UAAS flags parsed',
                name='dummy_logger',
                levelno=logging.INFO,
                _context=has_entries(
                    triggered_testids=['1', '2', '3'],
                    flags={
                        'yandex_pay_plus.cashback_category': '0.15',
                        'yandex_pay_plus.something_else': r'¯\_(ツ)_/¯',
                    },
                ),
            ),
        ),
    )


@pytest.mark.asyncio
async def test_exp_flags_parsing_errors_logged(uaas_headers, caplog, dummy_logger):
    caplog.set_level(logging.INFO)
    bad_content = [{'HANDLER': API_UAAS_HANDLER, 'CONTEXT': []}]

    invalid_flags = [
        'not_b64_encoded',
        '',
        b64encode(b"not a json").decode(),
    ]
    uaas_headers['X-Yandex-ExpFlags'] = ','.join(
        [
            uaas_headers["X-Yandex-ExpFlags"],
            *invalid_flags,
            b64encode(json.dumps(bad_content).encode()).decode(),
            uaas_headers["X-Yandex-ExpFlags"],
        ]
    )

    UaasParser(uaas_headers, API_UAAS_HANDLER, dummy_logger).exp_flags

    def _invalid_flag(flag):
        return has_properties(
            message='Invalid exp flag',
            name='dummy_logger',
            levelno=logging.ERROR,
            _context=has_entries(invalid_flag=flag),
        )

    assert_that(
        caplog.records,
        has_items(
            *(_invalid_flag(flag) for flag in invalid_flags),
            has_properties(
                message='Invalid test item content',
                name='dummy_logger',
                levelno=logging.ERROR,
            ),
            has_properties(
                message='Duplicate test flag',
                name='dummy_logger',
                levelno=logging.WARNING,
                _context=has_entries(key='yandex_pay_plus.cashback_category'),
            ),
            has_properties(
                message='Duplicate test flag',
                name='dummy_logger',
                levelno=logging.WARNING,
                _context=has_entries(key='yandex_pay_plus.something_else'),
            ),
        ),
    )
