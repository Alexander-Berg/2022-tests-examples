# -*- coding: utf-8 -*-
from tests import object_builder as ob

from balance.corba_buffers import StateBuffer, RequestBuffer


params = {
    'prot_remote_ip': '95.108.172.0',
    'prot_host': 'balance.yandex.ru'
}
def get_state_obj():
    return StateBuffer(params=params)


def get_request_obj(additional_cookies=[]):
    in_headers = [
        ('User-Agent', 'Mozilla/5.0')
    ]
    in_cookies = [
        ('Session_id', str(ob.get_big_number())),
        ('sessionid2', str(ob.get_big_number())),
        ('yandexuid', str(ob.get_big_number()))
    ] + additional_cookies
    return RequestBuffer(
        params=[
            [],  # in_params
            in_headers,  # in_headers
            in_cookies,  # in_cookies
        ],
    )


def test_redirect_for_docs(muzzle_logic):
    state_obj = get_state_obj()

    state_obj.setParam('balance_user_docs', '1')

    res = muzzle_logic.redirect_for_docs_experiment(state_obj)

    assert res.tag == 'redirect'
    assert res.text == 'docs.xml?'
    assert state_obj.getParam('skip') == '1'


def test_redirect_for_docs_with_path(muzzle_logic):
    state_obj = get_state_obj()

    state_obj.setParam('balance_user_docs', '1')

    res = muzzle_logic.redirect_for_docs_experiment(state_obj, '#/acts')

    assert res.tag == 'redirect'
    assert res.text == 'docs.xml?#/acts'
    assert state_obj.getParam('skip') == '1'


def test_non_redirect(muzzle_logic):
    state_obj = get_state_obj()
    request_obj = get_request_obj()

    res = muzzle_logic.redirect_for_docs_experiment(state_obj)

    assert res.tag == 'empty'
    assert res.text == None
    assert not state_obj.hasParam('skip')
