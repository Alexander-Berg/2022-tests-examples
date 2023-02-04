# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest
from hamcrest import is_not, equal_to

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Firms

YANDEX = Firms.YANDEX_1.id
UA = Firms.YANDEX_UA_2.id
SW = Firms.EUROPE_AG_7.id
VERTIKAL = Firms.VERTICAL_12.id
TAXI = Firms.TAXI_13.id
MARKET = Firms.MARKET_111.id


DT = utils.Date.date_to_iso_format(datetime.datetime.now())
person_mapping = {YANDEX: 'ph',
                  UA: 'ua',
                  SW: 'sw_ph',
                  VERTIKAL: 'ph',
                  TAXI: 'ph',
                  MARKET: 'ph'}



@pytest.mark.slow
@pytest.mark.priority('low')
@reporter.feature(Features.NOTIFICATION, Features.CONTRACT)
@pytest.mark.parametrize('p', [

    (YANDEX, 'commiss', [7], 11),  ## директ (7) (комиссионный)  - 11 писем (факс)
    (YANDEX, 'opt_agency', [7], 11),  ## директ (7) (оптовый агентский)  - 11 писем факс
    (YANDEX, 'opt_agency_prem', [7], 11),  ## директ (7) (оптовый агентский премия)  - 11 писем факс
    (YANDEX, 'opt_client', [7], 11),  ## директ (7) (оптовый клиентский)  - 11 писем факс
    (YANDEX, 'pr_agency', [7], 11),  ## директ (7) (оптовый клиентский)  - 11 писем факс
    (YANDEX, 'no_agency', [7], 11),  ## директ   11# (YANDEX, 'no_agency', [7], 11),  ## директ   11
    (YANDEX, 'no_agency', [67, 70, 77], 11),  ## баян, медиаселлинг, баннеры   11
    (YANDEX, 'no_agency', [37], 11),  ## справочник (37) (не агентский)  - 11 писем

    (YANDEX, 'no_agency', [102], 17),  ## adfox   17
    (YANDEX, 'no_agency', [113], 17),  ## банки (113)(не агентский) - 17 писем  факс
    (YANDEX, 'no_agency', [118], 17),  ## билеты   17
    (YANDEX, 'no_agency', [126], 17),  ## билеты на мероприятия  17

    pytest.mark.skip(reason=u'отключили Украину')((UA, 'ua_opt_agency_prem', [7], 0)),
    (SW, 'sw_opt_agency', [7, 70, 35], 6),
    (SW, 'sw_opt_client', [7, 70, 35], 6),

    (VERTIKAL, 'commiss', [70], 11),  ## медийка (70) (комиссионный)  - 11 писем (факс)
    (VERTIKAL, 'opt_agency', [7], 11),  ## директ (7) (оптовый агентский)  - 11 писем факс
    (VERTIKAL, 'opt_agency_prem', [70], 11),  ## медийка (70) (оптовый агентский премия)  - 11 писем факс
    (VERTIKAL, 'no_agency', [81,131], 11),  ## недвижимость   11
    (VERTIKAL, 'no_agency', [98], 17),  ## туры (98)  - 11 писем факс
    pytest.mark.skip(reason='https://st.yandex-team.ru/BALANCE-24162')((VERTIKAL, 'auto_otp_agency_prem', [99], 17)),
    ## авто.ру- 17 писем  факс

    (TAXI, 'no_agency', [111], 11),  ## такси (111)(не агентский)
    (TAXI, 'no_agency', [50], 17),  ## рит (50) (не агентский)
    (TAXI, 'no_agency', [111, 124, 128], 11),  ## такси    11

    (MARKET, 'commiss', [11], 11),  ## маркет (11) (комиссионный)  - 11 писем (факс)
    (MARKET, 'opt_agency', [11], 11),  ## маркет (11) (оптовый агентский)  - 11 писем факс
    (MARKET, 'opt_agency_prem', [11], 11),  ## маркет (11) (оптовый агентский премия)  - 11 писем факс
    (MARKET, 'pr_agency', [1], 11),  ## маркет (11)  (прямой агентскй)  - 11 писем факс
    (MARKET, 'no_agency', [11], 11),  ## маркет (11)
    (MARKET, 'no_agency', [101, 120], 17),  ## доставка и платеди по ней   17

    ###  договоры ниже неактуальны
    ## (YANDEX, 'no_agency', [131], 11),  ## билеты на мероприятия(схема2)  11  - не подключено
    ## (VERTIKAL, 'opt_client', [7], 11),  ## директ (7) (оптовый клиентский)  - 11 писем факс
    ## (VERTIKAL, 'no_agency', [7], 11),  ## директ   11
    ## (VERTIKAL, 'no_agency', [37], 11),  ## справочник (37) (не агентский)  - 11 писем факс
    ## (VERTIKAL, 'no_agency', [67, 70, 77], 11),  ## баян, медиаселлинг, баннеры   11
    ## (VERTIKAL, 'no_agency', [101, 120], 17),  ## доставка и платеди по ней   17
    ## (VERTIKAL, 'no_agency', [102], 17),  ## adfox   17
    ## (VERTIKAL, 'no_agency', [113], 17),  ## банки (113)(не агентский) - 17 писем  факс
    ## (VERTIKAL, 'no_agency', [118], 17),  ## билеты   17
    ## (VERTIKAL, 'no_agency', [126], 17),  ## билеты на мероприятия  17
    ## (VERTIKAL, 'no_agency', [90], 11),  ## работа  11  - не подключено

    ## (TAXI, 'commiss', [7], 11),  ## директ (7) (комиссионный)  - 11 писем (факс)
    ## (TAXI, 'opt_agency', [7], 11),  ## директ (7) (оптовый агентский)  - 11 писем факс
    ## (TAXI, 'opt_agency_prem', [7], 11),  ## директ (7) (оптовый агентский премия)  - 11 писем факс
    ## (TAXI, 'opt_client', [7], 11),  ## директ (7) (оптовый клиентский)  - 11 писем факс
    ## (TAXI, 'no_agency', [7], 11),  ## директ   11
    ## (TAXI, 'no_agency', [37], 11),  ## справочник (37) (не агентский)  - 11 писем факс
    ## (TAXI, 'no_agency', [67, 70, 77], 11),  ## баян, медиаселлинг, баннеры   11
    ## (TAXI, 'no_agency', [102], 17),  ## adfox   17
    ## (TAXI, 'no_agency', [113], 17),  ## банки (113)(не агентский) - 17 писем  факс
    ## (TAXI, 'no_agency', [118], 17),  ## билеты   17
    ## (TAXI, 'no_agency', [126], 17),  ## билеты на мероприятия  17

    ## (MARKET, 'opt_client', [7], 11),  ## директ (7) (оптовый клиентский)  - 11 писем факс
    ## (MARKET, 'no_agency', [37], 11),  ## справочник (37) (не агентский)  - 11 писем факс
    ## (MARKET, 'no_agency', [67, 70, 77], 11),  ## баян, медиаселлинг, баннеры   11
    ## (MARKET, 'no_agency', [102], 17),  ## adfox   17
    ## (MARKET, 'no_agency', [113], 17),  ## банки (113)(не агентский) - 17 писем  факс
    ## (MARKET, 'no_agency', [118], 17),  ## билеты   17
    ## (MARKET, 'no_agency', [119], 11),  ## маркетплейс    11
    ## (MARKET, 'no_agency', [126], 17),  ## билеты на мероприятия  17
],
                         ids=lambda x: 'firm_{}_{}_services_{}'.format(x[0], x[1], x[2])
                         )
def contract_notify_services(p):
    firm_id, contract_type, services, mail_count = p

    client_id = steps.ClientSteps.create()
    person_category = person_mapping[firm_id]
    person_id = steps.PersonSteps.create(client_id, person_category, {'email': 'test-balance-notify@yandex-team.ru'})

    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type,
                                                             {'CLIENT_ID': client_id,
                                                              'PERSON_ID': person_id,
                                                              'IS_FAXED': DT,
                                                              'DT': DT,
                                                              'FIRM': firm_id,
                                                              'SERVICES': services,
                                                              'PAYMENT_TYPE': 2})

    steps.ContractSteps.contract_notify_flipping_dates(contract_id, 'is_faxed', _type=0)
    dt, cnt = steps.ContractSteps.contract_notify_check(contract_id)

    utils.check_that(dt[0]['dt'], is_not(None), step=u'Проверяем дату приостановки договора')
    utils.check_that(cnt[0]['cnt'], equal_to(mail_count), step=u'Проверяем количество писем')
