# coding=utf-8
__author__ = 'atkaya'

from dateutil.relativedelta import relativedelta
from datetime import timedelta, datetime
from decimal import Decimal
import json

from balance import balance_steps as steps
from btestlib.constants import PersonTypes, Services, Paysyses, Products, \
    Currencies, ContractCommissionType, Firms, Collateral
import balance.balance_db as db
import btestlib.utils as utils
from temp.igogor.balance_objects import Contexts
from decimal import Decimal as D


def test_deferpays_nonactive_contracts():
    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    NOW = datetime.today()
    to_iso = utils.Date.date_to_iso_format

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(agency_id, 'yb-static-deferpays-1')

    subclient = steps.ClientSteps.create()

    params = {'CREDIT_TYPE': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'COMMISSION_TYPE': 47,
              'EXTERNAL_ID': 'Завершившийся договор',
              }

    # создаем завершившийся договор с использованным лимитом
    _, person_id, contract_id1, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                           client_id=agency_id,
                                                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                                                           postpay=True,
                                                                                           start_dt=NOW - timedelta(
                                                                                               days=180),
                                                                                           finish_dt=NOW,
                                                                                           additional_params=params)
    create_invoice_and_act(DIRECT_YANDEX, subclient, person_id, contract_id1, qty=Decimal('5.44'),
                           agency_id=agency_id, dt=NOW - timedelta(days=30), act_needed=False)

    # создаем приостановленный договор с использованным лимитом
    params.update({'EXTERNAL_ID': 'Приостановленный договор', 'CREDIT_LIMIT_SINGLE': 5000})
    _, _, contract_id2, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                   client_id=agency_id,
                                                                                   person_id=person_id,
                                                                                   contract_type=ContractCommissionType.COMMISS.id,
                                                                                   postpay=True,
                                                                                   finish_dt=NOW + timedelta(days=180),
                                                                                   additional_params=params)
    create_invoice_and_act(DIRECT_YANDEX, subclient, person_id, contract_id2, qty=Decimal('10'),
                           agency_id=agency_id, act_needed=False)
    # приостанавливаем договор
    collateral_id = steps.ContractSteps.get_collateral_id(contract_id2)
    query = "update t_contract_attributes set value_dt = :suspended_date where code = 'IS_SUSPENDED' and collateral_id = :collateral_id"
    params_col = {'collateral_id': collateral_id,
                  'suspended_date': NOW}
    db.balance().execute(query, params_col)
    steps.ContractSteps.refresh_contracts_cache(contract_id2)

    # создаем договор с датой начала в будущем (не должен отображаться)
    params.update({'EXTERNAL_ID': 'Договор из будущего'})
    steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                           client_id=agency_id,
                                                           person_id=person_id,
                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                           postpay=True,
                                                           start_dt=NOW + timedelta(days=180),
                                                           finish_dt=NOW + timedelta(days=280),
                                                           additional_params=params)

    # создаем договор с датой окончания в прошлом (не должен отображаться)
    params.update({'EXTERNAL_ID': 'Договор из прошлого'})
    steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                           client_id=agency_id,
                                                           person_id=person_id,
                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                           postpay=True,
                                                           start_dt=NOW - timedelta(days=180),
                                                           finish_dt=NOW,
                                                           additional_params=params)

    # создаем приостановленный договор (не должен отображаться)
    params.update({'EXTERNAL_ID': 'Приостановленный договор', 'IS_SUSPENDED': to_iso(NOW)})
    steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                           client_id=agency_id,
                                                           person_id=person_id,
                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                           postpay=True,
                                                           start_dt=NOW,
                                                           finish_dt=NOW + timedelta(days=180),
                                                           additional_params=params)

    # создаем аннулированный договор (не должен отображаться)
    params.update({'EXTERNAL_ID': 'Аннулированный договор'})
    params.pop('IS_SUSPENDED')
    _, _, contract_id6, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                   client_id=agency_id,
                                                                                   person_id=person_id,
                                                                                   contract_type=ContractCommissionType.COMMISS.id,
                                                                                   postpay=True,
                                                                                   start_dt=NOW,
                                                                                   finish_dt=NOW + timedelta(days=180),
                                                                                   additional_params=params)
    # аннулируем договор
    query = "update t_contract_collateral set is_cancelled = :cancelled_date where contract2_id = :contract_id"
    params_col = {'cancelled_date': NOW, 'contract_id': contract_id6}
    db.balance().execute(query, params_col)
    steps.ContractSteps.refresh_contracts_cache(contract_id6)


