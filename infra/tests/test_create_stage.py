import pytest
import logging

from infra.dctl.src.lib import stage


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-create-stage-' + suffix_fixture


def test_full_cycle_stage(stage_name, stage_fixture, yp_client_fixture, yp_xdc_client_fixture):
    logging.info('----Create stage test start----')
    assert stage.get(stage_name, yp_xdc_client_fixture).meta.id == stage_name
    logging.info('----Create stage test end----')
