import pytest
import logging

from infra.dctl.src.lib import stage
from yp.common import YpNoSuchObjectError
import utils


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-full-cycle-stage-' + suffix_fixture


def test_full_cycle_stage(stage_name, stage_fixture,  yp_xdc_client_fixture):
    logging.info('----Full cycle stage test start----')
    assert stage.get(stage_name, yp_xdc_client_fixture)
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)

    stage.remove(stage_name, yp_xdc_client_fixture)
    with pytest.raises(YpNoSuchObjectError):
        stage.get(stage_name, yp_xdc_client_fixture)
    logging.info('----Full cycle stage test end----')
