# -*- coding: utf-8 -*-

import json
from datetime import datetime
from datetime import timedelta
from decimal import Decimal

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Currencies, \
    Firms, PersonTypes, ContractCommissionType, Paysyses, Collateral
from temp.igogor.balance_objects import Contexts
from jsonrpc import dispatcher

to_iso = utils.Date.date_to_iso_format
NOW = utils.Date.nullify_time_of_date(datetime.now())
DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                               contract_type=ContractCommissionType.NO_AGENCY)
DIRECT_YANDEX_NOT_RESIDENT_USD = DIRECT_YANDEX.new(currency=Currencies.USD, paysys=Paysyses.BANK_YT_USD_AGENCY)
DIRECT_YANDEX_NOT_RESIDENT_RUB = DIRECT_YANDEX_NOT_RESIDENT_USD.new(currency=Currencies.RUB,
                                                                    paysys=Paysyses.BANK_YT_RUB_AGENCY)
DIRECT_SW = DIRECT_YANDEX.new(firm=Firms.EUROPE_AG_7, currency=Currencies.USD,
                                                person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_USD,
                                                contract_type=ContractCommissionType.SW_OPT_CLIENT)
MEDIA = Contexts.MEDIA_70_SHOWS_RUB.new(firm=Firms.MARKET_111, contract_type=ContractCommissionType.NO_AGENCY)


@dispatcher.add_method
def test_credits_currency():
    params = {'CREDIT_LIMIT_SINGLE': Decimal(123456),
               'EXTERNAL_ID': 'test-hermione-contract-credits-02/1'}
    client_id, person_id, contract_id1, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_SW,
                                                                                                  postpay=True,
                                                                                                  old_pa=True,
                                                                                                  additional_params=params)
    create_invoice_and_act(DIRECT_SW, client_id, person_id, contract_id1, qty=Decimal('5.44'), act_needed=False)

    return {'client_id': client_id}


@dispatcher.add_method
def test_credits_agency_with_individual_limits():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    subclient_1 = steps.ClientSteps.create({'NAME': u'Резидент 1'})
    subclient_2 = steps.ClientSteps.create({'NAME': u'Резидент 2'})
    subclient_3 = steps.ClientSteps.create_sub_client_non_resident(Currencies.USD.char_code, params={'NAME': u'Нерезидент 1'})
    subclient_4 = steps.ClientSteps.create()
    subclient_5 = steps.ClientSteps.create_sub_client_non_resident(Currencies.RUB.char_code, params={'NAME': u'Нерезидент 2'})

    params = {'CREDIT_TYPE': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'COMMISSION_TYPE': 47,
              'EXTERNAL_ID': 'test-hermione-contract-credits-01/1',
              }

    # создаем первый договор для работы с субклиентами резидентами
    _, person_id, contract_id1, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX, client_id=agency_id,
                                                                                        contract_type=ContractCommissionType.COMMISS.id,
                                                                                        postpay=True,
                                                                                        finish_dt=NOW + timedelta(days=180),
                                                                                        start_dt=NOW - timedelta(days=30),
                                                                                        additional_params=params)

    # создаем второй договор для работы с субклиентами нерезидентами
    params.update({'NON_RESIDENT_CLIENTS': 1,
                   'CREDIT_LIMIT_SINGLE': 50000,
                   'EXTERNAL_ID': 'test-hermione-contract-nonres-credits-01/2',})
    _, _, contract_id2, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX, client_id=agency_id, person_id=person_id,
                                                                                        contract_type=ContractCommissionType.COMMISS.id,
                                                                                        postpay=True,
                                                                                        finish_dt=NOW + timedelta(days=180),
                                                                                        start_dt=NOW - timedelta(days=30),
                                                                                        additional_params=params)

    # создаем договорам дс на индивидуальные лимиты
    create_collateral_limit(contract_id1, subclient_1, days_delta=0)
    create_collateral_limit(contract_id1, subclient_2, limit=950, credit_type=1, days_delta=1)
    create_collateral_limit(contract_id2, subclient_3, limit=3000, days_delta=2)

    act_id1, _ = create_invoice_and_act(DIRECT_YANDEX, subclient_1, person_id, contract_id1, qty=Decimal('5.44'), agency_id=agency_id)
    _, invoice_id_2_individ = create_invoice_and_act(DIRECT_YANDEX_NOT_RESIDENT_USD, subclient_3, person_id, contract_id2, qty=Decimal('3.44'), agency_id=agency_id, act_needed=False)
    create_invoice_and_act(DIRECT_YANDEX, subclient_4, person_id, contract_id1, qty=Decimal('10.33'), agency_id=agency_id, act_needed=False)
    act_id2, invoice_id2 = create_invoice_and_act(DIRECT_YANDEX_NOT_RESIDENT_RUB, subclient_5, person_id, contract_id2, qty=Decimal('80.87'), agency_id=agency_id)

    steps.ActsSteps.set_payment_term_dt(act_id1, NOW - timedelta(days=1))
    steps.ActsSteps.set_payment_term_dt(act_id2, NOW - timedelta(days=1))

    return {'client_id': agency_id, 'contract_id': contract_id2, 'invoice_id': invoice_id2,
            'individ_invoice_id': invoice_id_2_individ}


