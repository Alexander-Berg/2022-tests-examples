# coding: utf-8
__author__ = 'blubimov'

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import reporter
from btestlib import utils
from btestlib.constants import ContractCommissionType, Firms, PersonTypes, Currencies, Services, ContractPaymentType, \
    ContractCreditType, Collateral
from btestlib.data.defaults import Date
from post_restore_common import ContractTemplate, get_client_linked_with_login_or_create, \
    check_and_hide_existing_test_contracts_and_persons, BASE_CONTRACT_PARAMS, CollateralTemplate

"""
Восстановление данных необходимых для проверки интеграции AdOffice -> Balance
Заказчики: olimiya, mestet

Тикеты: TESTBALANCE-751
"""

CREDIT_LIMIT_ADOFFICE_DEFAULT = 1000000000


class ContractTemplates(object):
    BASE_ADOFFICE_POSTPAY_PARAMS = utils.merge_dicts([BASE_CONTRACT_PARAMS, {
        'FIRM': Firms.YANDEX_1,
        'CURRENCY': Currencies.RUB,
        'SERVICES': [Services.MEDIA_70],
        'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
        'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
        'PAYMENT_TERM': 180,
        'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_ADOFFICE_DEFAULT,
        'DT': Date.TODAY,
        'FINISH_DT': Date.YEAR_AFTER_TODAY,
        'IS_SIGNED': Date.TODAY,
        'DEAL_PASSPORT': Date.TODAY,
    }])

    COMMISS_YT_RUB_POSTPAY = ContractTemplate(type=ContractCommissionType.COMMISS,
                                              person_type=PersonTypes.YT,
                                              params=BASE_ADOFFICE_POSTPAY_PARAMS,
                                              add_params={
                                                  'CURRENCY': Currencies.RUB,
                                              })

    COMMISS_YT_USD_POSTPAY = ContractTemplate(type=ContractCommissionType.COMMISS,
                                              person_type=PersonTypes.YT,
                                              params=BASE_ADOFFICE_POSTPAY_PARAMS,
                                              add_params={
                                                  'CURRENCY': Currencies.USD,
                                              })

    COMMISS_YT_EUR_POSTPAY = ContractTemplate(type=ContractCommissionType.COMMISS,
                                              person_type=PersonTypes.YT,
                                              params=BASE_ADOFFICE_POSTPAY_PARAMS,
                                              add_params={
                                                  'CURRENCY': Currencies.EUR,
                                              })


LOGIN_TO_CONTRACTS_MAP = {
    'display-test-multicurrency': [ContractTemplates.COMMISS_YT_RUB_POSTPAY,
                                   ContractTemplates.COMMISS_YT_USD_POSTPAY,
                                   ContractTemplates.COMMISS_YT_EUR_POSTPAY]
}


@pytest.mark.parametrize('login, contract_templates', LOGIN_TO_CONTRACTS_MAP.items(),
                         ids=lambda login, tpl: '{}'.format(login))
def test_restore_contracts(login, contract_templates):
    client_id = get_client_linked_with_login_or_create(login)

    check_and_hide_existing_test_contracts_and_persons(client_id)

    for contract_tpl in contract_templates:
        contract_tpl.create(client_id)


class CollateralTemplates(object):
    PROLONG = CollateralTemplate(type=Collateral.PROLONG,
                                 params={
                                     'DT': Date.TODAY,
                                     'FINISH_DT': utils.Date.shift_date(Date.TODAY, years=1),
                                     'IS_SIGNED': Date.TODAY,
                                 })


# Продлеваем договора тестового агентства тест_09_06_аг (999311)
# (договора заведены в проде - PAYSUP-140468)
@pytest.mark.parametrize('contract_id, collateral_template', [
    (41193, CollateralTemplates.PROLONG),
    (184568, CollateralTemplates.PROLONG),
], ids=lambda contract_id, tpl: '{}'.format(contract_id))
def test_restore_collaterals(contract_id, collateral_template):
    collateral_template.create(contract_id)


# Оплата всех кредитных счетов выставленных по договору,
# чтобы кредит не блокировался из-за неоплаченных счетов
def test_unlock_credit():
    contract_id = 176800

    not_paid_credit_invoices = []
    not_paid_credit_invoices.extend(get_all_not_paid_postpay_invoices(contract_id))
    not_paid_credit_invoices.extend(get_all_not_paid_personal_invoices(contract_id))

    if not_paid_credit_invoices:
        with reporter.step(u'По договору {} найдено {} неоплаченных кредитных счетов - сделаем их оплаченными' \
                                   .format(contract_id, len(not_paid_credit_invoices))):
            for invoice_id in not_paid_credit_invoices:
                steps.InvoiceSteps.pay_fair(invoice_id, enqueue_only=True)
    else:
        reporter.attach(u'По договору {} неоплаченных счетов нет'.format(contract_id))


def get_all_not_paid_postpay_invoices(contract_id):
    query = "SELECT id FROM t_invoice WHERE contract_id = :contract_id AND credit = 1 AND receipt_sum_1c < total_sum"
    resp = db.balance().execute(query, {'contract_id': contract_id})
    return [d['id'] for d in resp]


def get_all_not_paid_personal_invoices(contract_id):
    query = "SELECT id FROM t_invoice WHERE contract_id = :contract_id AND postpay = 1 AND receipt_sum_1c < consume_sum"
    resp = db.balance().execute(query, {'contract_id': contract_id})
    return [d['id'] for d in resp]
