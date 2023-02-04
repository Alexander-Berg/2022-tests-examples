import json
import pytest

from common import BACKEND_REQ, CONFIG1
from configs import Config

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.process import BalancerStartError
from balancer.test.util.stdlib.multirun import Multirun


CONFIG2 = {
    'crypt_secret_key': 'duoYujaikieng9airah4Aexai4yek4qu',
    'crypt_preffixes': '/prefix/',
    'crypt_enable_trailing_slash': False,
    'backend_url_re': r'https://backend\.local/.*',
}


def check_backend_req(ctx, client_path, backend_path):
    asserts.status(ctx.perform_request(http.request.get(client_path)), 200)
    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, backend_path)


def update_secrets_file(ctx, data):
    ctx.perform_request(http.request.get())
    old = ctx.get_unistat()['cryprox-update_secrets_summ']

    ctx.manager.fs.rewrite('secrets_file', data)

    for run in Multirun():
        with run:
            ctx.perform_request(http.request.get())
            assert ctx.get_unistat()['cryprox-update_secrets_summ'] > old


def test_bad_secrets_json(ctx):
    secrets_file = ctx.manager.fs.create_file('secrets_file')
    ctx.manager.fs.rewrite('secrets_file', '{')

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(Config(partner_token='token', secrets_file=secrets_file))


@pytest.mark.parametrize('param', ['crypt_secret_key', 'crypt_preffixes', 'crypt_enable_trailing_slash'])
def test_bad_secrets_no_param(ctx, param):
    config = dict(CONFIG2)
    config.pop(param)

    secrets_file = ctx.manager.fs.create_file('secrets_file')
    ctx.manager.fs.rewrite('secrets_file', json.dumps(config))

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(Config(partner_token='token', secrets_file=secrets_file))


def test_bad_secrets_backend_matcher(ctx):
    config = dict(CONFIG2)
    config['backend_url_re'] = '*'

    secrets_file = ctx.manager.fs.create_file('secrets_file')
    ctx.manager.fs.rewrite('secrets_file', json.dumps(config))

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(Config(partner_token='token', secrets_file=secrets_file))


def test_bad_secrets_update_json(ctx):
    ctx.start_backend(SimpleConfig())

    secrets_file = ctx.manager.fs.create_file('secrets_file')
    ctx.manager.fs.rewrite('secrets_file', json.dumps(CONFIG2))
    ctx.start_balancer(Config(partner_token='token', secrets_file=secrets_file))

    check_backend_req(ctx, BACKEND_REQ, BACKEND_REQ)

    update_secrets_file(ctx, '{')
    assert ctx.get_unistat()['cryprox-bad_secrets_file_summ'] == 1

    update_secrets_file(ctx, json.dumps(CONFIG1))
    check_backend_req(ctx, BACKEND_REQ, '/')


@pytest.mark.parametrize('param', ['crypt_secret_key', 'crypt_preffixes', 'crypt_enable_trailing_slash'])
def test_bad_secrets_update_no_param(ctx, param):
    ctx.start_backend(SimpleConfig())

    secrets_file = ctx.manager.fs.create_file('secrets_file')
    ctx.manager.fs.rewrite('secrets_file', json.dumps(CONFIG2))
    ctx.start_balancer(Config(partner_token='token', secrets_file=secrets_file))

    check_backend_req(ctx, BACKEND_REQ, BACKEND_REQ)

    config = dict(CONFIG2)
    config.pop(param)
    update_secrets_file(ctx, json.dumps(config))
    assert ctx.get_unistat()['cryprox-bad_secrets_file_summ'] == 1

    update_secrets_file(ctx, json.dumps(CONFIG1))
    check_backend_req(ctx, BACKEND_REQ, '/')


def test_bad_secrets_update_backend_matcher(ctx):
    ctx.start_backend(SimpleConfig())

    secrets_file = ctx.manager.fs.create_file('secrets_file')
    ctx.manager.fs.rewrite('secrets_file', json.dumps(CONFIG2))
    ctx.start_balancer(Config(partner_token='token', secrets_file=secrets_file))

    check_backend_req(ctx, BACKEND_REQ, BACKEND_REQ)

    config = dict(CONFIG2)
    config['backend_url_re'] = '*'
    update_secrets_file(ctx, json.dumps(config))
    assert ctx.get_unistat()['cryprox-bad_secrets_file_summ'] == 1

    update_secrets_file(ctx, json.dumps(CONFIG1))
    check_backend_req(ctx, BACKEND_REQ, '/')
