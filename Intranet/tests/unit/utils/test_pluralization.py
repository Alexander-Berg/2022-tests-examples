import pytest

from intranet.femida.src.utils.pluralization import get_russian_plural_form, get_plural_form


russian_plural_form_test_strings = ('месяц', 'месяца', 'месяцев')
english_plural_form_test_strings = ('month', 'months')


@pytest.mark.parametrize('count, result', (
    (0, 'месяцев'),
    (1, 'месяц'),
    (2, 'месяца'),
    (4, 'месяца'),
    (5, 'месяцев'),
    (10, 'месяцев'),
    (11, 'месяцев'),
    (13, 'месяцев'),
    (21, 'месяц'),
    (24, 'месяца'),
    (25, 'месяцев'),
))
def test_get_russian_plural_form(count, result):
    assert result == get_russian_plural_form(count, *russian_plural_form_test_strings)


@pytest.mark.parametrize('count, result', (
    (1, 'month'),
    (10, 'months'),
))
def test_get_english_plural_form(count, result):
    assert result == get_plural_form(count, lang='en', *english_plural_form_test_strings)
