# coding: utf-8
__author__ = 'blubimov'

import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import PersonTypes, ContractCommissionType, ContractCreditType, ContractPaymentType, Services, \
    Firms, Currencies, Collateral, ClientCategories
from btestlib.data.defaults import Date
from post_restore_common import (ContractTemplate, CollateralTemplate, get_client_linked_with_login_or_create,
                                 check_and_hide_existing_test_contracts_and_persons, BASE_CONTRACT_PARAMS,
                                 get_client_persons_with_type, restore_person_if_not_exist, make_contracts_unsigned)

"""
Восстановление данных необходимых для интеграционных тестов Direct -> Balance
Заказчики: mariabye, pavryabov, ginger, andy-ilyin

Тикеты: TESTBALANCE-685, TESTBALANCE-1446
"""

CREDIT_LIMIT_DIRECT_DEFAULT = 100000000


class ContractTemplates(object):
    BASE_DIRECT_POSTPAY_PARAMS = utils.merge_dicts([BASE_CONTRACT_PARAMS, {
        'FIRM': Firms.YANDEX_1,
        'CURRENCY': Currencies.RUB,
        'SERVICES': [Services.DIRECT],
        'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
        'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
        'PAYMENT_TERM': 180,
        'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_DIRECT_DEFAULT,
        'DT': Date.TODAY,
        'FINISH_DT': Date.YEAR_AFTER_TODAY,
        'IS_SIGNED': Date.TODAY,
        'DEAL_PASSPORT': Date.TODAY,
    }])

    COMMISS_PREPAY = ContractTemplate(type=ContractCommissionType.COMMISS,
                                      person_type=PersonTypes.UR,
                                      params=BASE_CONTRACT_PARAMS,
                                      add_params={
                                          'FIRM': Firms.YANDEX_1,
                                          'CURRENCY': Currencies.RUB,
                                          'SERVICES': [Services.DIRECT],
                                          'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                          'DT': Date.TODAY,
                                          'FINISH_DT': Date.YEAR_AFTER_TODAY,
                                          'IS_SIGNED': Date.TODAY,
                                      })

    COMMISS_POSTPAY = ContractTemplate(type=ContractCommissionType.COMMISS,
                                       person_type=PersonTypes.UR,
                                       params=BASE_DIRECT_POSTPAY_PARAMS)

    COMMISS_POSTPAY_ZERO_LIMIT = ContractTemplate(type=ContractCommissionType.COMMISS,
                                                  person_type=PersonTypes.UR,
                                                  params=BASE_DIRECT_POSTPAY_PARAMS,
                                                  add_params={
                                                      'CREDIT_LIMIT_SINGLE': 1,  # 0 - нельзя ввести в поле
                                                  })

    COMMISS_POSTPAY_EXPIRED = ContractTemplate(type=ContractCommissionType.COMMISS,
                                               person_type=PersonTypes.UR,
                                               params=BASE_DIRECT_POSTPAY_PARAMS,
                                               add_params={
                                                   'DT': utils.Date.shift_date(Date.TODAY, years=-1),
                                                   'FINISH_DT': Date.TODAY,
                                               })

    COMMISS_POSTPAY_TERMINATED = ContractTemplate(type=ContractCommissionType.COMMISS,
                                                  person_type=PersonTypes.UR,
                                                  params=BASE_DIRECT_POSTPAY_PARAMS,
                                                  add_params={
                                                      'DT': utils.Date.shift_date(Date.TODAY, days=-1),
                                                  },
                                                  collateral_template=CollateralTemplate(
                                                      type=Collateral.TERMINATE,
                                                      params={
                                                          'DT': Date.TODAY,
                                                          'FINISH_DT': Date.TODAY,
                                                          'IS_BOOKED': Date.TODAY,
                                                          'IS_FAXED': Date.TODAY,
                                                          'IS_SIGNED': Date.TODAY,
                                                      }))

    COMMISS_YT_BYN_POSTPAY = ContractTemplate(type=ContractCommissionType.COMMISS,
                                              person_type=PersonTypes.YT,
                                              params=BASE_DIRECT_POSTPAY_PARAMS,
                                              add_params={
                                                  'CURRENCY': Currencies.BYN,
                                              })

    OPT_AGENCY_YT_BYN_POSTPAY = ContractTemplate(type=ContractCommissionType.OPT_AGENCY,
                                                 person_type=PersonTypes.YT,
                                                 params=BASE_DIRECT_POSTPAY_PARAMS,
                                                 add_params={
                                                     'CURRENCY': Currencies.BYN,
                                                 })

    OPT_AGENCY_PREM_POSTPAY = ContractTemplate(type=ContractCommissionType.OPT_AGENCY_PREM,
                                               person_type=PersonTypes.UR,
                                               params=BASE_DIRECT_POSTPAY_PARAMS,
                                               collateral_template=CollateralTemplate(
                                                   type=Collateral.PRIVATE_DEALS,
                                                   params={
                                                       'DT': Date.TODAY,
                                                       'IS_SIGNED': Date.TODAY,
                                                   }))

    PR_AGENCY_PREPAY_NOT_SIGNED = ContractTemplate(type=ContractCommissionType.PR_AGENCY,
                                                   person_type=PersonTypes.UR,
                                                   params=BASE_CONTRACT_PARAMS
                                                   )

    PR_AGENCY_PREPAY_SIGNED = ContractTemplate(type=ContractCommissionType.PR_AGENCY,
                                               person_type=PersonTypes.UR,
                                               params=BASE_CONTRACT_PARAMS,
                                               add_params={
                                                   'IS_SIGNED': Date.TODAY,
                                               })

    PR_AGENCY_POSTPAY = ContractTemplate(type=ContractCommissionType.PR_AGENCY,
                                         person_type=PersonTypes.UR,
                                         params=BASE_DIRECT_POSTPAY_PARAMS
                                         )

    PR_AGENCY_POSTPAY_ZERO_LIMIT = ContractTemplate(type=ContractCommissionType.PR_AGENCY,
                                                    person_type=PersonTypes.UR,
                                                    params=BASE_DIRECT_POSTPAY_PARAMS,
                                                    add_params={
                                                        'CREDIT_LIMIT_SINGLE': 1,  # 0 - нельзя ввести в поле
                                                    }
                                                    )

    PR_AGENCY_POSTPAY_EXPIRED = ContractTemplate(type=ContractCommissionType.PR_AGENCY,
                                         person_type=PersonTypes.UR,
                                         params=BASE_DIRECT_POSTPAY_PARAMS,
                                         add_params={
                                             'DT': utils.Date.shift_date(Date.TODAY, years=-1),
                                             'FINISH_DT': Date.TODAY,
                                         }
                                         )

    USA_OPT_AGENCY_POSTPAY = ContractTemplate(type=ContractCommissionType.USA_OPT_AGENCY,
                                              person_type=PersonTypes.USU,
                                              params=BASE_DIRECT_POSTPAY_PARAMS,
                                              add_params={
                                                  'FIRM': Firms.YANDEX_INC_4,
                                                  'CURRENCY': Currencies.USD,
                                              })

    SW_OPT_AGENCY_POSTPAY = ContractTemplate(type=ContractCommissionType.SW_OPT_AGENCY,
                                             person_type=PersonTypes.SW_YT,
                                             params=BASE_DIRECT_POSTPAY_PARAMS,
                                             add_params={
                                                 'FIRM': Firms.EUROPE_AG_7,
                                                 'CURRENCY': Currencies.EUR,
                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                             })

    REKLAMA_BYN_OPT_AGENCY_POSTPAY = ContractTemplate(type=ContractCommissionType.BEL_OPT_AGENCY,
                                                      person_type=PersonTypes.BYU,
                                                      params=BASE_DIRECT_POSTPAY_PARAMS,
                                                      add_params={
                                                          'FIRM': Firms.REKLAMA_BEL_27,
                                                          'CURRENCY': Currencies.BYN,
                                                      })


