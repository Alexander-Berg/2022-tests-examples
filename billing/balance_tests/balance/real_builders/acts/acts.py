# -*- coding: utf-8 -*-

import datetime
from dateutil.relativedelta import relativedelta
from decimal import Decimal as D
from balance import balance_db as db

from . import steps
from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, Paysyses, PersonTypes, ContractCommissionType, Currencies
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED
from .. import common_defaults
from jsonrpc import dispatcher

NOW = datetime.datetime.now()
COMPLETIONS_DT = NOW
ACT_DT = NOW
_, _, contract_start_dt, act_dt_1, _, act_dt_2 = utils.Date.previous_three_months_start_end_dates()

CONTEXT = steps.CONTEXT

DIRECT_CONTEXT_FIRM_4 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4,
                                                             person_type=PersonTypes.USU,
                                                             paysys=Paysyses.BANK_US_UR_USD,
                                                             contract_type=ContractCommissionType.USA_OPT_CLIENT,
                                                             currency=Currencies.USD.num_code,
                                                             person_params=common_defaults.FIXED_USU_PARAMS)
DIRECT_CONTEXT_FIRM_7 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                             person_type=PersonTypes.SW_UR,
                                                             paysys=Paysyses.BANK_SW_UR_CHF,
                                                             contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                             currency=Currencies.CHF.num_code,
                                                             person_params=common_defaults.FIXED_SW_UR_PARAMS)
QTY = D('100')
COMPLETIONS = D('50')
TODAY = utils.Date.nullify_time_of_date(datetime.datetime.today())
CONTRACT_START_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=4))
CONTRACT_END_DT = utils.Date.date_to_iso_format(TODAY + relativedelta(months=3))
CREDIT_LIMIT_USD = D('1000')


# акт, директ
@dispatcher.add_method
def test_act():
    client_id, person_id, invoice_id, external_invoice_id, _, external_act_id, _ = steps.create_base_act()
    return client_id, person_id, _, external_invoice_id, external_act_id


# создаем и удаляем акт, директ
@dispatcher.add_method
def test_hidden_act():
    _, _, _, _, act_id, external_act_id, _ = steps.create_base_act()
    balance_steps.ActsSteps.hide(act_id)
    return external_act_id


# создаем, удаляем и восстанавливаем акт, директ
def test_unhidden_act():
    _, _, _, _, act_id, external_act_id, _ = steps.create_base_act()
    balance_steps.ActsSteps.hide(act_id)
    balance_steps.ActsSteps.unhide(act_id)
    return external_act_id


# внутренний акт
@dispatcher.add_method
def test_internal_act():
    client_id, _, _, _, act_id, external_act_id, service_order_id_list = steps.create_base_act()
    db.balance().execute('UPDATE t_act_internal SET amount_nds = 249.99 WHERE id = :act_id', {'act_id': act_id})
    db.balance().execute('UPDATE t_act_trans SET amount_nds = 249.99 WHERE act_id = :act_id', {'act_id': act_id})
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.service.id, service_order_id_list[0],
                                      {CONTEXT.product.type.code: 50.0004}, 0, ACT_DT)
    act_id_internal = balance_steps.ActsSteps.generate(client_id, force=1, date=ACT_DT)[0]
    # >_<
    external_act_id_internal = db.balance().execute('SELECT external_id FROM t_act_internal '
                                                    'WHERE id = :act_id', {'act_id': act_id_internal})[0]
    return external_act_id_internal


# акт, архивация плательщика, директ
def test_hidden_person_act():
    _, person_id, _, _, _, external_act_id, _ = steps.create_base_act()
    balance_steps.PersonSteps.hide_person(person_id)
    return external_act_id


# акты на директ и маркет у одного клиента
@dispatcher.add_method
def test_two_services_and_firms_act():
    client_id, _, _, _, act_id_direct, external_id_direct, _ = steps.create_base_act()
    context = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111,
                                              person_params=common_defaults.FIXED_UR_PARAMS)
    _, _, _, _, act_id_market, external_id_market, _ = steps.create_base_act(context=context, client_id=client_id,
                                                                             act_num=1)
    return client_id, act_id_direct, external_id_direct, act_id_market, external_id_market


# акты за предыдущий и предпредыдущий месяц, корптакси
def test_two_months_with_contract_act():
    context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            additional_params={'start_dt': contract_start_dt})

    _, external_id_1 = steps.create_partner_act(context, client_id, contract_id, act_dt_1)

    _, external_id_2 = steps.create_partner_act(context, client_id, contract_id, act_dt_2, coef=D('3.5'))
    return contract_eid, external_id_1, external_id_2, act_dt_1, act_dt_2