@dispatcher.add_method
def test_credits_nonactive_contracts():

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    subclient = steps.ClientSteps.create()

    params = {'CREDIT_TYPE': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'COMMISSION_TYPE': 47,
              'EXTERNAL_ID': 'test-hermione-contract-credits-03/1',
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
                                    agency_id=agency_id, dt=NOW-timedelta(days=30), act_needed=False)

    # создаем приостановленный договор с использованным лимитом
    params.update({'EXTERNAL_ID': 'test-hermione-contract-credits-03/2', 'CREDIT_LIMIT_SINGLE': 5000})
    _, _, contract_id2, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                           client_id=agency_id,
                                                                                           person_id=person_id,
                                                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                                                           postpay=True,
                                                                                           finish_dt=NOW + timedelta(days=180),
                                                                                           additional_params=params)
    create_invoice_and_act(DIRECT_YANDEX, subclient, person_id, contract_id2, qty=Decimal('10'),
                                    agency_id=agency_id, act_needed=False)
    #приостанавливаем договор
    collateral_id = steps.ContractSteps.get_collateral_id(contract_id2)
    query = "update t_contract_attributes set value_dt = :suspended_date where code = 'IS_SUSPENDED' and collateral_id = :collateral_id"
    params_col = {'collateral_id': collateral_id,
              'suspended_date': NOW}
    db.balance().execute(query, params_col)
    steps.ContractSteps.refresh_contracts_cache(contract_id2)

    # создаем договор с датой начала в будущем (не должен отображаться)
    params.update({'EXTERNAL_ID': 'test-hermione-contract-credits-03/3'})
    steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                           client_id=agency_id,
                                                                                           person_id=person_id,
                                                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                                                           postpay=True,
                                                                                           start_dt=NOW + timedelta(days=30),
                                                                                           finish_dt=NOW + timedelta(days=180),
                                                                                           additional_params=params)

    # создаем договор с датой окончания в прошлом (не должен отображаться)
    params.update({'EXTERNAL_ID': 'test-hermione-contract-credits-03/4'})
    steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                           client_id=agency_id,
                                                                                           person_id=person_id,
                                                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                                                           postpay=True,
                                                                                           start_dt=NOW - timedelta(days=180),
                                                                                           finish_dt=NOW,
                                                                                           additional_params=params)

    # создаем приостановленный договор (не должен отображаться)
    params.update({'EXTERNAL_ID': 'test-hermione-contract-credits-03/5', 'IS_SUSPENDED': to_iso(NOW)})
    steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                           client_id=agency_id,
                                                                                           person_id=person_id,
                                                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                                                           postpay=True,
                                                                                           start_dt=NOW,
                                                                                           finish_dt=NOW + timedelta(days=180),
                                                                                           additional_params=params)

    # создаем аннулированный договор (не должен отображаться)
    params.update({'EXTERNAL_ID': 'test-hermione-contract-credits-03/6'})
    params.pop('IS_SUSPENDED')
    _, _, contract_id6, _ = steps.ContractSteps.create_general_contract_by_context(DIRECT_YANDEX,
                                                                                           client_id=agency_id,
                                                                                           person_id=person_id,
                                                                                           contract_type=ContractCommissionType.COMMISS.id,
                                                                                           postpay=True,
                                                                                           start_dt=NOW,
                                                                                           finish_dt=NOW + timedelta(days=180),
                                                                                           additional_params=params)
    #аннулируем договор
    query = "update t_contract_collateral set is_cancelled = :cancelled_date where contract2_id = :contract_id"
    params_col = {'cancelled_date': NOW, 'contract_id': contract_id6}
    db.balance().execute(query, params_col)
    steps.ContractSteps.refresh_contracts_cache(contract_id6)

    return {'client_id': agency_id}


