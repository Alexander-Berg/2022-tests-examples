import os

import pytest

import yt.wrapper as yt


PATH_v_firm_tax_dynamic = "//home/balance/test/new_billing/references/v_firm_tax_dynamic"
SCHEMA_v_firm_tax_dynamic = [{
    'name': 'id',
    'required': False,
    'sort_order': 'ascending',
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'title',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'export_type',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'oebs_org_id',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'oebs_user_id',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'default_currency',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'contract_id',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'unilateral',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'invoice_paysys_id',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'postpay',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'nds_pct',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'email',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'phone',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'payment_invoice_email',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'alter_permition_code',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'pa_prefix',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'region_id',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'config',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': 'currency_rate_src',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'inn',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'tax_policy_id',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'kpp',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'legaladdress',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'default_iso_currency',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'test_env',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': 'mdh_id',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'mnclose_email',
    'required': False,
    'type': 'utf8',
    'type_v3': {
        'type_name': 'optional',
        'item': 'utf8'
    }
}, {
    'name': 'tax_policies',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': 'person_categories',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}]
PATH_currency_rate = "//home/balance/test/transfer/currency/_balance_test_iso-currency-rate"
SCHEMA_currency_rate = [{
    'name': 'src_cc',
    'required': False,
    'sort_order': 'ascending',
    'type': 'string',
    'type_v3': {
        'type_name': 'optional',
        'item': 'string'
    }
}, {
    'name': 'dt',
    'required': False,
    'sort_order': 'ascending',
    'type': 'uint64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint64'
    }
}, {
    'name': 'iso_currency_from',
    'required': False,
    'sort_order': 'ascending',
    'type': 'string',
    'type_v3': {
        'type_name': 'optional',
        'item': 'string'
    }
}, {
    'name': 'iso_currency_to',
    'required': False,
    'sort_order': 'ascending',
    'type': 'string',
    'type_v3': {
        'type_name': 'optional',
        'item': 'string'
    }
}, {
    'name': 'obj',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': 'id',
    'required': False,
    'type': 'uint64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint64'
    }
}, {
    'name': '_rest',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': '_timestamp',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': '_partition',
    'required': False,
    'type': 'string',
    'type_v3': {
        'type_name': 'optional',
        'item': 'string'
    }
}, {
    'name': '_offset',
    'required': False,
    'type': 'uint64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint64'
    }
}, {
    'name': '_idx',
    'required': False,
    'type': 'uint32',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint32'
    }
}]
PATH_balance_contract = "//home/balance/test/transfer/contracts/_balance_test_contract"
SCHEMA_balance_contract = [{
    'name': 'id',
    'required': False,
    'sort_order': 'ascending',
    'type': 'uint64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint64'
    }
}, {
    'name': 'obj',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': 'client_id',
    'required': False,
    'type': 'uint64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint64'
    }
}, {
    'name': 'version',
    'required': False,
    'type': 'int64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'int64'
    }
}, {
    'name': '_rest',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': '_timestamp',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': '_partition',
    'required': False,
    'type': 'string',
    'type_v3': {
        'type_name': 'optional',
        'item': 'string'
    }
}, {
    'name': '_offset',
    'required': False,
    'type': 'uint64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint64'
    }
}, {
    'name': '_idx',
    'required': False,
    'type': 'uint32',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint32'
    }
}]
PATH_mdh_products = "//home/balance/test/transfer/products/mdh_test_domains_nomenclature_nom_product"
SCHEMA_mdh_products = [{
    'name': 'master_uid',
    'required': False,
    'sort_order': 'ascending',
    'type': 'string',
    'type_v3': {
        'type_name': 'optional',
        'item': 'string'
    }
}, {
    'name': 'obj',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}, {
    'name': 'version',
    'required': False,
    'type': 'uint64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint64'
    }
}, {
    'name': 'ErrorMessage',
    'required': False,
    'type': 'string',
    'type_v3': {
        'type_name': 'optional',
        'item': 'string'
    }
}, {
    'name': 'ParsingStage',
    'required': False,
    'type': 'string',
    'type_v3': {
        'type_name': 'optional',
        'item': 'string'
    }
}, {
    'name': 'Successed',
    'required': False,
    'type': 'boolean',
    'type_v3': {
        'type_name': 'optional',
        'item': 'bool'
    }
}, {
    'name': 'id',
    'required': False,
    'type': 'uint64',
    'type_v3': {
        'type_name': 'optional',
        'item': 'uint64'
    }
}, {
    'name': '_rest',
    'required': False,
    'type': 'any',
    'type_v3': {
        'type_name': 'optional',
        'item': 'yson'
    }
}]

