import pytest

from billing.hot.processor.tests.utils.conftest import *  # noqa F401, F403


RUB_to_USD = {
    'src_cc': 'cbr',
    'dt': 1,
    'iso_currency_from': 'RUB',
    'iso_currency_to': 'USD',
    'obj': {
        'rate_from': '63.4536',
        'rate_to': '1',
        'src_cc': 'cbr',
        'iso_currency_to': 'USD',
        'iso_currency_from': 'RUB',
        'version_id': 1.0,
        'dt': 1.0,
        'id': 1274347.0
    },
    'id': 1274347,
    '_rest': {
        'classname': 'IsoCurrencyRate',
        'version': 1.0
    },
    '_timestamp': -1635342910604000000,
    '_partition': '{"cluster":"kafka-bs","partition":0,"topic":"balance/test/iso-currency-rate"}',
    '_offset': 1245985,
    '_idx': 1
}
KZT_TO_RUB = {
    'src_cc': 'cbr',
    'dt': 0,
    'iso_currency_from': 'KZT',
    'iso_currency_to': 'RUB',
    'obj': {
        'rate_from': '100',
        'rate_to': '16.5524',
        'src_cc': 'cbr',
        'iso_currency_to': 'RUB',
        'iso_currency_from': 'KZT',
        'version_id': 1.0,
        'dt': 0.0,
        'id': 1171804.0
    },
    'id': 1171804,
    '_rest': {
        'classname': 'IsoCurrencyRate',
        'version': 1.0
    },
    '_timestamp': -1635341254326000000,
    '_partition': '{"cluster":"kafka-bs","partition":0,"topic":"balance/test/iso-currency-rate"}',
    '_offset': 1067960,
    '_idx': 1
}
CONTRACT_2144400 = {
    'id': 2144400,
    'obj': {
        'version_id': 7.0,
        'person_type': 'ur',
        'client_id': 90735678.0,
        'collaterals': {
            '0': {
                'manager_code': 22545.0,
                'atypical_conditions': 0.0,
                'repayment_on_consume': 0.0,
                'partner_credit': 1.0,
                'is_suspended': None,
                'bank_details_id': 2.0,
                'sent_dt': None,
                'num': None,
                'discount_policy_type': None,
                'calc_defermant': 0.0,
                'memo': 'Богданова\r\nINTERCO-1231 BILLINGPLAN-498\r\n(Я. Плюс 2.0 с Такси)\r\n\r\nскан двусторонний PROEXPERT-205',
                'fake_id': 0.0,
                'payment_type': 3.0,
                'id': 2589829.0,
                'update_dt': '2021-02-01T13:14:49',
                'is_faxed': '2021-02-01T00:00:00',
                'is_cancelled': None,
                'print_form_dt': None,
                'payment_term': 30.0,
                'credit_type': 1.0,
                'finish_dt': None,
                'manager_bo_code': None,
                'contract2_id': 2144400.0,
                'passport_id': 1120000000041057.0,
                'unilateral': 1.0,
                'create_dt': '2020-09-30T13:10:56',
                'collateral_type_id': None,
                'integration': None,
                'is_booked_dt': '2020-09-30T17:58:20',
                'no_acts': 0.0,
                'individual_docs': 0.0,
                'personal_account': 1.0,
                'is_signed': '2021-02-01T00:00:00',
                'is_booked': 0.0,
                'attribute_batch_id': 13566116.0,
                'print_tpl_barcode': 999997626477.0,
                'commission': 0.0,
                'dt': '2020-08-01T00:00:00',
                'lift_credit_on_payment': 1.0,
                'firm': 1.0,
                'services': {
                    '703': 1.0
                },
                'tickets': 'INTERCO-1231 BILLINGPLAN-498',
                'currency': 810.0
            },
            '1': {
                'attribute_batch_id': 14857810.0,
                'print_form_type': 0.0,
                'contract2_id': 2144400.0,
                'dt': '2020-12-01T00:00:00',
                'tickets': 'INTERCO-1423',
                'collateral_type_id': 1003.0,
                'is_booked': 0.0,
                'is_cancelled': None,
                'is_faxed': None,
                'is_signed': '2021-03-01T00:00:00',
                'id': 3087052.0,
                'sent_dt': None,
                'num': '01',
                'print_tpl_barcode': 999997129049.0,
                'passport_id': 1120000000041057.0,
                'create_dt': '2020-12-21T15:08:04',
                'update_dt': '2021-03-01T16:20:31',
                'memo': 'Богданова Е.\r\nINTERCO-1423\r\nподписывали в оригинале PROEXPERT-205'
            }
        },
        'passport_id': 1120000000041057.0,
        'external_id': '1135730/20',
        'extprops': {
            'service_code': {
                'value': None,
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_str',
                    'native_type': None
                }
            },
            'is_process_taxi_netting_in_oebs_': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_num',
                    'native_type': 'bool'
                },
                'value': False
            },
            'cpf_netting_last_dt': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_dt',
                    'native_type': None
                },
                'value': None
            },
            'offer_accepted': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_num',
                    'native_type': 'bool'
                },
                'value': None
            },
            'daily_state': {
                '_meta': {
                    'data_column': 'value_dt',
                    'native_type': None,
                    'values_native_type': None
                },
                'value': None
            }
        },
        'update_dt': '2021-09-08T14:56:09',
        'type': 'GENERAL',
        'id': 2144400.0,
        'person_id': 11712045.0
    },
    'client_id': 90735678,
    'version': 7,
    '_rest': {
        'classname': 'Contract'
    },
    '_timestamp': -1631116389042000000,
    '_partition': '{"cluster":"kafka-bs","partition":0,"topic":"balance/prod/contract"}',
    '_offset': 3133986,
    '_idx': 1
}
CONTRACT_4836456 = {
    'id': 4836456,
    'obj': {
        'extprops': {
            'is_process_taxi_netting_in_oebs_': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_num',
                    'native_type': 'bool'
                },
                'value': False
            },
            'cpf_netting_last_dt': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_dt',
                    'native_type': None
                },
                'value': None
            },
            'offer_accepted': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_num',
                    'native_type': 'bool'
                },
                'value': None
            },
            'daily_state': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_dt',
                    'native_type': None
                },
                'value': None
            },
            'service_code': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_str',
                    'native_type': None
                },
                'value': None
            }
        },
        'update_dt': '2021-12-31T08:55:11',
        'person_type': 'yt',
        'passport_id': 1120000000041057.0,
        'person_id': 17057107.0,
        'collaterals': {
            '0': {
                'print_form_dt': None,
                'fake_id': 0.0,
                'personal_account': 1.0,
                'integration': None,
                'manager_bo_code': None,
                'memo': 'Богданова Елизавета\r\nINTERCO-2032\r\nЯндекс.Такси Корп Плюс 2.0',
                'currency': 840.0,
                'credit_type': 1.0,
                'num': None,
                'update_dt': '2021-12-31T08:55:11',
                'commission': 0.0,
                'attribute_batch_id': 21977593.0,
                'print_tpl_barcode': 999994545548.0,
                'lift_credit_on_payment': 1.0,
                'firm': 1.0,
                'partner_credit': 1.0,
                'sent_dt': None,
                'id': 5667362.0,
                'tickets': 'INTERCO-2032',
                'individual_docs': 0.0,
                'is_booked_dt': '2021-12-31T08:55:09',
                'is_cancelled': None,
                'no_acts': 0.0,
                'unilateral': 1.0,
                'is_suspended': None,
                'bank_details_id': 5.0,
                'create_dt': '2021-12-30T13:01:34',
                'atypical_conditions': 0.0,
                'contract2_id': 4836456.0,
                'discount_policy_type': None,
                'is_booked': 1.0,
                'finish_dt': None,
                'calc_defermant': 0.0,
                'services': {
                    '703': 1.0
                },
                'payment_term': 30.0,
                'is_faxed': '2021-12-31T00:00:00',
                'repayment_on_consume': 0.0,
                'payment_type': 3.0,
                'passport_id': 1120000000041057.0,
                'collateral_type_id': None,
                'manager_code': 22545.0,
                'is_signed': None,
                'dt': '2021-12-29T00:00:00'
            }
        },
        'external_id': '2948143/21',
        'type': 'GENERAL',
        'version_id': 1.0,
        'id': 4836456.0,
        'client_id': 96577858.0
    },
    'client_id': 96577858,
    'version': 1,
    '_rest': {
        'classname': 'Contract'
    },
    '_timestamp': -1640930111654000000,
    '_partition': '{"cluster":"kafka-bs","partition":0,"topic":"balance/prod/contract"}',
    '_offset': 7842553,
    '_idx': 1
}
CONTRACT_4834697 = {
    'id': 4834697,
    'obj': {
        'person_type': 'ur',
        'passport_id': 1120000000041057.0,
        'type': 'SPENDABLE',
        'version_id': 1.0,
        'id': 4834697.0,
        'person_id': 17054270.0,
        'collaterals': {
            '0': {
                'manager_code': 22545.0,
                'services': {
                    '704': 1.0
                },
                'dt': '2021-12-29T00:00:00',
                'attribute_batch_id': 21972747.0,
                'is_offer': 0.0,
                'passport_id': 1120000000041057.0,
                'is_suspended': None,
                'currency': 643.0,
                'print_tpl_barcode': 999994547574.0,
                'update_dt': '2021-12-31T09:16:35',
                'memo': 'Богданова Елизавета\r\nINTERCO-2340\r\nплюс для Доставки\r\nБронь подписи по требованию в тикете',
                'id': 5665347.0,
                'selfemployed': 0.0,
                'print_form_dt': None,
                'collateral_type_id': None,
                'is_booked_dt': '2021-12-31T09:16:35',
                'firm': 1.0,
                'is_booked': 1.0,
                'sent_dt': None,
                'num': None,
                'contract2_id': 4834697.0,
                'nds': 18.0,
                'end_dt': None,
                'payment_type': 1.0,
                'individual_docs': 0.0,
                'create_dt': '2021-12-30T07:56:27',
                'pay_to': 1.0,
                'is_signed': None,
                'is_cancelled': None,
                'integration': None,
                'atypical_conditions': 0.0,
                'manager_bo_code': None,
                'is_faxed': '2021-12-31T00:00:00'
            }
        },
        'external_id': '2947007/21',
        'extprops': {
            'is_process_taxi_netting_in_oebs_': {
                'value': False,
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_num',
                    'native_type': 'bool'
                }
            },
            'cpf_netting_last_dt': {
                'value': None,
                '_meta': {
                    'native_type': None,
                    'values_native_type': None,
                    'data_column': 'value_dt'
                }
            },
            'offer_accepted': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_num',
                    'native_type': 'bool'
                },
                'value': None
            },
            'daily_state': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_dt',
                    'native_type': None
                },
                'value': None
            },
            'service_code': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_str',
                    'native_type': None
                },
                'value': None
            }
        },
        'update_dt': '2021-12-31T09:16:35',
        'client_id': 96574869.0
    },
    'client_id': 96574869,
    'version': 1,
    '_rest': {
        'classname': 'Contract'
    },
    '_timestamp': -1640931395984000000,
    '_partition': '{"cluster":"kafka-bs","partition":0,"topic":"balance/prod/contract"}',
    '_offset': 7842622,
    '_idx': 1
}
CONTRACT_2144410 = {
    'id': 2144410,
    'obj': {
        'passport_id': 1120000000017843.0,
        'version_id': 7.0,
        'person_type': 'ur',
        'collaterals': {
            '1': {
                'create_dt': '2020-12-21T15:13:45',
                'sent_dt': None,
                'is_faxed': None,
                'attribute_batch_id': 14857942.0,
                'is_booked': 0.0,
                'is_signed': '2021-03-01T00:00:00',
                'id': 3087105.0,
                'collateral_type_id': 7020.0,
                'update_dt': '2021-03-01T16:24:09',
                'dt': '2020-12-01T00:00:00',
                'print_tpl_barcode': 999997128996.0,
                'passport_id': 1120000000041057.0,
                'memo': 'INTERCO-1423\r\nБогданова Е.\r\nподписывали в оригинале PROEXPERT-205',
                'num': '01',
                'contract2_id': 2144410.0,
                'is_cancelled': None
            },
            '0': {
                'passport_id': 1120000000041057.0,
                'memo': 'Богданова\r\nINTERCO-1231 BILLINGPLAN-498\r\n(Я. Плюс 2.0 с Такси)\r\n\r\nскан двусторонний PROEXPERT-205',
                'id': 2589845.0,
                'manager_code': 22545.0,
                'is_booked_dt': '2020-09-30T17:59:05',
                'is_booked': 0.0,
                'is_cancelled': None,
                'selfemployed': 0.0,
                'attribute_batch_id': 13566170.0,
                'print_tpl_barcode': 999997626460.0,
                'end_dt': None,
                'firm': 1.0,
                'print_form_dt': None,
                'integration': None,
                'create_dt': '2020-09-30T13:14:03',
                'manager_bo_code': None,
                'num': None,
                'is_signed': '2021-02-01T00:00:00',
                'dt': '2020-08-01T00:00:00',
                'is_offer': 0.0,
                'contract2_id': 2144410.0,
                'payment_type': 1.0,
                'update_dt': '2021-02-01T14:10:06',
                'collateral_type_id': None,
                'currency': 643.0,
                'pay_to': 1.0,
                'is_faxed': '2021-02-01T00:00:00',
                'sent_dt': None,
                'nds': 18.0,
                'individual_docs': 0.0,
                'is_suspended': None,
                'services': {
                    '704': 1.0
                },
                'atypical_conditions': 0.0
            }
        },
        'external_id': '1135496/20',
        'client_id': 90735678.0,
        'person_id': 11712064.0,
        'extprops': {
            'is_process_taxi_netting_in_oebs_': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_num',
                    'native_type': 'bool'
                },
                'value': False
            },
            'cpf_netting_last_dt': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_dt',
                    'native_type': None
                },
                'value': None
            },
            'offer_accepted': {
                '_meta': {
                    'values_native_type': None,
                    'data_column': 'value_num',
                    'native_type': 'bool'
                },
                'value': None
            },
            'daily_state': {
                '_meta': {
                    'data_column': 'value_dt',
                    'native_type': None,
                    'values_native_type': None
                },
                'value': None
            },
            'service_code': {
                '_meta': {
                    'native_type': None,
                    'values_native_type': None,
                    'data_column': 'value_str'
                },
                'value': None
            }
        },
        'update_dt': '2021-09-08T14:56:09',
        'type': 'SPENDABLE',
        'id': 2144410.0
    },
    'client_id': 90735678,
    'version': 7,
    '_rest': {
        'classname': 'Contract'
    },
    '_timestamp': -1631116350312000000,
    '_partition': '{"cluster":"kafka-bs","partition":0,"topic":"balance/prod/contract"}',
    '_offset': 3130169,
    '_idx': 1
}
PRODUCT_1_RU_RUB = {
    'master_uid': '90495dc9-3655-4abe-a72d-0e9a795d2e2e',
    'obj': {
        'version': 1,
        'foreign': {
            'reference_price_iso_currency': {
                'record_uid': '25eab990-88d9-4d7a-af20-acbdfea1c098',
                'status': 6,
                'status_alias': 'published',
                'version': 1,
                'version_master': 1,
                'attrs': {
                    'num_code': 643,
                    'alpha_code': 'RUB',
                    'minor_unit': 2,
                    'name': 'Russian Ruble',
                },
                'master_uid': '25eab990-88d9-4d7a-af20-acbdfea1c098',
            },
            'unit_id': {
                'status_alias': 'published',
                'version': 1,
                'version_master': 1,
                'attrs': {
                    'iso_currency': '25eab990-88d9-4d7a-af20-acbdfea1c098',
                    'name': 'рубли',
                    'precision': 6,
                    'product_type_id': '39cffdeb-8fcc-4f9b-a651-f5f3f2c1e5ee',
                    'type_rate': 1,
                    'englishname': 'roubles',
                    'id': 850,
                },
                'master_uid': '6b3b9e1f-dee3-4cc1-adc6-7da5a41955be',
                'record_uid': '6b3b9e1f-dee3-4cc1-adc6-7da5a41955be',
                'status': 6,
            },
        },
        'master_uid': '90495dc9-3655-4abe-a72d-0e9a795d2e2e',
        'nested': {
            'nom_price': [
                {
                    'status_alias': 'published',
                    'version': 1,
                    'version_master': 1,
                    'attrs': {
                        'only_test_env': False,
                        'price': '1',
                        'product_id': '90495dc9-3655-4abe-a72d-0e9a795d2e2e',
                        'tax_policy_pct_id': '91f77dca-81d8-4f6e-b3d1-04fa78ce6b50',
                        'dt': '2020-08-01T00:00:00',
                        'id': 42193,
                        'internal': False,
                        'iso_currency': '25eab990-88d9-4d7a-af20-acbdfea1c098',
                    },
                    'foreign': {
                        'iso_currency': {
                            'attrs': {
                                'name': 'Russian Ruble',
                                'num_code': 643,
                                'alpha_code': 'RUB',
                                'minor_unit': 2,
                            },
                            'master_uid': '25eab990-88d9-4d7a-af20-acbdfea1c098',
                            'record_uid': '25eab990-88d9-4d7a-af20-acbdfea1c098',
                            'status': 6,
                            'status_alias': 'published',
                            'version': 1,
                            'version_master': 1,
                        },
                        'tax_policy_pct_id': {
                            'foreign': {
                                'tax_policy_id': {
                                    'version': 1,
                                    'version_master': 2,
                                    'attrs': {
                                        'resident': True,
                                        'default_tax': True,
                                        'id': 1,
                                        'name': 'Россия, резидент, НДС облагается',
                                        'region_id': '32f85e09-bdef-4569-8fb5-24e593ec5623',
                                    },
                                    'master_uid': '93fb537f-e424-49b6-8b3a-b735f9164708',
                                    'record_uid': '9e73bacb-2ddd-4b0e-9b7a-9fdce1a37339',
                                    'status': 6,
                                    'status_alias': 'published',
                                }
                            },
                            'master_uid': '91f77dca-81d8-4f6e-b3d1-04fa78ce6b50',
                            'record_uid': '91f77dca-81d8-4f6e-b3d1-04fa78ce6b50',
                            'status': 6,
                            'status_alias': 'published',
                            'version': 1,
                            'version_master': 1,
                            'attrs': {
                                'nsp_pct': '0',
                                'tax_policy_id': '93fb537f-e424-49b6-8b3a-b735f9164708',
                                'dt': '2019-01-01T00:00:00',
                                'id': 281,
                                'nds_pct': '20',
                            },
                        },
                    },
                    'master_uid': '1bc74ce9-203d-4c98-85ea-9238cd29d01c',
                    'record_uid': '1bc74ce9-203d-4c98-85ea-9238cd29d01c',
                    'status': 6,
                }
            ],
            'nom_tax': [
                {
                    'foreign': {
                        'iso_currency': {
                            'status_alias': 'published',
                            'version': 1,
                            'version_master': 1,
                            'attrs': {
                                'name': 'Russian Ruble',
                                'num_code': 643,
                                'alpha_code': 'RUB',
                                'minor_unit': 2,
                            },
                            'master_uid': '25eab990-88d9-4d7a-af20-acbdfea1c098',
                            'record_uid': '25eab990-88d9-4d7a-af20-acbdfea1c098',
                            'status': 6,
                        },
                        'tax_policy_id': {
                            'version': 1,
                            'version_master': 2,
                            'attrs': {
                                'default_tax': True,
                                'id': 1,
                                'name': 'Россия, резидент, НДС облагается',
                                'region_id': '32f85e09-bdef-4569-8fb5-24e593ec5623',
                                'resident': True,
                            },
                            'foreign': {
                                'region_id': {
                                    'status': 6,
                                    'status_alias': 'published',
                                    'version': 1,
                                    'version_master': 1,
                                    'attrs': {
                                        'iso_code': 643,
                                        'region_id': 225,
                                        'region_name': 'Россия',
                                        'region_name_en': 'Russia',
                                    },
                                    'master_uid': '32f85e09-bdef-4569-8fb5-24e593ec5623',
                                    'record_uid': '32f85e09-bdef-4569-8fb5-24e593ec5623',
                                }
                            },
                            'master_uid': '93fb537f-e424-49b6-8b3a-b735f9164708',
                            'record_uid': '9e73bacb-2ddd-4b0e-9b7a-9fdce1a37339',
                            'status': 6,
                            'status_alias': 'published',
                        },
                    },
                    'master_uid': '0546f91e-14d9-4454-9720-086865e5fdb4',
                    'record_uid': '0546f91e-14d9-4454-9720-086865e5fdb4',
                    'status': 6,
                    'status_alias': 'published',
                    'version': 1,
                    'version_master': 1,
                    'attrs': {
                        'nds_operation_code': None,
                        'only_test_env': False,
                        'product_id': '90495dc9-3655-4abe-a72d-0e9a795d2e2e',
                        'tax_policy_id': '93fb537f-e424-49b6-8b3a-b735f9164708',
                        'dt': '2020-08-01T00:00:00',
                        'id': 39736,
                        'iso_currency': '25eab990-88d9-4d7a-af20-acbdfea1c098',
                    },
                }
            ],
        },
        'resync': True,
        'status_alias': 'published',
        'attrs': {
            'manual_discount': False,
            'media_discount': '8762e918-cea3-4e95-bdff-e7eb48b42fdc',
            'unit_id': '6b3b9e1f-dee3-4cc1-adc6-7da5a41955be',
            'activ_dt': '2020-08-01T00:00:00',
            'commission_type': '8762e918-cea3-4e95-bdff-e7eb48b42fdc',
            'engine_id': '8a1a4075-ce3a-4bac-ab27-bdf8377e5b47',
            'id': 511614,
            'name': 'Маркетинговые услуги согласно п. 2.1 договора',
            'reference_price_iso_currency': '25eab990-88d9-4d7a-af20-acbdfea1c098',
            'service_code': None,
            'adv_kind_id': None,
            'common': False,
            'fullname': 'Маркетинговые услуги согласно п. 2.1 договора',
            'main_product_id': None,
            'show_in_shop': False,
            'activity_type_id': '346ef64e-3a39-4251-81b5-e7072302cb5f',
            'firm_id': '146c1b7a-1d6e-4c80-9b59-acb34c6d6dc6',
            'only_test_env': False,
            'product_group_id': 'a9650a27-9f99-4915-bd0c-77b268285c66',
            'comments': 'https://st.yandex-team.ru/DOCUMENT-47749 Бортник',
            'englishname': None,
        },
        'record_uid': '90495dc9-3655-4abe-a72d-0e9a795d2e2e',
        'status': 6,
        'version_composite': 4,
        'version_master': 1,
    },
    'version': 1,
    'ErrorMessage': None,
    'ParsingStage': None,
    'Successed': True,
    'id': 511614,
    '_rest': None,
}


