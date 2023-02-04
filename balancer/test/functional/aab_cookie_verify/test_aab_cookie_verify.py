# -*- coding: utf-8 -*-
import pytest
import time

from configs import ModAabCookieVerifyConfig

from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http
from balancer.test.util import asserts
from Crypto.Cipher import AES
import base64

KEY = 'very_long_super_private_key_________'


def hash_string(data):
    return str(sum(ord(ch) * (i + 1) for i, ch in enumerate(data)))


def decrypt(data, crypt_key):
    cipher = AES.new(crypt_key[:16], AES.MODE_ECB)
    decoded = base64.b64decode(data)
    return cipher.decrypt(decoded).strip()


def encrypt(data, crypt_key):
    cipher = AES.new(crypt_key[:16], AES.MODE_ECB)
    data = data.ljust((len(data) // 16 + 1) * 16, " ")
    encrypted = cipher.encrypt(data)
    return base64.b64encode(encrypted)


def get_crypted_cookie_value(crypt_key, ip=None, user_agent=None, accept_language=None, generate_time=None):
    ip = ip if ip else '127.0.100'  # invalid ip for testenv mismatch
    user_agent = user_agent if user_agent else 'mozilla'
    accept_language = accept_language if accept_language else 'ru'
    generate_time = generate_time if generate_time else int(time.time())
    data = "{}\t{}\t{}\t{}".format(generate_time, hash_string(ip), hash_string(user_agent), hash_string(accept_language))
    return encrypt(data, crypt_key)


def start_balancer(ctx, **kwargs):
    f = ctx.manager.fs.create_file('key.priv')
    ctx.manager.fs.rewrite(f, KEY)
    ctx.start_balancer(ModAabCookieVerifyConfig(aes_key_path=f, **kwargs))


def run_test(ctx, request_headers, cookie, content):
    headers = {}
    if cookie:
        headers.update({
            'Cookie': 'cycada={}'.format(cookie)
        })
    headers.update(request_headers)
    resp = ctx.perform_request(http.request.get(headers=headers))
    asserts.status(resp, 200)
    asserts.content(resp, content)


@pytest.mark.parametrize(
    ['ip', 'req_ip', 'content'],
    [
        ('127.0.0.1', '127.0.0.5', 'antiadblock'),
        ('127.0.0.1', '127.0.0.1', 'default'),
    ]
)
def test_ip(ctx, ip, req_ip, content):
    start_balancer(ctx, ip_header='x-real-ip')
    cookie = get_crypted_cookie_value(KEY, ip=ip)
    run_test(ctx, {'x-real-ip': req_ip}, cookie, content)


def test_expired_cookie(ctx):
    start_balancer(ctx)
    cookie = get_crypted_cookie_value(KEY, generate_time=1000)
    run_test(ctx, {}, cookie, 'antiadblock')


@pytest.mark.parametrize(
    # default 'mozilla', 'ru'
    ['user_agent', 'accept_language', 'content'],
    [
        ('mozilla', 'ru', 'default'),
        ('mozilla ', 'ru', 'default'),
        ('asdf', 'ru', 'antiadblock'),
        ('mozilla', 'eng', 'antiadblock')
    ]
)
def test_all_mismatch(ctx, user_agent, accept_language, content):
    start_balancer(ctx)
    cookie = get_crypted_cookie_value(KEY)
    run_test(ctx, {'user-agent': user_agent, 'Accept-Language': accept_language}, cookie, content)


@pytest.mark.parametrize(
    ['cookie', 'content'],
    [
        ('abcad', 'antiadblock'),
        (None, 'antiadblock'),
        (encrypt('abc\tdef', KEY), 'antiadblock')
    ]
)
def test_cookie_invalid(ctx, cookie, content):
    start_balancer(ctx)
    run_test(ctx, {}, cookie, content)


def test_disable_file(ctx):
    priv = ctx.manager.fs.create_file('key.priv')
    ctx.manager.fs.rewrite(priv, KEY)
    disable = ctx.manager.fs.create_file('disable')
    ctx.start_balancer(ModAabCookieVerifyConfig(aes_key_path=priv, disable_antiadblock_file=disable))
    run_test(ctx, {}, None, 'default')


def test_balancer_not_start(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(ModAabCookieVerifyConfig())
