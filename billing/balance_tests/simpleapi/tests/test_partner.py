import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.utils import DataObject
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.steps import balance_steps as balance
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import simple_steps as simple

__author__ = 'fellow'

log = logger.get_logger()

pytestmark = marks.simple_internal_logic


@reporter.feature(features.General.Partner)
class TestPartners(object):
    @reporter.story(stories.Partner.Load)
    @pytest.mark.parametrize("service", [Services.TICKETS], ids=DataObject.ids_service)
    def test_partner_load(self, service):
        """
        create balance client in bo
        load it to bs scheme
        compare created and loaded clients
        N.B. client in bo is the same as partner in bs
        """
        bo_client, bo_client_id = balance.create_client()
        with check_mode(CheckMode.FAILED):
            simple.load_partner(service, bo_client_id)
        bs_partner = db_steps.ng().get_partner_by_id(bo_client_id)
        assert bs_partner, "Partner hasn't load to bs scheme"

        check.check_dicts_equals(bs_partner, bo_client['info'],
                                 compare_only=['name', 'city', 'fax', 'url',
                                               'phone', 'email'])

    @reporter.story(stories.Partner.Load)
    @pytest.mark.parametrize("service", [Services.TICKETS], ids=DataObject.ids_service)
    def test_partner_change(self, service):
        """
        create balance client in bo
        load it to bs scheme
        update balance client
        load changed client to bs
        compare changed and loaded clients
        """
        _, bo_client_id = balance.create_client()
        with check_mode(CheckMode.FAILED):
            simple.load_partner(service, bo_client_id)
            bo_client, bo_client_id = \
                balance.create_client(name='new_test_name', client_id=bo_client_id)
            simple.load_partner(service, bo_client['client_id'])
        bs_partner = db_steps.ng().get_partner_by_id(bo_client_id)
        assert bs_partner, "Partner hasn't load to bs scheme"

        check.check_dicts_equals(bs_partner, bo_client['info'],
                                 compare_only=['name', 'city', 'fax', 'url',
                                               'phone', 'email'])

    @reporter.story(stories.Partner.Create)
    @pytest.mark.parametrize("service", [Services.SHAD,
                                         Services.DISK,
                                         Services.YAC,
                                         Services.REALTYPAY],
                             ids=DataObject.ids_service)
    def test_services_cannot_be_with_partner(self, service):
        with check_mode(CheckMode.IGNORED):
            resp, _ = simple.create_partner(service)
            assert resp['status'] == 'error', 'Error while CreateServiceProduct with partner, response: %s' % resp
            assert resp['status_desc'] == 'Forbidden for non-partner services'.format(service.token), \
                'Wrong error, response: %s' % resp


if __name__ == '__main__':
    pytest.main()
