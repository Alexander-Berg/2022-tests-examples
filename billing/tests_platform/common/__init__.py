import datetime
import itertools
import os
import unittest
from decimal import Decimal
from enum import Enum
from functools import lru_cache
from typing import Dict, Optional, Type, Union

import sqlalchemy as sa
from yql.api.v1.client import YqlClient
from yt.wrapper import YtClient

from agency_rewards.cashback.utils import CashBackCalc
from agency_rewards.rewards.application import Application
from agency_rewards.rewards.butils.session import SessionBase as Session
from agency_rewards.rewards.platform.bunker import prepare_bunker_client
from agency_rewards.rewards.scheme import runs
from agency_rewards.rewards.utils.bunker import BunkerCalc
from agency_rewards.rewards.utils.const import (
    ARScale,
    CommType,
    CommissionType as CT,
    ContractJoinType,
    InvoiceType,
    RewardType,
    Scale,
)
from agency_rewards.rewards.utils.dates import (
    add_months,
    get_first_dt_prev_month,
    get_last_dt_prev_month,
    get_previous_quarter_first_day,
    get_previous_quarter_last_day,
)
from agency_rewards.rewards.utils.yql_crutches import create_yt_client
from .scheme import regtest_data

NDS = 1.2
TEMP_DATA_DIR_NAME = 'temp_test_data'


class Currency(str, Enum):
    KZT = 'KZT'
    BYN = 'BYN'
    USD = 'USD'
    RUR = 'RUR'


# For backward compatibility
KZT = Currency.KZT
BYN = Currency.BYN
USD = Currency.USD
RUR = Currency.RUR

#
# Helpers
#


yt_base_path = "//home/balance/dev/yb-ar/regress"
act_div_fmt = '%Y-%m-%d'
sales_fmt = '%Y-%m-%d'
act_page_fmt = '%Y-%m'
act_fmt = '%Y%m'


def fmt_path(node_name, dt, fmt):
    dt_str = dt.strftime(fmt)
    return f"{yt_base_path}/{node_name}/{dt_str}"


def yt_type(name: str, type_name: str) -> Dict:
    """
    Описание типа для YT
    """
    return {'name': name, 'type': type_name}


PAGES_YT_COLUMNS = [
    yt_type('PageID', 'int64'),
    yt_type('TargetType', 'int64'),
]


def new_page(page_id, target_type):
    return {'PageID': page_id, 'TargetType': target_type}


PAGES_ACT_YT_COLUMNS = [
    yt_type('ExportID', 'int64'),
    yt_type('EngineID', 'int64'),
    yt_type('PageID', 'int64'),
    yt_type('amt_rur', 'double'),
]


def new_page_act(export_id, page_id, amt, engine_id=7):
    return {'ExportID': export_id, 'EngineID': engine_id, 'PageID': page_id, 'amt_rur': amt}


ACT_DIV_YT_COLUMNS = [
    yt_type('act_id', 'int64'),
    yt_type('service_id', 'int64'),
    yt_type('group_service_order_id', 'int64'),
    yt_type('service_order_id', 'int64'),
    yt_type('inv_amount', 'double'),
    yt_type('order_amount', 'double'),
]


def new_act_div(
    act_id: int,
    group_service_order_id: int,
    service_order_id: int,
    order_amount: float = 0.0,
    inv_amount: float = 0.0,
    service_id: int = 7,
):
    return {
        'act_id': act_id,
        'group_service_order_id': group_service_order_id,
        'service_order_id': service_order_id,
        'order_amount': order_amount,
        'inv_amount': inv_amount,
        'service_id': service_id,
    }


SALES_YT_COLUMNS = [
    yt_type("agency_id", "int64"),
    yt_type("act_id", "int64"),
    yt_type("dt_month", "int64"),
    yt_type("service_id", "int64"),
    yt_type("service_order_id", "int64"),
    yt_type("ar_commission_type", "int64"),
    yt_type("amt_rur", "double"),
]


