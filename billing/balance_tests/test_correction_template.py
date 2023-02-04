# -*- coding: utf-8 -*-

import hamcrest as hm
import mock
import pytest
from datetime import datetime
from balance import correction_template
from balance.utils.ya_bunker import BunkerRepository


TEST_CORRECTION_TEMPLATE = {
    'title': 'Стандартный шаблон',
    'fields': [
        {
            'name': 'partner_id',
            'type': 'integer',
            'title': 'Partner_ID',
            'required': True,
            'values': []
        },
        {
            'name': 'service_id',
            'type': 'integer',
            'title': 'ID_Service',
            'required': True,
            'values': [121]
        }
    ]
}


TEST_CORRECTION_TEMPLATES_REF = {
    'name': 'active',
    'mime': 'application/json; charset=utf-8; schema=\"bunker:/template#\"',
    'fullName': '/active'
}


class MockBunkerRepository(object):
    @staticmethod
    def get(*args, **kwargs):
        return TEST_CORRECTION_TEMPLATE

    @staticmethod
    def list_with_references(*args, **kwargs):
        return [TEST_CORRECTION_TEMPLATE], [TEST_CORRECTION_TEMPLATES_REF]


@pytest.fixture(autouse=True)
def mock_required_fields():
    # для упрощения тестов подменим обязательные поля.
    correction_template.TemplateWrapper.REQUIRED_FIELDS = [
        'contract_id', 'partner_id', 'service_id', 'paysys_type_cc', 'transaction_type'
    ]


def test_list_templates(session):
    mock.patch.object(BunkerRepository, '_get_bunker_client').start()
    mock.patch.object(BunkerRepository, 'list_with_references',
                      return_value=([TEST_CORRECTION_TEMPLATE], [TEST_CORRECTION_TEMPLATES_REF])).start()

    templates = correction_template.list_templates()

    hm.assert_that(templates, hm.has_length(1))
    hm.assert_that([t.get_prepared_template() for t in templates], hm.contains(TEST_CORRECTION_TEMPLATE))