FIRM_yandex = {
    'id': 1,
    'title': 'ООО «Яндекс»',
    'export_type': 'OEBS',
    'oebs_org_id': 121,
    'oebs_user_id': 20678,
    'default_currency': 'RUR',
    'contract_id': None,
    'unilateral': 1,
    'invoice_paysys_id': None,
    'postpay': 0,
    'nds_pct': None,
    'email': 'info@balance.yandex.ru',
    'phone': '(495) 739–22–22 добавочный 2345',
    'payment_invoice_email': 'payment-invoice@yandex-team.ru',
    'alter_permition_code': 'AlterFirmRU',
    'pa_prefix': 'ЛС',
    'region_id': 225,
    'config': {
        'mnclose_close_tasks': ['monthly_close_firms']
    },
    'currency_rate_src': 'cbr',
    'inn': '7736207543',
    'tax_policy_id': None,
    'kpp': '997750001',
    'legaladdress': '19021, Россия, г. Москва, ул. Льва Толстого, д. 16',
    'default_iso_currency': 'RUB',
    'test_env': 0,
    'mdh_id': '146c1b7a-1d6e-4c80-9b59-acb34c6d6dc6',
    'mnclose_email': '2320@direct.yandex.ru',
    'tax_policies': [{
        'hidden': 0,
        'resident': 1,
        'default_tax': 1,
        'id': 1,
        'mdh_id': '93fb537f-e424-49b6-8b3a-b735f9164708',
        'spendable_nds_id': 18,
        'region_id': 225,
        'name': 'Россия, резидент, НДС облагается',
        'percents': [{
            'dt': '2019-01-01T00:00:00+03:00',
            'nsp_pct': 0,
            'nds_pct': 20,
            'id': 281,
            'hidden': 0,
            'mdh_id': '91f77dca-81d8-4f6e-b3d1-04fa78ce6b50'
        }, {
            'dt': '2003-01-01T00:00:00+03:00',
            'nsp_pct': 5,
            'nds_pct': 20,
            'id': 2,
            'hidden': 0,
            'mdh_id': '9feab74e-4252-4507-a572-124304d9b9eb'
        }, {
            'dt': '2004-01-01T00:00:00+03:00',
            'nsp_pct': 0,
            'nds_pct': 18,
            'id': 1,
            'hidden': 0,
            'mdh_id': 'cd174c94-b588-451a-81df-78d765bd258f'
        }]
    }, {
        'hidden': 1,
        'resident': 1,
        'default_tax': 0,
        'id': 203,
        'mdh_id': '418f42d1-552f-411d-a110-c851d961c19b',
        'name': 'Без НДС (осв.) - медицина',
        'region_id': 225,
        'percents': [{
            'dt': '2019-01-01T00:00:00+03:00',
            'nsp_pct': 0,
            'nds_pct': 0,
            'id': 343,
            'hidden': 0,
            'mdh_id': '855c453c-7bf6-48b7-9b80-94b043198cf3'
        }]
    }, {
        'hidden': 1,
        'resident': 1,
        'default_tax': 0,
        'id': 12,
        'mdh_id': '10cad9b2-4e02-4502-a440-3c002b2caf87',
        'name': 'Резидент РФ для Авто.ру',
        'region_id': 225,
        'percents': [{
            'dt': '2015-01-01T00:00:00+03:00',
            'nsp_pct': 0,
            'nds_pct': 18,
            'id': 101,
            'hidden': 1,
            'mdh_id': '18311966-8888-47af-93cb-aef87f0234e7'
        }]
    }, {
        'hidden': 0,
        'resident': 1,
        'default_tax': 0,
        'id': 11,
        'mdh_id': '67d9783c-9d14-4ae9-b526-8e8f5186d2b4',
        'name': 'Россия, резидент, НДС не облагается',
        'region_id': 225,
        'percents': [{
            'dt': '2004-01-01T00:00:00+03:00',
            'nsp_pct': 0,
            'nds_pct': 0,
            'id': 81,
            'hidden': 0,
            'mdh_id': '6696ede3-7505-42a2-b4aa-559c4d57886b'
        }]
    }, {
        'hidden': 0,
        'resident': 0,
        'default_tax': 0,
        'id': 10,
        'mdh_id': '201d0a70-430a-4606-b826-6c85ccc98740',
        'name': 'Россия, нерезидент, НДС облагается',
        'region_id': 225,
        'percents': [{
            'dt': '2019-01-01T00:00:00+03:00',
            'nsp_pct': 0,
            'nds_pct': 20,
            'id': 301,
            'hidden': 0,
            'mdh_id': '02af481c-7bec-43ac-b90b-1f6a1f544e1c'
        }, {
            'dt': '2004-01-01T00:00:00+03:00',
            'nsp_pct': 0,
            'nds_pct': 18,
            'id': 61,
            'hidden': 0,
            'mdh_id': 'a281beab-0d17-49a1-88e7-90256c769557'
        }]
    }, {
        'hidden': 0,
        'resident': 0,
        'default_tax': 1,
        'id': 2,
        'mdh_id': '799ed901-ab97-47e9-8120-d07d78c146fc',
        'spendable_nds_id': 0,
        'region_id': 225,
        'name': 'Россия, нерезидент, НДС не облагается',
        'percents': [{
            'dt': '2004-01-01T00:00:00+03:00',
            'nsp_pct': 0,
            'nds_pct': 0,
            'id': 3,
            'hidden': 0,
            'mdh_id': 'cfd45ffb-c654-4d98-8fc1-355ca295d4b8'
        }, {
            'dt': '2003-01-01T00:00:00+03:00',
            'nsp_pct': 5,
            'nds_pct': 0,
            'id': 4,
            'hidden': 0,
            'mdh_id': '7afb2d22-bbc2-4592-b033-8861b90e3cf3'
        }]
    }],
    'person_categories': [{
        'category': 'yt',
        'is_resident': 0,
        'is_legal': 1
    }, {
        'category': 'endbuyer_yt',
        'is_resident': 0,
        'is_legal': 1
    }, {
        'category': 'ph_autoru',
        'is_resident': 1,
        'is_legal': 0
    }, {
        'category': 'ur_autoru',
        'is_resident': 1,
        'is_legal': 1
    }, {
        'category': 'endbuyer_ur',
        'is_resident': 1,
        'is_legal': 1
    }, {
        'category': 'endbuyer_ph',
        'is_resident': 1,
        'is_legal': 0
    }, {
        'category': 'yt_kzu',
        'is_resident': 0,
        'is_legal': 1
    }, {
        'category': 'yt_kzp',
        'is_resident': 0,
        'is_legal': 0
    }, {
        'category': 'ytph',
        'is_resident': 0,
        'is_legal': 0
    }, {
        'category': 'ph',
        'is_resident': 1,
        'is_legal': 0
    }, {
        'category': 'ur',
        'is_resident': 1,
        'is_legal': 1
    }]
}
FIRM_yabank = {
    'alter_permition_code': None,
    'config': None,
    'contract_id': None,
    'currency_rate_src': None,
    'default_currency': 'RUR',
    'default_iso_currency': 'RUB',
    'email': 'info@balance.yandex.ru',
    'export_type': None,
    'id': 1098,
    'inn': None,
    'invoice_paysys_id': None,
    'kpp': '770301001',
    'legaladdress': '123557, Российская Федерация, г. Москва, ул. Грузинский Вал, дом 10, строение 4',
    'mdh_id': 'c20fa378-b5a5-4b88-a24e-6aabab126ce4',
    'mnclose_email': '2320@direct.yandex.ru',
    'nds_pct': None,
    'oebs_org_id': None,
    'oebs_user_id': None,
    'pa_prefix': None,
    'payment_invoice_email': 'payment-invoice@yandex-team.ru',
    'person_categories': [
        {
            'category': 'yt',
            'is_legal': 1,
            'is_resident': 0
        },
        {
            'category': 'endbuyer_yt',
            'is_legal': 1,
            'is_resident': 0
        },
        {
            'category': 'ph_autoru',
            'is_legal': 0,
            'is_resident': 1
        },
        {
            'category': 'ur_autoru',
            'is_legal': 1,
            'is_resident': 1
        },
        {
            'category': 'endbuyer_ur',
            'is_legal': 1,
            'is_resident': 1
        },
        {
            'category': 'endbuyer_ph',
            'is_legal': 0,
            'is_resident': 1
        },
        {
            'category': 'yt_kzu',
            'is_legal': 1,
            'is_resident': 0
        },
        {
            'category': 'yt_kzp',
            'is_legal': 0,
            'is_resident': 0
        },
        {
            'category': 'ytph',
            'is_legal': 0,
            'is_resident': 0
        },
        {
            'category': 'ph',
            'is_legal': 0,
            'is_resident': 1
        },
        {
            'category': 'ur',
            'is_legal': 1,
            'is_resident': 1
        }
    ],
    'phone': '+7 (499) 253-31-33',
    'postpay': None,
    'region_id': 225,
    'tax_policies': [
        {
            'default_tax': 1,
            'hidden': 0,
            'id': 1,
            'mdh_id': '93fb537f-e424-49b6-8b3a-b735f9164708',
            'name': 'Россия, резидент, НДС облагается',
            'percents': [
                {
                    'dt': '2019-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 281,
                    'mdh_id': '91f77dca-81d8-4f6e-b3d1-04fa78ce6b50',
                    'nds_pct': 20,
                    'nsp_pct': 0
                },
                {
                    'dt': '2003-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 2,
                    'mdh_id': '9feab74e-4252-4507-a572-124304d9b9eb',
                    'nds_pct': 20,
                    'nsp_pct': 5
                },
                {
                    'dt': '2004-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 1,
                    'mdh_id': 'cd174c94-b588-451a-81df-78d765bd258f',
                    'nds_pct': 18,
                    'nsp_pct': 0
                }
            ],
            'region_id': 225,
            'resident': 1,
            'spendable_nds_id': 18
        },
        {
            'default_tax': 0,
            'hidden': 1,
            'id': 203,
            'mdh_id': '418f42d1-552f-411d-a110-c851d961c19b',
            'name': 'Без НДС (осв.) - медицина',
            'percents': [
                {
                    'dt': '2019-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 343,
                    'mdh_id': '855c453c-7bf6-48b7-9b80-94b043198cf3',
                    'nds_pct': 0,
                    'nsp_pct': 0
                }
            ],
            'region_id': 225,
            'resident': 1
        },
        {
            'default_tax': 0,
            'hidden': 1,
            'id': 12,
            'mdh_id': '10cad9b2-4e02-4502-a440-3c002b2caf87',
            'name': 'Резидент РФ для Авто.ру',
            'percents': [
                {
                    'dt': '2015-01-01T00:00:00+03:00',
                    'hidden': 1,
                    'id': 101,
                    'mdh_id': '18311966-8888-47af-93cb-aef87f0234e7',
                    'nds_pct': 18,
                    'nsp_pct': 0
                }
            ],
            'region_id': 225,
            'resident': 1
        },
        {
            'default_tax': 0,
            'hidden': 0,
            'id': 11,
            'mdh_id': '67d9783c-9d14-4ae9-b526-8e8f5186d2b4',
            'name': 'Россия, резидент, НДС не облагается',
            'percents': [
                {
                    'dt': '2004-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 81,
                    'mdh_id': '6696ede3-7505-42a2-b4aa-559c4d57886b',
                    'nds_pct': 0,
                    'nsp_pct': 0
                }
            ],
            'region_id': 225,
            'resident': 1
        },
        {
            'default_tax': 0,
            'hidden': 0,
            'id': 10,
            'mdh_id': '201d0a70-430a-4606-b826-6c85ccc98740',
            'name': 'Россия, нерезидент, НДС облагается',
            'percents': [
                {
                    'dt': '2019-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 301,
                    'mdh_id': '02af481c-7bec-43ac-b90b-1f6a1f544e1c',
                    'nds_pct': 20,
                    'nsp_pct': 0
                },
                {
                    'dt': '2004-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 61,
                    'mdh_id': 'a281beab-0d17-49a1-88e7-90256c769557',
                    'nds_pct': 18,
                    'nsp_pct': 0
                }
            ],
            'region_id': 225,
            'resident': 0
        },
        {
            'default_tax': 1,
            'hidden': 0,
            'id': 2,
            'mdh_id': '799ed901-ab97-47e9-8120-d07d78c146fc',
            'name': 'Россия, нерезидент, НДС не облагается',
            'percents': [
                {
                    'dt': '2004-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 3,
                    'mdh_id': 'cfd45ffb-c654-4d98-8fc1-355ca295d4b8',
                    'nds_pct': 0,
                    'nsp_pct': 0
                },
                {
                    'dt': '2003-01-01T00:00:00+03:00',
                    'hidden': 0,
                    'id': 4,
                    'mdh_id': '7afb2d22-bbc2-4592-b033-8861b90e3cf3',
                    'nds_pct': 0,
                    'nsp_pct': 5
                }
            ],
            'region_id': 225,
            'resident': 0,
            'spendable_nds_id': 0
        }
    ],
    'tax_policy_id': None,
    'test_env': 0,
    'title': 'АO "Яндекс Банк"',
    'unilateral': 0
}