LOGIN_TO_CONTRACTS_MAP = {
    'at-agency-tmoney': [ContractTemplates.COMMISS_POSTPAY],
    'at-agency-invoice1': [ContractTemplates.COMMISS_POSTPAY],
    'at-agency-invoice2': [ContractTemplates.COMMISS_POSTPAY],
    'at-agency-deposit1': [ContractTemplates.COMMISS_POSTPAY],
    'at-agency-deposit2': [ContractTemplates.COMMISS_POSTPAY],
    'agency-enable-sa': [ContractTemplates.COMMISS_POSTPAY],
    'test-agency-rub': [ContractTemplates.COMMISS_POSTPAY,
                        # по просьбе kaerber@yandex-team.ru
                        ContractTemplates.PR_AGENCY_POSTPAY,
                        ContractTemplates.PR_AGENCY_PREPAY_NOT_SIGNED,
                        ContractTemplates.PR_AGENCY_PREPAY_SIGNED,  # по просьбе ajkon@yandex-team.ru
                        ContractTemplates.PR_AGENCY_POSTPAY_ZERO_LIMIT,
                        ContractTemplates.PR_AGENCY_POSTPAY_EXPIRED
                        ],
    'at-agency-shard': [ContractTemplates.COMMISS_POSTPAY],
    'api-ag-shard2': [ContractTemplates.COMMISS_POSTPAY],
    'at-direct-ag-another': [ContractTemplates.COMMISS_POSTPAY,
                             ContractTemplates.COMMISS_POSTPAY],
    'test-agency-usd': [ContractTemplates.USA_OPT_AGENCY_POSTPAY],
    'test-agency-eur': [ContractTemplates.SW_OPT_AGENCY_POSTPAY],
    'at-direct-api-byn-agency1': [ContractTemplates.COMMISS_YT_BYN_POSTPAY],
    # 'api-direct-byn-agency3': [ContractTemplates.OPT_AGENCY_YT_BYN_POSTPAY],  # По просьбе kaerber@yandex-team.ru
    'api-direct-byn-agency3': [ContractTemplates.REKLAMA_BYN_OPT_AGENCY_POSTPAY],
    'at-direct-ag-full': [ContractTemplates.COMMISS_PREPAY,
                          ContractTemplates.COMMISS_POSTPAY,
                          ContractTemplates.COMMISS_POSTPAY,
                          ContractTemplates.COMMISS_POSTPAY,
                          ContractTemplates.COMMISS_POSTPAY,
                          ContractTemplates.COMMISS_POSTPAY,
                          ContractTemplates.COMMISS_POSTPAY,
                          ContractTemplates.COMMISS_POSTPAY_ZERO_LIMIT,
                          ContractTemplates.COMMISS_POSTPAY_EXPIRED,
                          ContractTemplates.COMMISS_POSTPAY_TERMINATED,
                          # по просьбе kaerber@yandex-team.ru
                          ContractTemplates.PR_AGENCY_POSTPAY],
    'test-agency-deals': [ContractTemplates.OPT_AGENCY_PREM_POSTPAY],
}


