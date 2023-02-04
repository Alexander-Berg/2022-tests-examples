"""Тестирование DSP-income тарификатора

При запуске тестов:
- поднимается локальный YT (прописан в зависимостях ya.make, используется receipt)
- готовятся тестовые данные (например, CHECK_CONTRACT_CASES) и кладутся в соответствующие таблицы YT
- запускается тарификатор, возвращаются пути к таблицам с результатами тарификации
- значения из таблиц сравниваются с каконизированными значениями

Для построения данных используется два билдера
- DSPDataCase - готовит данные для таблиц логов, контрактов, валют, dsp
- FirmBuilder - данные фирмы, налоговые политики, тип плательщика
"""

import itertools
import json
from operator import itemgetter
from typing import Dict, List, Any, Optional

import pytest
import yt.wrapper as yt
from dataclasses import dataclass

from billing.library.python.logfeller_utils import log_interval
from billing.library.python.logmeta_utils.meta import get_log_tariff_meta, set_log_tariff_meta
from billing.library.python.yql_utils import query_metrics
from billing.library.python.yt_utils.test_utils.utils import create_subdirectory
from billing.log_tariffication.py.jobs.partner_tariff import dsp_income
from billing.log_tariffication.py.tests import constants as tests_constants
from billing.log_tariffication.py.tests.integration import conftest_partner
from billing.log_tariffication.py.tests.integration.conftest_partner import to_ts, date_from_string

DSP_SERVICE_ID = 80
INTERNAL_DSP_CLIENT = 2881904

# Вспомогательная колонка для идентификации кейсов
TEST_CASE_UID_COLUMN = '_TestCaseUID'

# Дополнение к схеме YT-таблицы для добавления вспомогательной колонки
TEST_CASE_UID_COLUMN_SCHEME_ADD = [{'name': TEST_CASE_UID_COLUMN, 'type': 'string'}]


class Currency:
    USD = 840
    RUB = 810
    EUR = 978
    WRONG = 9999999


def get_product_data() -> List[Dict[str, Any]]:
    return [
        {
            "PRODUCT_CURRENCY": "RUB",
            "PRODUCT_ID": 503364,
            "PRODUCT_MDH_ID": "675abbd6-bd4c-48cd-b0ec-75d8bc92c36d"
        },
        {
            "PRODUCT_CURRENCY": "USD",
            "PRODUCT_ID": 503365,
            "PRODUCT_MDH_ID": "b6e2e55e-511c-487b-81d9-195837d1c298"
        },
        {
            "PRODUCT_CURRENCY": "EUR",
            "PRODUCT_ID": 503366,
            "PRODUCT_MDH_ID": "0b4846f5-3844-4974-a6ef-3c153a81f161"
        },
        {
            "PRODUCT_CURRENCY": "CHF",
            "PRODUCT_ID": 503367,
            "PRODUCT_MDH_ID": "24c35de0-77f3-4256-9fa6-8dab5bc180fb"
        }
    ]


# noinspection DuplicatedCode
RUN_ID = '2021-01-01T00:00:00'
PREV_RUN_ID = '2020-12-31T11:00:00'


def gen_topics_meta(first_offset: int, next_offset: int) -> Dict:
    return {
        'topics': [
            {
                'topic': 't1',
                'cluster': 'c1',
                'partitions': [
                    {
                        'partition': 0,
                        'first_offset': first_offset,
                        'next_offset': next_offset,
                    },
                ],
            },
        ]
    }


class Sequence:
    """Генератор последовательных значений начиная со start.
     Например, для Sequence(5) вызовы next() возвращают 5, 6, 7, ..."""

    def __init__(self, start: int = 1):
        self.value = start

    def next(self):
        current = self.value
        self.value += 1
        return current


