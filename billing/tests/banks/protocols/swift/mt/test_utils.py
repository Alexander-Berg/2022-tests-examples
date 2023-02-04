from datetime import datetime
from decimal import Decimal

import pytest

from bcl.banks.protocols.swift.mt.utils import ReTranslator, ValueFormatter as Fmt
from bcl.banks.protocols.swift.mt.exceptions import RuleTokenizerError
from bcl.banks.protocols.swift.mt.utils import MtSubtagParser


def test_re_translator():
    assert ReTranslator.translate('4c') == '^[A-Z0-9]{1,4}$'
    assert ReTranslator.translate('/8c/4!n1!x4!n') == '^\/[A-Z0-9]{1,8}\/[0-9]{4}[a-zA-Z0-9+\/\-?:().,\' ]{1}[0-9]{4}$'
    assert ReTranslator.translate('4!c//[N]15d') == '^[A-Z0-9]{4}\/\/(N)?[0-9,]{1,15}$'

    with pytest.raises(RuleTokenizerError):
        ReTranslator.translate('home')

    assert ReTranslator.translate('3*1a') == r'^[A-Z]{1,1}(\r\n[A-Z]{1,1})?(\r\n[A-Z]{1,1})?$'

    assert ReTranslator.translate('2*25x') == r"^[a-zA-Z0-9+\/\-?:().,' ]{1,25}(\r\n[a-zA-Z0-9+\/\-?:().,' ]{1,25})?$"


def test_value_formatter():

    assert Fmt.parse_date('160930') == datetime(2016, 9, 30)
    assert Fmt.format_money_str('1 123,456 ') == '1123.456'

    assert Fmt.format_money(Decimal('1.15')) == '1,15'
    assert Fmt.format_money(Decimal('13')) == '13,'

    assert Fmt.format_date(datetime(2016, 9, 30)) == '160930'


def test_mt_subtag_extractor():

    tag_parser = MtSubtagParser(('TAG1',))
    tags_parser = MtSubtagParser(('TAG1', 'TAG2'))

    assert tag_parser.parse('/TAG1/Info') == {'TAG1': 'Info'}
    assert tag_parser.parse('UNSTRUCTED/TAG1/Info') == {'TAG1': 'Info'}
    assert tag_parser.parse('/TAG\r\n1/Info') == {'TAG1': 'Info'}
    assert tag_parser.parse('/\r\nT\r\nAG1/Info') == {'TAG1': 'Info'}

    assert tag_parser.parse('/TAG1/Subtag1/Subtag2/Subtag3') == {'TAG1': 'Subtag1/Subtag2/Subtag3'}
    assert tag_parser.parse('/TAG1//Subtag2/Subtag3') == {'TAG1': '/Subtag2/Subtag3'}

    assert tags_parser.parse('/TAG1/Info1/TAG2/Info2') == {'TAG1': 'Info1', 'TAG2': 'Info2'}
    assert tags_parser.parse('/TAG1/Info1\r\n/TAG2/Info2') == {'TAG1': 'Info1', 'TAG2': 'Info2'}

    assert tags_parser.parse('/TAG2//TAG1/') == {'TAG1': '', 'TAG2': ''}
