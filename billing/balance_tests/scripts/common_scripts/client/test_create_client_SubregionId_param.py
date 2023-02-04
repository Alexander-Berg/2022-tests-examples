__author__ = 'aikawa'

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as ut

pytestmark = [reporter.feature(Features.CLIENT)]


def create_client_with_subregion(SUBREGION_ID):
    return steps.ClientSteps.create(SUBREGION_ID)


@pytest.mark.parametrize('SUBREGION_ID',
                         [
                             {'SUBREGION_ID': 225, 'expected_subregion_id': 225},
                             {'expected_subregion_id': None},
                             {'SUBREGION_ID': None, 'expected_subregion_id': None}
                         ],
                         ids=['subregion_id == 225'
                             , 'w/o subregion'
                             , 'subregion_id == None']
                         )
def test_create_client_with_subregion(SUBREGION_ID):
    client_id = create_client_with_subregion(SUBREGION_ID)
    # reporter.log(db.get_client_by_id(client_id)[0]['subregion_id'])
    client = db.get_client_by_id(client_id)[0]
    reporter.log(client['subregion_id'])
    ut.check_that(client['subregion_id'], equal_to(SUBREGION_ID['expected_subregion_id']))


if __name__ == "__main__":
    pytest.main("test_create_client_SubregionId_param.py -v")
