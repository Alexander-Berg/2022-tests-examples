from decimal import Decimal

import pytest
from datetime import datetime

from bcl.banks.protocols.swift.mt.base import MtUserHeader, OutAppHeader
from bcl.banks.protocols.swift.mt.exceptions import ValidationError
from bcl.banks.protocols.swift.mt.mt940 import Mt940


def test_basic():
    header = OutAppHeader(
        '0001', '925858', (('160930', '2000'), ('161003', '1054')),
        message_type=940, priority='N', recipient='OWHBDEFFZXXX'
    )

    user_header = MtUserHeader()
    user_header.add_value('108', 'GL1609301443')

    doc = Mt940.build('NDEXTAXIZXXX', 'OWHBDEFFZXXX', 1, 5, header_app=header, header_user=user_header)

    doc.ref_transaction = 'GL1609301443'
    doc.account_id = '50320000/0206701419'
    doc.num_statement_sequence = '2/1'
    doc.balance_opening = 'C160929USD10000,00'
    doc.balance_closing = 'C160930USD9990,00'

    # optional
    doc.statement_line = '1610010930DD10,00FINTCharges'
    doc.balance_closing_available = 'C160930USD10000,00'

    with pytest.raises(ValidationError):
        doc.acc_owner_info = [
            None,
            ''.join(
            'YANDEX.TAXI B.V.\r\n'
            'SCHIPHOL BOULEVARD 165\r\n'
            '1118 BG  SCHIPHOL\r\n'
            'OWN FUNDS\r\n'
            'Original Amount        10000,00 USD\r\n'
            'CALCULATION\r\n'
            'BRUTTO                 10000,00 USD\r\n'
            'TOTAL                  10000,00 USD'
            )
        ]

    doc.acc_owner_info = [
        'US-126255115-1 YANDEX.TAXI B.V.\r\n'
        'OWN FUNDS\r\n'
        'Original Amount        10000,00 USD\r\n'
        'BRUTTO                 10000,00 USD\r\n'
        'TOTAL                  10000,00 USD'
    ]

    assert doc.compile() == (
        '{1:F01NDEXTAXIZXXX0001000005}{2:O9402000160930OWHBDEFFZXXX00019258581610031054N}{3:{108:GL1609301443}}{4:\r\n'
        ':20:GL1609301443\r\n'
        ':25:50320000/0206701419\r\n'
        ':28C:2/1\r\n'
        ':60F:C160929USD10000,00\r\n'
        ':61:1610010930DD10,00FINTCharges\r\n'
        ':62F:C160930USD9990,00\r\n'
        ':64:C160930USD10000,00\r\n'
        ':86:US-126255115-1 YANDEX.TAXI B.V.\r\n'
        'OWN FUNDS\r\n'
        'Original Amount        10000,00 USD\r\n'
        'BRUTTO                 10000,00 USD\r\n'
        'TOTAL                  10000,00 USD\r\n'
        '-}'
    )

