# -*- coding: utf-8 -*-

__author__ = 'torvald'

import datetime

import pytest
from hamcrest import greater_than

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import User

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT, Features.UI),
    pytest.mark.tickets('BALANCE-18325'),
]

NOW = datetime.datetime.now()

SERVICE_ID = 7
# PRODUCT_ID = 502953 ##502918
PRODUCT_ID = 1475
PAYSYS_ID = 1003
QTY = 118
BASE_DT = datetime.datetime.now()

manager_uid = '244916211'

# UID = 426178010
# LOGIN = 'yb-atst-user-1'
# PASSWORD = secrets.get_secret(*secrets.Passport.CLIENTUID_PWD)

# user = User(426178010, 'yb-atst-user-1')

#TODO: free_users: не работает с shared \ Cache
# сейчас не работает из-за https://st.yandex-team.ru/BALANCE-30763
def async_xls_act_report_simple_case(data_cache, get_free_user):
    with utils.CachedData(data_cache, ['client_id'], force_invalidate=False) as c:
        if not c: raise utils.SkipContextManagerBodyException()
        client_id = None or steps.ClientSteps.create({'REGION_ID': '225'})
        agency_id = None
        person_id = None or steps.PersonSteps.create(client_id, 'ur')
        contract_id = None

        orders_list = []
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        # service_order_id =11443682
        for _ in xrange(1):
            steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                    params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
            orders_list.append(
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

            request_id = steps.RequestSteps.create(client_id, orders_list)
            invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0,
                                                         contract_id=contract_id,
                                                         overdraft=0, endbuyer_id=None)

        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

        steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)

    query = "select update_dt, state from t_export where type = 'EMAIL_DOCUMENT' and object_id = :clientuid"
    query_params = {'clientuid': user.uid}
    result = db.balance().execute(query, query_params)
    first_update_dt, state = (result[0]['update_dt'], result[0]['state']) if result else (NOW, 1)

    # utils.check_that(state, equal_to(1))

    with web.Driver(user=user) as driver:
        page = web.ClientInterface.ActsPage.open_dates(driver, from_dt=NOW)
        page.request_async_xls_report('test-balance-notify@yandex-team.ru')

    # Ожидаем выгрузки объекта
    steps.CommonSteps.wait_for_export('EMAIL_DOCUMENT', user.uid)

    result = db.balance().execute(query, query_params)[0]
    next_update_dt = result['update_dt']

    # Проверяем, что
    utils.check_that(next_update_dt, greater_than(first_update_dt))
