__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

SERVICE_ID = 7
PRODUCT_ID_AFTER_MULTICURRENCY = 503162
PAYSYS_ID = 1001
PERSON_TYPE = 'ph'
OVERDRAFT_LIMIT = 100
QUANT = 10
MAIN_DT = datetime.datetime.now()


@pytest.mark.priority('low')
@reporter.feature(Features.NOTIFICATION, Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-21679')
def test_simple_overdraft_notification():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=MAIN_DT,
                                             currency=None)
    # client_params = {'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
    #                  'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
    #                  'SERVICE_ID': SERVICE_ID, 'CURRENCY_CONVERT_TYPE': 'MODIFY'}
    # client_id = steps.ClientSteps.create(client_params)
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY',
                              dt=datetime.datetime.now() + datetime.timedelta(seconds=5),
                              service_id=SERVICE_ID, region_id='225', currency='RUB')
    steps.CommonSteps.wait_and_get_notification(10, client_id, 3, timeout=420)
    overdraft_limit = steps.CommonSteps.parse_notification (10, client_id, 2, 'info', 'overdraft_limit')
    print overdraft_limit
    return overdraft_limit



if __name__ == "__main__":
    pytest.main("simple_overdraft_notification.py")
    overdraft_limit = test_simple_overdraft_notification()
    assert overdraft_limit == '3000'