@pytest.fixture(scope="session")
def yt_client():
    yt_proxy = os.environ["YT_PROXY"]
    yt_client = yt.client.YtClient(proxy=yt_proxy)
    yield yt_client


@pytest.fixture(scope="session")
def processor_stuff(yt_client):
    yt_client.create(
        'table',
        PATH_v_firm_tax_dynamic,
        recursive=True,
        attributes=dict(
            dynamic=True,
            schema=SCHEMA_v_firm_tax_dynamic,
        ),
    )
    yt_client.create(
        'table',
        PATH_currency_rate,
        recursive=True,
        attributes=dict(
            dynamic=True,
            schema=SCHEMA_currency_rate,
        ),
    )
    yt_client.create(
        'table',
        PATH_balance_contract,
        recursive=True,
        attributes=dict(
            dynamic=True,
            schema=SCHEMA_balance_contract,
        ),
    )
    yt_client.create(
        'table',
        PATH_mdh_products,
        recursive=True,
        attributes=dict(
            dynamic=True,
            schema=SCHEMA_mdh_products,
        ),
    )
    yt_client.mount_table(PATH_v_firm_tax_dynamic, sync=True)
    yt_client.mount_table(PATH_currency_rate, sync=True)
    yt_client.mount_table(PATH_balance_contract, sync=True)
    yt_client.mount_table(PATH_mdh_products, sync=True)
    yield {
        'firm_tax': PATH_v_firm_tax_dynamic,
        'currency': PATH_currency_rate,
        'contract': PATH_balance_contract,
        'mdh_products': PATH_mdh_products,
    }
    yt_client.unmount_table(PATH_v_firm_tax_dynamic, sync=True)
    yt_client.unmount_table(PATH_currency_rate, sync=True)
    yt_client.unmount_table(PATH_balance_contract, sync=True)
    yt_client.unmount_table(PATH_mdh_products, sync=True)
    yt_client.remove(PATH_v_firm_tax_dynamic)
    yt_client.remove(PATH_currency_rate)
    yt_client.remove(PATH_balance_contract)
    yt_client.remove(PATH_mdh_products)


@pytest.fixture()
def firm_yandex(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['firm_tax'], [
        FIRM_yandex,
    ])
    yield FIRM_yandex
    yt_client.delete_rows(processor_stuff['firm_tax'], [
        {
            'id': FIRM_yandex['id']
        },
    ])


@pytest.fixture()
def firm_yabank(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['firm_tax'], [
        FIRM_yabank,
    ])
    yield FIRM_yabank
    yt_client.delete_rows(processor_stuff['firm_tax'], [
        {
            'id': FIRM_yabank['id']
        },
    ])
