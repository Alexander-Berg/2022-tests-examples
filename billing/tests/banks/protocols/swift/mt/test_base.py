import pytest

from bcl.banks.protocols.swift.mt.base import MtDocument, MtTag, MtTrailerBlock, MtUserHeader, get_bic12
from bcl.banks.protocols.swift.mt.exceptions import ValidationError


class MyDoc(MtDocument):

    name1 = MtTag('name1', '10', '2!d', req=True, choices=['10', '100'])
    name2 = MtTag('name2', '30[C/B]', {
        'A': [
            ['2*25x', 'описание5'],
        ],
        'C': [
            ['2!d', 'описание2'],
        ],
        'B': [
            ['/34x', 'описание3'],
            ['2*25x', 'описание4'],
        ],
    })
    repeated = MtTag('repeated1', '20', '16x', repeat=True)


def test_get_bic12():
    assert get_bic12('OWHBDEFF') == 'OWHBDEFFXXXX'
    assert get_bic12('OWHBDEFF', 'Z') == 'OWHBDEFFZXXX'
    assert get_bic12('OWHBDEFFZZZ') == 'OWHBDEFFXZZZ'
    assert get_bic12('OWHBDEFFZZZ', 'Y') == 'OWHBDEFFYZZZ'


def test_doc_headers():

    with pytest.raises(ValidationError):
        MtDocument.build('ERROR', '123456789123', 1, 1)

    with pytest.raises(ValidationError):
        MtDocument.build('123456789123', 'ERROR', 1, 1)

    doc = MtDocument.build('123456789123', '987654321987', 1, 1)

    assert doc.header_basic == '{1:F011234567891230001000001}'
    assert doc.header_app == '{2:I000987654321987N}'

    assert doc.header_user == ''
    assert doc.trailer == ''

    header_user = MtUserHeader()
    header_user.add_value('103', 'CAD')
    header_user.add_value('108', '2RDRQDHM3WO')

    doc.header_user = header_user

    assert doc.header_user == '{3:{103:CAD}{108:2RDRQDHM3WO}}'

    trailer = MtTrailerBlock()
    trailer.add_value('CHK', 'C77F8E009597')
    trailer.add_value('XXX', 'YYY')

    doc.trailer = trailer

    assert doc.trailer == '{5:{CHK:C77F8E009597}{XXX:YYY}}'


def test_tag():
    doc = MyDoc.build('123456789123', '987654321987', 1, 1)
    doc.name1 = 10

    assert doc.name1 == '10'

    doc2 = MyDoc.build('123456789123', '987654321987', 1, 1)

    with pytest.raises(KeyError):
        doc2.name1

    doc.repeated = 2
    assert doc.repeated == ['2']
    doc.repeated = 3
    assert doc.repeated == ['2', '3']

    assert doc.body == '{4:\r\n:10:10\r\n:20:2\r\n:20:3\r\n-}'

    assert doc.compile() == (
        '{1:F011234567891230001000001}{2:I000987654321987N}{4:\r\n:10:10\r\n:20:2\r\n:20:3\r\n-}')

    trailer = MtTrailerBlock()
    trailer.add_value('XXX', 'YYY')

    doc.trailer = trailer

    assert doc.compile() == (
        '{1:F011234567891230001000001}{2:I000987654321987N}{4:\r\n:10:10\r\n:20:2\r\n:20:3\r\n-}{5:{XXX:YYY}}')

    with pytest.raises(ValidationError):
        doc.name1 = 20  # Нет в choices.


def test_tag_validation():

    doc = MyDoc('123456789123', '987654321987', 1, 1)

    with pytest.raises(ValidationError):
        doc.compile()  # name1 обязательное

    doc.name1 = 10

    with pytest.raises(ValidationError):
        doc.name1 = 100

    doc.name2 = [10]
    assert doc._fields['30C'] == '10'

    with pytest.raises(ValidationError):
        # Количество не подходит не под одну опцию.
        doc.name2 = [1, 2, 3, 4, 5]

    doc.name2 = ['/abc', 'qq']

    assert '30C' not in doc._fields
    assert doc._fields['30B'] == '/abc\r\nqq'
