import json
import os

from infra.ya_salt.lib.components import server_info
from infra.ya_salt.lib import lldputil
from infra.ya_salt.proto import ya_salt_pb2

import yatest


def test_generate_info():
    grains = {
        # Should not be in info
        'pythonpath': '/usr/lib',
        # Should be in info (sorted)
        'gencfg': [
            'ZZZZZ',
            'ALL_INFRA_PRESTABLE',
        ],
    }

    # Bad case: lldp failed
    def recv_lldp_fail():
        return None, 'lldp in test failed'

    info = server_info.ServerInfo._generate_info(grains, _recv_lldp=recv_lldp_fail)
    assert 'pythonpath' not in info
    assert info['gencfg'] == sorted(grains['gencfg'])
    assert 'pillar' not in info
    assert 'name' in info['lui']
    assert 'timestamp' in info['lui']
    assert info['lldp'] == [{'switch': 'unknown', 'port': 'unknown'}]

    # Good case: lldp received
    def recv_lldp_ok():
        return lldputil.LldpInfo('sas1-s668', 'GE1/0/27'), None

    info = server_info.ServerInfo._generate_info(grains, _recv_lldp=recv_lldp_ok)
    assert info['lldp'] == [{'switch': 'sas1-s668', 'port': 'GE1/0/27'}]


def test_lui_default():
    l = server_info.Lui.from_disk('non-existing.json')
    assert l.ts_seconds == 0
    assert l.name == 'unknown'


def test_lui_ok():
    p = yatest.common.source_path('infra/ya_salt/lib/components/tests/lui-config.json')
    l = server_info.Lui.from_disk(p)
    assert l.name == 'web'
    assert l.ts_seconds == int(os.stat(p).st_mtime)


def test_lui_to_info():
    d = server_info.Lui.from_disk('non-existing.json').to_info()
    assert d == {'name': 'unknown', 'timestamp': 0}


def test_calc_next_update():
    for _ in xrange(10):
        next_time = server_info.ServerInfo.calc_next_update(time_func=lambda: 1000)
        min_time = 1000 + server_info.ServerInfo.PERIOD_SECONDS
        assert min_time <= next_time <= min_time + server_info.ServerInfo.JITTER_SECONDS


def test_update_info():
    info_path = './save_here'
    info = {
        'server_number': 52,
        'gencfg': ['ALL_INFRA_PRESTABLE', 'ZZZZZ'],
        'id': 'vla2-5600.search.yandex.net'
    }
    condition = ya_salt_pb2.Condition()
    condition.status = 'False'
    server_info.ServerInfo._update_info(info, condition, info_path=info_path)
    assert condition.status == 'True'
    assert condition.transition_time.seconds
    assert condition.message == 'OK'
    with open(info_path) as f:
        assert info == json.load(f)
