# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Services, Firms


@pytest.mark.smoke
def test_service_changing_collateral():
    contract_id = create_contract()
    create_change_service_collateral(contract_id, [Services.EVENTS_TICKETS.id])


def create_contract():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('events_tickets2',
                                                         {
                                                             'CLIENT_ID': client_id,
                                                             'PERSON_ID': person_id,
                                                             'FIRM': Firms.MEDIASERVICES_121.id
                                                         })
    return contract_id


def create_change_service_collateral(contract_id, services):
    start_dt = utils.Date.first_day_of_month() + relativedelta(months=1)
    sign_dt = (utils.Date.nullify_time_of_date(datetime.now())).isoformat()

    steps.ContractSteps.create_collateral(1001,
                                          {
                                              'CONTRACT2_ID': contract_id,
                                              'DT': start_dt,
                                              'IS_SIGNED': sign_dt,
                                              'SERVICES': services
                                          })
