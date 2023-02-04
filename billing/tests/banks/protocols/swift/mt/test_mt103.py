import pytest

from bcl.banks.protocols.swift.mt.base import MtUserHeader
from bcl.banks.protocols.swift.mt.exceptions import ValidationError
from bcl.banks.protocols.swift.mt.mt103 import Mt103


def test_basic():
    header_user = MtUserHeader()
    header_user.add_value(108, 'MT103')

    doc = Mt103.build('OWHBDEFF0XXX', 'OWHBDEFFZXXX', 1, 1, header_user=header_user)

    doc.sender_ref = 'REF37532418'
    doc.mult_date_curr_sum = '161118USD5267,59'
    doc.mult_curr_sum_pay = 'USD5267,59'
    doc.customer = [
        '/DE85503200000206701419',
        'Yandex.Taxi B.V.\r\n'
        'Schiphol Boulevard 165, 1118 BG Sch\r\n'
        'iphol'
    ]
    doc.customer_beneficiary = [
        '/1930058344550101',
        'NICE DRIVE LLC\r\n'
        'RA, Yerevan, Arabkir 0019, Gyulbenk\r\n'
        'yan str., building 2, apt 47'
    ]
    doc.info_remittance = (
        'Invoice No.05 dated 29/08/16 advanc\r\n'
        'e payment for services under the Ma\r\n'
        'rketing agrement 10106826 dd 27/07/\r\n'
        '16 US-126254449-1'
    )

    assert doc.compile() == (
        '{1:F01OWHBDEFF0XXX0001000001}{2:I103OWHBDEFFZXXXN}{3:{108:MT103}}{4:\r\n'
        ':20:REF37532418\r\n'
        ':23B:CRED\r\n'
        ':32A:161118USD5267,59\r\n'
        ':33B:USD5267,59\r\n'
        ':50F:/DE85503200000206701419\r\n'
        'Yandex.Taxi B.V.\r\n'
        'Schiphol Boulevard 165, 1118 BG Sch\r\n'
        'iphol\r\n'
        ':59:/1930058344550101\r\n'
        'NICE DRIVE LLC\r\n'
        'RA, Yerevan, Arabkir 0019, Gyulbenk\r\n'
        'yan str., building 2, apt 47\r\n'
        ':70:Invoice No.05 dated 29/08/16 advanc\r\n'
        'e payment for services under the Ma\r\n'
        'rketing agrement 10106826 dd 27/07/\r\n'
        '16 US-126254449-1\r\n'
        ':71A:OUR\r\n'
        '-}'
    )

    with pytest.raises(ValidationError):
        doc.sender_ref = 'текст'
