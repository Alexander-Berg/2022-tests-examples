from infra.rtc.janitor.common import is_fqdn, is_inv


def test_is_fqdn():
    assert is_fqdn('sas5-4422.search.yandex.net')
    assert is_fqdn('sas5-0961.search.yandex.net')
    assert not is_fqdn('101027234')
    assert not is_fqdn('Search')
    assert not is_fqdn('Services')
    assert not is_fqdn('infrastructure')
    assert not is_fqdn('services.infrastructure')
    assert not is_fqdn('|')
    assert not is_fqdn('>')


def test_is_inv():
    assert not is_inv('sas5-4422.search.yandex.net')
    assert not is_inv('sas5-0961.search.yandex.net')
    assert is_inv('101027234')
    assert not is_inv('| > ')
