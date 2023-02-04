# coding: utf-8
__author__ = 'a-vasin'

import pytest
from hamcrest import equal_to

import balance.balance_steps as steps
import balance.balance_web as web
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.features import Features
from btestlib.constants import Users
from btestlib.data.defaults import Order
import balance.balance_db as db

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT, Features.INVOICE_PRINT_FORM, Features.ROLE, Features.PERMISSION, Features.UI),
    pytest.mark.tickets('BALANCE-23065'),
]

# restore test
# maintenance/test_restore_logins.py

# NOTE: free_users: тут используются фиксированные логины с заранее привязанными к ним клиентами
@pytest.mark.parametrize('user_type, is_ur_downloadable',
                         [
                             ('not main', False),
                             ('main', True),
                             ('accountant', True)
                         ],
                         ids=['NOT_MAIN','MAIN', 'ACCOUNTANT'])
def test_printed_form_downloadable(user_type, is_ur_downloadable, get_free_user):
    user = get_free_user()
    client_id = steps.ClientSteps.get_client_id_by_passport_id(user.uid)
    if user_type == 'main':
        db.balance().execute('UPDATE t_passport SET is_main=1 WHERE passport_id=:uid', {'uid': user.uid})
    elif user_type == 'accountant':
        if not db.balance().execute('SELECT id FROM t_role_client_user WHERE passport_id=:uid AND role_id=100',
                                    {'uid': user.uid}):
            db.balance().execute('INSERT INTO t_role_client_user(role_id, passport_id) VALUES (100,:uid)',
                                 {'uid': user.uid})
        db.balance().execute('UPDATE t_role_client_user SET client_id=:client_id WHERE PASSPORT_ID=:uid',
                             {'client_id': client_id, 'uid': user.uid})

    ph_external_act_id = create_act(client_id, 'ph', Order.PH_PAYSYS_ID)
    ur_external_act_id = create_act(client_id, 'ur', Order.UR_PAYSYS_ID)

    with web.Driver(user=user) as driver:
        check_act_info(driver, ph_external_act_id, False)
        check_act_info(driver, ur_external_act_id, is_ur_downloadable)


def check_act_info(driver, external_act_id, is_downloadable):
    acts_page = web.ClientInterface.ActsPage.open_act(driver, external_act_id)

    is_act_present = acts_page.is_act_present(external_act_id)
    utils.check_that(is_act_present, equal_to(True), u"Проверяем, что акт присутствует")

    is_act_downloadable = acts_page.is_act_downloadable(external_act_id)
    utils.check_that(is_act_downloadable, equal_to(is_downloadable), u"Проверяем возможность скачивания файла")


def create_act(client_id, person_type, paysys_id):
    person_id = steps.PersonSteps.create(client_id, person_type)

    service_order_id = steps.OrderSteps.next_id(Order.PRODUCT.service_id)
    steps.OrderSteps.create(client_id, service_order_id, params={'ManagerUID': Order.MANAGER_UID})

    orders_list = Order.default_orders_list(service_order_id)
    request_id = steps.RequestSteps.create(client_id, orders_list)

    invoice_id, invoice_external_id, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id)

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(Order.PRODUCT.service_id, service_order_id,
                                      {Order.PRODUCT.shipment_type: Order.CAMPAIGN_QTY},
                                      campaigns_dt=Order.BASE_DT)

    acts_id = steps.ActsSteps.generate(client_id, date=Order.BASE_DT)
    external_act_id = steps.ActsSteps.get_act_external_id(acts_id[-1])

    return external_act_id