class DSPDataCase:
    """Билдер данных YT таблиц"""

    # Счетчик смещений для нумерации оффсетов в тестовых логах логброкера. Глобальный для всех DSPDataCase
    offset_seq = Sequence(start=0)

    def __init__(self, name):
        self.name = name  # имя тестового кейса для идентификации его в канонизаированных тестах

        # строки входящего и untariffed-лога
        self.dsp_log = []
        self.prev_untariffed_log = []

        # строки таблиц-справочников
        self.contract_rows = []
        self.product_rows = []
        self.currency_rows = []
        self.firm_rows = []
        self.client_dsp_rows = []

    def _append_to_list(self, item: Dict, target_list: List[Dict], test_uid: Optional[str] = None) -> None:
        assert isinstance(item, dict)
        case_uid = f'{self.name}_{str(test_uid)}' if test_uid else f'{self.name}'
        item = {
            **item,
            '_topic_cluster': 'c1',
            '_topic': 't1',
            '_partition': 0,
            '_offset': self.offset_seq.next(),
            '_chunk_record_index': 1,
            TEST_CASE_UID_COLUMN: case_uid,
        }
        target_list.append(item)

    def _generate_log_row(self, dt: str, dsp_id: int, price: float, event: str) -> Dict:
        return {
            'DSPID': dsp_id,
            'Price': float(price),
            'BillableEventType': event,
            'EventDate': to_ts(date_from_string(dt)),
            'EventTime': to_ts(date_from_string(dt)),
            'UnixTime': to_ts(date_from_string(dt)),
        }

    def _generate_prev_untariffed_row(self, dt: str, dsp_id: int = 1, price: float = 1.2, event: str = 'block-show',
                                      tariffication_info: Optional[Dict] = None) -> Dict:
        row = self._generate_log_row(dt=dt, dsp_id=dsp_id, price=price, event=event)
        return {
            **row,
            '_TarifficationInfo': tariffication_info
        }

    def add_prev_untariffed_log(self, dt: str, dsp_id: int = 1, price: float = 1.2, event: str = 'block-show',
                                tariffication_info: Optional[Dict] = None) -> 'DSPDataCase':
        row = self._generate_prev_untariffed_row(dt=dt, dsp_id=dsp_id, price=price, event=event,
                                                 tariffication_info=tariffication_info)
        self._append_to_list(row, self.prev_untariffed_log)
        return self

    def add_log(self, dt: str, dsp_id: int = 1, price: float = 1.2, event: str = 'block-show') -> 'DSPDataCase':
        row = self._generate_log_row(dt=dt, dsp_id=dsp_id, price=price, event=event)
        self._append_to_list(row, self.dsp_log)
        return self

    def add_contract(self, **kwargs) -> 'DSPDataCase':
        params = kwargs.copy()
        if 'services' not in kwargs:
            params['services'] = [DSP_SERVICE_ID]
        assert 'person_type' in params
        # в тестах даты задаем в формате ISO
        for date_key in ['dt', 'finish_dt', 'end_dt']:
            if date_key in params:
                params[date_key] = date_from_string(params[date_key])
        row = conftest_partner.gen_contract_ref_row(**params)
        contract_id = params['contract_id']
        self._append_to_list(row, self.contract_rows, test_uid=f'{contract_id}')
        return self

    def add_currency_rate(self, currency, rate, dt) -> 'DSPDataCase':
        dt = date_from_string(dt, msk_aware=True).isoformat()
        row = conftest_partner.gen_currency_ref_row(
            currency=currency,
            rate=rate,
            dt=dt,
        )
        self._append_to_list(row, self.currency_rows, test_uid=f'{currency}_{rate}_{dt}')
        return self

    def add_product(self, currency, product_id, product_mdh_id) -> 'DSPDataCase':
        row = conftest_partner.gen_product_ref_row(
            currency=currency,
            product_id=product_id,
            product_mdh_id=product_mdh_id
        )
        self._append_to_list(row, self.product_rows, test_uid=f'{currency}_{product_id}')
        return self

    def add_firm(self, firm_data: Dict) -> 'DSPDataCase':
        self.firm_rows.append(firm_data)
        return self

    def add_client_dsp(self, dsp_id, client_id) -> 'DSPDataCase':
        row = conftest_partner.gen_client_dsp_ref_row(
            dsp_id=dsp_id,
            client_id=client_id,
        )
        self._append_to_list(row, self.client_dsp_rows, test_uid=f'{dsp_id}_{client_id}')
        return self


class FirmBuilder:
    """Билдер фирм и налоговых политик"""

    def __init__(self, firm_id: int):
        self.firm_id = firm_id
        self.tax_percents = dict()
        self.policies = list()
        self.person_categories = list()

    def add_tax_percents(self, uid: str, dt_iso: str, nds_pct: int, id_: int = 100, hidden=0) -> 'FirmBuilder':
        self.tax_percents[uid] = {
            "dt": dt_iso,
            "hidden": hidden,
            "id": id_,
            "mdh_id": 'MDH_ID_' + str(id_),
            "nds_pct": nds_pct,
            "nsp_pct": 0,
        }
        return self

    def add_tax_policy(self, resident: int, percents_uids: List[str], default_tax: int = 0, id_: int = 200,
                       hidden=0) -> 'FirmBuilder':
        policy = {
            "default_tax": default_tax,
            "hidden": hidden,
            "id": id_,
            "mdh_id": 'MDH_ID_' + str(id_),
            "name": "TAX_POLICY_NAME_" + str(id_),
            "percents": [self.tax_percents[uid] for uid in percents_uids],
            "region_id": 84,
            "resident": resident,
        }
        self.policies.append(policy)
        return self

    def add_person_category(self, category: str, resident: int = 1) -> 'FirmBuilder':
        person_category = {
            "category": category.lower(),
            "is_legal": 1,
            "is_resident": resident
        }
        self.person_categories.append(person_category)
        return self

    def to_json(self) -> Dict:
        return {
            "id": self.firm_id,
            "title": 'FIRM_TITLE_' + str(self.firm_id),
            "mdh_id": 'MDH_ID_' + str(self.firm_id),
            "tax_policies": self.policies,
            "person_categories": self.person_categories,
        }


