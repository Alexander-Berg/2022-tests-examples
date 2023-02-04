import logging
import time

import pytest
from infra.dctl.src.lib import replica_set
from infra.dctl.src.lib import stage

import utils


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-sbr-' + suffix_fixture


@pytest.fixture(scope='function')
def stage_template(suffix_fixture):
    return '/stage-sbr.yml'


def test_sbr_resolve(stage_name, stage_template, stage_fixture, yp_client_fixture, yp_xdc_client_fixture):
    logging.info('----SBR resolve test start----')
    assert stage.get(stage_name, yp_xdc_client_fixture)
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)
    time.sleep(5)

    rs = replica_set.get_rs(stage_name + ".DeployUnit1", yp_client_fixture)
    ps = rs.spec.pod_template_spec.spec.pod_agent_payload.spec

    sbr_layer = utils.find_by_substr(ps.resources.layers, "sbr-test-layer")
    assert sbr_layer
    assert sbr_layer.url
    assert 'rbtorrent:d36f95e460af0a3173dfe16bd662e357867febea' in sbr_layer.url

    sbr_static_resource = utils.find_by_substr(ps.resources.static_resources, "sbr-test-static-resource")
    assert sbr_static_resource
    assert sbr_static_resource.url
    assert 'rbtorrent:68974bdc7759c1940f8667bd8ca9d16f69b6d26f' in sbr_static_resource.url

    logging.info('----SBR resolve test end----')
