# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from .. import steps
from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Currencies, Firms, ContractCommissionType, Services
from jsonrpc import dispatcher

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY)
QTY = D('50')
COMPLETIONS = D('99.99')


# фиктивный счет без предварительного, 1 заказ в счете
@dispatcher.add_method
def test_fictive_1_order():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    return client_id, fictive_invoice_id_1


# фиктивный счет без предварительного, 3 заказа в счете
@dispatcher.add_method
def test_fictive_3_orders():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=3, qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    return client_id, fictive_invoice_id_1


# 2 фиктивных счета без предварительных, разные плательщики
@dispatcher.add_method
def test_fictive_same_client_2_invoices():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    _, _, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_2, _ = \
        steps.create_base_invoice(client_id=client_id, qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)

    return client_id, fictive_invoice_id_1, fictive_invoice_id_2


# 2 фиктивных счета без предварительных, разные плательщики
@dispatcher.add_method
def test_fictive_same_person_2_invoices():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    _, _, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_2, _ = \
        steps.create_base_invoice(client_id=client_id, person_id=person_id, qty=QTY, credit=2,
                                  contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)

    return client_id, fictive_invoice_id_1, fictive_invoice_id_2


# фиктивный счет с предварительным, 1 заказ в счете
@dispatcher.add_method
def test_repayment_1_order():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_1 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_1,
                                                                               with_confirmation=False)[0]
    return client_id, fictive_invoice_id_1, repayment_invoice_id_1


# фиктивный счет с предварительным, 3 заказа в счете
@dispatcher.add_method
def test_repayment_3_orders():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=3, qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_1 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_1,
                                                                               with_confirmation=False)[0]
    return client_id, fictive_invoice_id_1, repayment_invoice_id_1


# 2 предварительных счета, один клиент, разные плательщики
@dispatcher.add_method
def test_repayment_same_client_2_invoices():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_1 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_1,
                                                                               with_confirmation=False)[0]
    _, _, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_2, _ = \
        steps.create_base_invoice(client_id=client_id, qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_2 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_2,
                                                                               with_confirmation=False)[0]

    return client_id, fictive_invoice_id_1, fictive_invoice_id_2, repayment_invoice_id_1, repayment_invoice_id_2


# 2 предварительных счета, один клиент, один плательщик
@dispatcher.add_method
def test_repayment_same_person_2_invoices():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_1 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_1,
                                                                               with_confirmation=False)[0]
    _, _, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_2, _ = \
        steps.create_base_invoice(client_id=client_id, person_id=person_id, qty=QTY, credit=2,
                                  contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_2 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_2,
                                                                               with_confirmation=False)[0]

    return client_id, fictive_invoice_id_1, fictive_invoice_id_2, repayment_invoice_id_1, repayment_invoice_id_2


# фиктивный и предварительный счет на одного клиента
@dispatcher.add_method
def test_fictive_and_repayment_invoices():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    _, _, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_2, _ = \
        steps.create_base_invoice(client_id=client_id, qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_2 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_2,
                                                                               with_confirmation=False)[0]

    return client_id, fictive_invoice_id_1, fictive_invoice_id_2, repayment_invoice_id_2


# счет на погашение, 3 заказа в счете
@dispatcher.add_method
def test_confirmed_repayment_3_orders():
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': CONTEXT.firm.id,
                       }

    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, fictive_invoice_id_1, _ = \
        steps.create_base_invoice(overdraft=0, orders_amount=3, qty=QTY, credit=2, contract_params=contract_params,
                                  contract_type=CONTEXT.contract_type, fictive_scheme=True)
    repayment_invoice_id_1 = balance_steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id_1,
                                                                               with_confirmation=True)[0]
    balance_steps.InvoiceSteps.pay(repayment_invoice_id_1)
    return client_id, fictive_invoice_id_1, repayment_invoice_id_1
