# coding: utf-8
import time

import inject
import pytest

from awacs.lib.itsclient import IItsClient, ItsClient
from awacs.model.cron.knobswatcher import ItsKnobsWatcher
from infra.awacs.proto import model_pb2


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        nanny_oauth_token = 'DUMMY'
        b.bind(IItsClient, ItsClient(url='https://its.yandex-team.ru/', token=nanny_oauth_token))
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.mark.vcr
def test_knobswatcher(zk_storage, cache, log):
    namespace_id = 'test-namespace'
    balancer_aspects_set_pb = model_pb2.BalancerAspectsSet()
    balancer_aspects_set_pb.meta.namespace_id = namespace_id
    sas_balancer_id = balancer_aspects_set_pb.meta.balancer_id = 'test-balancer-sas'
    its_pb = balancer_aspects_set_pb.content.its.content
    its_pb.location_paths.extend(['balancer/its/common/common', 'balancer/its/sas/sas'])
    zk_storage.create_balancer_aspects_set(namespace_id, sas_balancer_id, balancer_aspects_set_pb)

    balancer_aspects_set_pb = model_pb2.BalancerAspectsSet()
    balancer_aspects_set_pb.meta.namespace_id = namespace_id
    man_balancer_id = balancer_aspects_set_pb.meta.balancer_id = 'test-balancer-man'
    its_pb = balancer_aspects_set_pb.content.its.content
    its_pb.location_paths.extend(['balancer/its/common/common', 'balancer/its/man/man'])
    zk_storage.create_balancer_aspects_set(namespace_id, man_balancer_id, balancer_aspects_set_pb)

    class TestItsKnobsWatcher(ItsKnobsWatcher):
        def _list_all_namespace_ids(self):
            return ['test-namespace']

    knobs_watcher = TestItsKnobsWatcher()
    time.sleep(1)
    knobs_watcher._run()
    time.sleep(1)

    knob_pbs = cache.list_all_knobs(namespace_id=namespace_id)
    assert len(knob_pbs) == 4

    knob_pb = cache.must_get_knob(namespace_id=namespace_id, knob_id='cplb_balancer_load_switch')

    actual_spec_pb = knob_pb.spec

    expected_spec_pb = model_pb2.KnobSpec()
    expected_spec_pb.mode = model_pb2.KnobSpec.WATCHED
    expected_spec_pb.type = model_pb2.KnobSpec.YB_BACKEND_WEIGHTS
    expected_spec_pb.shared = True
    expected_spec_pb.its_watched_state.filename = 'traffic_control.weights'
    expected_spec_pb.its_watched_state.ruchka_id = 'cplb_balancer_load_switch'
    expected_spec_pb.its_watched_state.its_location_paths['test-balancer-man'] = 'balancer/its/common/common'
    expected_spec_pb.its_watched_state.its_location_paths['test-balancer-sas'] = 'balancer/its/common/common'

    assert actual_spec_pb == expected_spec_pb

    knob_pb = cache.must_get_knob(namespace_id=namespace_id, knob_id='balancer_disable_keepalive')

    actual_spec_pb = knob_pb.spec

    expected_spec_pb = model_pb2.KnobSpec()
    expected_spec_pb.mode = model_pb2.KnobSpec.WATCHED
    expected_spec_pb.type = model_pb2.KnobSpec.BOOLEAN
    expected_spec_pb.its_watched_state.filename = 'keepalive_disabled'
    expected_spec_pb.its_watched_state.ruchka_id = 'balancer_disable_keepalive'
    expected_spec_pb.its_watched_state.its_location_paths['test-balancer-man'] = 'balancer/its/man/man'
    expected_spec_pb.its_watched_state.its_location_paths['test-balancer-sas'] = 'balancer/its/sas/sas'

    assert actual_spec_pb == expected_spec_pb
