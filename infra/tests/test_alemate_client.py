import gevent.pool
import pytest
import requests
import requests_mock
from infra.watchdog.src.lib.alemate_client import get_value, AlemateClient


@pytest.fixture
def alemate_client():
    return AlemateClient(nanny_url="http://nanny.test/", token=None)


def test_get_value():
    d1 = {'1': {'2': 'str'}}
    assert get_value(d1, '1/2') == 'str'
    d2 = {'1': {'2': None}}
    assert get_value(d2, '1/2', default=8) is None
    d3 = {'1': {'2': None}}
    assert get_value(d3, '1/3') is None
    d2 = {'1': {'2': None}}
    assert get_value(d2, '1/3', default='str') == 'str'
    assert get_value(None, '1/3', default='str') == 'str'


def test_get_unprocessed_taskgroups_count(alemate_client):
    url = 'http://nanny.test/v1/alemate/task_groups/count/'
    with requests_mock.Mocker() as m:
        m.get(url, json={'count': 18})
        assert alemate_client.get_unprocessed_taskgroups_count() == 18

        m.get(url, json={})
        with pytest.raises(KeyError):
            alemate_client.get_unprocessed_taskgroups_count()

        m.get(url, status_code=400)
        with pytest.raises(requests.HTTPError):
            alemate_client.get_unprocessed_taskgroups_count()


def test_calculate_worker_params(alemate_client):
    per_worker_limit, skips = alemate_client.calculate_worker_params(total_tasks=10, pool_size=11)
    assert per_worker_limit == 1
    assert skips == [s for s in range(10)]

    per_worker_limit, skips = alemate_client.calculate_worker_params(total_tasks=10, pool_size=10)
    assert per_worker_limit == 1
    assert skips == [s for s in range(10)]

    per_worker_limit, skips = alemate_client.calculate_worker_params(total_tasks=11, pool_size=10)
    assert per_worker_limit == 2
    assert skips == [s for s in range(0, 11, 2)]

    assert alemate_client.calculate_worker_params(total_tasks=10, pool_size=0) == (0, [])

    assert alemate_client.calculate_worker_params(total_tasks=0, pool_size=10) == (0, [])


def test_collect_tasks_info_enqueued(alemate_client):
    url = 'http://nanny.test/v1/alemate/task_groups/'
    children_url = 'http://nanny.test/v1/alemate/task_groups/{}/children/'
    with requests_mock.Mocker() as m:
        m.get(url, headers={'X-Total-Items': '0'})
        _, enqueued = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert enqueued.get() == 0

        m.get(url, headers={'X-Total-Items': '1'}, json=[{'id': 'tg1'}])
        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'schedulerOptions': {'status': 'NEW',
                                                                      'state': {'state': 'NEW'}}}])
        _, enqueued = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert enqueued.get() == 1

        m.get(url, headers={'X-Total-Items': '2'}, json=[{'id': 'tg1'}, {'id': 'tg2'}])
        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'schedulerOptions': {'status': 'NEW',
                                                                      'state': {'state': 'NEW'}}}])
        m.get(children_url.format('tg2'), json=[{'id': 't2',
                                                 'schedulerOptions': {'status': 'NEW',
                                                                      'state': {'state': 'NEW'}}}])
        # larger pool here breaks mocked response, so using 1 worker
        _, enqueued = alemate_client.collect_tasks_info(gevent.pool.Pool(size=1))
        assert enqueued.get() == 2

        m.get(url, headers={'X-Total-Items': '1'}, json=[{'id': 'tg1'}])
        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'schedulerOptions': {'status': 'NEW',
                                                                      'state': {'state': 'NEW'}}},
                                                {'id': 't2',
                                                 'schedulerOptions': {'status': 'NEW',
                                                                      'state': {'state': 'NEW'}}, }
                                                ])
        _, enqueued = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert enqueued.get() == 2

        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'schedulerOptions': {'status': 'NEW',
                                                                      'state': {'state': 'NEW'}}},
                                                {'id': 't2',
                                                 'schedulerOptions': {'status': 'NEW',
                                                                      'state': {'state': 'NEW'},
                                                                      'dependencies': ['t1']},
                                                 }
                                                ])
        _, enqueued = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert enqueued.get() == 1

        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'schedulerOptions': {'status': 'DONE',
                                                                      'state': {'state': 'DONE'}}},
                                                {'id': 't2',
                                                 'schedulerOptions': {'status': 'NEW',
                                                                      'state': {'state': 'NEW'},
                                                                      'dependencies': ['t1']},
                                                 }
                                                ])
        _, enqueued = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert enqueued.get() == 1

        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'schedulerOptions': {'status': 'DONE',
                                                                      'state': {'state': 'DONE'}}},
                                                {'id': 't2',
                                                 'schedulerOptions': {'status': 'DONE',
                                                                      'state': {'state': 'DONE'},
                                                                      'dependencies': ['t1']},
                                                 }
                                                ])
        _, enqueued = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert enqueued.get() == 0


