import pytest

from billing.yandex_pay.yandex_pay.utils.domain import get_canonical_origin


@pytest.mark.parametrize(('input', 'output'), [
    ('http://ya.ru', 'http://ya.ru:80'),
    ('https://ya.ru', 'https://ya.ru:443'),
    ('http://ya.ru:567', 'http://ya.ru:567'),
    ('https://ya.ru:567', 'https://ya.ru:567'),
    ('http://привет.рф', 'http://xn--b1agh1afp.xn--p1ai:80'),
    ('https://привет.рф', 'https://xn--b1agh1afp.xn--p1ai:443'),
    ('http://привет.рф:567', 'http://xn--b1agh1afp.xn--p1ai:567'),
    ('https://привет.рф:567', 'https://xn--b1agh1afp.xn--p1ai:567'),
    ('http://xn--b1agh1afp.xn--p1ai', 'http://xn--b1agh1afp.xn--p1ai:80'),
    ('http://xn--b1agh1afp.xn--p1ai:567', 'http://xn--b1agh1afp.xn--p1ai:567'),
    ('https://xn--b1agh1afp.xn--p1ai', 'https://xn--b1agh1afp.xn--p1ai:443'),
    ('https://xn--b1agh1afp.xn--p1ai:567', 'https://xn--b1agh1afp.xn--p1ai:567'),
])
def test_canonical_origin(input, output):
    """
    Фиксируем ожидаемое поведение канонизатора ориджинов.
    """
    assert get_canonical_origin(input) == output