def new_sale(agency_id, act_id, service_order_id, amt, act_month, service_id=7, commission_type=CommType.Direct.value):
    return {
        'agency_id': agency_id,
        'act_id': act_id,
        'dt_month': act_month,
        'service_id': service_id,
        'service_order_id': service_order_id,
        'ar_commission_type': commission_type,
        'amt_rur': amt,
    }


ACTS_YT_COLUMNS = [
    yt_type('contract_id', 'int64'),
    yt_type('contract_eid', 'string'),
    yt_type('act_id', 'int64'),
    yt_type('brand_id', 'int64'),
    yt_type("invoice_id", 'int64'),
    yt_type('agency_id', 'int64'),
    yt_type('service_id', 'int64'),
    yt_type('service_order_id', 'int64'),
    yt_type('commission_type', 'int64'),
    yt_type('discount_type', 'int64'),
    yt_type('amt', 'double'),
    yt_type('amt_w_nds', 'double'),
]


def new_act(
    contract_id: int,
    agency_id: int,
    service_order_id: int,
    act_id: int,
    amt: float,
    scale=Scale.Prof.value,
    comm_type=CommType.Direct.value,
    brand_id: int = None,
    service_id: int = 7,
    invoice_id: int = None,
) -> Dict:
    """
    Акт за прошлый месяц для выгрузки в YT
    """
    return dict(
        contract_id=contract_id,
        contract_eid='C-{}'.format(contract_id),
        act_id=act_id,
        brand_id=brand_id,
        agency_id=agency_id,
        invoice_id=invoice_id,
        service_id=service_id,
        service_order_id=service_order_id,
        commission_type=scale,
        discount_type=comm_type,
        amt=amt,
        amt_w_nds=amt * NDS,
    )


CONS_YT_COLUMNS = [
    yt_type("contract_id", "int64"),
    yt_type("contract_eid", "string"),
    yt_type("agency_id", "int64"),
    yt_type("commission_type", "int64"),
    yt_type("contract_from_dt", "string"),
    yt_type("contract_till_dt", "string"),
    yt_type("start_dt", "string"),
    yt_type("finish_dt", "string"),
    yt_type("sign_dt", "string"),
    yt_type("linked_contract_id", "int64"),
    yt_type("linked_agency_id", "int64"),
    yt_type("cons_type", "int64"),
]


def new_cons(
    contract_id: int,
    agency_id: int,
    linked_contract_id: int,
    linked_agency_id: int,
    cons_type: int = ContractJoinType.Main,
    comm_type=CommType.Direct,
) -> Dict:
    """
    Акт за прошлый месяц для выгрузки в YT
    """
    ld = prev_month_from_dt()
    dt_fmt = '%Y-%m-%d %H:%M:%S'
    return dict(
        contract_id=contract_id,
        contract_eid='C-{}'.format(contract_id),
        agency_id=agency_id,
        commission_type=comm_type.value,
        contract_from_dt=datetime.datetime(ld.year, 1, 1).strftime(dt_fmt),
        contract_till_dt=datetime.datetime(ld.year, 12, 31, 23, 59, 59).strftime(dt_fmt),
        start_dt=datetime.datetime(ld.year, 1, 1).strftime(dt_fmt),
        finish_dt=datetime.datetime(ld.year, 12, 31, 23, 59, 59).strftime(dt_fmt),
        sign_dt=datetime.datetime(ld.year, 1, 1).strftime(dt_fmt),
        linked_contract_id=linked_contract_id,
        linked_agency_id=linked_agency_id,
        cons_type=cons_type,
    )


def new_reward(
    contract_id: int,
    reward: float = 0,
    amt: float = 0,
    comm_type: CommType = CommType.Direct,
    currency: str = 'RUR',
    invoice_cnt: int = 0,
    invoice_prep_cnt: int = 0,
    **kwargs,
) -> Dict:
    """
    Строчка вознаграждения
    """
    r = dict(
        contract_id=contract_id,
        contract_eid='C-{}'.format(contract_id),
        discount_type=comm_type.value,
        currency=currency,
        amt=amt,
        reward=reward,
        invoice_cnt=invoice_cnt,
        invoice_prep_cnt=invoice_prep_cnt,
    )
    r.update(kwargs)
    return r


