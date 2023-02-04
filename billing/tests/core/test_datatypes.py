from decimal import Decimal as Decimal_

from mdh.core.schemas.datatypes import *


def test_represent():
    assert Int.represent(None) == '<unspecified>'
    assert Int.represent(33) == '33'

    assert Bool.represent(33) == 'yes'
    assert Bool.represent(False) == 'no'
    assert Bool.represent(None) == '<unspecified>'

    class RecordMock:

        me_localized = 'sample'

    assert ForeignKey.represent(None) == '<unspecified>'
    assert ForeignKey.represent(RecordMock()) == 'sample'

    assert DateTime.represent(None) == '<unspecified>'
    assert DateTime.represent(datetime(2020, 1, 10, 15, 42)) == 'Jan. 10, 2020, 3:42 p.m.'

    assert DateTime.represent(1234) == '1234'


def test_foreign_key():
    defdict = {
        'alias': 'fk',
        'params': {
            'ref': 'some',
            'flt': {
                'key1': 'staticvalue',
                'key2': '_master_uid',
            },
        },
    }
    fk = DataType.spawn(defdict)
    assert fk.to_dict() == defdict


def test_casting():
    assert Bool.cast('true') is True
    assert Bool.cast(True) is True

    assert Int.cast(' 20') == 20
    assert Int.cast(20) == 20

    assert Map.cast((('a', 'b'),)) == {'a': 'b'}

    assert Str.cast(125) == '125'
    assert Str.cast('125') == '125'

    assert Decimal.cast('1.0002') == Decimal_('1.0002')
    assert Decimal.cast(Decimal_('1.0002')) == Decimal_('1.0002')

    assert Date.cast('2021-05-20') == date(2021, 5, 20)
    assert Date.cast(date(2021, 5, 20)) == date(2021, 5, 20)

    assert DateTime.cast('2021-05-20T10:30') == datetime(2021, 5, 20, 10, 30)
    assert DateTime.cast(datetime(2021, 5, 20, 10, 30)) == datetime(2021, 5, 20, 10, 30)

    assert ForeignKey.cast('9ee977a6-dfd5-40fe-9e01-538fd2676090') == UUID('9ee977a6-dfd5-40fe-9e01-538fd2676090')
    assert ForeignKey.cast(UUID('9ee977a6-dfd5-40fe-9e01-538fd2676090')) == UUID('9ee977a6-dfd5-40fe-9e01-538fd2676090')
