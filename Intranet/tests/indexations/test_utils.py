import pytest

from intranet.search.core.sources.doc import utils
from intranet.search.core.sources.map import utils as maps_utils


@pytest.mark.parametrize('inp,out', [
    ('AsdAsAd', ['asdasad', 'asd as ad', 'asdas ad', 'asd asad']),
    ('qweQwe', ['qweqwe', 'qwe qwe']),
    ('UTCTime', ['utctime', 'utc time']),
    ('just SOME trash Text', []),
    ('''
    text to test
    foundMultiline
    hope it works
    ''', ['foundmultiline', 'found multiline']),
])
def test_get_camelcase_keywords(inp, out):
    assert utils.get_camelcase_keywords(inp) == out


@pytest.mark.parametrize('inp,out', [
    ('раз два три', ['раз', 'два', 'три']),
    ('раз.два-три', ['раз', 'два', 'три']),
    ('раз_два_?_!_три,4четыре', ['раз', 'два', 'три', '4четыре']),
])
def test_split_by_not_letters(inp, out):
    assert maps_utils.split_by_not_letters(inp) == out


@pytest.mark.parametrize('inp,out', [
    ('100.AAA', None),
    ('1.АБВ', ['1', 'АБВ']),
    ('2,AAA', None),
    ('-1.AAA', None),
    ('4.', None),
    ('5..1', ['5', '.1']),
    ('1.404', ['1', '404']),
])
def test_get_enumerated_name(inp, out):
    if out is not None:
        assert list(maps_utils.get_enumerated_name(inp).groups()) == out
    else:
        assert maps_utils.get_enumerated_name(inp) is None