def test_parse():

    doc_src = '''{1:F01NDEXTAXIZXXX0001000003}{2:O9401930160929OWHBDEFFZXXX00019214221610031054N}{3:{108:GL1609291071}}{4:
:20:GL1609291071
:25:50320000/0206701419
:28C:1/1
:60F:C160911USD0,00
:61:1609300929CD10000,00S103C402469OCP092916//10FFMM1U07587424
OBg:10000,00 USD B/O:YANDEX.TAXI B
:62F:C160929USD10000,00
:64:C160929USD0,00
:86:YANDEX.TAXI B.V.
OWN FUNDS
Original Amount        10000,00 USD
BRUTTO                 10000,00 USD
TOTAL                  10000,00 USD
-}{1:F01NDEXTAXIZXXX0001000005}{2:O9402000160930OWHBDEFFZXXX00019258581610031054N}{3:{108:GL1609301443}}{4:
:20:GL1609301443
:25:50320000/0206701419
:28C:2/1
:60F:C160929USD10000,00
:61:1610010930DD10,00FINTCharges
:62F:C160930USD9990,00
:64:C160930USD10000,00
-}'''

    docs = Mt940.fromstring(doc_src)

    assert len(docs) == 2

    compiled = docs[0].compile() + docs[1].compile()
    expected = doc_src.replace('\n', '\r\n')
    assert compiled == expected

    assert docs[0].statement_line_date_value == [datetime(2016, 9, 30)]
    assert docs[0].balance_opening_date == datetime(2016, 9, 11)
    assert docs[0].balance_closing_date == datetime(2016, 9, 29)
    assert docs[0].payer_account == '' == docs[0].payer_account

    doc_src = '''{1:F01NDEXTAXIZXXX0001000004}{2:O9402000160930OWHBDEFFZXXX00019258571610031055N}{3:{108:GL1609301442}}{4:
:20:GL1609301442
:25:50320000/0206701013
:28C:2/1
:60F:D160913EUR50,00
:61:1610010930DR0,43FINTInterestforlas
t period
:61:1610010930DR10,00FINTCharges
:62F:D160930EUR60,43
:64:D160930EUR50,00
-}'''

    # Повторяющиеся теги.
    docs = Mt940.fromstring(doc_src)
    assert docs[0].statement_line_amount == [Decimal('0.43'), Decimal('10.00')]

    doc_src = '''{1:F01NDEXTAXIZXXX0001000001}{2:O9401900160913OWHBDEFFZXXX00018771421610031055N}{3:{108:GL1609131102}}{4:
:20:GL1609131102
:25:50320000/0206701013
:28C:1/1
:60F:C160911EUR0,00
:61:1609130913DR50,00FSTOMIPVTBDANNUAL
FEE
:62F:D160913EUR50,00
:64:D160913EUR50,00
-}{1:F01NDEXTAXIZXXX0001000004}{2:O9402000160930OWHBDEFFZXXX00019258571610031055N}{3:{108:GL1609301442}}{4:
:20:GL1609301442
:25:50320000/0206701013
:28C:2/1
:60F:D160913EUR50,00
:61:1610010930DR0,43FINTInterestforlas
t period
:61:1610010930DR10,00FINTCharges
:62F:D160930EUR60,43
:64:D160930EUR50,00
-}
{1:F01NDEXTAXIZXXX0001000001}{2:O9401900160913OWHBDEFFZXXX00018771421610031055N}{3:{108:GL1609131102}}{4:
:20:GL1609131102
:25:50320000/0206701013
:28C:1/1
:60F:C160911EUR0,00
:61:1609130913DR50,00FSTOMIPVTBDANNUAL
FEE
:62F:D160913EUR50,00
:64:D160913EUR50,00
-}{1:F01NDEXTAXIZXXX0001000004}{2:O9402000160930OWHBDEFFZXXX00019258571610031055N}{3:{108:GL1609301442}}{4:
:20:GL1609301442
:25:50320000/0206701013
:28C:2/1
:60F:D160913EUR50,00
:61:1610010930DR0,43FINTInterestforlas
t period
:61:1610010930DR10,00FINTCharges
:62F:D160930EUR60,43
:64:D160930EUR50,00
-}
{1:F01NDEXTAXIZXXX0001000001}{2:O9401900160913OWHBDEFFZXXX00018771421610031055N}{3:{108:GL1609131102}}{4:
:20:GL1609131102
:25:50320000/0206701013
:28C:1/1
:60F:C160911EUR0,00
:61:1609130913DR50,00FSTOMIPVTBDANNUAL
FEE
:62F:D160913EUR50,00
:64:D160913EUR50,00
-}{1:F01NDEXTAXIZXXX0001000004}{2:O9402000160930OWHBDEFFZXXX00019258571610031055N}{3:{108:GL1609301442}}{4:
:20:GL1609301442
:25:50320000/0206701013
:28C:2/1
:60F:D160913EUR50,00
:61:1610010930DR0,43FINTInterestforlas
t period
:61:1610010930DR10,00FINTCharges
:62F:D160930EUR60,43
:64:D160930EUR50,00
-}'''

    # Отсеивание дублей.
    docs = Mt940.fromstring(doc_src)
    assert len(docs) == 2
    assert len(docs[1].statement_line) == 2
    assert docs[1].statement_line_amount == [Decimal('0.43'), Decimal('10.00')]
    assert docs[1].statement_line_date_entry == ['0930', '0930']
    assert docs[1].statement_line_date_value == [datetime(2016, 10, 1), datetime(2016, 10, 1)]
    assert docs[1].statement_line_dc_mark == ['D', 'D']
    assert docs[1].statement_line_funds_code == ['R', 'R']
    assert docs[1].statement_line_id_code == ['INT', 'INT']
    assert docs[1].statement_line_trans_type == ['F', 'F']


