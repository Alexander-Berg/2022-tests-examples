from maps.wikimap.stat.tasks_payment.dictionaries.puid_map.lib.puid_tree import outsource_company


def test_should_get_company_name_from_acl_group():
    assert outsource_company(b'outsource-group.company-name') == b'company-name'


def test_should_return_unknown_for_common_outsource_group():
    assert outsource_company(b'outsource-group') == b'unknown'


def test_should_return_unknown_for_empty_string():
    assert outsource_company(b'') == b'unknown'


def test_should_return_none_for_none():
    assert outsource_company(None) is None