def test_deferpays_indlimits():
    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    DIRECT_YANDEX_NOT_RESIDENT_USD = DIRECT_YANDEX.new(currency=Currencies.USD, paysys=Paysyses.BANK_YT_USD_AGENCY)
    DIRECT_YANDEX_NOT_RESIDENT_RUB = DIRECT_YANDEX_NOT_RESIDENT_USD.new(currency=Currencies.RUB,
                                                                        paysys=Paysyses.BANK_YT_RUB_AGENCY)

    NOW = datetime.today()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(agency_id, 'yb-static-deferpays-2')
    subclient_1 = steps.ClientSteps.create({'NAME': u'Резидент 1'})
    subclient_2 = steps.ClientSteps.create({'NAME': u'Резидент 2'})
    subclient_3 = steps.ClientSteps.create_sub_client_non_resident(Currencies.USD.char_code,
                                                                   params={'NAME': u'Нерезидент 1'})
    subclient_4 = steps.ClientSteps.create()
    subclient_5 = steps.ClientSteps.create_sub_client_non_resident(Currencies.RUB.char_code,
                                                                   params={'NAME': u'Нерезидент 2'})

    params = {'CREDIT_TYPE': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'COMMISSION_TYPE': 47,
              'EXTERNAL_ID': 'Договор c резидентами',
              }

    # создаем первый договор для работы с субклиентами резидентами
    _, person_id, contract_id1, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                           client_id=agency_id,
                                                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                                                           postpay=True,
                                                                                           finish_dt=NOW + timedelta(
                                                                                               days=180),
                                                                                           additional_params=params)

    # создаем второй договор для работы с субклиентами нерезидентами
    params.update({'NON_RESIDENT_CLIENTS': 1,
                   'CREDIT_LIMIT_SINGLE': 50000,
                   'EXTERNAL_ID': 'Договор c нерезидентами', })
    _, _, contract_id2, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX, client_id=agency_id,
                                                                                   person_id=person_id,
                                                                                   contract_type=ContractCommissionType.COMMISS.id,
                                                                                   postpay=True,
                                                                                   finish_dt=NOW + timedelta(days=180),
                                                                                   additional_params=params)

    # создаем договорам дс на индивидуальные лимиты
    create_collateral_limit(contract_id1, subclient_1)
    create_collateral_limit(contract_id1, subclient_2, limit=950, credit_type=1)
    create_collateral_limit(contract_id2, subclient_3, limit=3000)

    act_id1, _ = create_invoice_and_act(DIRECT_YANDEX, subclient_1, person_id, contract_id1, qty=Decimal('5.44'),
                                        agency_id=agency_id)
    _, invoice_id_2_individ = create_invoice_and_act(DIRECT_YANDEX_NOT_RESIDENT_USD, subclient_3, person_id,
                                                     contract_id2, qty=Decimal('3.44'), agency_id=agency_id,
                                                     act_needed=False)
    create_invoice_and_act(DIRECT_YANDEX, subclient_4, person_id, contract_id1, qty=Decimal('10.33'),
                           agency_id=agency_id, act_needed=False)
    act_id2, invoice_id2 = create_invoice_and_act(DIRECT_YANDEX_NOT_RESIDENT_RUB, subclient_5, person_id, contract_id2,
                                                  qty=Decimal('80.87'), agency_id=agency_id)

    steps.ActsSteps.set_payment_term_dt(act_id1, NOW - timedelta(days=1))
    steps.ActsSteps.set_payment_term_dt(act_id2, NOW - timedelta(days=1))


def test_deferpays_currency():
    DIRECT_SW = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7, currency=Currencies.USD,
                                                     person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_USD,
                                                     contract_type=ContractCommissionType.SW_OPT_CLIENT)
    params = {'CREDIT_LIMIT_SINGLE': Decimal(123456),
              'EXTERNAL_ID': 'Долларовый договор'}
    client_id, person_id, contract_id1, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_SW,
                                                                                                   postpay=True,
                                                                                                   old_pa=True,
                                                                                                   additional_params=params)
    steps.ClientSteps.link(client_id, 'yndx-static-deferpays-3')
    create_invoice_and_act(DIRECT_SW, client_id, person_id, contract_id1, qty=Decimal('5.44'), act_needed=False)