@pytest.mark.parametrize('template_cfg, fields, with_error, expected_error', [
    # проверки соответствия полей шаблону
    pytest.param(
        {
            'title': 'Шаблон c несколькими полями',
            'fields': [
                {
                    'name': 'partner_id',
                    'type': 'integer',
                    'title': 'Partner_ID',
                    'required': True,
                    'values': [1, 2, 3]
                },
                {
                    'name': 'paysys_type_cc',
                    'type': 'number',
                    'title': 'Paysys_type_cc',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'transaction_type',
                    'type': 'string',
                    'title': 'Transaction_Type',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'service_id',
                    'type': 'boolean',
                    'title': 'Service_ID',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'invoice_id',
                    'type': 'datetime',
                    'title': 'Invoice_ID',
                    'required': False,
                    'values': []
                },
            ]
        },
        {
            'partner_id': 1,
            'contract_id': 2,
            'service_id': 215,
            'paysys_type_cc': 'direct_card',
            'transaction_type': 'payment',
            'invoice_id': 'B-1201',
        },
        False,
        '',
        id='Check availability. All request fields available. Ok'
    ),
    pytest.param(
        {
            'title': 'Шаблон с недоступным вообще полем',
            'fields': [
                {
                    'name': 'order_service_id',
                    'type': 'number',
                    'title': 'Нет в Template.AVAILABLE_FIELDS',
                    'required': True,
                    'values': []
                }
            ]
        },
        {
            'order_service_id': '1060'
        },
        True,
        'Next fields are\'nt available (order_service_id)',
        id='Check availability. Nonexistent field in request. Error'
    ),
    pytest.param(
        {
            'title': 'Шаблон с недоступным в шаблоне полем',
            'fields': [
                {
                    'name': 'partner_id',
                    'type': 'number',
                    'title': 'Нет в текущем шаблоне',
                    'required': True,
                    'values': []
                }
            ]
        },
        {
            'payment_id': '1060'
        },
        True,
        'Next fields are\'nt available (payment_id)',
        id='Check availability. Unavailable field in request. Error'
    ),
    # end
    # проверки обязательности
    pytest.param(
        {
            'title': 'Шаблон с обязательным и необязательным полем',
            'fields': [
                {
                    'name': 'partner_id',
                    'type': 'number',
                    'title': 'Partner_ID',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'service_id',
                    'type': 'number',
                    'title': 'Service_ID',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'payment_id',
                    'type': 'number',
                    'title': 'Payment_ID',
                    'required': False,
                    'values': []
                }
            ]
        },
        {
            'partner_id': 1,
            'contract_id': '1050',
            'service_id': 131,
            'payment_id': '1060',
            'paysys_type_cc': 'direct_card',
            'transaction_type': 'payment',
        },
        False,
        '',
        id='Check required. Required and optional fields in request. Ok'
    ),
    pytest.param(
        {
            'title': 'Шаблон с обязательным и необязательным полем',
            'fields': [
                {
                    'name': 'partner_id',
                    'type': 'number',
                    'title': 'Contract_ID',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'service_id',
                    'type': 'boolean',
                    'title': 'Service_ID',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'payment_id',
                    'type': 'number',
                    'title': 'Не укажем необязательное',
                    'required': False,
                    'values': []
                }
            ]
        },
        {
            'partner_id': 100,
            'contract_id': '1050',
            'service_id': 131,
            'paysys_type_cc': 'direct_card',
            'transaction_type': 'payment',
        },
        False,
        '',
        id='Check required. Without optional fields in request. Ok'
    ),
    pytest.param(
        {
            'title': 'Шаблон с обязательным и необязательным полем',
            'fields': [
                {
                    'name': 'partner_id',
                    'type': 'number',
                    'title': 'Не укажем обязательное',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'service_id',
                    'type': 'boolean',
                    'title': 'Service_ID',
                    'required': True,
                    'values': []
                },
                {
                    'name': 'payment_id',
                    'type': 'number',
                    'title': 'Payment_ID',
                    'required': False,
                    'values': []
                }
            ]
        },
        {
            'partner_id': 1,
            'contract_id': 1,
            'payment_id': '1050',
        },
        True,
        'Field "service_id" required',
        id='Check required. Without required fields in request. Error'
    ),
    # end
    # проверки доступности полей
    pytest.param(
        {
            'title': 'Шаблон с доступными значениями',
            'fields': [
                {
                    'name': 'partner_id',
                    'type': 'number',
                    'title': 'Partner_ID',
                    'required': True,
                    'values': ['0', '1', '2'],
                },
                {
                    'name': 'service_id',
                    'type': 'boolean',
                    'title': 'Service_ID',
                    'required': True,
                    'values': []
                },
            ]
        },
        {
            'partner_id': 1,
            'contract_id': 1,
            'service_id': 131,
            'paysys_type_cc': 'direct_card',
            'transaction_type': 'payment',
        },
        False,
        '',
        id='Check available values for field. Value in. Ok'
    ),
    pytest.param(
        {
            'title': 'Шаблон с доступными значениями',
            'fields': [
                {
                    'name': 'partner_id',
                    'type': 'string',
                    'title': 'Contract_ID',
                    'required': True,
                    'values': ['a', 'b', 'c'],
                },
                {
                    'name': 'service_id',
                    'type': 'boolean',
                    'title': 'Service_ID',
                    'required': True,
                    'values': []
                },
            ]
        },
        {
            'partner_id': 1,
            'contract_id': 'd',
            'service_id': 131,
            'paysys_type_cc': 'direct_card',
            'transaction_type': 'payment',
        },
        True,
        'Only (a,b,c) values for field "partner_id" available',
        id='Check available values for field. Value missed. Error'
    ),
    # end
])
def test_template_checking(template_cfg, fields, with_error, expected_error):
    template = correction_template.TemplateWrapper('test', template_cfg, session=mock.MagicMock())
    if with_error:
        with pytest.raises(ValueError) as e:
            template.check(fields)
        hm.assert_that(e.value.message, hm.equal_to_ignoring_case(expected_error))
    else:
        template.check(fields)


