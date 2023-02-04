# -*- coding: utf-8 -*-
import json
from six import iteritems
try:
    from itertools import izip as zip
except:  # py3
    pass
from library.python import resource

EPS_EXACT = 2e-5
EPS_APPROX = 1e-4


def compare_dicts(dict_actual, dict_expected, tag, eps):
    assert set(dict_actual.keys()) == set(dict_expected.keys()), 'Different keys %s' % tag
    for (hash, vector_actual) in iteritems(dict_actual):
        vector_expected = dict_expected[hash]
        for elem_actual, elem_expected in zip(vector_actual.split(' '), vector_expected.split(' ')):
            assert abs(float(elem_actual) - float(elem_expected)) < eps, (hash, tag)


def test_dssm_step(yt_client, yql_client, dssm_main_retro_instance):
    dssm_main_retro_instance.step_2_dssm_step()

    hash2vector = dict()
    for row in yt_client.read_table(dssm_main_retro_instance.config.vectors_table):
        hash2vector[row['hash']] = row['vector']

    compare_dicts(
        dict_actual=hash2vector,
        dict_expected=json.loads(resource.find('expected/vectors_table_exact')),
        tag='new_vector',
        eps=EPS_EXACT
    )

    compare_dicts(
        dict_actual=hash2vector,
        dict_expected=json.loads(resource.find('expected/vectors_table_approx')),
        tag='old_vector',
        eps=EPS_APPROX
    )