REWARD_YT_COLUMNS = [
    yt_type("contract_id", "int64"),
    yt_type("contract_eid", "string"),
    yt_type("discount_type", "int64"),
    yt_type("invoice_cnt", "int64"),
    yt_type("invoice_prep_cnt", "int64"),
    yt_type("currency", "utf8"),
    yt_type("amt", "double"),
    yt_type("reward", "double"),
]
# yt_type("delkredere",           "double"),


@lru_cache()
def get_bunker_calc(
    calc_path: str, calc_class: Union[Type[BunkerCalc], Type[CashBackCalc]] = BunkerCalc
) -> Union[BunkerCalc, CashBackCalc]:
    """
    Конфиг берем из бункера, чтобы пути не хардкодить.
    Заодно кэшируем, чтобы несколько раз не ходить.
    """
    bunker_client = prepare_bunker_client('dev', 'agency-rewards', '/calc', False, False)
    short_name = calc_path.split('/')[-1]
    insert_dt = datetime.datetime.now()
    return calc_class(
        bunker_client.cat(calc_path, bunker_client.version_type),
        bunker_client.env,
        insert_dt,
        short_name,
        calc_path,
        client_testing=False,
        forecast=False,
        prod_testing=False,
        platform_run_dt=insert_dt,
        version_type=bunker_client.version_type,
    )


def id(_id):
    """
    Генератор уникальных ID для сущностей.
    Т.к. генерация тестовых данных и прогон тестов разделены по времени,
    то надо как-то сообщать им, какие ID надо использовать в работе.
     Через переменную окружения YB_AR_ID передается первый ID,
    от которого можно генерировать новые id.
    :param _id: относительный ID
    :return: абсоблютный ID
    """
    test_id = int(os.getenv('YB_AR_ID', '1'))
    return test_id + _id


def prev_month_from_dt(dt=None):
    dt = dt or datetime.datetime.now()
    return get_first_dt_prev_month(dt)


def prev_month_till_dt(dt=None, zero_seconds=0):
    dt = dt or datetime.datetime.now()
    return get_last_dt_prev_month(dt, zero_seconds)


def act(
    contract_id,
    invoice_id,
    amt,
    client_id=None,
    agency_id=None,
    currency=None,
    invoice_type=InvoiceType.prepayment,
    scale=None,
    contract_till_dt=None,
    ct=None,
    from_dt=None,
    till_dt=None,
    act_dt=None,
    brand_id=None,
    payment_control_type=0,
    invoice_dt=None,
):
    return {
        'contract_id': contract_id,
        'contract_eid': 'C-{}'.format(contract_id),
        'invoice_id': invoice_id,
        'invoice_dt': invoice_dt or datetime.datetime.now(),
        'invoice_type': invoice_type,
        'currency': currency or 'RUR',
        'nds': 1,
        'discount_type': ct or CT.Direct,
        'client_id': client_id or 123,
        'brand_id': brand_id or client_id or 123,
        'from_dt': from_dt or prev_month_from_dt(),
        'till_dt': till_dt or prev_month_till_dt(),
        'amt': Decimal(amt),
        'amt_w_nds': Decimal(amt) * Decimal('1.18'),
        'commission_type': scale or ARScale.BaseLight,
        'agency_id': agency_id or 1,
        'act_dt': act_dt or prev_month_from_dt(),
        'payment_control_type': payment_control_type,
        'contract_till_dt': contract_till_dt or datetime.datetime.now(),
    }


