from products.lib.canonize.python.lib.canonize_url import canonize_url
import pytest


@pytest.mark.parametrize(
    'url,canonization',
    [
        ("https://www.mvideo.ru/products/nastolnaya-igra-18-915187-40074123/specification", "mvideo.ru/products/nastolnaya-igra-18-915187-40074123"),
        ("https://www.ozon.ru/context/detail/id/261626668/", "ozon.ru/context/detail/id/261626668"),
    ]
)
def test_canonize_url(url, canonization):
    assert canonize_url(url) == canonization