def test_collect_tasks_info_snapshots(alemate_client):
    url = 'http://nanny.test/v1/alemate/task_groups/'
    children_url = 'http://nanny.test/v1/alemate/task_groups/{}/children/'
    with requests_mock.Mocker() as m:
        m.get(url, headers={'X-Total-Items': '0'})
        snapshots, _ = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert snapshots == {}

        m.get(url, headers={'X-Total-Items': '1'}, json=[{'id': 'tg1'}])
        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'processorOptions': {'type': 'SetSnapshotTargetState',
                                                                      'options': {
                                                                          'service_id': 'test',
                                                                          'snapshot_id': 'sn1',
                                                                          'target_state': 'active',
                                                                      }},
                                                 'dispatcherTaskInfo': {'metaTask': {'state': {'state': 'WAITING'}}}}])
        snapshots, _ = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert snapshots == {'test': {'sn1': {'active': 1}}}

        m.get(url, headers={'X-Total-Items': '2'}, json=[{'id': 'tg1'}, {'id': 'tg2'}])
        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'processorOptions': {'type': 'SetSnapshotTargetState',
                                                                      'options': {
                                                                          'service_id': 'test',
                                                                          'snapshot_id': 'sn1',
                                                                          'target_state': 'active',
                                                                      }},
                                                 'dispatcherTaskInfo': {'metaTask': {'state': {'state': 'WAITING'}}}}])
        m.get(children_url.format('tg2'), json=[{'id': 't2',
                                                 'processorOptions': {'type': 'SetSnapshotTargetState',
                                                                      'options': {
                                                                          'service_id': 'test',
                                                                          'snapshot_id': 'sn1',
                                                                          'target_state': 'active',
                                                                      }},
                                                 'dispatcherTaskInfo': {'metaTask': {'state': {'state': 'WAITING'}}}}])
        # larger pool here breaks mocked response, so using 1 worker
        snapshots, _ = alemate_client.collect_tasks_info(gevent.pool.Pool(size=1))
        assert snapshots == {'test': {'sn1': {'active': 2}}}

        m.get(url, headers={'X-Total-Items': '1'}, json=[{'id': 'tg1'}])
        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'processorOptions': {'type': 'SetSnapshotTargetState',
                                                                      'options': {
                                                                          'service_id': 'test',
                                                                          'snapshot_id': 'sn1',
                                                                          'target_state': 'active',
                                                                      }},
                                                 'dispatcherTaskInfo': {'metaTask': {'state': {'state': 'DONE'}}}}])
        snapshots, _ = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert snapshots == {}

        m.get(children_url.format('tg1'), json=[{'id': 't1',
                                                 'processorOptions': {'type': 'SetSnapshotTargetState',
                                                                      'options': {
                                                                          'service_id': 'test',
                                                                          'snapshot_id': 'sn1',
                                                                          'target_state': 'active',
                                                                      }},
                                                 'dispatcherTaskInfo': {'metaTask': {'state': {'state': 'WAITING'}}}},
                                                {'id': 't2',
                                                 'processorOptions': {'type': 'SetSnapshotTargetState',
                                                                      'options': {
                                                                          'service_id': 'test',
                                                                          'snapshot_id': 'sn1',
                                                                          'target_state': 'active',
                                                                      }},
                                                 'dispatcherTaskInfo': {'metaTask': {'state': {'state': 'WAITING'}}}},
                                                {'id': 't2',
                                                 'processorOptions': {'type': 'SetSnapshotTargetState',
                                                                      'options': {
                                                                          'service_id': 'test',
                                                                          'snapshot_id': 'sn1',
                                                                          'target_state': 'prepared',
                                                                      }},
                                                 'dispatcherTaskInfo': {'metaTask': {'state': {'state': 'WAITING'}}}},
                                                ])
        snapshots, _ = alemate_client.collect_tasks_info(gevent.pool.Pool(size=10))
        assert snapshots == {'test': {'sn1': {'active': 2, 'prepared': 1}}}