@pytest.fixture()
def contract_2144400(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['contract'], [
        CONTRACT_2144400,
    ])
    yield CONTRACT_2144400
    yt_client.delete_rows(processor_stuff['contract'], [
        {
            'id': CONTRACT_2144400['id']
        },
    ])


@pytest.fixture()
def contract_4836456(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['contract'], [
        CONTRACT_4836456,
    ])
    yield CONTRACT_4836456
    yt_client.delete_rows(processor_stuff['contract'], [
        {
            'id': CONTRACT_4836456['id']
        },
    ])


@pytest.fixture()
def contract_4834697(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['contract'], [
        CONTRACT_4834697,
    ])
    yield CONTRACT_4834697
    yt_client.delete_rows(processor_stuff['contract'], [
        {
            'id': CONTRACT_4834697['id']
        },
    ])


@pytest.fixture()
def contract_2144410(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['contract'], [
        CONTRACT_2144410,
    ])
    yield CONTRACT_2144410
    yt_client.delete_rows(processor_stuff['contract'], [
        {
            'id': CONTRACT_2144410['id']
        },
    ])


@pytest.fixture()
def rub_to_usd(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['currency'], [RUB_to_USD])
    yield RUB_to_USD
    yt_client.delete_rows(processor_stuff['currency'], [
        {
            'src_cc': RUB_to_USD['src_cc'],
            'dt': RUB_to_USD['dt'],
            'iso_currency_from': RUB_to_USD['iso_currency_from'],
            'iso_currency_to': RUB_to_USD['iso_currency_to'],
        },
    ])


@pytest.fixture()
def kzt_to_rub(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['currency'], [KZT_TO_RUB])
    yield KZT_TO_RUB
    yt_client.delete_rows(processor_stuff['currency'], [
        {
            'src_cc': KZT_TO_RUB['src_cc'],
            'dt': KZT_TO_RUB['dt'],
            'iso_currency_from': KZT_TO_RUB['iso_currency_from'],
            'iso_currency_to': KZT_TO_RUB['iso_currency_to'],
        },
    ])


@pytest.fixture()
def product_1_ru_rub(yt_client, processor_stuff):
    yt_client.insert_rows(processor_stuff['mdh_products'], [PRODUCT_1_RU_RUB])
    yield PRODUCT_1_RU_RUB
    yt_client.delete_rows(processor_stuff['mdh_products'], [
        {
            'master_uid': PRODUCT_1_RU_RUB['master_uid'],
        },
    ])
