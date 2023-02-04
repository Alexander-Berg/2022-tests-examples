import pytest

from intranet.femida.src.utils.strings import slugify


slugify_test_cases = [
    ('Файл.jpg', 'Fayl.jpg'),
    ('Снимок экрана.pdf', 'Snimok ekrana.pdf'),
    ('lorem ipsum', 'lorem ipsum'),
    ('Чщщ!!', 'Chschsch!!'),
]


@pytest.mark.parametrize('test_case', slugify_test_cases)
def test_slugify(test_case):
    value, result = test_case
    assert result == slugify(value)
