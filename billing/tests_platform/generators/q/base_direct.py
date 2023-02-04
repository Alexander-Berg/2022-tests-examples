"""
    Тесты для квартальной премии по базовым договрам за Директ
    BALANCE-31015
"""

from datetime import datetime

from dateutil.relativedelta import relativedelta

from agency_rewards.rewards.utils.const import Scale
from agency_rewards.rewards.utils.dates import get_previous_quarter_first_day
from agency_rewards.rewards.utils.yql_crutches import export_to_yt

from billing.agency_rewards.tests_platform.common import TestBase, BKPageType
from ...common import yt_base_path, act_div_fmt, sales_fmt, act_fmt
from ...common import fmt_path, act_page_fmt
from ...common import PAGES_YT_COLUMNS, new_page
from ...common import PAGES_ACT_YT_COLUMNS, new_page_act
from ...common import ACT_DIV_YT_COLUMNS, new_act_div
from ...common import SALES_YT_COLUMNS, new_sale
from ...common import ACTS_YT_COLUMNS, new_act


class TestBaseLowerBounds(TestBase):
    """
    Проверка квартального расчета по Базовым за Директ
    Проверяем корректность нижней границы шкалы.

    BALANCE-31803

    За основу взят TestProfQDirect

    Прирост по аг-ву считается по sales_daily + [act_div] + act_by_page
    Оборот по договору - по acts + [act_div] + act_by_page
    """

    domain_rsya = TestBase.next_id()
    domain_other = TestBase.next_id()

    service_order_1 = TestBase.next_id()

    contract_1 = TestBase.next_id()
    agency_1 = TestBase.next_id()
    act_1 = TestBase.next_id()
    act_2 = TestBase.next_id()
    act_3 = TestBase.next_id()

    client_1 = TestBase.next_id()
    client_2 = TestBase.next_id()
    client_3 = TestBase.next_id()
    client_4 = TestBase.next_id()
    client_5 = TestBase.next_id()

    contract_turnover = 5000.0
    # Ниже через страницы сконфигурировали, что доля РСЯ - 25%.
    # 5k * 0.25 = 1.25k
    contract_turnover_rsya = 1250.0

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        """
        act_div не используем. только лишь создаем таблицу.
        это распил по общему счету. его может и не быть.
        """

        dt = get_previous_quarter_first_day(datetime.now())
        dt_prev = dt.replace(dt.year - 1)

        #
        # act_div просто создаем пустые таблицы
        # (имитировать общий счет не будем)
        #
        for ad_dt in (dt, dt_prev):
            export_to_yt(
                yt_client,
                fmt_path('group_order_act_div', ad_dt, act_div_fmt),
                lambda: (new_act_div(1, 2, 3),),
                ACT_DIV_YT_COLUMNS,
            )

        #
        # 2 страницы. Одна из них - РСЯ
        #
        export_to_yt(
            yt_client,
            f"{yt_base_path}/page",
            lambda: [new_page(cls.domain_rsya, BKPageType.RSYA), new_page(cls.domain_other, BKPageType.NonRSYA)],
            PAGES_YT_COLUMNS,
        )

        #
        # Акты по страницам (нужны при джойне с sales_daily & acts, чтобы
        # иметь статистику, сколько на РСЯ ушло)
        # по заказу будет 25% РСЯ в текущем и прошлом кварталх
        #
        for ap_dt in (dt, dt_prev):
            export_to_yt(
                yt_client,
                fmt_path('act_by_page', ap_dt, act_page_fmt),
                lambda: [
                    new_page_act(cls.service_order_1, cls.domain_rsya, 100.0),
                    new_page_act(cls.service_order_1, cls.domain_other, 300.0),
                ],
                PAGES_ACT_YT_COLUMNS,
            )

        #
        # Данные с учетом оферты (для расчета прироста аг-ва)
        # sales_daily
        # Делаем прирост по аг-ву - 31% (100k -> 131k)
        #
        def create_yt_sale(dt, act, amt=5_000.0):
            export_to_yt(
                yt_client,
                fmt_path('f_sales_daily', dt, sales_fmt),
                lambda: [new_sale(cls.agency_1, act, cls.service_order_1, amt, dt.month)],
                SALES_YT_COLUMNS,
            )

        create_yt_sale(dt, cls.act_1, 131_000.0)

        # вставляем данные в каждый месяц квартала,
        # чтобы не было штрафа за кол-во месяцев
        create_yt_sale(dt_prev, cls.act_1, 50_000.0)
        create_yt_sale(dt_prev + relativedelta(months=1), cls.act_2, 30_000.0)
        create_yt_sale(dt_prev + relativedelta(months=2), cls.act_3, 20_000.0)

        #
        # Акты (для расчета оброта договора)
        # Премия будет считаться от РСЯ части указанного оборота,
        # что будет составлять 25%
        #
        export_to_yt(
            yt_client,
            fmt_path('acts_q_base_direct', dt, act_fmt),
            lambda: [
                new_act(
                    cls.contract_1,
                    cls.agency_1,
                    cls.service_order_1,
                    cls.act_1,
                    cls.contract_turnover / 5,
                    Scale.BaseMsk.value,
                    brand_id=cls.client_1,
                ),
                new_act(
                    cls.contract_1,
                    cls.agency_1,
                    cls.service_order_1,
                    cls.act_1,
                    cls.contract_turnover / 5,
                    Scale.BaseMsk.value,
                    brand_id=cls.client_2,
                ),
                new_act(
                    cls.contract_1,
                    cls.agency_1,
                    cls.service_order_1,
                    cls.act_1,
                    cls.contract_turnover / 5,
                    Scale.BaseMsk.value,
                    brand_id=cls.client_3,
                ),
                new_act(
                    cls.contract_1,
                    cls.agency_1,
                    cls.service_order_1,
                    cls.act_1,
                    cls.contract_turnover / 5,
                    Scale.BaseMsk.value,
                    brand_id=cls.client_4,
                ),
                new_act(
                    cls.contract_1,
                    cls.agency_1,
                    cls.service_order_1,
                    cls.act_1,
                    cls.contract_turnover / 5,
                    Scale.BaseMsk.value,
                    brand_id=cls.client_5,
                ),
            ],
            ACTS_YT_COLUMNS,
        )