# акты в трех валютах, директ
def test_three_currencies_eur_usd_chf_act():
    context_eur = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_params=common_defaults.FIXED_SW_UR_PARAMS)
    client_id, _, _, _, act_id_eur, external_id_eur, _ = steps.create_base_act(context=context_eur)
    context_chf = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(paysys=Paysyses.BANK_SW_UR_CHF,
                                                          person_params=common_defaults.FIXED_SW_UR_PARAMS)
    _, _, _, _,  act_id_chf, external_id_chf, _ = steps.create_base_act(context=context_chf, client_id=client_id,
                                                                        act_num=1)
    context_usd = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(paysys=Paysyses.BANK_SW_UR_USD,
                                                          person_params=common_defaults.FIXED_SW_UR_PARAMS)
    _, _, _, _, act_id_usd, external_id_usd, _ = steps.create_base_act(context=context_usd, client_id=client_id,
                                                                       act_num=1)
    return client_id, act_id_eur, external_id_eur, act_id_chf, external_id_chf, act_id_usd, external_id_usd


# акты в трех валютах, директ
@dispatcher.add_method
def test_three_currencies_eur_usd_chf_act_ci(login='yndx-balance-assessor-100'):
    context_eur = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_params=common_defaults.FIXED_SW_UR_PARAMS)
    client_id, _, _, _, act_id_eur, external_id_eur, _ = steps.create_base_act(context=context_eur)
    context_chf = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(paysys=Paysyses.BANK_SW_UR_CHF,
                                                          person_params=common_defaults.FIXED_SW_UR_PARAMS)
    _, _, _, _,  act_id_chf, external_id_chf, _ = steps.create_base_act(context=context_chf, client_id=client_id,
                                                                        act_num=1)
    context_usd = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(paysys=Paysyses.BANK_SW_UR_USD,
                                                          person_params=common_defaults.FIXED_SW_UR_PARAMS)
    _, _, _, _, act_id_usd, external_id_usd, _ = steps.create_base_invoice(context=context_usd, client_id=client_id, qty=D(100))
    _, _, _, _, act_id_usd, external_id_usd, _ = steps.create_base_act(context=context_usd, client_id=client_id,
                                                                       act_num=1)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, act_id_eur, external_id_eur, act_id_chf, external_id_chf, act_id_usd, external_id_usd


# акт с печатной формой (директ, 7 фирма, кредит)
@dispatcher.add_method
def test_print_form_postpay_firm_7_act():
    context = DIRECT_CONTEXT_FIRM_7
    contract_params = {'DT': CONTRACT_START_DT,
                       'FINISH_DT': CONTRACT_END_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_USD,
                       'SERVICES': [context.service.id],
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(context.currency),
                       'FIRM': context.firm.id,
                       }
    client_id, _, _, _, act_id, external_id, _ = \
        steps.create_base_act(overdraft=0, orders_amount=3, qty=QTY, credit=2, context=context,
                              contract_params=contract_params, contract_type=context.contract_type)

    query = "update t_act set is_docs_separated=1, is_docs_detailed=1, " \
            "memo='{\"memo\": \"Текст заметки к акту\"}' where id = :act_id"
    db.balance().execute(query, {'act_id': act_id})
    return client_id, act_id, external_id


# акт с печатной формой (директ, 1 фирма, isTRP)
@dispatcher.add_method
def test_print_form_trp_act(login=None):
    client_id, _, _, _, act_id, external_id, _ = steps.create_base_act()

    query = "update t_act set is_trp=1 where id = :act_id"
    db.balance().execute(query, {'act_id': act_id})
    if login:
        steps.ClientSteps.link(client_id, login)
    return client_id, act_id, external_id


# акт с печатной формой (директ, 4 фирма)
@dispatcher.add_method
def test_print_form_firm_4_act(login=None):
    context = DIRECT_CONTEXT_FIRM_4
    client_id, _, _, _, act_id, external_id, _ = \
        steps.create_base_act(overdraft=0, orders_amount=3, qty=QTY, context=context,
                              completions_dt=TODAY - relativedelta(months=4),
                              act_dt=TODAY - relativedelta(months=3),
                              invoice_desired_dt=TODAY - relativedelta(months=4))

    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, act_id, external_id


# акт с галкой "Хороший долг"
@dispatcher.add_method
def test_good_debt_act():
    client_id, person_id, invoice_id, external_invoice_id, act_id, external_id, _ = steps.create_base_act()
    query = "update t_act set good_debt=1 where id = :act_id"
    db.balance().execute(query, {'act_id': act_id})
    return client_id, act_id, external_id


@dispatcher.add_method
def test_unlink_accountant(login):
    passport_id = balance_steps.PassportSteps.get_passport_by_login(login)['Uid']
    query = 'delete from t_role_client_user where passport_id = :passport_id'
    params = {'passport_id': passport_id}
    res = db.balance().execute(query, params)
    return res

@dispatcher.add_method
def test_unlink_client(login):
    passport_id = balance_steps.PassportSteps.get_passport_by_login(login)['Uid']
    query = 'update T_PASSPORT set CLIENT_ID = NULL where passport_id = :passport_id'
    params = {'passport_id': passport_id}
    db.balance().execute(query, params)
    return login
