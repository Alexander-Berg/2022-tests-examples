import mock

from sepelib.yandex.alemate import (ContainerResourceLimits, Iss3HooksResourceLimits, MetaData, Iss3HooksTimeLimits,
                                    IssHookTimeLimits, Instance)


def test_container_res_limits_from_dict(res_container_dict):
    con = ContainerResourceLimits.from_dict(res_container_dict)
    for limit in res_container_dict:
        assert res_container_dict[limit] == getattr(con, limit)


def test_container_res_limits_nanny_response():
    params = {'memory_limit': 8, 'cpu_limit': 4000, 'disk_guarantee': 500}
    con = ContainerResourceLimits.from_nanny_response(params)
    assert con.memory_limit == 8 * 1024 * 1024
    assert con.cpu_limit == 4
    assert con.disk_guarantee == 500


def test_container_res_limits_allocation_order():
    aloc_order = mock.Mock()
    aloc_order.cpu = 1000
    aloc_order.memory = 256
    aloc_order.disk = 1
    con = ContainerResourceLimits.from_allocation_order(aloc_order)
    assert con.cpu_limit == aloc_order.cpu / 1000
    assert con.memory_limit == aloc_order.memory * 1024 * 1024
    assert con.disk_limit == aloc_order.disk


def test_container_res_limits_gencfg_res():
    params = {'cpu_cores_limit': 16, 'cpu_cores_guarantee': 12}
    con = ContainerResourceLimits.from_gencfg_response(params)
    assert con.cpu_limit == 16
    assert con.cpu_guarantee == 12


def test_container_res_limits_update(res_container_dict):
    con = ContainerResourceLimits.from_dict(res_container_dict)
    other_limits = ContainerResourceLimits.from_dict({
        'memory_guarantee': 100, 'memory_limit': 150, 'cpu_guarantee': 8, 'cpu_limit': 16}
    )
    con.update(other_limits)
    assert con.memory_guarantee == 100
    assert con.memory_limit == 150
    assert con.cpu_guarantee == 8
    assert con.cpu_limit == 16


def test_container_res_limits_to_iss_dict(res_container_dict):
    con = ContainerResourceLimits.from_dict(res_container_dict)
    iss_dict = con.to_iss_dict()
    for limit in iss_dict:
        if limit in ['cpu_guarantee', 'cpu_limit']:
            assert '{:f}c'.format(getattr(con, limit)) == iss_dict[limit]
        elif limit in ['net_guarantee', 'net_limit']:
            assert 'default: ' + str(getattr(con, limit)) == iss_dict[limit]
        else:
            assert getattr(con, limit) == iss_dict[limit]


def test_container_res_limits_to_dict(res_container_dict):
    con = ContainerResourceLimits.from_dict(res_container_dict)
    d = con.to_dict()
    assert d == res_container_dict


def test_container_res_limits_repr():
    params = {'memory_guarantee': 250, 'memory_limit': 250}
    con = ContainerResourceLimits.from_dict(params)
    assert str(con).startswith("Instance(['memory_guarantee=250', 'memory_limit=250'")
    assert con.__repr__() == str(con)


def test_iss3hook_res_limits():
    params = {'iss_hook_stop': {'memory_limit': 8, 'cpu_limit': 4000}}
    i = Iss3HooksResourceLimits.from_nanny_response(params)
    assert i.iss_hook_stop.memory_limit == 8 * 1024 * 1024
    assert i.iss_hook_stop.cpu_limit == 4


def test_iss3hook_res_limits_to_iss_dict(res_container_dict):
    i = Iss3HooksResourceLimits.from_nanny_response({'iss_hook_install': res_container_dict})
    iss_dict = i.to_iss_dict()
    iss_dict_res_limits = i.iss_hook_install.to_iss_dict()
    for limit in iss_dict:
        assert iss_dict[limit] == iss_dict_res_limits[limit.split('.')[1]]


def test_metadata_from_nanny_res():
    res = {'annotations': [{'key': 'a', 'value': 'A'}]}
    m = MetaData.from_nanny_response(res)
    assert m.annotations['a'] == 'A'
    m = MetaData.from_nanny_response({})
    assert not m.annotations


def test_iss3_hooks_time_limits(iss_hook_time_limits_dict):
    i = Iss3HooksTimeLimits.from_dict(
        {'iss_hook_start': iss_hook_time_limits_dict, 'iss_hook_status': iss_hook_time_limits_dict}
    )
    assert i.iss_hook_start.min_restart_period == 1
    assert i.iss_hook_start.max_execution_time == 2
    assert i.iss_hook_start.restart_period_backoff == 3
    assert i.iss_hook_start.max_restart_period == 4
    assert i.iss_hook_start.restart_period_scale == 5
    assert i.iss_hook_status.min_restart_period == 1
    assert i.iss_hook_status.max_execution_time == 2
    assert i.iss_hook_status.restart_period_backoff == 3
    assert i.iss_hook_status.max_restart_period == 4
    assert i.iss_hook_status.restart_period_scale == 5
    assert i.iss_hook_stop.min_restart_period is None


def test_iss_hook_time_limit(iss_hook_time_limits_dict):
    del iss_hook_time_limits_dict['restart_period_scale']
    i = IssHookTimeLimits.from_dict(iss_hook_time_limits_dict)
    assert i.min_restart_period == 1
    assert i.max_execution_time == 2
    assert i.restart_period_backoff == 3
    assert i.max_restart_period == 4
    assert i.restart_period_scale is None


def test_instance_as_string():
    i = Instance('host.name', 7777, conf='some_conf')
    assert i.as_string() == 'host:7777@some_conf'
    assert i.as_string() == str(i)
    assert Instance('host.name', 7777).as_string() == 'host:7777@None'


def test_instance_from_dict(res_container_dict):
    i = Instance.from_dict({'host': 'host.host', 'port': 0, 'limits': res_container_dict})
    for limit in res_container_dict:
        assert res_container_dict[limit] == getattr(i.resource_limits, limit)


def test_instance_gencfg_res(res_container_dict):
    params = {'hostname': 'host', 'port': 8888, 'shard_name': 'shard_name', 'limits': res_container_dict}
    i = Instance.from_gencfg_response(params)
    for limit in res_container_dict:
        assert res_container_dict[limit] == getattr(i.resource_limits, limit)
    assert i.virtual_shard == params['shard_name']
    assert i.host == params['hostname']
    assert i.port == params['port']


def test_instance_repr():
    i = Instance(host='host', port=666)
    assert i.__repr__().startswith("Instance([\"host='host'\", ")


def test_instance_cmp():
    i1 = Instance(host='host', port=666)

    i2 = Instance(host='host', port=666)
    assert i1.__cmp__(i2) == 0

    i2 = Instance(host='a_host', port=666)
    assert i1.__cmp__(i2) == 1

    i2 = Instance(host='host', port=667)
    assert i1.__cmp__(i2) == -1


def test_instance_hash_and_eq():
    i1 = Instance(host='host', port=666)
    i2 = Instance(host='host', port=666)
    assert hash(i1) == hash(i2)
    assert i1 == i2

    i2.port = 667
    assert hash(i1) != hash(i2)
    assert i1 != i2