FIRM_1 = (
    FirmBuilder(firm_id=1)
        .add_tax_percents(uid='nds_20', dt_iso='2020-01-01T00:00:00+03:00', nds_pct=20)
        .add_tax_policy(resident=1, percents_uids=['nds_20'])
        .add_person_category(category='ur', resident=1)
        .to_json()
)

FIRM_4 = (
    FirmBuilder(firm_id=4)
        .add_tax_percents(uid='nds_0', dt_iso='2020-01-01T00:00:00+03:00', nds_pct=0)
        .add_tax_policy(resident=1, percents_uids=['nds_0'])
        .add_person_category(category='ur', resident=1)
        .to_json()
)

CHECK_CONTRACT_CASES = [
    (
        # Строки, используемые в нескольких тестах (валюты, фирмы)
        # В каждом тесте будем создавать свои dsp, client, contract
        DSPDataCase('base')
            .add_firm(FIRM_1)
            .add_firm(FIRM_4)
            .add_currency_rate(currency='EUR', rate=50, dt='2021-01-01')
            .add_currency_rate(currency='USD', rate=20, dt='2021-01-01')
    ),
    (
        # типовой кейс
        # - есть нетарифицрованные логи из предыдущего запуска
        # - один PARTNERS-контракт, действующий на момент события
        # - валюта USD (840)
        DSPDataCase('normal_case')
            .add_prev_untariffed_log(dt='2021-01-01', dsp_id=1, price=300, event='show')
            .add_log(dt='2021-02-02', dsp_id=1, price=100, event='block-show')
            .add_client_dsp(dsp_id=1, client_id=1)
            .add_contract(client_id=1, contract_id=10, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # событие show
        DSPDataCase('event_show')
            .add_log(dt='2021-02-02', dsp_id=2, price=100, event='show')
            .add_client_dsp(dsp_id=2, client_id=2)
            .add_contract(client_id=2, contract_id=20, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # событие undo-block-show
        DSPDataCase('event_undo_block_show')
            .add_log(dt='2021-02-02', dsp_id=3, price=100, event='undo-block-show')
            .add_client_dsp(dsp_id=3, client_id=3)
            .add_contract(client_id=3, contract_id=30, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # GENERAL-контракт с FINISH_DT (событие в последний день договора)
        DSPDataCase('contract_general_last_day')
            .add_log(dt='2021-06-30', dsp_id=4, price=100, event='block-show')
            .add_client_dsp(dsp_id=4, client_id=4)
            .add_contract(client_id=4, contract_id=40, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # PARTNERS-контракт с END_DT (событие в последний день договора)
        DSPDataCase('contract_partners_last_day')
            .add_log(dt='2021-06-30', dsp_id=5, price=100, event='block-show')
            .add_client_dsp(dsp_id=5, client_id=5)
            .add_contract(client_id=5, contract_id=50, contract_type='PARTNERS',
                          dt='2021-01-01', end_dt='2021-06-30',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # у клиента два контракта. Выбирается действующий на дату события
        DSPDataCase('two_contracts')
            .add_log(dt='2021-05-01', dsp_id=6, price=100, event='block-show')
            .add_client_dsp(dsp_id=6, client_id=6)
            .add_contract(client_id=6, contract_id=60, contract_type='PARTNERS',
                          dt='2021-01-01', end_dt='2021-03-31',
                          person_type='ur', currency=Currency.USD)
            .add_contract(client_id=6, contract_id=61, contract_type='PARTNERS',
                          dt='2021-04-01', end_dt='2021-06-30',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # GENERAL-контракт, договор завершился непосредственно перед событием
        DSPDataCase('general_contract_not_active')
            .add_log(dt='2021-07-01', dsp_id=7, price=100, event='block-show')
            .add_client_dsp(dsp_id=7, client_id=7)
            .add_contract(client_id=7, contract_id=70, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # PARTNERS-контракт, договор завершился непосредственно перед событием
        DSPDataCase('partners_contract_not_active')
            .add_log(dt='2021-07-01', dsp_id=8, price=100, event='block-show')
            .add_client_dsp(dsp_id=8, client_id=8)
            .add_contract(client_id=8, contract_id=80, contract_type='PARTNERS',
                          dt='2021-01-01', end_dt='2021-06-30',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # отсутствует клиент DSP
        DSPDataCase('no_client')
            .add_log(dt='2021-06-30', dsp_id=9, price=100, event='block-show')
    ),
    (
        # отсутствует контракт для клиента (прошло более 180 дней - отбрасываем событие)
        DSPDataCase('without_contract_outdated_event')
            .add_log(dt='2020-03-31', dsp_id=10, price=100, event='block-show')
            .add_client_dsp(dsp_id=10, client_id=10)
    ),
    (
        # контракт INTERNAL_DSP_CLIENT
        # валюты нет, контракта нет
        DSPDataCase('internal_dsp_client')
            .add_log(dt='2021-01-02', dsp_id=11, price=100, event='block-show')
            .add_client_dsp(dsp_id=11, client_id=INTERNAL_DSP_CLIENT)
    ),
    (
        # контракт присутствует (прошло более 180 дней - отбрасываем событие)
        DSPDataCase('with_contract_outdated_event')
            .add_log(dt='2020-03-31', dsp_id=12, price=100, event='block-show')
            .add_client_dsp(dsp_id=12, client_id=12)
            .add_contract(client_id=12, contract_id=120, contract_type='PARTNERS',
                          dt='2020-01-01', end_dt='2021-12-31',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # отсутствует контракт для клиента (прошло менее 180 дней - сделаем тарификацию позже)
        DSPDataCase('without_contract_fresh_event')
            .add_log(dt='2020-09-30', dsp_id=13, price=100, event='block-show')
            .add_client_dsp(dsp_id=13, client_id=13)
    ),
    # далее - те же кейсы, но для Yandex Inc
    (
        # типовой кейс
        # - есть нетарифицрованные логи из предыдущего запуска
        # - один PARTNERS-контракт, действующий на момент события
        # - валюта USD (840)
        DSPDataCase('normal_case_firm4')
            .add_prev_untariffed_log(dt='2021-01-01', dsp_id=14, price=300, event='show')
            .add_log(dt='2021-02-02', dsp_id=14, price=100, event='block-show')
            .add_client_dsp(dsp_id=14, client_id=14)
            .add_contract(client_id=14, contract_id=140, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # событие show
        DSPDataCase('event_show_firm4')
            .add_log(dt='2021-02-02', dsp_id=15, price=100, event='show')
            .add_client_dsp(dsp_id=15, client_id=15)
            .add_contract(client_id=15, contract_id=150, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # событие undo-block-show
        DSPDataCase('event_undo_block_show_firm4')
            .add_log(dt='2021-02-02', dsp_id=16, price=100, event='undo-block-show')
            .add_client_dsp(dsp_id=16, client_id=16)
            .add_contract(client_id=16, contract_id=160, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # GENERAL-контракт с FINISH_DT (событие в последний день договора)
        DSPDataCase('contract_general_last_day_firm4')
            .add_log(dt='2021-06-30', dsp_id=17, price=100, event='block-show')
            .add_client_dsp(dsp_id=17, client_id=17)
            .add_contract(client_id=17, contract_id=170, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # PARTNERS-контракт с END_DT (событие в последний день договора)
        DSPDataCase('contract_partners_last_day_firm4')
            .add_log(dt='2021-06-30', dsp_id=18, price=100, event='block-show')
            .add_client_dsp(dsp_id=18, client_id=18)
            .add_contract(client_id=18, contract_id=180, contract_type='PARTNERS',
                          dt='2021-01-01', end_dt='2021-06-30',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # у клиента два контракта. Выбирается действующий на дату события
        DSPDataCase('two_contracts_firm4')
            .add_log(dt='2021-05-01', dsp_id=19, price=100, event='block-show')
            .add_client_dsp(dsp_id=19, client_id=19)
            .add_contract(client_id=19, contract_id=190, contract_type='PARTNERS',
                          dt='2021-01-01', end_dt='2021-03-31',
                          person_type='ur', currency=Currency.USD, firm=4)
            .add_contract(client_id=19, contract_id=191, contract_type='PARTNERS',
                          dt='2021-04-01', end_dt='2021-06-30',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # GENERAL-контракт, договор завершился непосредственно перед событием
        DSPDataCase('general_contract_not_active_firm4')
            .add_log(dt='2021-07-01', dsp_id=20, price=100, event='block-show')
            .add_client_dsp(dsp_id=20, client_id=20)
            .add_contract(client_id=20, contract_id=200, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # PARTNERS-контракт, договор завершился непосредственно перед событием
        DSPDataCase('partners_contract_not_active_firm4')
            .add_log(dt='2021-07-01', dsp_id=21, price=100, event='block-show')
            .add_client_dsp(dsp_id=21, client_id=21)
            .add_contract(client_id=21, contract_id=210, contract_type='PARTNERS',
                          dt='2021-01-01', end_dt='2021-06-30',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # контракт присутствует (прошло более 180 дней - отбрасываем событие)
        DSPDataCase('with_contract_outdated_event_firm4')
            .add_log(dt='2020-03-31', dsp_id=25, price=100, event='block-show')
            .add_client_dsp(dsp_id=25, client_id=25)
            .add_contract(client_id=25, contract_id=250, contract_type='PARTNERS',
                          dt='2020-01-01', end_dt='2021-12-31',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # у клиента два действующих контракта для разных сервисов. Выбирается контракт для сервиса DSP
        DSPDataCase('contracts_different_services_firm4')
            .add_log(dt='2021-05-01', dsp_id=27, price=100, event='block-show')
            .add_client_dsp(dsp_id=27, client_id=27)
            .add_contract(client_id=27, contract_id=270, contract_type='PARTNERS',
                          dt='2021-01-01', end_dt='2021-12-31',
                          person_type='ur', currency=Currency.USD, firm=4, services=[DSP_SERVICE_ID])
            .add_contract(client_id=27, contract_id=271, contract_type='PARTNERS',
                          dt='2021-01-01', end_dt='2021-12-31',
                          person_type='ur', currency=Currency.USD, firm=4, services=[129])
    ),
]

CHECK_CURRENCY_CASES = [
    (
        # Строки, используемые в нескольких тестах (dsp, клиент)
        # В каждом тесте будем создавать курсы валют и контракт на эту дату
        DSPDataCase('base')
            .add_firm(FIRM_1)
            .add_firm(FIRM_4)
    ),
    (
        # отсутствует курс
        DSPDataCase('no_currency_rate')
            .add_log(dt='2021-01-01', dsp_id=1, price=100, event='show')
            .add_client_dsp(dsp_id=1, client_id=1)
            .add_contract(client_id=1, contract_id=10, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
    ),
    (
        # контракт с валютой в долларах
        DSPDataCase('currency_usd')
            .add_log(dt='2021-01-02', dsp_id=2, price=100, event='show')
            .add_client_dsp(dsp_id=2, client_id=2)
            .add_contract(client_id=2, contract_id=20, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-02')
    ),
    (
        # контракт с валютой в EUR
        DSPDataCase('currency_eur')
            .add_log(dt='2021-01-02', dsp_id=3, price=100, event='show')
            .add_client_dsp(dsp_id=3, client_id=3)
            .add_contract(client_id=3, contract_id=30, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.EUR)
            .add_currency_rate(currency='EUR', rate=25, dt='2021-01-02')
    ),
    (
        # контракт с валютой в RUB (курсов валюты нет)
        DSPDataCase('currency_rur')
            .add_log(dt='2021-01-03', dsp_id=4, price=100, event='show')
            .add_client_dsp(dsp_id=4, client_id=4)
            .add_contract(client_id=4, contract_id=40, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.RUB)
    ),
    (
        # дублирование валюты
        DSPDataCase('duplicate_currency')
            .add_log(dt='2021-01-04', dsp_id=5, price=100, event='show')
            .add_client_dsp(dsp_id=5, client_id=5)
            .add_contract(client_id=5, contract_id=50, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-04')
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-04')
    ),
    (
        # несколько валют, выбираем по дате
        DSPDataCase('mutilple_currency')
            .add_log(dt='2021-01-06', dsp_id=6, price=100, event='show')
            .add_client_dsp(dsp_id=6, client_id=6)
            .add_contract(client_id=6, contract_id=60, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
            .add_currency_rate(currency='USD', rate=12345, dt='2021-01-05')
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-06')
            .add_currency_rate(currency='USD', rate=123456, dt='2021-01-07')
    ),
    (
        # берем последнюю активную валюту
        DSPDataCase('last_active_currency')
            .add_log(dt='2021-01-09', dsp_id=7, price=100, event='show')
            .add_client_dsp(dsp_id=7, client_id=7)
            .add_contract(client_id=7, contract_id=70, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD)
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-08')
    ),
    (
        # неизвестная валюта
        DSPDataCase('unknown_currency')
            .add_log(dt='2021-01-10', dsp_id=8, price=100, event='show')
            .add_client_dsp(dsp_id=8, client_id=8)
            .add_contract(client_id=8, contract_id=80, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.WRONG)
    ),
    # далее кейсы для Yandex Inc
    (
        # отсутствует курс
        DSPDataCase('no_currency_rate')
            .add_log(dt='2021-01-01', dsp_id=9, price=100, event='show')
            .add_client_dsp(dsp_id=9, client_id=9)
            .add_contract(client_id=9, contract_id=90, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
    ),
    (
        # контракт с валютой в долларах
        DSPDataCase('currency_usd')
            .add_log(dt='2021-01-02', dsp_id=10, price=100, event='show')
            .add_client_dsp(dsp_id=10, client_id=10)
            .add_contract(client_id=10, contract_id=100, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-02')
    ),
    (
        # дублирование валюты
        DSPDataCase('duplicate_currency')
            .add_log(dt='2021-01-04', dsp_id=13, price=100, event='show')
            .add_client_dsp(dsp_id=13, client_id=13)
            .add_contract(client_id=13, contract_id=130, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-04')
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-04')
    ),
    (
        # несколько валют, выбираем по дате
        DSPDataCase('mutilple_currency')
            .add_log(dt='2021-01-06', dsp_id=14, price=100, event='show')
            .add_client_dsp(dsp_id=14, client_id=14)
            .add_contract(client_id=14, contract_id=140, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
            .add_currency_rate(currency='USD', rate=12345, dt='2021-01-05')
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-06')
            .add_currency_rate(currency='USD', rate=123456, dt='2021-01-07')
    ),
    (
        # берем последнюю активную валюту
        DSPDataCase('last_active_currency')
            .add_log(dt='2021-01-09', dsp_id=15, price=100, event='show')
            .add_client_dsp(dsp_id=15, client_id=15)
            .add_contract(client_id=15, contract_id=150, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD, firm=4)
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-08')
    ),
    (
        # неизвестная валюта
        DSPDataCase('unknown_currency')
            .add_log(dt='2021-01-10', dsp_id=16, price=100, event='show')
            .add_client_dsp(dsp_id=16, client_id=16)
            .add_contract(client_id=16, contract_id=160, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.WRONG, firm=4)
    ),
]

CHECK_TAX_CASES = [
    # здесь считаем все в рублях, чтобы не добавлять валюту в тестах
    (
        # отсутствует фирма
        DSPDataCase('no_firm')
            .add_log(dt='2021-02-02', dsp_id=1, price=100, event='block-show')
            .add_client_dsp(dsp_id=1, client_id=1)
            .add_contract(client_id=1, contract_id=10, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.RUB,
                          firm=123456789)
    ),
    (
        # дубликат фирмы
        DSPDataCase('duplicate_firm')
            .add_log(dt='2021-02-02', dsp_id=2, price=100, event='block-show')
            .add_client_dsp(dsp_id=2, client_id=2)
            .add_contract(client_id=2, contract_id=20, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.RUB,
                          firm=1)
            .add_firm(FIRM_1)
            .add_firm(FIRM_1)
    ),
    (
        # выбор налоговой политики по дате
        DSPDataCase('choose_tax_by_date')
            .add_log(dt='2021-02-02', dsp_id=3, price=100, event='block-show')
            .add_client_dsp(dsp_id=3, client_id=3)
            .add_contract(client_id=3, contract_id=30, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.RUB,
                          firm=3)
            .add_firm(FirmBuilder(firm_id=3)
                      .add_tax_percents(uid='nds_1', dt_iso='2020-01-01T00:00:00+03:00', nds_pct=1)
                      .add_tax_percents(uid='nds_5', dt_iso='2021-01-01T00:00:00+03:00', nds_pct=5)
                      .add_tax_percents(uid='nds_20', dt_iso='2021-02-01T00:00:00+03:00', nds_pct=20)
                      .add_tax_policy(resident=1, percents_uids=['nds_1', 'nds_5', 'nds_20'])
                      .add_person_category(category='ur', resident=1)
                      .to_json())
    ),
    (
        # политика задублирована
        DSPDataCase('duplicate_percent')
            .add_log(dt='2021-02-02', dsp_id=4, price=100, event='block-show')
            .add_client_dsp(dsp_id=4, client_id=4)
            .add_contract(client_id=4, contract_id=40, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.RUB,
                          firm=44)
            .add_firm(FirmBuilder(firm_id=44)
                      .add_tax_percents(uid='p1', dt_iso='2021-01-01T00:00:00+03:00', nds_pct=20)
                      .add_tax_percents(uid='p2', dt_iso='2021-01-01T00:00:00+03:00', nds_pct=20)
                      .add_tax_policy(resident=1, percents_uids=['p1', 'p2'])
                      .add_person_category(category='ur', resident=1)
                      .to_json())
    ),
    (
        # выбираем политику по default_tax
        DSPDataCase('choose_by_default_tax')
            .add_log(dt='2021-02-02', dsp_id=5, price=100, event='block-show')
            .add_client_dsp(dsp_id=5, client_id=5)
            .add_contract(client_id=5, contract_id=50, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.RUB,
                          firm=5)
            .add_firm(FirmBuilder(firm_id=5)
                      .add_tax_percents(uid='p1', dt_iso='2021-01-01T00:00:00+03:00', nds_pct=10)
                      .add_tax_policy(default_tax=0, resident=1, percents_uids=['p1'])
                      .add_tax_percents(uid='p2', dt_iso='2021-01-01T00:00:00+03:00', nds_pct=20)
                      .add_tax_policy(default_tax=1, resident=1, percents_uids=['p2'])
                      .add_person_category(category='ur', resident=1)
                      .to_json())
    ),
    # далее добавлены кейсы с договорами в долларах
    (
        # отсутствует фирма
        DSPDataCase('no_firm')
            .add_log(dt='2021-02-02', dsp_id=6, price=100, event='block-show')
            .add_client_dsp(dsp_id=6, client_id=6)
            .add_contract(client_id=6, contract_id=60, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD,
                          firm=123456789)
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-02')
    ),
    (
        # дубликат фирмы (фирма - Yandex Inc)
        DSPDataCase('duplicate_firm')
            .add_log(dt='2021-02-02', dsp_id=7, price=100, event='block-show')
            .add_client_dsp(dsp_id=7, client_id=7)
            .add_contract(client_id=7, contract_id=70, contract_type='GENERAL',
                          dt='2021-01-01', finish_dt='2021-07-01',
                          person_type='ur', currency=Currency.USD,
                          firm=4)
            .add_firm(FIRM_4)
            .add_firm(FIRM_4)
            .add_currency_rate(currency='USD', rate=10, dt='2021-01-02')
    ),
]


@dataclass
class DataTable:
    path: str
    schema: Dict
    rows: List[Dict]


def canonize_contract(contract_dict):
    """Возвращает уменьшенное представление контракта.
    Чтобы при канонизацию не попадали лишние данные и тесты были более стабильные"""

    if contract_dict is None:
        return None

    canonize_keys = [
        "id",
        "client_id",
        "type",
        "dt",
        "currency",
        "finish_dt",
        "end_dt",
        "firm",
        "person_id",
        "person_type",
        "services",
    ]
    return {k: v for k, v in contract_dict.items() if k in canonize_keys}


def canonize_tariffed_log(rows):
    rows = list(rows)
    for row in rows:
        contract_json = row.get('ContractObject')
        if contract_json is None:
            continue

        contract = json.loads(contract_json)
        row['ContractObject'] = canonize_contract(contract)
    return sorted(rows, key=itemgetter('_offset'))


def canonize_tariffed_table(yt_client, path):
    rows = list(yt_client.read_table(path))
    return canonize_tariffed_log(rows)


def canonize_untariffed_log(rows):
    rows = list(rows)
    for row in rows:
        contract = (row.get('_TarifficationInfo') or {}).get('ContractObject')
        if contract is None:
            continue

        row['_TarifficationInfo']['ContractObject'] = canonize_contract(contract)
    return sorted(rows, key=itemgetter('_offset'))


def canonize_untariffed_table(yt_client, path):
    rows = list(yt_client.read_table(path))
    return canonize_untariffed_log(rows)


def canonize_metrics_data(yt_client, path):
    # noinspection PyUnresolvedReferences
    return yt.yson.convert.yson_to_json(query_metrics.get_table_metrics_data(yt_client, path))


# noinspection DuplicatedCode
@pytest.mark.parametrize(
    ['case_list'],
    [
        pytest.param(CHECK_CONTRACT_CASES, id='contract'),
        pytest.param(CHECK_CURRENCY_CASES, id='currency'),
        pytest.param(CHECK_TAX_CASES, id='tax'),
    ]
)
def test_tariffication_layout(yt_root,
                              yt_client,
                              yt_transaction,
                              yql_client,
                              udf_server_file_url,
                              case_list: List[DSPDataCase]):
    # На основе тестовых кейсов формируем таблицы в локальном YT (таблицы входящего лога и справочников)

    current_meta = {
        'run_id': RUN_ID,
        'log_interval': gen_topics_meta(0, DSPDataCase.offset_seq.value),
        'ref_contract_interval': gen_topics_meta(0, DSPDataCase.offset_seq.value),
        'ref_currency_interval': gen_topics_meta(0, DSPDataCase.offset_seq.value),
        'ref_client_dsp_interval': gen_topics_meta(0, DSPDataCase.offset_seq.value),
    }

    dsp_log_dir = yt.ypath_join(yt_root, 'dsp_log')
    contract_table = DataTable(
        path=yt.ypath_join(yt_root, 'ref_contract'),
        schema=tests_constants.REFERENCE_LOG_TABLE_SCHEMA + TEST_CASE_UID_COLUMN_SCHEME_ADD,
        rows=list(itertools.chain(*[case.contract_rows for case in case_list]))
    )
    currency_table = DataTable(
        path=yt.ypath_join(yt_root, 'ref_currency'),
        schema=tests_constants.REFERENCE_LOG_TABLE_SCHEMA + TEST_CASE_UID_COLUMN_SCHEME_ADD,
        rows=list(itertools.chain(*[case.currency_rows for case in case_list]))
    )
    firm_table = DataTable(
        path=yt.ypath_join(yt_root, 'ref_firm'),
        schema=tests_constants.REFERENCE_FIRM_SCHEMA,
        rows=list(itertools.chain(*[case.firm_rows for case in case_list]))
    )
    client_dsp_table = DataTable(
        path=yt.ypath_join(yt_root, 'ref_client_dsp'),
        schema=tests_constants.REFERENCE_LOG_TABLE_SCHEMA + TEST_CASE_UID_COLUMN_SCHEME_ADD,
        rows=list(itertools.chain(*[case.client_dsp_rows for case in case_list]))
    )
    dsp_log_table = DataTable(
        path=yt.ypath_join(dsp_log_dir, RUN_ID),
        schema=tests_constants.BILLABLE_DSP_LOG_TABLE_SCHEMA + TEST_CASE_UID_COLUMN_SCHEME_ADD,
        rows=list(itertools.chain(*[case.dsp_log for case in case_list]))
    )

    input_tables = [
        contract_table,
        currency_table,
        firm_table,
        client_dsp_table,
        dsp_log_table,
    ]

    prev_untariffed_merged_dir = create_subdirectory(yt_client, yt_root, 'merged_untariffed')
    prev_untariffed_table = DataTable(
        path=yt.ypath_join(prev_untariffed_merged_dir, PREV_RUN_ID),
        schema=tests_constants.UNTARIFFED_DSP_LOG_TABLE_SCHEMA + TEST_CASE_UID_COLUMN_SCHEME_ADD,
        rows=list(itertools.chain(*[case.prev_untariffed_log for case in case_list]))
    )
    is_exist_prev_untariffed = len(prev_untariffed_table.rows) > 0
    if is_exist_prev_untariffed:
        current_meta['prev_run_id'] = PREV_RUN_ID
        input_tables.append(prev_untariffed_table)

    for table in input_tables:
        attributes = {
            log_interval.LB_META_ATTR: gen_topics_meta(0, DSPDataCase.offset_seq.value),
            'schema': table.schema
        }
        yt_client.create('table', table.path, recursive=True, attributes=attributes)
        yt_client.write_table(table.path, table.rows)
        set_log_tariff_meta(yt_client, table.path, current_meta)

    output_tariffed_dir = create_subdirectory(yt_client, yt_root, 'output_tariffed')
    output_untariffed_dir = create_subdirectory(yt_client, yt_root, 'output_untariffed')
    output_tariffed_internal_dir = create_subdirectory(yt_client, yt_root, 'output_tariffed_internal')

    # Запускаем локально процесс тарификации

    output_tariffed_path, output_untariffed_path, output_tariffed_internal_path = dsp_income.run_job(
        yt_client,
        yql_client,
        current_meta,

        ref_contract=contract_table.path,
        ref_currency=currency_table.path,
        ref_firm=firm_table.path,
        ref_client_dsp=client_dsp_table.path,

        ref_product_json_content=json.dumps(get_product_data()),

        dsp_logs_dir=dsp_log_dir,
        prev_untariffed_merged_dir=prev_untariffed_merged_dir,

        output_tariffed_dir=output_tariffed_dir,
        output_untariffed_dir=output_untariffed_dir,
        output_tariffed_internal_dir=output_tariffed_internal_dir,

        udf_file_url=udf_server_file_url,
        transaction=yt_transaction,
    )

    for table in input_tables:
        yt_client.remove(table.path)

    # Считываем результаты и канонизируем их для отслеживания изменений

    canonized_data = {
        'output_log_tariffed': canonize_tariffed_table(yt_client, output_tariffed_path),
        'output_log_tariffed_internal': canonize_tariffed_table(yt_client, output_tariffed_internal_path),
        'output_log_untariffed': canonize_untariffed_table(yt_client, output_untariffed_path),

        'output_meta_tariffed': get_log_tariff_meta(yt_client, output_tariffed_path),
        'output_meta_tariffed_internal': get_log_tariff_meta(yt_client, output_tariffed_internal_path),
        'output_meta_untariffed': get_log_tariff_meta(yt_client, output_untariffed_path),

        'output_metrics_tariffed': canonize_metrics_data(yt_client, output_tariffed_path),
        'output_metrics_tariffed_internal': canonize_metrics_data(yt_client, output_tariffed_internal_path),
        'output_metrics_untariffed': canonize_metrics_data(yt_client, output_untariffed_path)
    }

    # для дебага удобно добавить в канонизацию содержимое исходных таблиц (нужно расскоментировать)
    debug_data = {
        # 'input_dsp_log': canonize_tariffed_log(dsp_log_table.rows),
        # 'input_dsp_log_untariffed': \
        #     canonize_untariffed_log(prev_untariffed_table.rows) if is_exist_prev_untariffed else None,
        # 'reference_contract': sorted(contract_table.rows, key=itemgetter('_offset')),
        # 'reference_currency': sorted(currency_table.rows, key=itemgetter('_offset')),
        # 'reference_firm': sorted(firm_table.rows, key=itemgetter('id')),
        # 'reference_client_dsp': sorted(client_dsp_table.rows, key=itemgetter('_offset')),
    }

    return {**canonized_data, **debug_data}
