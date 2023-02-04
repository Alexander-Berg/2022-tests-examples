import pytest

from bcl.banks.protocols.swift.mt.exceptions import ValidationError
from bcl.banks.protocols.swift.mt.mt101 import Mt101


def test_basic():
    doc = Mt101.build('YANDEXDE', 'OWHBDEFFZXX', 1, 1, use_tag_options={
        'customer': 'H',
        'beneficiary': '-',
    })

    with pytest.raises(ValidationError):
        # Проверка работы явного указания опций (use_tag_options)
        doc.beneficiary = [
            '/KZ83722S000001183688',
            'HOME кириллица не поддерживается'
        ]

    doc.sender_ref = '170210163433RMPy'
    doc.message_counter = '1/1'
    doc.customer = [
        '/DE85503200000206701419',
        'Yandex.Taxi B.V.\r\n'
        'Schiphol Boulevard 165, 1118 BG Sch\r\n'
        'iphol'
    ]
    doc.execution_date = '170215'
    doc.transaction_ref = '170210163433RMPy'
    doc.mult_curr_sum_trans ='USD5122,00'
    doc.inst_account_with = 'CASPKZKA'
    doc.beneficiary = [
        '/KZ83722S000001183688',
        'HOME SYSTEMS GROUP LLP'
    ]
    doc.info_remittance = (
        'Advance payment for services under\r\n'
        'contract 10116960 dd 151116'
    )

    doc.mult_date_curr_sum = '161118USD5267,59'
    doc.mult_curr_sum_pay = 'USD5267,59'
    doc.charges_details = 'OUR'

    assert doc.compile() == (
        '''{1:F01YANDEXDEXXXX0001000001}{2:I101OWHBDEFFXZXXN}{4:
        :20:170210163433RMPy
        :28D:1/1
        :50H:/DE85503200000206701419
        Yandex.Taxi B.V.
        Schiphol Boulevard 165, 1118 BG Sch
        iphol
        :30:170215
        :21:170210163433RMPy
        :32B:USD5122,00
        :57A:CASPKZKA
        :59:/KZ83722S000001183688
        HOME SYSTEMS GROUP LLP
        :70:Advance payment for services under
        contract 10116960 dd 151116
        :71A:OUR
        -}'''.replace('\n', '\r\n').replace('        ', '')
    )

    with pytest.raises(ValidationError):
        doc.sender_ref = 'текст'
