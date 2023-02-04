import pytest

from awacs.model.l3_balancer.errors import L3BalancerTransportError, RSGroupsConflict
from awacs.model.l3_balancer.l3mgr import RealServer, RSGroup


@pytest.mark.parametrize('line, fqdn, ip, config', [
    ('ya.ru=1.1.2.3 weight=3', 'ya.ru', '1.1.2.3', {'weight': 3}),
    ('ya.ru', 'ya.ru', '', {}),
    ('ya.ru weight=3', 'ya.ru', '', {'weight': 3}),
])
def test_rs_init_ok(line, fqdn, ip, config):
    rs = RealServer.from_l3mgr_string(line)
    assert fqdn == rs.fqdn
    assert ip == rs.ip
    assert config == rs.config


@pytest.mark.parametrize('line', [
    'ya.ru=1.1.2.3 bad_param=3',
])
def test_rs_init_error(line):
    with pytest.raises(L3BalancerTransportError):
        RealServer.from_l3mgr_string(line)


@pytest.mark.parametrize('line', [
    'ya.ru=1.1.2.3 weight=3',
    'ya.ru',
    'ya.ru weight=3',
    'ya.ru=1.2.3.4',
])
def test_rs_str(line):
    rs = RealServer.from_l3mgr_string(line)
    assert line == str(rs)


@pytest.mark.parametrize('first, second, result', [
    ('ya.ru', 'ya.ru=1.2.3.4', True),
    ('ya.ru=1.2.3.4', 'ya.ru', True),
    ('ya.ru=0.0.0.0', 'ya.ru=1.2.3.4', False),
    ('ya.ru=1.2.3.4 weight=1', 'ya.ru weight=1', True),
    ('ya.ru', 'ty.ru', False),
    ('ya.ru=1.2.3.4 weight=1', 'ya.ru', False),
    ('ya.ru=1.2.3.4', 'ya.ru weight=1', False),
])
def test_rs_similar(first, second, result):
    one = RealServer.from_l3mgr_string(first)
    other = RealServer.from_l3mgr_string(second)
    assert result is one.matches(other, ignore_ip=True)


@pytest.mark.parametrize('line', [
    'ya.ru=1.1.2.3 weight=3',
    'ya.ru',
    'ya.ru weight=3',
    'ya.ru=1.2.3.4',
])
def test_rs_hash(line):
    rg = RSGroup()
    rg.add_rs(RealServer.from_l3mgr_string(line))
    rg.add_rs(RealServer.from_l3mgr_string(line))
    assert len(rg.real_servers) == 1


def test_rs_group_init_ok():
    c = RSGroup.from_l3mgr_virtual_servers([{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5'], 'id': 1},
                                            {'group': ['ty.ru=2.3.4.5', 'ya.ru=1.2.3.4'], 'id': 2}])
    assert 2 == len(c.real_servers)
    assert 'ty.ru=2.3.4.5' == str(c.real_servers[0])


@pytest.mark.parametrize('vs', [
    [{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5'], 'id': 1},
     {'group': ['on.ru=2.3.4.5', 'ona.ru=1.2.3.4'], 'id': 2}],

    [{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5'], 'id': 1},
     {'group': ['ya.ru=1.2.3.4 weight=1', 'ty.ru=2.3.4.5'], 'id': 2}],

    [{'group': ['ya.ru=1.2.3.4 weight=2', 'ty.ru=2.3.4.5'], 'id': 1},
     {'group': ['ya.ru=1.2.3.4 weight=1', 'ty.ru=2.3.4.5'], 'id': 2}],

    [{'group': ['ya.ru=1.2.3.4 weight=2'], 'id': 1},
     {'group': ['ya.ru=1.2.3.4 weight=1', 'ty.ru=2.3.4.5'], 'id': 2}],
])
def test_rs_group_init_error(vs):
    with pytest.raises(RSGroupsConflict):
        RSGroup.from_l3mgr_virtual_servers(vs)


@pytest.mark.parametrize('vs1, vs2, result', [
    ([{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5'], 'id': 1}],
     [{'group': ['ya.ru', 'ty.ru'], 'id': 2}], True),

    ([{'group': ['ya.ru=1.2.3.4', 'ty.ru'], 'id': 1}],
     [{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5'], 'id': 2}], True),

    ([{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5 weight=1'], 'id': 1}],
     [{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5'], 'id': 2}], False),

    ([{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5'], 'id': 1}],
     [{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5 weight=1'], 'id': 2}], False),
])
def test_compare(vs1, vs2, result):
    g1 = RSGroup.from_l3mgr_virtual_servers(vs1)
    g2 = RSGroup.from_l3mgr_virtual_servers(vs2)
    assert g1.matches(g2, ignore_ip=True) == result


def test_need_to_update_ip_addresses():
    g1 = RSGroup.from_l3mgr_virtual_servers([{'group': ['ya.ru=1.2.3.4', 'ty.ru=2.3.4.5'], 'id': 1},
                                             {'group': ['ty.ru=2.3.4.5', 'ya.ru'], 'id': 2}])
    assert g1.need_to_update_ip_addresses