def payment(
    contract_id,
    invoice_id,
    amt,
    client_id=None,
    invoice_type=InvoiceType.prepayment,
    scale=None,
    invoice_ttl_sum=0,
    ct=CT.Direct,
    from_dt=None,
    till_dt=None,
    act_dt=None,
    is_fully_paid=0,
    is_early_paid=0,
    payment_control_type=0,
    invoice_dt=None,
):
    return {
        'contract_id': contract_id,
        'contract_eid': 'C-{}'.format(contract_id),
        'invoice_id': invoice_id,
        'invoice_dt': invoice_dt or datetime.datetime.now(),
        'invoice_type': invoice_type,
        'currency': 'RUR',
        'nds': 1,
        'discount_type': ct,
        'client_id': client_id or 123,
        'from_dt': from_dt or prev_month_from_dt(),
        'till_dt': till_dt or prev_month_till_dt(),
        'amt': Decimal(amt),
        'amt_w_nds': Decimal(amt) * Decimal('1.18'),
        'commission_type': scale or ARScale.BaseLight,
        'act_dt': act_dt or prev_month_from_dt(),
        'is_fully_paid': is_fully_paid,
        'is_early_payment_true': is_early_paid,
        'payment_control_type': payment_control_type,
        'invoice_total_sum': invoice_ttl_sum,
        'invoice_total_sum_w_nds': invoice_ttl_sum * Decimal('1.2'),
    }


def reward(
    contract_id,
    amt=0,
    client_id=None,
    reward_type=RewardType.MonthActs,
    invoice_type=InvoiceType.prepayment,
    scale=None,
    ct=None,
    from_dt=None,
    till_dt=None,
    act_dt=None,
    insert_dt=None,
    reward_to_pay=0,
    reward_to_charge=0,
    turnover_to_charge=0,
    turnover_to_pay=0,
    delkredere_to_charge=0,
    delkredere_to_pay=0,
    currency=Currency.RUR,
    calc: Optional[str] = None,
):
    return {
        'contract_id': contract_id,
        'contract_eid': 'C-{}'.format(contract_id),
        'invoice_dt': prev_month_till_dt(),
        'invoice_type': invoice_type,
        'currency': currency,
        'nds': 1,
        'discount_type': ct or CT.Direct,
        'client_id': client_id or 123,
        'from_dt': from_dt or prev_month_from_dt(),
        'till_dt': till_dt or prev_month_till_dt(),
        'amt': Decimal(amt),
        'amt_w_nds': Decimal(amt) * Decimal('1.18'),
        'commission_type': scale or ARScale.BaseLight,
        'act_dt': act_dt or prev_month_from_dt(),
        'insert_dt': insert_dt,
        'reward_type': reward_type,
        'turnover_to_charge': turnover_to_charge,
        'turnover_to_pay': turnover_to_pay,
        'reward_to_charge': reward_to_charge,
        'reward_to_pay': reward_to_pay,
        'delkredere_to_charge': delkredere_to_charge,
        'delkredere_to_pay': delkredere_to_pay,
        'calc': calc,
    }


def invoice_payment(
    invoice_id, amt=0, is_early_payment=0, reward=0, scale=None, ct=None, paid_dt=None, paid_till_dt=None
):
    """
    Оплаты по счетам у договоров, у которых разница между
    агентской кредитной линией и индивидуальной кредитной линией больше месяца
    в таблицу t_ar_invoice_payments
    """
    return {
        'invoice_id': invoice_id,
        'commission_type': ct or CT.Direct,
        'from_dt': paid_dt or prev_month_from_dt(),
        'till_dt': paid_till_dt or prev_month_till_dt(),
        'scale': scale,
        'is_early_payment_true': is_early_payment,
        'reward': reward,
    }


