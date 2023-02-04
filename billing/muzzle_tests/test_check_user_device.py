# -*- coding: utf-8 -*-
import pytest
from tests import object_builder as ob
from balance.corba_buffers import RequestBuffer

desktop = ' Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.41 YaBrowser/21.2.0.1122 Yowser/2.5 Safari/537.36'
mobile = 'Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1'


def get_request_obj(user_agent):
    in_headers = [
        ('User-Agent', user_agent)
    ]
    in_cookies = [
        ('Session_id', str(ob.get_big_number())),
        ('sessionid2', str(ob.get_big_number())),
        ('yandexuid', str(ob.get_big_number()))
    ]
    return RequestBuffer(
        params=[
            [],  # in_params
            in_headers,
            in_cookies
        ],
    )


def test_mobile_device(muzzle_logic):
    request_obj = get_request_obj(mobile)

    res = muzzle_logic.check_user_device(request_obj)

    assert res.findtext('is-mobile') == 'true'
    assert res.findtext('is-pc') == 'false'


def test_desktop_device(muzzle_logic):
    request_obj = get_request_obj(desktop)

    res = muzzle_logic.check_user_device(request_obj)

    assert res.findtext('is-mobile') == 'false'
    assert res.findtext('is-pc') == 'true'


def test_invalid_user_agent(muzzle_logic):
    request_obj = get_request_obj('Some invalid header 1000')

    res = muzzle_logic.check_user_device(request_obj)

    assert res.findtext('is-mobile') == 'false'
    assert res.findtext('is-pc') == 'false'


@pytest.mark.parametrize('header', [
    'INVALID',
    ''
])
def test_invalid_user_agent(muzzle_logic, header):
    request_obj = get_request_obj(header)

    res = muzzle_logic.check_user_device(request_obj)

    assert res.findtext('is-mobile') == 'false'
    assert res.findtext('is-pc') == 'false'
