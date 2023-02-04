from __future__ import unicode_literals

import yp.data_model

from infra.mc_rsc.src import storage


def test_pod_storage():
    s = storage.PodClusterStorage()
    assert s.size() == 0

    p1 = yp.data_model.TPod()
    p1.meta.id = 'p1-id'
    p1.meta.pod_set_id = 'ps1-id'
    p2 = yp.data_model.TPod()
    p2.meta.id = 'p2-id'
    p2.meta.pod_set_id = 'ps1-id'
    p3 = yp.data_model.TPod()
    p3.meta.id = 'p3-id'
    p3.meta.pod_set_id = 'ps2-id'
    pods = [p1, p2, p3]
    ts1 = 2
    ts2 = 1
    ts3 = 3
    s.put(p1, ts1)
    s.put(p2, ts2)
    s.put(p3, ts3)
    assert s.size() == 3
    for p in pods:
        assert s.get(p.meta.id) == p
    assert {p.meta.id: p for p in s.list_by_ps_id('ps1-id')} == {'p1-id': p1, 'p2-id': p2}
    assert {p.meta.id: p for p in s.list_by_ps_id('ps2-id')} == {'p3-id': p3}
    assert s.get_last_deploy_timestamp('ps1-id') == ts1
    assert s.get_last_deploy_timestamp('ps2-id') == ts3

    p2.spec.node_filter = 'filter'
    s.replace(p2, 4)
    assert s.size() == 3
    assert s.get(p2.meta.id) == p2
    assert s.get_last_deploy_timestamp('ps1-id') == 4

    s.remove(p1.meta.id, 5)
    assert s.size() == 2
    assert s.get(p1.meta.id) is None
    assert {p.meta.id: p for p in s.list_by_ps_id('ps1-id')} == {'p2-id': p2}
    assert s.get_last_deploy_timestamp('ps1-id') == 5
