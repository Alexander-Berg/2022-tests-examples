from smarttv.droideka.proxy.common import fix_schema, is_onto_id


def test_fix_schema():
    assert fix_schema('ya.ru') == 'https://ya.ru'
    assert fix_schema('//ya.ru') == 'https://ya.ru'
    assert fix_schema('http://ya.ru') == 'http://ya.ru'
    assert fix_schema('ftp://ya.ru') == 'ftp://ya.ru'
    assert fix_schema('home-app://ya.ru') == 'home-app://ya.ru'


def test_is_onto_id():
    assert is_onto_id('bce3e4c8fad5442c85b4b1ab53f05230') is False
    assert is_onto_id('d3e360a0578041a19f3c0cb0bb042c7d') is False
    assert is_onto_id('ruw7700023') is True
