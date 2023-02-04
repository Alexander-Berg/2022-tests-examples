# coding=utf-8

import datetime

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import User, Services, Products, ContractCommissionType, Firms, \
    ContractPaymentType, Currencies
from btestlib.data import defaults
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()

PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)
PREVIOUS_MONTH_FIRST_DAY_ISO = utils.Date.date_to_iso_format(PREVIOUS_MONTH_LAST_DAY.replace(day=1))
NOW_ISO = utils.Date.date_to_iso_format(NOW + datetime.timedelta(days=30))

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
MARKET = Contexts.MARKET_RUB_CONTEXT.new()
VZGLYAD = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.VZGLYAD, product=Products.VZGLYAD)
QTY = 100


def create_order(context, client_id, agency_id=None, result={}, descr=''):
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': agency_id,
                                                                              'Text': descr})
    result[descr] = '{}-{}'.format(str(context.service.id), service_order_id)
    return order_id, service_order_id


def create_invoice(context, client_id, agency_id=None, result={}, descr='', with_payment=False, contract_id=None,
                   credit=0, person_id=None):
    if not person_id:
        person_id = steps.PersonSteps.create(agency_id or client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': agency_id,
                                                                              'Text': descr})

    orders_list = [{'ServiceID': context.service.id,
                    'ServiceOrderID': service_order_id,
                    'Qty': QTY,
                    'BeginDT': NOW},
                   ]
    request_id = steps.RequestSteps.create(agency_id or client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=credit,
                                                           contract_id=contract_id, overdraft=0, endbuyer_id=None)
    if with_payment:
        steps.InvoiceSteps.pay(invoice_id)
    print [
        'https://user-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoices.xml',
        'https://user-balance.greed-ts.paysys.yandex.ru/invoices.xml']
    result[descr] = [external_id]
    return service_order_id, order_id, invoice_id


def create_act(context, client_id, agency_id=None, result={}, descr='', with_payment=False, contract_id=None,
               credit=0, person_id=None):
    if not person_id:
        person_id = steps.PersonSteps.create(agency_id or client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': agency_id,
                                                                              'Text': descr})

    orders_list = [{'ServiceID': context.service.id,
                    'ServiceOrderID': service_order_id,
                    'Qty': QTY,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id or client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=credit,
                                                           contract_id=contract_id, overdraft=0, endbuyer_id=None)
    if with_payment:
        steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id,
                                      {context.product.type.code: 100}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id or client_id, force=1, date=NOW)
    result[descr] = [act_id]
    return service_order_id, order_id, invoice_id


def create_request(context_list, client_id, agency_id=None, result={}, descr=''):
    orders_list = []
    order_ids = []
    for context in context_list:
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                           product_id=context.product.id, params={'AgencyID': agency_id,
                                                                                  'Text': descr})
        orders_list.append({'ServiceID': context.service.id,
                            'ServiceOrderID': service_order_id,
                            'Qty': QTY,
                            'BeginDT': NOW})
        order_ids.append(order_id)
    request_id = steps.RequestSteps.create(agency_id or client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))
    result[descr] = [
        'https://user-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/paypreview.xml?request_id={}'.format(
            request_id),
        'https://user-balance.greed-ts.paysys.yandex.ru/paypreview.xml?request_id={}'.format(request_id)]
    return request_id, order_ids


def create_contract(client_id, person_id, contract_params):
    contract_params_default = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'SERVICES': [Services.DIRECT.id],
        'DT': PREVIOUS_MONTH_FIRST_DAY_ISO,
        'FINISH_DT': NOW_ISO,
        'IS_SIGNED': PREVIOUS_MONTH_FIRST_DAY_ISO,
    }
    contract_params_default.update(contract_params['contract_params'])
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_params['contract_type'], contract_params_default,
                                                             prevent_oebs_export=True)
    return contract_id, person_id


