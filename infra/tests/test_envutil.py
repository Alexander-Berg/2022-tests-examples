import pytest
from infra.ya_salt.lib import envutil


@pytest.mark.parametrize(
    ('tag', 'result'),
    [('rtc', None),
     ('rtc.stage-prestable', 'prestable'),
     ('rtc.stage-production', 'production'),
     ('rtc.stage-exp', 'exp')])
def test_from_tags(tag, result):
    r, e = envutil.stage_from_tags(['mock1', tag, 'mock2'])
    assert e is None
    assert r == result


def test_from_tags_error():
    r, e = envutil.stage_from_tags(['mock1', 'rtc.stage-production', 'rtc.stage-unknown', 'mock2'])
    assert r is None
    assert 'multiple' in e
    r, e = envutil.stage_from_tags(['mock1', 'mock2'])
    assert r is None
    assert e is None


@pytest.mark.parametrize('tag', ['rtc.stage-prestable', 'rtc.stage-testing', 'rtc.stage-experiment'])
def test_non_prod_stage_true(tag):
    r, e = envutil.non_prod_stage(['mock1', tag, 'mock2'])
    assert e is None
    assert r is True


@pytest.mark.parametrize('tag', ['rtc.stage-production', 'rtc.stage-unknown', 'rtc.stage-stable'])
def test_non_prod_stage_false(tag):
    r, e = envutil.non_prod_stage(['mock1', tag, 'mock2'])
    assert e is None
    assert r is False


def test_non_prod_stage_error():
    r, e = envutil.non_prod_stage(['mock1', 'mock2'])
    assert r is None
    assert e.startswith('cannot determine production or prestable environment from tag set')


@pytest.mark.parametrize('loc', ['sas', 'vla', 'man', 'msk'])
def test_repo_hosts_from_tags_location_prod(loc):
    m, e = envutil.repo_hosts_from_tags_location(['mock', 'rtc.stage-production', 'mock2'], loc)
    assert e is None
    assert m == envutil.PROD_LOCATIONS_SETTINGS[loc][envutil.REPO_HOSTS]


@pytest.mark.parametrize('loc', ['sas', 'vla', 'man', 'msk'])
def test_repo_hosts_from_tags_location_prestable(loc):
    m, e = envutil.repo_hosts_from_tags_location(['mock', 'rtc.stage-prestable', 'mock2'], loc)
    assert e is None
    assert m == envutil.PRESTABLE_SETTINGS[envutil.REPO_HOSTS]


def test_repo_hosts_from_tags_location_error():
    m, e = envutil.repo_hosts_from_tags_location(['mock', 'rtc.stage-production', 'mock2'], 'xdc')
    assert e is not None
    assert m is None
    m, e = envutil.repo_hosts_from_tags_location(['mock', 'mock2'], 'msk')
    assert e is not None
    assert m is None


@pytest.mark.parametrize('loc', ['sas', 'vla', 'man', 'msk'])
def test_report_addr_from_tags_location_prod(loc):
    m, e = envutil.report_addr_from_tags_location(['mock', 'rtc.stage-production', 'mock2'], loc)
    assert e is None
    assert m == envutil.PROD_LOCATIONS_SETTINGS[loc][envutil.REPORT_ADDRS]


@pytest.mark.parametrize('loc', ['sas', 'vla', 'man', 'msk'])
def test_report_addr_from_tags_location_prestable(loc):
    m, e = envutil.report_addr_from_tags_location(['mock', 'rtc.stage-prestable', 'mock2'], loc)
    assert e is None
    assert m == envutil.PRESTABLE_SETTINGS[envutil.REPORT_ADDRS]


def test_report_addr_from_tags_location_error():
    m, e = envutil.report_addr_from_tags_location(['mock', 'rtc.stage-production', 'mock2'], 'xdc')
    assert e is not None
    assert m is None
    m, e = envutil.report_addr_from_tags_location(['mock', 'mock2'], 'msk')
    assert e is not None
    assert m is None