@pytest.mark.parametrize('template_cfg, fields, expected', [
    [
        {
            'title': 'Шаблон с числовым значением',
            'fields': [
                {
                    'name': 'contract_id',
                    'type': 'integer',
                    'title': 'Contract_ID',
                    'required': True,
                },
                {
                    'name': 'payment_type',
                    'type': 'number',
                    'title': 'Payment_Type',
                    'required': True,
                }
            ]
        },
        {
            'contract_id': '100',
            'payment_type': '100.2'
        },
        {
            'contract_id': 100,
            'payment_type': 100.2
        },
    ],
    [
        {
            'title': 'Шаблон со строковым значением',
            'fields': [
                {
                    'name': 'contract_id',
                    'type': 'string',
                    'title': 'Contract_ID',
                    'required': True,
                },
                {
                    'name': 'payment_type',
                    'type': 'string',
                    'title': 'Payment_Type',
                    'required': True,
                },
                {
                    'name': 'comments',
                    'type': 'string',
                    'title': 'Comments',
                    'required': True,
                }
            ]
        },
        {
            'contract_id': 100.21,
            'payment_type': 'payment',
            'comments': 'test'
        },
        {
            'contract_id': '100.21',
            'payment_type': 'payment',
            'comments': 'test'
        },
    ],
    [
        {
            'title': 'Шаблон с булевыми значениями',
            'fields': [
                {
                    'name': 'contract_id',
                    'type': 'boolean',
                    'title': 'Contract_ID',
                    'required': True,
                },
                {
                    'name': 'payment_type',
                    'type': 'boolean',
                    'title': 'Payment_Type',
                    'required': True,
                }
            ]
        },
        {
            'contract_id': 1,
            'payment_type': '0'
        },
        {
            'contract_id': True,
            'payment_type': False
        },
    ],
    [
        {
            'title': 'Шаблон с датами',
            'fields': [
                {
                    'name': 'contract_id',
                    'type': 'datetime',
                    'title': 'Contract_ID',
                    'required': True,
                },
                {
                    'name': 'payment_type',
                    'type': 'datetime',
                    'title': 'Payment_Type',
                    'required': True,
                }
            ]
        },
        {
            'contract_id': '2020-01-01 00:00:00',
            'payment_type': '20200202'
        },
        {
            'contract_id': datetime(2020, 1, 1),
            'payment_type': datetime(2020, 2, 2)
        },
    ],
    [
        {
            'title': 'Шаблон с числовым значением',
            'fields': [
                {
                    'name': 'CONTRACT_ID',
                    'type': 'number',
                    'title': 'Contract_ID',
                    'required': True,
                },
                {
                    'name': 'PAYMENT_TYPE',
                    'type': 'number',
                    'title': 'Payment_Type',
                    'required': True,
                }
            ]
        },
        {
            'Contract_ID': '100.20',
            'Payment_Type': 12
        },
        {
            'contract_id': 100.2,
            'payment_type': 12
        },
    ],
], ids=['Parsing Numbers', 'Parsing Strings', 'Parsing Booleans', 'Parsing dates', 'Lowercase field conversion'])
def test_template_apply(template_cfg, fields, expected):
    mock.patch.object(correction_template.TemplateWrapper, '_get_inner_fields', return_value=dict()).start()

    template = correction_template.TemplateWrapper('test', template_cfg, session=mock.MagicMock())
    res = template.apply_to(fields, session=None)
    hm.assert_that(res, hm.equal_to(expected))
