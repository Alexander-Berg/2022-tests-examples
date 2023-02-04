import pytest
from intranet.search.core.sources.utils import swap_layout, LAYOUT_EN, LAYOUT_RU, date_to_timestamp, get_suggest_parts


def test_swap_layout_ru():
    assert swap_layout(LAYOUT_RU, 'ru') == LAYOUT_EN


def test_swap_layout_en():
    assert swap_layout(LAYOUT_EN, 'en') == LAYOUT_RU


def test_swap_layout_skip_unknown():
    """ Неизвестные символы в раскладке остаются как есть
    """
    assert swap_layout(LAYOUT_EN, 'ru') == LAYOUT_EN
    assert swap_layout(LAYOUT_RU, 'en') == LAYOUT_RU
    assert swap_layout('Яндекс.Connect', 'en') == 'ЯндексюСщттусе'
    assert swap_layout('Яндекс.Connect', 'ru') == 'Zyltrc.Connect'


def test_date_to_timestamp():
    """ Даты в любой таймзоне и в utc правильно переводятся в таймстамп
    """
    assert date_to_timestamp('2017-10-04T18:47:28Z') == 1507142848
    assert date_to_timestamp('2017-10-04T18:47:28+03:00') == 1507132048
    assert date_to_timestamp('2017-10-04') == 1507075200
    assert date_to_timestamp('2017-10-04T00:00:00') == 1507075200
    assert date_to_timestamp('2017-10-04T00:00:00+0000') == 1507075200
    assert date_to_timestamp('2017-10-04T03:00:00+0300') == 1507075200
    assert date_to_timestamp('2017-10-04T00:00:00+0300') == 1507064400


@pytest.mark.parametrize('case', (
    ('маликова', ['маликова']),
    ('мали-кова', ['мали', 'кова']),
    ('мама мыла раму', ['мама', 'мыла', 'раму']),
    ('some-killer_feature? With, suggest.', ['some', 'killer', 'feature', 'With', 'suggest']),
    ('strip: colon: and; semicolon', ['strip', 'colon', 'and', 'semicolon']),
    ('skip a short word s', ['skip', 'short', 'word']),
))
def test_get_suggest_parts(case):
    """ Проверяем разделение строк на части для саджеста
    """
    value, expected = case
    assert list(get_suggest_parts(value)) == expected
