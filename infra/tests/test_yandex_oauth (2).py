import pytest

from sepelib.yandex import oauth


def test_token_checker():
    c = oauth.TokenChecker('1', [])
    # Check no scopes case
    with pytest.raises(oauth.OAuthException):
        c.check('2', 'nanny:all')
    assert c.check('1') is True
    # Check scopes
    c = oauth.TokenChecker('1', ['nanny:all'])
    with pytest.raises(oauth.OAuthException):
        c.check('2', 'crypta:all')
    assert c.check('2', 'nanny:all')


def check_oauth_init():
    cfg = {
        'url': 'http://oauth.yandex-team.ru',
        'client_id': '32480s9dfsdfqsa',
        'client_secret': '2342dsfasdfadsf',
        'scopes': 'nanny:all',
    }
    o = oauth.OAuth.from_config(cfg)
    assert o.url == cfg['url']
    assert o.client_id == cfg['client_id']
    assert o.client_secret == 'client_secret'
    assert o.scopes == ['nanny:all']
    # Check when 'scopes' value is a string with spaces
    cfg['scopes'] = 'nanny:all qloud:all'
    o = oauth.OAuth.from_config(cfg)
    assert o.url == cfg['url']
    assert o.client_id == cfg['client_id']
    assert o.client_secret == 'client_secret'
    assert o.scopes == ['nanny:all', 'qloud:all']
    # Check when 'scopes' value is a list
    cfg['scopes'] = ['nanny:all', 'qloud:all']
    o = oauth.OAuth.from_config(cfg)
    assert o.url == cfg['url']
    assert o.client_id == cfg['client_id']
    assert o.client_secret == 'client_secret'
    assert o.scopes == ['nanny:all', 'qloud:all']
