#!/usr/bin/env python
# -*- coding: utf-8 -*-
from ..common.shooters import shoot_geosaas_service_routemn
from tqdm import tqdm

points_msk = {
    'ryazanka': (55.730475, 37.745264),
    'frunz': (55.729070, 37.586681),
    'volgogradka1': (55.700413, 37.798605),
    'volgogradka2': (55.714237, 37.717924),
}

def get_src_dst(from_name='ryazanka', to_name='frunz'):
    return points_msk[from_name][::-1], points_msk[to_name][::-1]


def test_unlim_key(n_tries=10):
    src, dst = get_src_dst()
    apikey = '2bc1dda5-3f34-4cce-abb2-45b9bdc8f0d8'

    n_has_distance = 0
    save_failed_res = None
    for _ in tqdm(range(n_tries)):
        res = shoot_geosaas_service_routemn(
            src,
            dst,
            debug=True,
            extra_params='&prestable=true&apikey=%s' % apikey,
            #         host='saas-searchproxy-maps.yandex.net',
            #         host='saas-searchproxy-outgone.yandex.net',
            service='default-router-external'
        )
        if res[0] > 0:
            n_has_distance += 1
        else:
            save_failed_res = res

    if save_failed_res is not None:
        print('test_unlim_key')
        print(save_failed_res)

    assert n_has_distance == n_tries


def test_nonexisting_key(n_tries=10):
    src, dst = get_src_dst()
    apikey = '544a4fcc-cdad-46c2-85ef-fa9a6bce46d3'

    n_has_distance = 0
    save_failed_res = None
    for _ in tqdm(range(n_tries)):
        res = shoot_geosaas_service_routemn(
            src,
            dst,
            debug=True,
            extra_params='&prestable=true&apikey=%s' % apikey,
            #         host='saas-searchproxy-maps.yandex.net',
            #         host='saas-searchproxy-outgone.yandex.net',
            service='default-router-external'
        )
        if res[0] > 0:
            n_has_distance += 1
        else:
            save_failed_res = res

    if save_failed_res is not None:
        print('test_nonexisting_key')
        print(save_failed_res)

    assert n_has_distance == 0




