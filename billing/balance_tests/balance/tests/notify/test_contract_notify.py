# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
from decimal import Decimal

import pytest
from hamcrest import is_not, equal_to

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import Firms

pytestmark = [reporter.feature(Features.NOTIFICATION, Features.CONTRACT),
              pytest.mark.tickets('BALANCE-16973')]

# cluster_tools.contract_notify2.get_notifications_general

DT = utils.Date.date_to_iso_format(datetime.datetime.now())
FINISH_DT = utils.Date.date_to_iso_format(datetime.datetime.now() + datetime.timedelta(days=5))

YANDEX = Firms.YANDEX_1.id
UA = Firms.YANDEX_UA_2.id
SW = Firms.EUROPE_AG_7.id
VERTIKAL = Firms.VERTICAL_12.id
TAXI = Firms.TAXI_13.id
MARKET = Firms.MARKET_111.id
OFD = Firms.OFD_18.id
KZT = Firms.KZ_25.id
BEL = Firms.REKLAMA_BEL_27.id
USA = Firms.YANDEX_INC_4.id
TR = Firms.YANDEX_TURKEY_8.id
HEALTH = Firms.HEALTH_114.id
MEDIA = Firms.MEDIASERVICES_121.id
KZ_TAXI = Firms.TAXI_CORP_KZT_31.id

person_mapping = {YANDEX: 'ur',
                  UA: 'ua',
                  SW: 'sw_ph',
                  VERTIKAL: 'ph',
                  TAXI: 'ph',
                  MARKET: 'ph',
                  OFD: 'ur',
                  KZT: 'kzu',
                  BEL: 'byu',
                  USA: 'usu',
                  TR: 'tru',
                  HEALTH: 'ur',
                  MEDIA: 'ur',
                  KZ_TAXI: 'kzu'}

## по дефолту договор/дс не подписан
sign_contract_mapper = {'not_signed': {},
                        'is_booked': {'IS_FAXED': DT, 'IS_BOOKED': 1},
                        'is_faxed': {'IS_FAXED': DT},
                        'signed': {'IS_SIGNED': DT}}

sign_coll_mapper = {'not_signed': {},
                    'is_booked': {'IS_FAXED': DT, 'IS_BOOKED': DT},
                    'is_faxed': {'IS_FAXED': DT},
                    'signed': {'IS_SIGNED': DT}}


def check_suspended(contract_id, dt, suspended_type):
    if suspended_type == 'suspended':
        utils.check_that(dt[0]['dt'], is_not(None), step=u'Проверяем дату приостановки договора')
    if suspended_type == 'nothing':
        utils.check_that(dt[0]['dt'], equal_to(None), step=u'Проверяем, что в договоре ничего не изменилось')
    if suspended_type == 'cancelled':
        cancell_dt = db.balance().execute(
            'SELECT IS_CANCELLED AS dt FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID = :contract_id',
            {'contract_id': contract_id})
        utils.check_that(cancell_dt[0]['dt'], is_not(None), step=u'Проверяем, что договор аннулирован')


