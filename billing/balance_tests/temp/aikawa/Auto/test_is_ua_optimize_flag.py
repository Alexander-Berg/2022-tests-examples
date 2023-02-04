import datetime

import hamcrest
import pytest

import btestlib.utils as utils
from balance import balance_steps as steps

dt = datetime.datetime.now()
ORDER_DT = dt

PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003

pytest.mark.xfail(reason='BALANCE-20958')


def test_make_optimized():
    client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    try:
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                service_order_id=service_order_id, params={'is_ua_optimize': 1})
    except Exception, exc:
        print exc.__class__
        print exc
        contents = 'could not turn on unified account optimize of this order'
        print steps.CommonSteps.get_exception_code(exc, 'contents')
        utils.check_that(contents == steps.CommonSteps.get_exception_code(exc, 'contents'), hamcrest.equal_to(True))
    else:
        raise Exception