def test_deferpays_fictive():
    to_iso = utils.Date.date_to_iso_format
    DATE = datetime(2021, 1, 1, 0, 0, 0)
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                   contract_type=ContractCommissionType.NO_AGENCY)

    contract_params = {'DT': to_iso(DATE),
                       'IS_SIGNED': to_iso(DATE),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'Договор'
                       }

    client_id = steps.ClientSteps.create({'NAME': 'Клиент'})
    steps.ClientSteps.link(client_id, 'yb-static-deferpays-4')
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    contract_params.update({'CLIENT_ID': client_id,
                            'PERSON_ID': person_id})
    contract_id, contract_external_id = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)

    def create_orders_and_invoices(service_id, product_id, i, need_repayment, need_confirmation, paid):
        service_order_id = steps.OrderSteps.next_id(service_id=service_id)
        steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                                service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': i,
             'BeginDT': DATE + relativedelta(days=i)}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params={'InvoiceDesireDT':
                                                                      DATE + relativedelta(days=i)})
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=context.paysys.id,
                                                     credit=2, contract_id=contract_id)
        if need_repayment:
            repayment_invoice_id = \
                steps.InvoiceSteps.make_repayment_invoice(invoice_id, with_confirmation=need_confirmation)[0]
            if paid:
                steps.InvoiceSteps.pay(repayment_invoice_id)
        steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': i}, 0,
                                          DATE + relativedelta(days=i))

        query = 'UPDATE T_DEFERPAY SET ISSUE_DT = :issue_dt WHERE INVOICE_ID = :invoice_id'
        params = {'issue_dt': DATE + relativedelta(days=i), 'invoice_id': invoice_id}
        db.balance().execute(query, params)

    for i in range(1, 6):
        create_orders_and_invoices(context.service.id, context.product.id, i, True, False, False)

    for i in range(1, 6):
        create_orders_and_invoices(Services.MEDIA_BANNERS.id, Products.BAYAN.id, i, True, True, True)

    for i in range(1, 6):
        create_orders_and_invoices(Services.GEO.id, Products.GEO.id, i, False, False, False)


def test_deferpays_limit_exceeded():
    DIRECT_SW = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7, currency=Currencies.USD,
                                                     person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_USD,
                                                     contract_type=ContractCommissionType.SW_OPT_CLIENT)
    params = {'CREDIT_LIMIT_SINGLE': Decimal(6),
              'EXTERNAL_ID': 'Долларовый договор'}
    client_id, person_id, contract_id1, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_SW,
                                                                                                   postpay=True,
                                                                                                   old_pa=True,
                                                                                                   additional_params=params)
    steps.ClientSteps.link(client_id, 'yb-static-deferpays-5')
    create_invoice_and_act(DIRECT_SW, client_id, person_id, contract_id1, qty=Decimal('5.44'), act_needed=False)
    steps.ContractSteps.create_collateral_real(contract_id1, Collateral.CHANGE_CREDIT, {
        'DT': datetime.today(),
        'IS_SIGNED': datetime.today(),
        'CREDIT_LIMIT_SINGLE': D(1),
        'PAYMENT_TYPE': 3,
        'PAYMENT_TERM': 100,
        'CREDIT_TYPE': 2
    })


# utils
def create_invoice_and_act(context, client_id, person_id, contract_id, qty=1, agency_id=None, dt=datetime.today(),
                           act_needed=True):
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=agency_id or client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': dt})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=1, contract_id=contract_id)
    act_id = None
    if act_needed:
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: qty}, 0)
        act_id = steps.ActsSteps.generate(agency_id or client_id, force=1)[0]
    return act_id, invoice_id


def create_collateral_limit(contract_id, subclient_id, limit=9000, credit_type=2):
    to_iso = utils.Date.date_to_iso_format
    individual_limits = json.dumps([{u'client_credit_type': credit_type, u'id': u'1', u'client_limit_currency': u'',
                                     u'num': subclient_id, u'client': subclient_id,
                                     u'client_payment_term': u'10', u'client_limit': limit}])
    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, {'CONTRACT2_ID': contract_id,
                                                                              'DT': to_iso(datetime.today()),
                                                                              'IS_SIGNED': to_iso(datetime.today()),
                                                                              'CLIENT_LIMITS': individual_limits})
