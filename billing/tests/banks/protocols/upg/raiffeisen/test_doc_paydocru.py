from datetime import date

import pytest

from bcl.banks.protocols.upg.exceptions import UpgValidationError
from bcl.banks.protocols.upg.raiffeisen.doc_paydocru import PayDocRu


def test_get_digest():
    doc = PayDocRu({
        'doc_ext_id': '1',
        'payee': {
            'acc': 'acc12345678901234567',
            'inn': 'inn123',
            'name': 'name123',
        },
        'payer': {
            'acc': 'acc12345678901234567',
            'inn': 'inn123',
            'name': 'name123',
        },
        'document_data': {
            'tax_sum': 123123,
            'tax_type': 'Vat1',
            'tax_rate': '1',
            'sum': 123123,
            'payment_kind': '',
            'priority': 1,
            'date': date(2016, 11, 17),
            'op_kind': '1',
            'num': '100',
            'purpose': 'a purpose',
        }
    })

    digest = doc.get_digest()
    assert '[Платежное поручение]' in digest
    assert 'Дата документа=17.11.2016' in digest
    assert 'Счет плательщика=acc12345678901234567' in digest

    assert 'PayDocRu' in doc.to_xml()


def test_xml_export():
    with pytest.raises(UpgValidationError):
        PayDocRu({})
