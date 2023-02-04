# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import datetime

import pytest
from dateutil.relativedelta import relativedelta

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import AuditFeatures, Features
from btestlib import utils
from btestlib.constants import Firms, Services, ContractCommissionType, Regions, CountryRegion, NdsNew
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT

#для этих фирм проверяем приостановку
YANDEX_FIRM = Firms.YANDEX_1.id
TAXI_FIRM = Firms.TAXI_13.id
MARKET_FIRM = Firms.MARKET_111.id
VERTICAL_FIRM = Firms.VERTICAL_12.id

#дата брони/подписанности по факсу на три месяца назад
SIGNED_DT = utils.Date.shift_date(datetime.datetime.now(), months=-3, days=-1)
SIGNED_DT_FORMATTED = utils.Date.date_to_iso_format(SIGNED_DT)

# когда сделаем возможность создавать любой договор через CreateCommonContract,
# добавить контексты и избавиться от этих параметров:
taxi_additional_params = {'SERVICES': TAXI_RU_CONTEXT.contract_services,
                          'PERSONAL_ACCOUNT': 1, 'FIRM': Firms.TAXI_13.id, 'PAYMENT_TYPE': 2,
                          'DT': utils.Date.nullify_time_of_date(datetime.datetime.now()),
                          'COUNTRY': Regions.RU.id, 'REGION': CountryRegion.RUS,
                          'NDS_FOR_RECEIPT': NdsNew.DEFAULT.nds_id}
yandex_additional_params = {'SERVICES': [Services.DIRECT.id], 'FIRM': Firms.YANDEX_1.id, 'PAYMENT_TYPE': 2,
                            'DT': utils.Date.nullify_time_of_date(datetime.datetime.now()),
                            'COMMISSION': ContractCommissionType.COMMISS.id,
                            'FINISH_DT': datetime.datetime.now() + relativedelta(years=1),
                            'COMMISSION_TYPE': 54}
market_additional_params = {'SERVICES': [Services.MARKET.id], 'FIRM': Firms.MARKET_111.id, 'PAYMENT_TYPE': 2,
                            'DT': utils.Date.nullify_time_of_date(datetime.datetime.now()),}
vertical_additional_params = {'SERVICES': [Services.REALTY_COMM.id], 'FIRM': Firms.VERTICAL_12.id, 'PAYMENT_TYPE': 2,
                            'DT': utils.Date.nullify_time_of_date(datetime.datetime.now()),}

# набор доп полей в создании договора в зависимости от желаемой подписанности договора
sign_contract_mapper = {'not_signed': {},
                        'is_booked': {'IS_BOOKED': 1, 'IS_FAXED': SIGNED_DT_FORMATTED},
                        'is_faxed': {'IS_FAXED': SIGNED_DT_FORMATTED},
                        'signed': {'IS_SIGNED': SIGNED_DT_FORMATTED}}

# маппинг фирмы и доп полей при создании договора
contract_params_mapper = {TAXI_FIRM: taxi_additional_params,
                          YANDEX_FIRM: yandex_additional_params,
                          MARKET_FIRM: market_additional_params,
                          VERTICAL_FIRM: vertical_additional_params}

# тест проверяет, что
# договор приостанавливается через 3 месяца после даты брони подписи, через 3 месяца после даты подписан по факсу,
# договор не приостанавливается, если договор не подписан или подписан
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_1))
@pytest.mark.parametrize('contract_state, expected_suspended_value, firm', [
    ('is_faxed', 1, TAXI_FIRM),
    ('is_booked', 1, TAXI_FIRM),
    ('not_signed', 0, TAXI_FIRM),
    ('signed', 0, TAXI_FIRM),
    ('is_faxed', 1, YANDEX_FIRM),
    ('is_booked', 1, YANDEX_FIRM),
    ('not_signed', 0, YANDEX_FIRM),
    ('signed', 0, YANDEX_FIRM),
    ('is_faxed', 1, MARKET_FIRM),
    ('is_booked', 1, MARKET_FIRM),
    ('not_signed', 0, MARKET_FIRM),
    ('signed', 0, MARKET_FIRM),
    ('is_faxed', 1, VERTICAL_FIRM),
    ('is_booked', 1, VERTICAL_FIRM),
    ('not_signed', 0, VERTICAL_FIRM),
    ('signed', 0, VERTICAL_FIRM),
], ids=lambda state, exp, firm: state + ' firm_id = ' + str(firm))
def test_contract_suspension(contract_state, expected_suspended_value, firm):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    params = contract_params_mapper[firm].copy()
    params.update({'CLIENT_ID': client_id, 'PERSON_ID': person_id})
    params.update(sign_contract_mapper[contract_state])

    contract_id, external_contract_id = steps.ContractSteps.create_contract('general_default', params=params,
                                                                            remove_params=['IS_SIGNED'] if contract_state <> 'signed' else None)

    # для брони подписи дату через апи не прокинуть :( поэтому апдейтим в базе
    if contract_state == 'is_booked':
        query = "UPDATE (SELECT * FROM T_CONTRACT_COLLATERAL c JOIN t_contract_attributes a " \
                "ON c.attribute_batch_id = a.attribute_batch_id WHERE c.contract2_id =:contract_id " \
                "AND code = 'IS_BOOKED_DT' AND num IS NULL) SET value_dt  =  :update_dt"
        db.balance().execute(query, {'contract_id': contract_id, 'update_dt': SIGNED_DT})

    # запускаем проверку договора, здесь же приостанавливается, если попадает под условия
    steps.CommonSteps.export('CONTRACT_NOTIFY', 'Contract', contract_id)
    # дергаем GetClientContracts, ищем признак suspended (приоставнолен) и проверяем его значение,
    # если договор вообще никак не подписан, то ручка (по умолчанию) не вернет признак, его обрабатываем отдельно
    with reporter.step(u'Получаем признак приостановленности договора'):
        contracts = api.medium().GetClientContracts({'ClientID': client_id})
        if contracts:
            suspended = contracts[0]['IS_SUSPENDED']
        else:
            suspended = 0
    utils.check_that(str(suspended), str(expected_suspended_value),
                     step=u'Проверяем, что был возвращен признак приостановленности договора.')