@pytest.mark.priority('low')
@pytest.mark.parametrize(
    " firm_id, contract_type, person_vip, contract_sign, services, expect_data, is_collateral, collateral_sign, collateral_num, contract_params", [

        (YANDEX, 'commiss', 0,  'not_signed', [7], {'mail_count': 6, 'suspended_type': 'cancelled'}, 0, None, None, {}),             ## + договор не подписан
        (YANDEX, 'pr_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 0, None, None, {}),  ## + договор факс
        (YANDEX, 'opt_agency_prem', 0, 'signed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),  ## + договор подписан, дс нет
        (YANDEX, 'no_agency', 0, 'signed', [37], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),         ## + договор подписан, дс факс   справочник
        (YANDEX, 'no_agency', 0, 'is_booked', [67, 70, 77], {'mail_count': 6, 'suspended_type': 'suspended'}, 1,
             'not_signed', 80, {}),  ## + договор бронь, дс нет   баян, медиаселлинг, баннеры
        (YANDEX, 'no_agency', 0, 'is_booked', [102], {'mail_count': 12, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),        ## + договор бронь, дс бронь   adfox
        (YANDEX, 'opt_agency', 0, 'is_booked', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),          ## + договор бронь, дс факс
        (YANDEX, 'no_agency', 0, 'is_faxed', [129], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),        ## + договор факс, дс нет   кабинет разработчика
        (YANDEX, 'no_agency', 0, 'is_faxed', [141], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {'PAYMENT_TYPE': 3}),   ## + договор факс, дс бронь  Адаптер Девелопер
        (YANDEX, 'no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),            ## + договор факс, дс факс
        (YANDEX, 'no_agency', 0, 'is_faxed', [151], {'mail_count': 1, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),           ## + договор факс, дс расторж бронь  Автобусы
        (YANDEX, 'no_agency', 0, 'is_faxed', [142], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {'PAYMENT_TYPE': 3}),       ## + договор факс, дс бронь Адаптер Ретейлер
        (YANDEX, 'no_agency', 0, 'is_faxed', [171], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),   ## + договор факс, дс бронь Расписания ЖД УФС
        (YANDEX, 'no_agency', 0, 'signed', [202], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),  ## + договор подписан, дс нет Яндекс.Коннект
        (YANDEX, 'no_agency', 0, 'is_booked', [201], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),  ## + договор бронь, дс факс Яндекс.Медиана

        (MEDIA, 'no_agency', 0, 'is_booked', [118], {'mail_count': 24, 'suspended_type': 'suspended'}, 0, None, None, {}),  ## + договор бронь   билеты
        (MEDIA, 'no_agency', 0, 'signed', [131], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),  ## + договор подписан, дс бронь  Билеты на мероприятия (схема 2)
        (MEDIA, 'no_agency', 0, 'signed', [126], {'mail_count': 29, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),  ## + договор подписан, дс бронь  Билеты на мероприятия


        # Не агентский (только плательщики со статусом vip)
        (YANDEX, 'no_agency', 1, 'is_booked', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),          ## + договор бронь, дс факс
        # Договоры, где только разовые продажи(35), считаются невалидными и отправляется письмо на auto-letter@yandex-team.ru
        (YANDEX, 'no_agency', 1, 'is_booked', [35], {'mail_count': 1, 'suspended_type': 'nothing'}, 1, 'is_faxed', 80, {}),          ## + договор бронь, дс факс
        (YANDEX, 'no_agency', 0, 'is_booked', [35], {'mail_count': 1, 'suspended_type': 'nothing'}, 1, 'is_faxed', 80, {}),          ## + договор бронь, дс факс
        (YANDEX, 'no_agency', 1, 'is_booked', [35,7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),          ## + договор бронь, дс факс
        (YANDEX, 'no_agency', 0, 'is_booked', [35,7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),          ## + договор бронь, дс факс

        # Не агентский (с любым типом плательщика) например, для здоровья
        pytest.mark.skip((HEALTH, 'no_agency', 1, 'is_booked', [153], {'mail_count': 23, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}), reason='https://st.yandex-team.ru/BALANCE-27026'),  ## + договор бронь, дс факс
        pytest.mark.skip((HEALTH, 'no_agency', 0, 'signed', [153], {'mail_count': 23, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}), reason='https://st.yandex-team.ru/BALANCE-27026'),  ## + договор подписан, дс факс   Здоровье
        pytest.mark.skip((HEALTH, 'no_agency', 0, 'is_faxed', [170, 270], {'mail_count': 23, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {'MEDICINE_PAY_COMMISSION': 100, 'MEDICINE_PAY_COMMISSION2': 100}), reason='https://st.yandex-team.ru/BALANCE-27026'),        ## + договор факс, дс расторж бронь   Телемедицина

        (VERTIKAL, 'commiss', 0, 'not_signed', [70], {'mail_count': 6, 'suspended_type': 'cancelled'}, 0, None, None, {}),          ## договор не подписан
        (VERTIKAL, 'opt_agency', 0, 'is_booked', [7], {'mail_count': 6, 'suspended_type': 'suspended'}, 0, None,
             None, {}),  ## договор бронь
        (VERTIKAL, 'opt_agency_prem', 0, 'is_faxed', [70], {'mail_count': 11, 'suspended_type': 'suspended'}, 0,
             None, None, {}),  ## договор факс
        (VERTIKAL, 'no_agency', 0, 'signed', [81, 131], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),  ## договор подписан, дс нет
        pytest.mark.skip((VERTIKAL, 'no_agency', 0, 'signed', [98], {'mail_count': 17, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}), reason = 'https://st.yandex-team.ru/BALANCE-27026#1517494683000' ),        ## договор подписан, дс бронь
        (VERTIKAL, 'commiss', 0, 'signed', [70], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),           ## договор подписан, дс факс
        (VERTIKAL, 'opt_agency', 0, 'is_booked', [7], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),     ## договор бронь, дс нет
        (VERTIKAL, 'opt_agency_prem', 0, 'is_booked', [70], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),  ## договор бронь, дс бронь
        (VERTIKAL, 'no_agency', 0, 'is_booked', [81, 131], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed',  80, {}),  ## договор бронь, дс факс
        pytest.mark.skip((VERTIKAL, 'no_agency', 0, 'is_faxed', [98], {'mail_count': 17, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}), reason = 'https://st.yandex-team.ru/BALANCE-27026#1517494683000' ),       ## договор факс, дс нет
        (VERTIKAL, 'commiss', 0, 'is_faxed', [70], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),          ## договор факс, дс бронь
        (VERTIKAL, 'opt_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),         ## договор факс, дс факс
        (VERTIKAL, 'opt_agency_prem', 0, 'is_faxed', [70], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),    ## договор факс, дс расторж бронь
        pytest.mark.skip((VERTIKAL, 'no_agency', 0, 'is_faxed', [98], {'mail_count': 17, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}), reason = 'https://st.yandex-team.ru/BALANCE-27026#1517494683000' ),          ## + договор факс, дс расторж бронь

        (TAXI, 'no_agency', 0, 'not_signed', [135, 650], {'mail_count': 12, 'suspended_type': 'cancelled'}, 0, None, None, {}),           ## договор не подписан
        pytest.param(TAXI, 'no_agency', 0, 'is_booked', [50], {'mail_count': 1, 'suspended_type': 'nothing'}, 0, None, None, {},
                     id='Taxi Booked'),  ## договор бронь,
        pytest.param(TAXI, 'no_agency', 0, 'is_faxed', [124, 128, 125, 605], {'mail_count': 11, 'suspended_type': 'suspended'},
             0, None, None, {}, id='Taxi Faxed'),  ## договор факс
        (TAXI, 'no_agency', 0, 'signed', [111], {'mail_count': 17, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),        ## договор подписан, дс нет
        (TAXI, 'no_agency', 0, 'signed', [203], {'mail_count': 1, 'suspended_type': 'nothing'}, 1, 'is_booked', 80, {}),          ## договор подписан, дс бронь   Яндекс.Такси Обязательства брендирования
        (TAXI, 'no_agency', 0, 'signed', [135, 650], {'mail_count': 17, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),  ## договор подписан, дс факс
        (TAXI, 'no_agency', 0, 'is_booked', [50], {'mail_count': 1, 'suspended_type': 'nothing'}, 1, 'not_signed', 80, {}),        ## договор бронь, дс нет
        (TAXI, 'no_agency', 0, 'is_booked', [124, 128, 125,605], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),         ## договор бронь, дс бронь
        (TAXI, 'no_agency', 0, 'is_booked', [111], {'mail_count': 17, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),  ## договор бронь, дс факс
        (TAXI, 'no_agency', 0, 'is_faxed', [203], {'mail_count': 1, 'suspended_type': 'nothing'}, 1, 'not_signed', 80, {}),       ## договор факс, дс нет
        (TAXI, 'no_agency', 0, 'is_faxed', [135, 650], {'mail_count': 17, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),         ## договор факс, дс бронь
        (TAXI, 'no_agency', 0, 'is_faxed', [50], {'mail_count': 1, 'suspended_type': 'nothing'}, 1, 'is_faxed', 80, {}),  ## договор факс, дс факс
        (TAXI, 'no_agency', 0, 'is_faxed', [124, 128, 125,605], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),       ## договор факс, дс расторж бронь
        (TAXI, 'no_agency', 0, 'is_faxed', [203], {'mail_count': 1, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),        ## договор факс, дс расторж бронь

        (MARKET, 'commiss', 0, 'not_signed', [11], {'mail_count': 6, 'suspended_type': 'cancelled'}, 0, None, None, {}),        ## договор не подписан
        (
            MARKET, 'opt_agency', 0, 'is_booked', [11], {'mail_count': 6, 'suspended_type': 'suspended'}, 0, None, None,
            {}),  ## договор бронь
        (MARKET, 'opt_agency_prem', 0, 'is_faxed', [11], {'mail_count': 11, 'suspended_type': 'suspended'}, 0, None,
             None, {}),  ## договор факс
        (MARKET, 'pr_agency', 0, 'signed', [11], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),   ## договор подписан, дс нет
        (MARKET, 'no_agency', 0, 'signed', [11], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),    ## договор подписан, дс бронь
        (MARKET, 'no_agency', 0, 'signed', [101, 120], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {'MINIMAL_PAYMENT_COMMISSION': Decimal('0')}),  ## + договор подписан, дс факс
        (MARKET, 'commiss', 0, 'is_booked', [11], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),      ## договор бронь, дс нет
        (MARKET, 'opt_agency', 0, 'is_booked', [11], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),    ## договор бронь, дс бронь
        (MARKET, 'opt_agency_prem', 0, 'is_booked', [11], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),  ## договор бронь, дс факс
        (MARKET, 'pr_agency', 0, 'is_faxed', [11], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),        ## договор факс, дс нет
        (MARKET, 'no_agency', 0, 'is_faxed', [11], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),         ## договор факс, дс бронь
        (MARKET, 'no_agency', 0, 'is_faxed', [101, 120], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {'MINIMAL_PAYMENT_COMMISSION': Decimal('0')}),    ## + договор факс, дс факс
        (MARKET, 'commiss', 0, 'is_faxed', [11], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),             ## договор факс, дс расторж бронь
        (MARKET, 'no_agency', 0, 'is_faxed', [101, 120], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {'MINIMAL_PAYMENT_COMMISSION': Decimal('0')}),     ## договор факс, дс расторж бронь

        (OFD, 'ofd_wo_count', 0, 'not_signed', [26], {'mail_count': 6, 'suspended_type': 'cancelled'}, 0, None, None, {}),          ## договор не подписан
        (OFD, 'ofd_wo_count', 0, 'is_booked', [26], {'mail_count': 6, 'suspended_type': 'suspended'}, 0, None, None, {}),         ## договор бронь
        (OFD, 'ofd_wo_count', 0, 'is_faxed', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 0, None, None, {}),   ## договор факс
        (OFD, 'ofd_wo_count', 0, 'signed', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),  ## договор подписан, дс нет
        (OFD, 'ofd_wo_count', 0, 'signed', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),        ## договор подписан, дс бронь
        (OFD, 'ofd_wo_count', 0, 'signed', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),           ## договор подписан, дс факс
        (OFD, 'ofd_wo_count', 0, 'is_booked', [26], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),     ## договор бронь, дс нет
        (OFD, 'ofd_wo_count', 0, 'is_booked', [26], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),  ## договор бронь, дс бронь
        (OFD, 'ofd_wo_count', 0, 'is_booked', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed',  80, {}),  ## договор бронь, дс факс
        (OFD, 'ofd_wo_count', 0, 'is_faxed', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),       ## договор факс, дс нет
        (OFD, 'ofd_wo_count', 0, 'is_faxed', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),          ## договор факс, дс бронь
        (OFD, 'ofd_wo_count', 0, 'is_faxed', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),         ## договор факс, дс факс
        (OFD, 'ofd_wo_count', 0, 'is_faxed', [26], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),    ## договор факс, дс расторж бронь
        (OFD, 'ofd_wo_count', 0, 'is_faxed', [26], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),          ## договор факс, дс расторж бронь

        (KZT, 'kz_no_agency', 0, 'not_signed', [7], {'mail_count': 6, 'suspended_type': 'cancelled'}, 0, None, None, {}),        ## договор не подписан
        (KZT, 'kz_opt_agency', 0, 'is_booked', [7], {'mail_count': 6, 'suspended_type': 'suspended'}, 0, None, None, {}),      ## договор бронь
        (KZT, 'kz_no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 0, None, None, {}), ## договор факс
        (KZT, 'kz_opt_agency', 0, 'signed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),   ## договор подписан, дс нет
        (KZT, 'kz_no_agency', 0, 'signed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),    ## договор подписан, дс бронь
        (KZT, 'kz_opt_agency', 0, 'signed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),  ## + договор подписан, дс факс
        (KZT, 'kz_no_agency', 0, 'is_booked', [7], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),      ## договор бронь, дс нет
        (KZT, 'kz_opt_agency', 0, 'is_booked', [7], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),    ## договор бронь, дс бронь
        (KZT, 'kz_no_agency', 0, 'is_booked', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),  ## договор бронь, дс факс
        (KZT, 'kz_opt_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),        ## договор факс, дс нет
        (KZT, 'kz_no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),         ## договор факс, дс бронь
        (KZT, 'kz_opt_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),    ## + договор факс, дс факс
        (KZT, 'kz_no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),             ## договор факс, дс расторж бронь
        (KZT, 'kz_opt_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),     ## договор факс, дс расторж бронь

        (BEL, 'bel_no_agency', 0, 'not_signed', [7], {'mail_count': 6, 'suspended_type': 'cancelled'}, 0, None, None, {}),       ## договор не подписан
        (BEL, 'bel_pr_agency', 0, 'is_booked', [7], {'mail_count': 6, 'suspended_type': 'suspended'}, 0, None, None, {}),        ## договор бронь
        (BEL, 'bel_opt_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 0, None, None, {}),        ## договор факс
        (BEL, 'bel_no_agency', 0, 'signed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),  ## договор подписан, дс нет
        (BEL, 'bel_no_agency', 0, 'signed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),       ## договор подписан, дс бронь
        (BEL, 'bel_no_agency', 0, 'signed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),       ## + договор подписан, дс факс
        (BEL, 'bel_no_agency', 0, 'is_booked', [7], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),  ## договор бронь, дс нет
        (BEL, 'bel_no_agency', 0, 'is_booked', [7], {'mail_count': 6, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),  ## договор бронь, дс бронь
        (BEL, 'bel_no_agency', 0, 'is_booked', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80, {}),  ## договор бронь, дс факс
        (BEL, 'bel_no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),  ## договор факс, дс нет
        (BEL, 'bel_no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),  ## договор факс, дс бронь
        (BEL, 'bel_no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_faxed', 80,  {}), ## + договор факс, дс факс
        (BEL, 'bel_no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),    ## договор факс, дс расторж бронь
        (BEL, 'bel_no_agency', 0, 'is_faxed', [7], {'mail_count': 11, 'suspended_type': 'nothing'}, 1, 'is_booked', 90, {}),   ## договор факс, дс расторж бронь


        ## отключенные фирмы. проверяем, что по ним не уходят письма и договоры не приостанавливаются
        ## https://st.yandex-team.ru/BALANCE-26271
        (SW, 'sw_opt_client', 0, 'signed', [7, 70, 35], {'mail_count': 0, 'suspended_type': 'nothing'}, 1,
             'not_signed', 80, {}),
        (UA, 'ua_opt_agency_prem', 0, 'signed', [7], {'mail_count': 0, 'suspended_type': 'nothing'}, 1, 'not_signed', 80, {}),
        (USA, 'usa_opt_client', 0, 'signed', [7], {'mail_count': 0, 'suspended_type': 'nothing'}, 1, 'not_signed', 80, {}),
        # BALANCEDUTY-363 таких договоров в проде больше нет
        # (TR, 'tr_opt_client', 0, 'signed', [7], {'mail_count': 0, 'suspended_type': 'nothing'}, 1, 'not_signed', 80, {}),

        # проверяем всякие исключения и негативные кейсы
        # 1) ОФД : проверяем, что только договоры "ОФД: Без уч. в расчетах" участвуют в рассылках, остальные договоры этой фирмы  - нет
        (OFD, 'no_agency', 0, 'is_faxed', [26], {'mail_count': 11, 'suspended_type': 'suspended'}, 1, 'is_booked', 80, {}),         ## договор факс, дс бронь

        (KZ_TAXI, 'no_agency', 0, 'not_signed', [135, 650], {'mail_count': 12, 'suspended_type': 'cancelled'}, 0, None, None, {}),  ## договор не подписан
        (KZ_TAXI, 'no_agency', 0, 'signed', [135, 650], {'mail_count': 17, 'suspended_type': 'suspended'}, 1, 'not_signed', 80, {}),        ## договор подписан, дс нет

    ]
)
def test_contract_notify(firm_id, contract_type, person_vip, contract_sign, services, expect_data, is_collateral,
                         collateral_sign, collateral_num, contract_params):
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, person_mapping[firm_id],
                                         {'email': 'test-balance-notify@yandex-team.ru', 'vip': str(person_vip)})
    additional_params = dict(sign_contract_mapper[contract_sign], **contract_params)
    contract_id, contract_external_id = steps.ContractSteps.create_contract_new(contract_type, dict(
        {'CLIENT_ID': client_id, 'PERSON_ID': person_id, 'DT': DT, 'FIRM': firm_id, 'SERVICES': services,
         'PAYMENT_TYPE': 2},
        **additional_params))
    if collateral_sign:
        steps.ContractSteps.create_collateral(collateral_num,
                                              dict({'CONTRACT2_ID': contract_id, 'DT': DT, 'FINISH_DT': FINISH_DT},
                                                   **sign_coll_mapper[collateral_sign]))
    ## костыльный костыль для дс на расторжение с подписью по факсу, т.к. этот допник создается с бронью подписи по дефолту
    ## приходится менять на подпись по факсу через базу
    is_cancelling_col = True if collateral_num == 90 else False
    if is_cancelling_col and collateral_sign == 'is_booked' and expect_data['mail_count'] == 17:
        db.balance().execute(
            'UPDATE (SELECT  a.* FROM T_CONTRACT_COLLATERAL c JOIN T_CONTRACT_ATTRIBUTES a  '
            'ON c.attribute_batch_id = a.attribute_batch_id WHERE C.CONTRACT2_ID = :contract_id AND c.num = \'01\' AND code  = \'IS_BOOKED\') SET value_num = 0',
            {'contract_id': contract_id})
        db.balance().execute(
            'UPDATE (SELECT  a.* FROM T_CONTRACT_COLLATERAL c JOIN T_CONTRACT_ATTRIBUTES a  ON c.attribute_batch_id = a.attribute_batch_id WHERE C.CONTRACT2_ID = :contract_id AND c.num = \'01\' AND code  = \'IS_BOOKED_DT\') SET value_dt = NULL',
            {'contract_id': contract_id})
        collateral_sign = 'is_faxed'
    steps.ContractSteps.contract_notify_flipping_dates(contract_id,
                                                       collateral_sign if is_collateral == 1 else contract_sign,
                                                       _type=is_collateral, is_cancelling_col=is_cancelling_col)

    dt, cnt = steps.ContractSteps.contract_notify_check(contract_id)
    check_suspended(contract_id, dt, expect_data['suspended_type'])
    utils.check_that(cnt[0]['cnt'], equal_to(expect_data['mail_count']), step=u'Проверяем количество писем')
    print contract_id, contract_external_id
