# -*- coding: utf-8 -*-

import pytest
import datetime

from billing.contract_iface.constants import ContractTypeId

from balance import constants
import test_xmlrpc_create_common_contract

__author__ = 'quark'


@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
@pytest.mark.usefixtures("some_manager")
def test_create_everything(session, xmlrpcserver, some_manager):

    Balance = xmlrpcserver
    operator_id = session.oper_id
    manager_uid = some_manager.domain_passport_id

    # Клиент
    client_id = Balance.CreateClient(operator_id,
                                     {'NAME': 'Adfox auto test client'})[2]
    assert client_id, 'Ошибка создания клиента'

    # Юрик
    person_id_ur = Balance.CreatePerson(
        operator_id,
        {
            'client_id': client_id,
            'email': 'xxx@asdfasdf.asdfdsafd',
            'inn': '7837207726',
            'kpp': '912788793',
            'legaladdress': 'Test address',
            'longname': 'Adfox test offer person',
            'name': 'Adfox test person',
            'phone': '+7 812 3017123',
            'postaddress': 'Test address',
            'postcode': '456010',
            'type': 'ur'
        })
    assert person_id_ur, 'Ошибка создания плательщика юрика'

    # Физик
    person_id_ph = Balance.CreatePerson(
        operator_id,
        {
            'client_id': client_id,
            'type': 'ph',
            'lname': 'Pupken',
            'fname': 'Vasilii',
            'mname': 'Vasin',
            'phone': '+71234567890',
            'email': 'pupken@example.org'
        })
    assert person_id_ph, 'Ошибка создания плательщика физика'

    # Договор
    contract_res = Balance.CreateOffer(
        operator_id,
        {
            'client_id': client_id,
            'currency': 'RUB',
            'firm_id': 1, # ООО "Яндекс"
            'manager_uid': manager_uid,
            'payment_term': 20, # Срок оплаты счетов – 20 дней
            'payment_type': 3, # постоплата
            'person_id': person_id_ph,
            'services': [102], # ADFox
            'unilateral': 0,  # смотри BALANCE-34339, если придется поменять на unilateral = 1,
            # то нужен будет новый тест
            'start_dt': datetime.datetime(2019, 5, 1),
            'adfox_products': [
                {
                    "product_id": 505170, # "Основной продукт" - "ADFOX.Sites1 (shows)"
                    "scale": "adfox_sites_shows",
                    "account": "test_account" # "Учетная запись", обязательно для "основного продукта"
                },
                {
                    "product_id": 504400, # "Дефолтный продукт" - "ADFOX.Sites1 default"  обязателен к основному
                    "scale": "adfox_sites_default",
                },
                {
                    "product_id": 505426, # "Таргетирование Материалов по полу, возрасту и доходу для Sites (AS-PVD)"
                    "scale": "adfox_targeting_as_pvd",
                }],
            'vip_client': 1, # ценный клиент. 0 или 1
            'discount_product_id': 508212, # Скидочный продукт – Adfox.Sites + PDV. Обязательно если включен ценный клиент
            'dmp_segments': 1, # Доступны сегменты DMP. 1 или 0
        })

    expected_contract_params = {
        'type': 'GENERAL',
        'commission': ContractTypeId.OFFER,
        'active': True,
        'is_cancelled': None,
        'is_faxed': None,
        'dt': datetime.datetime(2019, 5, 1),
        'signed': 1,
        'suspended': False,
        'firm': 1,
        'manager_code': some_manager.manager_code,
        'currency': constants.NUM_CODE_RUR,
        'payment_type': constants.POSTPAY_PAYMENT_TYPE,
        'payment_term': 20,
        'services': {constants.ServiceId.ADFOX},
        'is_deactivated': None,
        'adfox_products': {
            504400: {u'account': u'', u'scale': u'adfox_sites_default'},
            505170: {u'account': u'test_account', u'scale': u'adfox_sites_shows'},
            505426: {u'account': u'', u'scale': u'adfox_targeting_as_pvd'}
        },
        'dmp_segments': 1,
        'vip_client': 1,
        'discount_product_id': 508212,
        'partner_credit': 1,
        'unilateral': 0,
        'credit_type': 1, #constants.CreditType.PO_SROKU,
    }

    test_xmlrpc_create_common_contract.check_create_contract_res_params(
        session, contract_res, some_manager, expected_contract_params)