def post_pay_contract_personal_account_fictive(contract_type, firm_id, service_list, currency,
                                               additional_params={}):
    default_params = {'contract_type': contract_type,
                      'contract_params': {
                          'SERVICES': service_list,
                          'FIRM': firm_id,
                          'CURRENCY': currency,
                          'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                          # 'PERSONAL_ACCOUNT': 1,
                          # 'PERSONAL_ACCOUNT_FICTIVE': 1,
                      }}
    if additional_params:
        default_params['contract_params'].update(additional_params)
    return default_params


def test_orders_page_jey():
    user = User(436363467, 'yb-atst-user-17')

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    linked_client = steps.ClientSteps.create()
    non_linked_client = steps.ClientSteps.create()

    steps.ClientSteps.unlink_from_login(user.uid)
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
                                                   [linked_client])
    # api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
    #                                                )
    result = {}

    # create_order(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #              descr='linked client order direct')
    # create_order(MARKET, client_id=linked_client, agency_id=agency_id, result=result,
    #              descr='linked client order market')
    create_order(DIRECT, client_id=non_linked_client, agency_id=agency_id, result=result,
                 descr='non_linked client order direct')
    create_order(MARKET, client_id=non_linked_client, agency_id=agency_id, result=result,
                 descr='non_linked client order market')
    create_order(DIRECT, client_id=agency_id, result=result,
                 descr='self agency order direct')
    # create_order(DIRECT, client_id=linked_client, result=result,
    #              descr='self linked client order direct')
    #
    # order_id = create_order(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                         descr='linked client order direct another agency')
    #
    # another_agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    # db.balance().execute('''UPDATE t_order SET agency_id = :agency_id WHERE id = :order_id''',
    #                      {'order_id': order_id, 'agency_id': another_agency_id})
    #
    # child_order = create_order(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                            descr='linked client order direct child')
    #
    # parent_order = create_order(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                             descr='linked client order direct parent')
    # steps.OrderSteps.merge(parent_order, [child_order])

    # child_order, _ = create_order(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                               descr='linked client order direct child')
    #
    # parent_order, _ = create_order(DIRECT, client_id=non_linked_client, agency_id=agency_id, result=result,
    #                             descr='non_linked client order direct parent')
    # db.balance().execute('''UPDATE t_order SET GROUP_ORDER_ID = :parent_order WHERE id = :child_order''',
    #                      {'child_order': child_order, 'parent_order': parent_order})

    print result


# def test_orders_page_with_consume():
#     user = User(436363467, 'yb-atst-user-17')
#
#     agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
#     linked_client = steps.ClientSteps.create()
#     non_linked_client = steps.ClientSteps.create()
#
#     steps.ClientSteps.unlink_from_login(user.uid)
#     api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
#                                                    [linked_client])
#     # api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
#     #                                                )
#     result = {}
#
#     create_order_with_consume(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
#                  descr='linked client order direct')


