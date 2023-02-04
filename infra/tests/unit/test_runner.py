import mock

from infra.mc_rsc.src import runner
from infra.mc_rsc.src import sync_status


def make_success_sync_condition(cluster):
    rv = sync_status.SyncCondition(cluster)
    rv.set_success()
    return rv


def make_failed_sync_condition(cluster):
    rv = sync_status.SyncCondition(cluster)
    rv.set_error(reason='Failed', message='Failed')
    return rv


def make_yp_client(ts):
    c = mock.Mock()
    c.generate_timestamp.return_value = ts
    return c


def test_all_clusters_ok():
    mc_rs_reflector = mock.Mock()
    mc_rs_reflector.start.return_value = make_success_sync_condition('xdc')
    relation_reflector = mock.Mock()
    relation_reflector.storage.size.return_value = 1
    relation_reflector.start.return_value = make_success_sync_condition('xdc')

    pod_reflector = mock.Mock()
    pod_reflector.start.return_value = make_success_sync_condition('man')
    pod_reflectors = {'man': pod_reflector}
    ps_reflector = mock.Mock()
    ps_reflector.start.return_value = make_success_sync_condition('man')
    ps_reflectors = {'man': ps_reflector}

    yp_clients = {
        'xdc': make_yp_client(1),
        'man': make_yp_client(2),
    }

    r = runner.Runner(ctl=mock.Mock(),
                      deploy_engine='MCRSC',
                      xdc='xdc',
                      yp_clients=yp_clients,
                      mc_rs_storage=mock.Mock(),
                      circuit_breaker=mock.Mock(),
                      pod_reflectors=pod_reflectors,
                      ps_reflectors=ps_reflectors,
                      mc_rs_reflector=mc_rs_reflector,
                      relation_reflector=relation_reflector,
                      threads_count=1,
                      relation_sync_delay=1,
                      allowed_mc_rs_ids=[],
                      ignored_mc_rs_ids=[],
                      metrics_registry=mock.Mock(),
                      metrics_collector=mock.Mock())
    with mock.patch.object(runner.Runner, 'start_and_wait_ctls') as start_mock:
        r.run_once()
        mc_rs_reflector.start.assert_called_once_with(timestamp=1)
        relation_reflector.start.assert_called_once_with(timestamp=1)
        pod_reflector.start.assert_called_once_with(timestamp=2)
        ps_reflector.start.assert_called_once_with(timestamp=2)
        start_mock.assert_called_once_with(failed_clusters={})


def test_xdc_failed():
    mc_rs_reflector = mock.Mock()
    mc_rs_reflector.start.return_value = make_failed_sync_condition('xdc')
    relation_reflector = mock.Mock()
    relation_reflector.storage.size.return_value = 1
    relation_reflector.start.return_value = make_success_sync_condition('xdc')

    pod_reflector = mock.Mock()
    pod_reflector.start.return_value = make_success_sync_condition('man')
    pod_reflectors = {'man': pod_reflector}
    ps_reflector = mock.Mock()
    ps_reflector.start.return_value = make_success_sync_condition('man')
    ps_reflectors = {'man': ps_reflector}

    yp_clients = {
        'xdc': make_yp_client(1),
        'man': make_yp_client(2),
    }

    r = runner.Runner(ctl=mock.Mock(),
                      deploy_engine='MCRSC',
                      xdc='xdc',
                      yp_clients=yp_clients,
                      mc_rs_storage=mock.Mock(),
                      circuit_breaker=mock.Mock(),
                      pod_reflectors=pod_reflectors,
                      ps_reflectors=ps_reflectors,
                      mc_rs_reflector=mc_rs_reflector,
                      relation_reflector=relation_reflector,
                      threads_count=1,
                      relation_sync_delay=1,
                      allowed_mc_rs_ids=[],
                      ignored_mc_rs_ids=[],
                      metrics_registry=mock.Mock(),
                      metrics_collector=mock.Mock())
    with mock.patch.object(runner.Runner, 'start_and_wait_ctls') as start_mock:
        r.run_once()
        mc_rs_reflector.start.assert_called_once_with(timestamp=1)
        relation_reflector.start.assert_called_once_with(timestamp=1)
        pod_reflector.start.assert_called_once_with(timestamp=2)
        ps_reflector.start.assert_called_once_with(timestamp=2)
        assert start_mock.call_count == 0


