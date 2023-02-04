from __future__ import unicode_literals

import flask
from infra.swatlib.auth import authenticator


def test_authenticator_groups_cache_ttl():
    a = flask.Flask('test')
    r = authenticator.Authenticator(app=a, enable=True, groups_cache_ttl=12345)
    assert r._user_groups_cache.ttl == 12345
    r = authenticator.Authenticator(app=a, enable=True)
    assert r._user_groups_cache.ttl == r.CACHE_TTL
