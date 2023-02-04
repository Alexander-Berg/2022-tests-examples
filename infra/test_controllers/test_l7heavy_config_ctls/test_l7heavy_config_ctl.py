# coding: utf-8
import pytest
import flaky

from awacs.lib.l7heavy_client import IL7HeavyClient
from awacs.model.l7heavy_config.ctl import L7HeavyConfigCtl
from awacs.model.util import COMMON_L7HEAVY_GROUP_ID
from awtest.api import Api
from infra.awacs.proto import model_pb2
from .conftest import NS_ID
from .util import L7HeavyState, f_f_f, t_f_f, t_f_t, u_f_f


MAX_RUNS = 3


@pytest.fixture()
def l7heavy_config():
    config = IL7HeavyClient.instance().create_config(NS_ID, 'other',
                                                     'balancer/namespace-id/common/common/cplb_balancer_load_switch')
    return config


def create_namespace(namespace_id, weight_section_ids):
    """
    :type namespace_id: six.text_type
    :type weight_section_ids: list[six.text_type]
    """
    Api.create_namespace(namespace_id=namespace_id)

    for i, weight_section_id in enumerate(weight_section_ids):
        weight_section_spec_pb = model_pb2.WeightSectionSpec()
        for loc in ('SAS', 'MAN', 'IVA', 'MYT'):
            weight_section_spec_pb.locations.add(name=loc, default_weight=25)
        Api.create_weight_section(namespace_id=namespace_id, weight_section_id=weight_section_id,
                                  spec_pb=weight_section_spec_pb)

    spec_pb = model_pb2.L7HeavyConfigSpec(l7_heavy_config_id=namespace_id, group_id='other')
    Api.create_l7heavy_config(namespace_id=namespace_id, l7heavy_config_id=namespace_id, spec_pb=spec_pb)
    Api.create_l7heavy_config_state(namespace_id=namespace_id, l7heavy_config_id=namespace_id)
    Api.pause_l7heavy_config_transport(namespace_id, namespace_id)


def assert_l7heavy_state(checker, expected_state):
    for a in checker:
        with a:
            actual_state = L7HeavyState.from_api(namespace_id=NS_ID, l7heavy_config_id=NS_ID)
            assert actual_state == expected_state
            return actual_state


@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
def test_ctl(ctx, cache, checker, l7heavy_config):
    create_namespace(
        namespace_id=NS_ID,
        weight_section_ids=['xxx', 'yyy']
    )
    Api.unpause_l7heavy_config_transport(NS_ID, NS_ID)
    ctl = L7HeavyConfigCtl(full_uid=(NS_ID, NS_ID), cache=cache)

    ctl.process(ctx)  # Discover
    assert_l7heavy_state(checker, L7HeavyState(l7heavy_config=[u_f_f],
                                               weight_sections={u'xxx': [u_f_f], u'yyy': [u_f_f]}))

    # Validate
    for a in checker:
        with a:
            ctl.process(ctx)
            state_pb = Api.get_l7heavy_config_state(NS_ID, NS_ID)
            actual_state = L7HeavyState.from_pb(state_pb)
            expected_state = L7HeavyState(
                l7heavy_config=[t_f_f],
                weight_sections={
                    'xxx': [t_f_f],
                    'yyy': [t_f_f],
                },
            )
            assert actual_state == expected_state

    # Transport
    for a in checker:
        with a:
            ctl.process(ctx)
            state_pb = Api.get_l7heavy_config_state(NS_ID, NS_ID)
            actual_state = L7HeavyState.from_pb(state_pb)
            expected_state = L7HeavyState(
                l7heavy_config=[t_f_t],
                weight_sections={
                    'xxx': [t_f_t],
                    'yyy': [t_f_t],
                },
            )
            assert actual_state == expected_state

    l7hc_pb = Api.get_l7heavy_config(NS_ID, NS_ID)
    l7hc_pb.spec.group_id = COMMON_L7HEAVY_GROUP_ID
    Api.update_l7heavy_config(NS_ID, NS_ID, l7hc_pb.meta.version, l7hc_pb.spec)

    for a in checker:
        with a:
            ctl.process(ctx)
            state_pb = Api.get_l7heavy_config_state(NS_ID, NS_ID)
            actual_state = L7HeavyState.from_pb(state_pb)
            expected_state = L7HeavyState(
                l7heavy_config=[t_f_t, f_f_f],
                weight_sections={
                    'xxx': [t_f_t],
                    'yyy': [t_f_t],
                },
            )
            assert actual_state == expected_state

    state_pb = Api.get_l7heavy_config_state(NS_ID, NS_ID)
    assert (L7HeavyState.from_pb(state_pb).l7heavy_config.rev_statuses[-1].v_message ==
            'All weights sections must contains locations (MAN, SAS, VLA) if L7Heavy config located in group "common"')

    for ws_id in ('xxx', 'yyy'):
        ws_pb = Api.get_weight_section(NS_ID, ws_id)
        ws_pb.spec.locations.add(name='VLA', default_weight=0)
        Api.update_weight_section(NS_ID, ws_id, ws_pb.meta.version, ws_pb.spec)

    assert IL7HeavyClient.instance().configs[NS_ID]['group_id'] == 'other'

    for a in checker:
        with a:
            ctl.process(ctx)
            state_pb = Api.get_l7heavy_config_state(NS_ID, NS_ID)
            actual_state = L7HeavyState.from_pb(state_pb)
            expected_state = L7HeavyState(
                l7heavy_config=[t_f_t],
                weight_sections={
                    'xxx': [t_f_t],
                    'yyy': [t_f_t],
                },
            )
            assert actual_state == expected_state

    assert IL7HeavyClient.instance().configs[NS_ID]['group_id'] == COMMON_L7HEAVY_GROUP_ID