def test_failed_clusters():
    mc_rs_reflector = mock.Mock()
    mc_rs_reflector.start.return_value = make_success_sync_condition('xdc')
    relation_reflector = mock.Mock()
    relation_reflector.storage.size.return_value = 1
    relation_reflector.start.return_value = make_success_sync_condition('xdc')

    pod_reflector = mock.Mock()
    pod_reflector.start.return_value = make_failed_sync_condition('man')
    pod_reflectors = {'man': pod_reflector}
    ps_reflector = mock.Mock()
    ps_reflector.start.return_value = make_success_sync_condition('man')
    ps_reflectors = {'man': ps_reflector}

    yp_clients = {
        'xdc': make_yp_client(1),
        'man': make_yp_client(2),
    }
    r = runner.Runner(ctl=mock.Mock(),
                      deploy_engine='MCRSC',
                      xdc='xdc',
                      yp_clients=yp_clients,
                      mc_rs_storage=mock.Mock(),
                      circuit_breaker=mock.Mock(),
                      pod_reflectors=pod_reflectors,
                      ps_reflectors=ps_reflectors,
                      mc_rs_reflector=mc_rs_reflector,
                      relation_reflector=relation_reflector,
                      threads_count=1,
                      relation_sync_delay=1,
                      allowed_mc_rs_ids=[],
                      ignored_mc_rs_ids=[],
                      metrics_registry=mock.Mock(),
                      metrics_collector=mock.Mock())
    with mock.patch.object(runner.Runner, 'start_and_wait_ctls') as start_mock:
        r.run_once()
        mc_rs_reflector.start.assert_called_once_with(timestamp=1)
        relation_reflector.start.assert_called_once_with(timestamp=1)
        pod_reflector.start.assert_called_once_with(timestamp=2)
        ps_reflector.start.assert_called_once_with(timestamp=2)
        assert len(start_mock.call_args_list) == 1
        _, kw = start_mock.call_args_list[0]
        assert set(kw['failed_clusters'].keys()) == {'man'}


def test_cluster_failed_rsc():
    mc_rs_reflector = mock.Mock()
    mc_rs_reflector.start.return_value = make_success_sync_condition('man')
    relation_reflector = mock.Mock()
    relation_reflector.storage.size.return_value = 1
    relation_reflector.start.return_value = make_success_sync_condition('man')

    pod_reflector = mock.Mock()
    pod_reflector.start.return_value = make_failed_sync_condition('man')
    pod_reflectors = {'man': pod_reflector}
    ps_reflector = mock.Mock()
    ps_reflector.start.return_value = make_success_sync_condition('man')
    ps_reflectors = {'man': ps_reflector}

    yp_clients = {
        'man': make_yp_client(1),
    }
    r = runner.Runner(ctl=mock.Mock(),
                      deploy_engine='RSC',
                      xdc='man',
                      yp_clients=yp_clients,
                      mc_rs_storage=mock.Mock(),
                      circuit_breaker=mock.Mock(),
                      pod_reflectors=pod_reflectors,
                      ps_reflectors=ps_reflectors,
                      mc_rs_reflector=mc_rs_reflector,
                      relation_reflector=relation_reflector,
                      threads_count=1,
                      relation_sync_delay=1,
                      allowed_mc_rs_ids=[],
                      ignored_mc_rs_ids=[],
                      metrics_registry=mock.Mock(),
                      metrics_collector=mock.Mock())
    with mock.patch.object(runner.Runner, 'start_and_wait_ctls') as start_mock:
        r.run_once()
        mc_rs_reflector.start.assert_called_once_with(timestamp=1)
        relation_reflector.start.assert_called_once_with(timestamp=1)
        pod_reflector.start.assert_called_once_with(timestamp=1)
        ps_reflector.start.assert_called_once_with(timestamp=1)
        assert start_mock.call_count == 0