def test_orders_request_1():
    user = User(436363467, 'yb-atst-user-17')

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    linked_client = steps.ClientSteps.create()
    non_linked_client = steps.ClientSteps.create()

    steps.ClientSteps.unlink_from_login(user.uid)
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
                                                   [linked_client])
    result = {}

    # create_request([DIRECT], client_id=agency_id, result=result,
    #                descr='self agency order direct')
    #
    # create_request([MARKET], client_id=agency_id, result=result,
    #                descr='self agency order market')
    #
    # create_request([DIRECT], client_id=linked_client, result=result,
    #                descr='self client order direct')
    #
    # create_request([MARKET], client_id=linked_client, result=result,
    #                descr='self client order market')

    create_request([DIRECT], client_id=linked_client, agency_id=agency_id, result=result,
                   descr='linked client request with agency direct')

    request_id, order_ids = create_request([DIRECT], client_id=linked_client, agency_id=agency_id, result=result,
                                           descr='linked client request with agency direct 2nd case')

    another_agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    db.balance().execute('''UPDATE T_REQUEST SET CLIENT_ID = :agency_id WHERE id = :request_id''',
                         {'request_id': request_id, 'agency_id': another_agency_id})

    #
    # create_request([MARKET], client_id=linked_client, agency_id=agency_id, result=result,
    #                descr='linked client request with agency market')
    #
    # create_request([DIRECT], client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='linked client request with agency direct')
    #
    # create_request([MARKET], client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='linked client request with agency market')

    # request_id, order_ids = create_request([DIRECT, VZGLYAD], client_id=non_linked_client, agency_id=agency_id,
    #                                        result=result,
    #                                        descr='linked client request with agency direct')
    #
    # request_id, order_ids = create_request([DIRECT, DIRECT], client_id=linked_client, agency_id=agency_id,
    #                                        result=result,
    #                                        descr='linked client request with agency direct and non linked')
    #
    # db.balance().execute('''UPDATE t_order SET CLIENT_ID = :client_id WHERE id = :order_id''',
    #                      {'client_id': non_linked_client, 'order_id': order_ids[0]})
    #
    # request_id, order_ids =  create_request([DIRECT], client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='linked client request with another agency direct')
    # another_agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    # db.balance().execute('''UPDATE T_REQUEST SET CLIENT_ID = :agency_id WHERE id = :request_id''',
    #                      {'request_id': request_id, 'agency_id': another_agency_id})
    #
    # request_id, order_ids =  create_request([MARKET], client_id=linked_client, agency_id=agency_id, result=result,
    #                descr='linked client request with another agency market')
    # another_agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    # db.balance().execute('''UPDATE t_order SET agency_id = :agency_id WHERE id = :order_id''',
    #                      {'order_id': order_ids[0], 'agency_id': another_agency_id})

    #
    # create_request([DIRECT], client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='non linked client request direct')
    #
    # create_request([DIRECT, VZGLYAD], client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='non linked client request direct')
    import pprint
    pprint.pprint(result)