def commission_correction(
    *,
    contract_id: int,
    contract_eid: Optional[str] = None,
    type: str = 'correction',
    from_dt: Optional[datetime.datetime] = None,
    till_dt: Optional[datetime.datetime] = None,
    currency: Currency = Currency.RUR,
    reward_to_charge: Optional[Decimal] = Decimal(),
    delkredere_to_charge: Optional[Decimal] = Decimal(),
    dkv_to_charge: Optional[Decimal] = Decimal(),
    reward_to_pay: Optional[Decimal] = Decimal(),
    reward_to_pay_src: Optional[Decimal] = Decimal(),
    delkredere_to_pay: Optional[Decimal] = Decimal(),
    dkv_to_pay: Optional[Decimal] = Decimal(),
    dsc: Optional[str] = None,
    turnover_to_charge: Optional[Decimal] = Decimal(),
    turnover_to_pay: Optional[Decimal] = Decimal(),
    turnover_to_pay_w_nds: Optional[Decimal] = Decimal(),
    nds: int = 1,
    reward_type: Optional[RewardType] = RewardType.MonthActs,
    discount_type: Optional[CommType] = CommType.Direct,
) -> dict:
    """
    Возвращает словарь для вставки в таблицу t_commission_correction.
    """
    return {
        'contract_id': contract_id,
        'contract_eid': contract_eid or 'C-{}'.format(contract_id),
        'type': type,
        'from_dt': from_dt or prev_month_from_dt(),
        'till_dt': till_dt or prev_month_till_dt(),
        'currency': currency,
        'reward_to_charge': reward_to_charge,
        'delkredere_to_charge': delkredere_to_charge,
        'dkv_to_charge': dkv_to_charge,
        'reward_to_pay': reward_to_pay,
        'reward_to_pay_src': reward_to_pay_src,
        'delkredere_to_pay': delkredere_to_pay,
        'dkv_to_pay': dkv_to_pay,
        'dsc': dsc,
        'turnover_to_charge': turnover_to_charge,
        'turnover_to_pay': turnover_to_pay,
        'turnover_to_pay_w_nds': turnover_to_pay_w_nds,
        'nds': nds,
        'reward_type': reward_type,
        'discount_type': discount_type.value if discount_type is not None else None,
    }