@dispatcher.add_method
def test_credits_fictive_scheme():
    params = {'EXTERNAL_ID': 'test-hermione-contract-credits-04/1',}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(MEDIA,
                                                                                           postpay=True, fictive_scheme=True,
                                                                                           additional_params=params)

    collateral_id = steps.ContractSteps.get_collateral_id(contract_id)
    attribute_batch_id = steps.ContractSteps.get_attribute_batch_id(contract_id)
    insert_query = "Insert into t_contract_attributes (ID,COLLATERAL_ID,DT,CODE,KEY_NUM,VALUE_STR," \
                       "VALUE_NUM,VALUE_DT,UPDATE_DT,PASSPORT_ID,VALUE_CLOB,ATTRIBUTE_BATCH_ID,RELATED_OBJECT_TABLE)  " \
                       "values (s_contract_attributes_id.nextval,:collateral_id,:dt,'CREDIT_LIMIT',113,null,2000000," \
                       "null, :dt,'16571028', EMPTY_CLOB(), :attribute_batch_id,'T_CONTRACT_COLLATERAL')"
    params = {'attribute_batch_id': attribute_batch_id, 'collateral_id': collateral_id, 'dt': NOW}
    db.balance().execute(insert_query, params)
    insert_query = "Insert into t_contract_attributes (ID,COLLATERAL_ID,DT,CODE,KEY_NUM,VALUE_STR," \
                       "VALUE_NUM,VALUE_DT,UPDATE_DT,PASSPORT_ID,VALUE_CLOB,ATTRIBUTE_BATCH_ID,RELATED_OBJECT_TABLE)  " \
                       "values (s_contract_attributes_id.nextval,:collateral_id,:dt,'CREDIT_LIMIT',19,null,6000," \
                       "null, :dt,'16571028', EMPTY_CLOB(), :attribute_batch_id,'T_CONTRACT_COLLATERAL')"
    db.balance().execute(insert_query, params)
    delete_query = "delete from t_contract_attributes where code = 'CREDIT_LIMIT_SINGLE' and collateral_id = :collateral_id"
    params = {'collateral_id': collateral_id}
    db.balance().execute(delete_query, params)

    create_invoice_and_act(MEDIA, client_id, person_id, contract_id, act_needed=False)
    steps.ContractSteps.refresh_contracts_cache(contract_id)

    return {'client_id': client_id, 'contract_id': contract_id}


def create_collateral_limit(contract_id, subclient_id, limit=9000, credit_type=2, days_delta = 0):
    individual_limits = json.dumps([{u'client_credit_type': credit_type, u'id': u'1', u'client_limit_currency': u'',
                                     u'num': subclient_id, u'client': subclient_id,
                                     u'client_payment_term': u'10', u'client_limit': limit}])
    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, {'CONTRACT2_ID': contract_id,
                                                 'DT': to_iso(NOW - timedelta(days=days_delta)),
                                                 'IS_SIGNED': to_iso(NOW - timedelta(days=days_delta)),
                                                 'CLIENT_LIMITS': individual_limits})


def create_invoice_and_act(context, client_id, person_id, contract_id, qty=1, agency_id=None, dt=NOW, act_needed=True):
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                                       service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=agency_id or client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': dt})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id,
                                                 credit=1, contract_id=contract_id)
    act_id = None
    if act_needed:
        steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: qty}, 0)
        act_id = steps.ActsSteps.generate(agency_id or client_id, force=1)[0]
    return act_id, invoice_id