def test_invoices():
    user = User(436363467, 'yb-atst-user-17')

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    another_agency = steps.ClientSteps.create({'IS_AGENCY': 1})
    linked_client = steps.ClientSteps.create()
    non_linked_client = steps.ClientSteps.create()

    steps.ClientSteps.unlink_from_login(user.uid)
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
                                                   [linked_client])

    # api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, 907781, user.uid,
    #                                                [1016416])
    result = {}

    # child_order = create_order(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                            descr='linked client order direct child')
    #
    # steps.InvoiceSteps.pay_with_certificate_or_compensation(child_order, 3000)
    # create_invoice(DIRECT, client_id=agency_id, result=result, descr='self agency order direct', with_payment=False)
    #
    # create_invoice(MARKET, client_id=agency_id, result=result, descr='self agency order market', with_payment=False)
    #
    # create_invoice(MARKET, client_id=linked_client, result=result, descr='self client order direct', with_payment=False)
    #
    # create_invoice(MARKET, client_id=linked_client, agency_id=agency_id, result=result,
    #                descr='agency order linked client market wo consume', with_payment=False)
    #
    # create_invoice(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                descr='agency order linked client direct wo consume', with_payment=False)
    #
    # create_invoice(MARKET, client_id=linked_client, agency_id=agency_id, result=result,
    #                descr='agency order linked client market with consume equal', with_payment=True)
    #
    # create_invoice(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                descr='agency order linked client direct with consume equal', with_payment=True)
    #
    # create_invoice(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                descr='agency order linked client direct with consume equal', with_payment=True)
    #
    # service_order_id, order_id, invoice_id = create_invoice(DIRECT, client_id=linked_client, agency_id=agency_id,
    #                                                         result=result,
    #                                                         descr='agency order linked client direct with consume equal',
    #                                                         with_payment=True)
    #
    # dst_order_id, dst_service_order_id = create_order(DIRECT, client_id=non_linked_client, agency_id=agency_id,
    #                                                   result=result,
    #                                                   descr='linked client order direct another agency')
    #
    # print steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 0, 'all_qty': 0}],
    #                                 [{'order_id': dst_order_id, 'qty_delta': 1}])
    # steps.CampaignsSteps.do_campaigns(DIRECT.service.id, dst_service_order_id,
    #                                   {DIRECT.product.type.code: 100}, 0, NOW)
    # steps.ActsSteps.generate(agency_id, force=1, date=NOW)
    #
    # create_invoice(DIRECT, client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='agency order non linked client direct wo consume', with_payment=True)
    #
    # create_invoice(DIRECT, client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='agency order non linked client direct with consume equal', with_payment=False)
    #
    # create_invoice(MARKET, client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='agency order non linked client market wo consume', with_payment=True)
    #
    # create_invoice(MARKET, client_id=non_linked_client, agency_id=agency_id, result=result,
    #                descr='agency order non linked client market with consume equal', with_payment=False)
    #
    # service_order_id, order_id, invoice_id = create_invoice(DIRECT, client_id=non_linked_client, agency_id=agency_id,
    #                                                         result=result,
    #                                                         descr='agency invoice non_linked client direct with consume equal',
    #                                                         with_payment=True)
    #
    # dst_order_id = create_order(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #                             descr='agency order non_linked client direct with consume equal')
    #
    # print steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 0, 'all_qty': 0}],
    #                                 [{'order_id': dst_order_id, 'qty_delta': 1}])

    person_id = steps.PersonSteps.create(agency_id, DIRECT.person_type.code)
    contract_id, person_id = create_contract(agency_id, person_id,
                                             post_pay_contract_personal_account_fictive(
                                                 contract_type=ContractCommissionType.NO_AGENCY,
                                                 firm_id=Firms.YANDEX_1.id,
                                                 service_list=[Services.DIRECT.id],
                                                 currency=Currencies.RUB.num_code
                                             ))
    service_order_id, order_id, invoice_id = create_invoice(DIRECT, client_id=linked_client, agency_id=agency_id,
                                                            result=result,
                                                            descr='agency invoice linked client direct_LS',
                                                            with_payment=True, contract_id=contract_id, credit=1,
                                                            person_id=person_id)
    #
    # steps.CampaignsSteps.do_campaigns(DIRECT.service.id, service_order_id,
    #                                   {DIRECT.product.type.code: 100}, 0, NOW)
    # act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]

    # person_id = steps.PersonSteps.create(agency_id, 'byu')
    # contract_id, person_id = create_contract(agency_id, person_id,
    #                                          post_pay_contract_personal_account_fictive(
    #                                              contract_type=ContractCommissionType.BEL_PR_AGENCY,
    #                                              firm_id=Firms.REKLAMA_BEL_27.id,
    #                                              service_list=[Services.DIRECT.id],
    #                                              currency=Currencies.BYN.num_code
    #                                          ))
    # service_order_id, order_id, invoice_id = create_invoice(DIRECT, client_id=linked_client, agency_id=agency_id,
    #                                                         result=result,
    #                                                         descr='agency invoice linked client direct_LS',
    #                                                         with_payment=True, contract_id=contract_id, credit=1,
    #                                                         person_id=person_id)

    steps.CampaignsSteps.do_campaigns(DIRECT.service.id, service_order_id,
                                      {DIRECT.product.type.code: 100}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]
    # #
    # service_order_id, order_id, invoice_id = create_invoice(MARKET, client_id=non_linked_client, agency_id=agency_id,
    #                                                         result=result,
    #                                                         descr='agency invoice non linked client direct with consume equal',
    #                                                         with_payment=True)
    #
    # dst_order_id = create_order(MARKET, client_id=linked_client, agency_id=agency_id, result=result,
    #                             descr='agency order non linked client direct with consume equal')
    #
    # print steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 0, 'all_qty': 0}],
    #                                 [{'order_id': dst_order_id, 'qty_delta': 1}])

    import pprint
    pprint.pprint(result)


def test_some_real_invoices():
    user = User(436363467, 'yb-atst-user-17')
    agency_id = 362257
    linked_client = 463723
    steps.ClientSteps.unlink_from_login(user.uid)
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
                                                   [linked_client])
    print [
        'https://user-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoices.xml',
        'https://user-balance.greed-ts.paysys.yandex.ru/invoices.xml']