def test_parse_statement_line_chunks():

    doc_src = '''{1:F01NDEXTAXIZXXX0001000007}{2:O9401101161005OWHBDEFFZXXX00019341071610280836N}{3:{108:GL161003001849}}{4:
:20:GL161003001849
:25:0206701013
:28C:3/1
:60F:D160930EUR60,43
:61:1610031003C1000,00FMSCSEPA Credit Tran//GD51003
SEPA Credit Tran CTB0000318597
:86:166
YandexTaxi BV
Schiphol Boulevard 165
1118 BG Schiphol
own funds
:62F:C161003EUR939,57
:64:C161003EUR939,57
-}{1:F01NDEXTAXIZXXX0001000010}{2:O9402130161027OWHBDEFFZXXX00019973241610280836N}{3:{108:GL161027002640}}{4:
:20:GL161027002640
:25:50320000/0206701013
:28C:4/1
:60F:C161003EUR939,57
:61:1610271027D37,00S103NONREF//10FFMAOU01055615
OBg:37,00 EUR Ben:YANDEX TAXI BV
:86:201  YANDEX TAXI BV  SCHIPHOL BOULEVARD 165  NETHERLANDS  TRANSFE
R OF OWN FUNDS  CALCULATION  BRUTTO     37,00 EUR
 TOTAL                     37,00 EUR
:62F:C161027EUR902,57
:64:C161027EUR902,57
-}'''

    docs = Mt940.fromstring(doc_src)
    assert len(docs) == 2

    assert docs[0].statement_line_ref_acc_owner == ['SEPA Credit Tran']
    assert docs[0].statement_line_ref_acc_serv_inst == ['GD51003']
    assert docs[0].statement_line_id_code == ['MSC']

    assert docs[1].statement_line_ref_acc_owner == ['NONREF']
    assert docs[1].statement_line_ref_acc_serv_inst == ['10FFMAOU01055615']
    assert docs[1].statement_line_id_code == ['103']


def test_repeatable():
    doc_src = '''{1:F01NDEXTAXIZXXX0001000007}{2:O9401101161005OWHBDEFFZXXX00019341071610280836N}{3:{108:GL161003001849}}{4:
:20:GL161003001849
:25:0206701013
:28C:3/1
:60F:D160930EUR60,43
:61:1610031003C1000,00FMSCSEPA Credit Tran//GD51003
SEPA Credit Tran CTB0000318597
:61:1610031003C2000,00FMSCSEPA Credit Tran//GD51003
SEPA Credit Tran CTB0000318597
:86:166
YandexTaxi BV
Schiphol Boulevard 165
1118 BG Schiphol
own funds
:62F:C161003EUR939,57
:64:C161003EUR939,57
-}{1:F01NDEXTAXIZXXX0001000010}{2:O9402130161027OWHBDEFFZXXX00019973241610280836N}{3:{108:GL161027002640}}{4:
:20:GL161027002640
:25:50320000/0206701013
:28C:4/1
:60F:C161003EUR939,57
:61:1610271027D37,00S103NONREF//10FFMAOU01055615
OBg:37,00 EUR Ben:YANDEX TAXI BV
:62F:C161027EUR902,57
:64:C161027EUR902,57
:86:201  YANDEX TAXI BV  SCHIPHOL BOULEVARD 165  NETHERLANDS  TRANSFE
R OF OWN FUNDS  CALCULATION  BRUTTO     37,00 EUR
 TOTAL                     37,00 EUR
-}'''

    docs = Mt940.fromstring(doc_src)
    doc = docs[1]

    assert len(docs[0].statement_line) == 2
    assert len(docs[0].acc_owner_info) == 2

    assert docs[0].statement_line_amount[0] == 1000
    assert docs[0].acc_owner_info[0] is None

    assert docs[0].statement_line_amount[1] == 2000
    assert docs[0].acc_owner_info[1].startswith('166\r\nYandexTaxi BV')

    assert len(docs[1].statement_line) == 1
    assert len(docs[1].acc_owner_info) == 2
    assert doc.acc_owner_info[0] is None
    assert doc.acc_owner_info[1].startswith('201  YANDEX TAXI')