@pytest.mark.parametrize('login, contract_templates', LOGIN_TO_CONTRACTS_MAP.items(),
                         ids=lambda login, tpl: '{}'.format(login))
def test_restore_contracts(login, contract_templates):
    client_id = get_client_linked_with_login_or_create(login)

    check_and_hide_existing_test_contracts_and_persons(client_id)

    person_id = steps.PersonSteps.create(client_id, contract_templates[0].person_type.code)

    prev_id = 0
    new_id = 0
    for contract_tpl in contract_templates:
        # Гарантируем порядок договор по ID. Директ на него завязан.
        # Порядок может нарушаться из-за кэширования сиквенсов: BALANCEDUTY-350
        while True:
            new_id = int(contract_tpl.create(client_id, person_id))
            if new_id <= prev_id:
                # Новый договор получил закешированый ID, удаляем его, и пытаемся создать заново.
                make_contracts_unsigned([new_id])
            else:
                break

        prev_id = new_id


@pytest.mark.parametrize('login, person_type, person_params', [
    ('at-direct-transfer', PersonTypes.PH, None),
    ('at-direct-transfer-rub', PersonTypes.PH, None),
    ('at-direct-transfer-kzt', PersonTypes.KZP, None),
    ('at-direct-transfer-usd', PersonTypes.USP, None),
    ('at-agency-transfer', PersonTypes.UR, None),
    ('at-direct-transfer-mngr', PersonTypes.UR, None),
    ('transfer-mngr-cl-1', PersonTypes.PH, None),
    ('at-client-disallow-transfer', PersonTypes.PH, None),
    ('at-light-firstHelp-client', PersonTypes.PH, None),
    ('at-client-curr-rus', PersonTypes.PH, None),
    ('at-serviced-shard', PersonTypes.PH, None),
    ('at-direct-api-test', PersonTypes.PH, None),
    ('api-subclient', PersonTypes.PH, None),
    ('at-direct-agency', PersonTypes.UR, None),
    ('at-direct-ag-client', PersonTypes.PH, None),
    ('at-direct-disc-c', PersonTypes.PH, None),
    ('at-direct-backend-newcl-c1', PersonTypes.PH, None),
    ('at-direct-src-camp-c1', PersonTypes.PH, None),
    ('at-api-self-overdraft', PersonTypes.PH, None),
    ('at-rub-discount-client', PersonTypes.PH, None),
    ('account-serv-shard2', PersonTypes.PH, None),
    ('at-ag-campaign-pay', PersonTypes.UR, None),
    ('at-free-dollar-account', PersonTypes.USP, None),
    ('at-direct-mngr-full', PersonTypes.UR, None),
    ('at-direct-api-byn-client2', PersonTypes.BY_YTPH, None),
    ('api-serv-eur-alt', PersonTypes.SW_YT, None),
    ('api-serv-chf', PersonTypes.SW_UR, None),
    ('api-serv-kzt', PersonTypes.KZP, None),
    ('api-serv-eur', PersonTypes.SW_YT, None),
    ('api-serv-usd', PersonTypes.USP, None),
    ('maria-baibik', PersonTypes.PH, None),
    ('api-direct-byn-client3', PersonTypes.BYP, None),
    ('at-direct-autooverdraft1', {PersonTypes.PH: 2}, None),

    # По просьбе kaerber@yandex-team.ru
    ('at-deposit-by-card-2', PersonTypes.PH, None),
    ('at-campaign-mngr-serviced', PersonTypes.PH, None),
    ('at-direct-adv-std', PersonTypes.PH, None),
    ('at-direct-back-finance', PersonTypes.PH, None),
    ('at-direct-without-wallet', PersonTypes.PH, None),
    ('at-transfer-without-wallet', PersonTypes.PH, None),
    # Тут что-то непонятное - не пересоздаём
    # ('api-serv-uah', PersonTypes.PH),

    ('at-client-dot1', PersonTypes.PH, None),
    ('at-direct-back-nowallet7', PersonTypes.PH, None),
    ('at-direct-src-camp-c3', PersonTypes.PH, None),
    # BALANCEDUTY-351
    ('dna-client-autooverdraft', PersonTypes.PH, {u'fname': u'Физик', u'lname': u'Плательщиков',
                                                  u'mname': u'Физикович'}),
])
def test_restore_persons(login, person_type, person_params):
    # если у логина нет привязанного клиента, по умолчанию будем создавать прямого клиента
    client_id = get_client_linked_with_login_or_create(login, client_category=ClientCategories.CLIENT)
    if isinstance(person_type, dict):
        for type_, count in person_type.iteritems():
            persons = get_client_persons_with_type(client_id, type_)
            persons_count_to_create = count - len(persons)
            if persons_count_to_create > 0:
                for x in range(persons_count_to_create):
                    steps.PersonSteps.create(client_id, type_.code, params=person_params)
    else:
        restore_person_if_not_exist(client_id, person_type, person_params)


@pytest.mark.parametrize('login, overdraft_limit', [
    ('maria-baibik', 100000),
])
def test_restore_overdraft(login, overdraft_limit):
    client_id = get_client_linked_with_login_or_create(login)
    steps.OverdraftSteps.set_force_overdraft(client_id, Services.DIRECT.id, overdraft_limit, Firms.YANDEX_1.id)