def test_acts():
    user = User(436363467, 'yb-atst-user-17')

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    another_agency = steps.ClientSteps.create({'IS_AGENCY': 1})
    linked_client = steps.ClientSteps.create()
    non_linked_client = steps.ClientSteps.create()

    steps.ClientSteps.unlink_from_login(user.uid)
    # api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
    #                                                [linked_client])
    steps.ClientSteps.link(client_id=agency_id, login='aikawa-test-10')

    result = {}

    # create_act(DIRECT, client_id=agency_id, result=result, descr='self agency order direct', with_payment=True)
    #
    # create_act(MARKET, client_id=agency_id, result=result, descr='self agency order market', with_payment=True)
    # #
    # create_act(DIRECT, client_id=linked_client, result=result, descr='self client order direct', with_payment=True)
    #
    # create_act(MARKET, client_id=linked_client, result=result, descr='self client order market', with_payment=True)
    #
    create_act(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
               descr='agency order linked client direct', with_payment=True)
    #
    # create_act(MARKET, client_id=linked_client, agency_id=agency_id, result=result,
    #            descr='agency order linked client market', with_payment=True)
    #
    # create_act(DIRECT, client_id=non_linked_client, agency_id=agency_id, result=result,
    #            descr='agency order non linked client direct', with_payment=True)
    #
    # create_act(MARKET, client_id=non_linked_client, agency_id=agency_id, result=result,
    #            descr='agency order non linked client market', with_payment=True)
    #


    import pprint
    pprint.pprint(result)


def test_completly_limited():
    # user = User(436363467, 'yb-atst-user-17')
    #
    # agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    # linked_client = steps.ClientSteps.create()
    # steps.ClientSteps.unlink_from_login(user.uid)
    # api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
    #                                                [None])
    api.medium().CreateUserClientAssociation(16571028, 82272840, 436363467, [82272841])


def test_acts_print_form():
    # user = User(436363467, 'yb-atst-user-17')

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    another_agency = steps.ClientSteps.create({'IS_AGENCY': 1})
    linked_client = steps.ClientSteps.create()
    non_linked_client = steps.ClientSteps.create()

    steps.ClientSteps.unlink_from_login(user.uid)
    # api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, user.uid,
    #                                                [linked_client])
    steps.ClientSteps.link(client_id=agency_id, login='aikawa-test-10')

    result = {}
    service_order_id, order_id, invoice_id = create_invoice(DIRECT, client_id=linked_client, agency_id=agency_id,
                                                            result=result,
                                                            descr='agency order linked client direct with consume equal',
                                                            with_payment=True)

    dst_order_id, dst_service_order_id = create_order(DIRECT, client_id=linked_client, agency_id=agency_id,
                                                      result=result,
                                                      descr='linked client order direct another agency')

    print steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 50, 'all_qty': 0}],
                                    [{'order_id': dst_order_id, 'qty_delta': 1}])
    db.balance().execute('''UPDATE (SELECT * FROM t_order WHERE id = :order_id) SET manager_code = 98700241''',
                         {'order_id': order_id})

    print steps.OrderSteps.transfer([{'order_id': dst_order_id, 'qty_old': 50, 'qty_new': 0, 'all_qty': 0}],
                                    [{'order_id': order_id, 'qty_delta': 1}])

    steps.CampaignsSteps.do_campaigns(DIRECT.service.id, service_order_id,
                                      {DIRECT.product.type.code: 100}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]
    #

    # print steps.OrderSteps.transfer([{'order_id': dst_order_id, 'qty_old': 50, 'qty_new': 0, 'all_qty': 0}],
    #                                 [{'order_id': order_id, 'qty_delta': 1}])


    # create_act(DIRECT, client_id=agency_id, result=result, descr='self agency order direct', with_payment=True)
    #
    # create_act(MARKET, client_id=agency_id, result=result, descr='self agency order market', with_payment=True)
    # #
    # create_act(DIRECT, client_id=linked_client, result=result, descr='self client order direct', with_payment=True)
    #
    # create_act(MARKET, client_id=linked_client, result=result, descr='self client order market', with_payment=True)
    #
    # create_act(DIRECT, client_id=linked_client, agency_id=agency_id, result=result,
    #            descr='agency order linked client direct', with_payment=True)
