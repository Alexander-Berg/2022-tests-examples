from infra.ya_salt.lib import hashutil


def test_gen_server_num():
    assert hashutil.gen_server_num('fqdn') == 44
    # Check consistent results
    for _ in xrange(10):
        assert hashutil.gen_server_num('sas1-5166.search.yandex.net') == 66
