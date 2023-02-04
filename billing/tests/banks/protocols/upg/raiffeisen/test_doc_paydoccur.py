from datetime import date
from decimal import Decimal

import pytest

from bcl.banks.protocols.upg.exceptions import UpgValidationError
from bcl.banks.protocols.upg.raiffeisen.doc_paydoccurraif import PayDocCurRaif


def test_get_digest_paydoc_currency():

    sum_info = {
        'code_num': '643',
        'code_alpha': 'RUB',
        'amount': Decimal('120.05'),
    }

    doc = PayDocCurRaif({
        'doc_ext_id': '1',
        'document_data': {
            'auth': {
                'name': 'Иванов',
                'phone': '128500',
            },
            'org': {'name': 'Организация'},
            'num': '100',
            'date': date(2018, 12, 4),
            'acc': '12345678909876543210',
            'bik': '123456789',
        },
        'charges': {'party': 'OUR'},
        'payer': {
            'name': 'Яндекс',
            'acc_info': {
                'num': '09876543211234567890',
                'bik': '987654321',
            },
        },
        'payee': {
            'name': 'Рога и копыта',
            'place': 'Москва',
            'country': {
                'name': 'США',
                'code_num': '840',
            },
            'iban': '6768',
        },
        'payee_bank': {
            'country': {
                'name': 'Росcия',
                'code_num': '643',
            },
            'name': 'Супербанк',
            'place': 'Новосибирск',
        },
        'purpose': 'Назначение',
        'amount_trans': {
            'multicurr': '0',
            'info': sum_info,
        },
        'amount_writeoff': {
            'info': sum_info,
        },
        'urgent': '0',
        'resident': '0',
        'currency_op_info': {
            'sum_info': {
                'code': '12345',
                'info': sum_info,
            }
        },
        'date_value': date(2018, 12, 4),
    })

    digest = doc.get_digest()
    assert '[Поручение на перевод валюты]' in digest
    assert 'Дата документа=04.12.2018' in digest
    assert 'Счет перевододателя=09876543211234567890' in digest

    xml = doc.to_xml()
    assert 'PayDocCurRaif' in xml


def test_xml_export():

    with pytest.raises(UpgValidationError):
        PayDocCurRaif({})