class TestBase(unittest.TestCase):

    # Чтобы diff'ы видеть полностью
    maxDiff = None

    counter = itertools.count()
    last_id = 0
    pickle_name_datetime_prefixes = ('insert_dt',)
    pickle_name_prefixes = (
        'contract',
        'client',
        'agency',
        'domain',
        'invoice',
        'service_order',
    ) + pickle_name_datetime_prefixes

    # начало и конец месяца, который расчитываем
    from_dt = prev_month_from_dt()
    till_dt = prev_month_till_dt()

    from_dt_after_1m = add_months(from_dt, 1)
    till_dt_after_1m = get_last_dt_prev_month(add_months(from_dt, 2), 0)

    from_dt_after_2m = add_months(from_dt, 2)
    till_dt_after_2m = get_last_dt_prev_month(add_months(till_dt, 3), 0)

    from_dt_after_3m = add_months(from_dt, 3)
    till_dt_after_3m = get_last_dt_prev_month(add_months(till_dt, 4), 0)

    from_dt_after_4m = add_months(from_dt, 4)
    till_dt_after_4m = get_last_dt_prev_month(add_months(till_dt, 5), 0)

    from_dt_after_5m = add_months(from_dt, 5)
    till_dt_after_5m = get_last_dt_prev_month(add_months(till_dt, 6), 0)

    from_dt_1m_ago = prev_month_from_dt(from_dt)
    till_dt_1m_ago = prev_month_till_dt(till_dt)

    from_dt_2m_ago = prev_month_from_dt(from_dt_1m_ago)
    till_dt_2m_ago = prev_month_till_dt(from_dt_1m_ago)

    @staticmethod
    def get_previous_q_first_month_ranges(dt):
        from_dt = get_previous_quarter_first_day(dt)
        till_dt = datetime.datetime(from_dt.year + from_dt.month // 12, from_dt.month % 12 + 1, 1) - datetime.timedelta(
            seconds=1
        )
        return from_dt, till_dt

    @staticmethod
    def get_previous_q_mid_month_ranges(dt):
        from_dt = get_previous_quarter_first_day(dt)
        from_dt = datetime.datetime(from_dt.year + from_dt.month // 12, from_dt.month % 12 + 1, 1)
        till_dt = datetime.datetime(from_dt.year + from_dt.month // 12, from_dt.month % 12 + 1, 1) - datetime.timedelta(
            seconds=1
        )
        return from_dt, till_dt

    @staticmethod
    def get_previous_q_last_month_ranges(dt):
        till_dt = get_previous_quarter_last_day(dt)
        from_dt = datetime.datetime(till_dt.year, till_dt.month, 1)
        return from_dt, till_dt

    @classmethod
    def next_id(cls):
        cls.last_id = next(cls.counter)
        return id(cls.last_id)

    @classmethod
    def setUpClass(cls):
        cls.app = Application()
        cls.session = cls.app.new_session(database_id='meta')

    def get_last_run_id(self, model: sa.Table = None) -> int:
        if model is None:
            model = runs
        id_statement = sa.select([sa.func.max(model.c.id)])
        return self.session.execute(id_statement).fetchone()[0]

    def get_last_insert_dt(self, model: sa.Table = None) -> datetime.datetime:
        if model is None:
            model = runs

        insert_dt_stmt = sa.select([sa.func.max(model.c.insert_dt)])
        return self.session.execute(insert_dt_stmt).fetchone()[0]

    @staticmethod
    def get_insert_dt() -> datetime.datetime:
        d = os.getenv('YA_AR_INSERT_DT')
        return datetime.datetime.strptime(d, '%Y.%m.%d %H:%M:%S')

    @classmethod
    def pickle_data(cls, session):
        module_name = cls.__module__.split('.')[-1]
        class_name = cls.__name__

        def prepare_value(value):
            if isinstance(value, datetime.datetime):
                value = value.timestamp()
            return str(value)

        insert_data = [
            dict(module_name=module_name, class_name=class_name, field=name, value=prepare_value(value))
            for name, value in cls.__dict__.items()
            if name.startswith(cls.pickle_name_prefixes)
        ]
        if len(insert_data) > 0:
            session.execute(regtest_data.insert(), insert_data)

    @classmethod
    def load_pickled_data(cls, session):
        module_name = cls.__module__.split('.')[-1][5:]  # префикс test не нужен
        class_name = cls.__name__
        saved_data = session.execute(
            sa.select([regtest_data.c.field, regtest_data.c.value]).where(
                sa.and_(regtest_data.c.module_name == module_name, regtest_data.c.class_name == class_name)
            )
        )
        for k, v in saved_data:
            try:
                if k.startswith(cls.pickle_name_datetime_prefixes):
                    v = datetime.datetime.fromtimestamp(float(v))
                else:
                    if '.' in v:
                        v = float(v)
                    v = int(v)
            except ValueError:
                pass
            setattr(cls, k, v)

    @classmethod
    def prev_month_from_dt(cls, dt=None):
        dt = dt or datetime.datetime.now()
        return get_first_dt_prev_month(dt)

    @classmethod
    def prev_month_till_dt(cls, dt=None, zero_seconds=0):
        dt = dt or datetime.datetime.now()
        return get_last_dt_prev_month(dt, zero_seconds)

    @classmethod
    def create_yt_tables(cls):
        yt_client = create_yt_client()
        direct_deal_notifications = '//home/balance/dev/yb-ar/direct_deal_notifications'
        adfox_deals_dict = '//home/balance/dev/yb-ar/adfox_deals_dict'

        test_data = [
            {
                'schema': [
                    {"name": "deal_id", "type": "uint64", "required": False},
                    {"name": "creation_time", "type": "string", "required": False},
                    {"name": "client_notification_id", "type": "string", "required": False},
                ],
                'path': direct_deal_notifications,
            },
            {
                'schema': [
                    {"name": "dealExportId", "type": "uint64", "required": False},
                    {"name": "name", "type": "string", "required": False},
                    {"name": "agencyRevenueRatio", "type": "uint64", "required": False},
                    {"name": "type", "type": "uint64", "required": False},
                ],
                'path': adfox_deals_dict,
            },
        ]

        for dct in test_data:
            if yt_client.exists(dct['path']):
                yt_client.remove(dct['path'])

            yt_client.create('table', dct['path'], attributes={'schema': dct['schema']})

    @classmethod
    def setup_fixtures(cls, session: Session) -> None:
        """
        Фикстуры для БД Oracle.
        """
        pass

    @classmethod
    def setup_fixtures_ext(cls, session: Session, yt_client: YtClient, yql_client: YqlClient) -> None:
        """
        Фикстуры для YT.
        """
        pass


class BKPageType:
    """
    Тип страницы РСЯ
    """

    RSYA = 3
    NonRSYA = 1
