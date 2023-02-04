# coding: utf-8


import hamcrest
import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from balance.features import Features
from btestlib import reporter

pytestmark = [reporter.feature(Features.OEBS, Features.ACT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

def test_person_is_none():
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    invoice_id = steps.InvoiceSteps.pay_with_certificate_or_compensation(order_id, 100)
    export_with_init = db.balance().execute('''select * from t_export where classname = 'Invoice' and object_id = :invoice_id
    ''', {'invoice_id': invoice_id})
    utils.check_that(export_with_init, hamcrest.equal_to([]))
