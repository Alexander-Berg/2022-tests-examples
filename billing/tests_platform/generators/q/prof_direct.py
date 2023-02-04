"""
    Тесты для квартальной премии по профам за Директ
    BALANCE-31015t s
"""

from datetime import datetime

from dateutil.relativedelta import relativedelta

from agency_rewards.rewards.utils.const import CommType
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
from ...common import CONS_YT_COLUMNS, new_cons


class TestProfQDirect(TestBase):
    """
    Проверка квартального расчета по Профам за Директ

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
        # Делаем прирост по аг-ву - 50% (10k -> 15k)
        #
        def create_yt_sale(dt, act, amt=5_000.0):
            export_to_yt(
                yt_client,
                fmt_path('f_sales_daily', dt, sales_fmt),
                lambda: [new_sale(cls.agency_1, act, cls.service_order_1, amt, dt.month)],
                SALES_YT_COLUMNS,
            )

        create_yt_sale(dt, cls.act_1, 22_500.0)

        # вставляем данные в каждый месяц квартала, чтобы БОК
        create_yt_sale(dt_prev, cls.act_1)
        create_yt_sale(dt_prev + relativedelta(months=1), cls.act_2)
        create_yt_sale(dt_prev + relativedelta(months=2), cls.act_3)

        #
        # Акты (для расчета оброта договора)
        # Премия будет считаться от РСЯ части указанного оборота,
        # что будет составлять 25%
        #
        export_to_yt(
            yt_client,
            fmt_path('acts_q_prof_direct', dt, act_fmt),
            lambda: [
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=1
                ),
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=2
                ),
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=3
                ),
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=4
                ),
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=5
                ),
            ],
            ACTS_YT_COLUMNS,
        )

        # Фейк объединения
        export_to_yt(
            yt_client,
            fmt_path('consolidations_q', dt, act_fmt),
            lambda: [
                new_cons(1, 2, 3, 4),
            ],
            CONS_YT_COLUMNS,
        )


class TestProfQDirectAllMonthsData(TestBase):
    """
    Проверка оборота во всех месяцах квартала по Профам за Директ

    За основу взят тест TestProfQDirect

    Прирост по аг-ву считается по sales_daily + [act_div] + act_by_page
    Оборот по договору - по acts + [act_div] + act_by_page
    """

    domain_rsya = TestBase.next_id()
    domain_other = TestBase.next_id()

    service_order_1 = TestBase.next_id()
    service_order_2 = TestBase.next_id()

    contract_1 = TestBase.next_id()
    agency_1 = TestBase.next_id()
    act_1 = TestBase.next_id()
    act_2 = TestBase.next_id()
    act_3 = TestBase.next_id()

    contract_turnover = 1000.0
    # В тесте будет такое распредение, что РСЯ будет составлять 25%.
    # То есть 25% от contract_turnover = 250
    contract_turnover_rsya = 250.0

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        """
        act_div не используем. только лишь создаем таблицу.
        это распил по общему счету. его может и не быть.
        """

        dt = get_previous_quarter_first_day(datetime.now())
        dt_prev = dt.replace(dt.year - 1)

        #
        # act_div создавать не будем, т.к он был создан в TestProfQDirect
        # (имитировать общий счет не будем)
        #

        #
        # 2 страницы. Одна из них - РСЯ
        #
        export_to_yt(
            yt_client,
            f"{yt_base_path}/page",
            lambda: [new_page(cls.domain_rsya, BKPageType.RSYA), new_page(cls.domain_other, BKPageType.NonRSYA)],
            PAGES_YT_COLUMNS,
            False,
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
                    new_page_act(cls.service_order_2, cls.domain_other, 300.0),
                ],
                PAGES_ACT_YT_COLUMNS,
                False,
            )

        # Т.к. данные по РСЯ только за 1 месяц квартала, то случится 0 премия
        # остальные 2 месяцв - не РСЯ
        def create_yt_sale(dt, act, order, amt=5_000.0, trunc=False):
            export_to_yt(
                yt_client,
                fmt_path('f_sales_daily', dt, sales_fmt),
                lambda: [new_sale(cls.agency_1, act, order, amt, dt.month)],
                SALES_YT_COLUMNS,
                trunc,
            )

        create_yt_sale(dt, cls.act_1, cls.service_order_1, trunc=False)
        create_yt_sale(dt + relativedelta(months=1), cls.act_2, cls.service_order_2, trunc=True)
        create_yt_sale(dt + relativedelta(months=2), cls.act_3, cls.service_order_2, trunc=True)

        # вставляем данные только в 1 месяц квартала, чтобы получить
        # штраф за кол-во месяцев квартала, в котороы
        create_yt_sale(dt_prev, cls.act_1, cls.service_order_1, amt=10_000.0)

        #
        # Акты (для расчета оброта договора)
        # Премия будет считаться от РСЯ части указанного оборота,
        # что будет составлять 25%
        #
        export_to_yt(
            yt_client,
            fmt_path('acts_q_prof_direct', dt, act_fmt),
            lambda: [
                new_act(cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover),
            ],
            ACTS_YT_COLUMNS,
            False,
        )


class TestProfQAccountDirectOnly(TestBase):
    """
    Проверка, что при приросте учитываем только Директ
    (охватный продукт не учитываем, ТК=37)

    За основу взят тест TestProfQDirect

    Прирост по аг-ву считается по sales_daily + [act_div] + act_by_page
    Оборот по договору - по acts + [act_div] + act_by_page
    """

    domain_rsya = TestBase.next_id()
    domain_other = TestBase.next_id()

    service_order_1 = TestBase.next_id()
    service_order_2 = TestBase.next_id()

    contract_1 = TestBase.next_id()
    agency_1 = TestBase.next_id()
    act_1 = TestBase.next_id()
    act_2 = TestBase.next_id()
    act_3 = TestBase.next_id()

    contract_turnover = 10000.0
    # В тесте будет такое распредение, что РСЯ будет составлять 25%.
    # То есть 25% от contract_turnover = 250
    contract_turnover_rsya = 2500

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        """
        act_div не используем. только лишь создаем таблицу.
        это распил по общему счету. его может и не быть.
        """

        dt = get_previous_quarter_first_day(datetime.now())
        dt_prev = dt.replace(dt.year - 1)

        #
        # act_div создавать не будем, т.к он был создан в TestProfQDirect
        # (имитировать общий счет не будем)
        #

        #
        # 2 страницы. Одна из них - РСЯ
        #
        export_to_yt(
            yt_client,
            f"{yt_base_path}/page",
            lambda: [new_page(cls.domain_rsya, BKPageType.RSYA), new_page(cls.domain_other, BKPageType.NonRSYA)],
            PAGES_YT_COLUMNS,
            False,
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
                    new_page_act(cls.service_order_2, cls.domain_other, 300.0),
                ],
                PAGES_ACT_YT_COLUMNS,
                False,
            )

        # Кладем данные по 7 и 37 ТК, но прирост считается только от 7
        # Прирост по Директу 5к --> 10k
        def create_yt_sale(dt, act, amt=5_000.0, comm_type=CommType.Direct.value):
            export_to_yt(
                yt_client,
                fmt_path('f_sales_daily', dt, sales_fmt),
                lambda: [new_sale(cls.agency_1, act, cls.service_order_1, amt, dt.month, commission_type=comm_type)],
                SALES_YT_COLUMNS,
                False,
            )

        create_yt_sale(dt, cls.act_1, comm_type=CommType.Direct.value)
        create_yt_sale(dt + relativedelta(months=1), cls.act_2, comm_type=CommType.Direct.value)
        create_yt_sale(dt + relativedelta(months=2), cls.act_3, comm_type=CommType.Direct.value)

        # вставляем данные в каждый месяц квартала,
        # чтобы не получить штраф за 3 месяца
        create_yt_sale(dt_prev, cls.act_1, comm_type=CommType.Direct.value)
        create_yt_sale(dt_prev + relativedelta(months=1), cls.act_2, amt=2_500.0, comm_type=CommType.Direct.value)
        create_yt_sale(
            dt_prev + relativedelta(months=2), cls.act_3, amt=1_000.0, comm_type=CommType.MediaInDirectUI.value
        )
        create_yt_sale(dt_prev + relativedelta(months=2), cls.act_3, amt=2_500.0, comm_type=CommType.Direct.value)

        #
        # Акты (для расчета оброта договора)
        # Премия будет считаться от РСЯ части указанного оборота,
        # что будет составлять 25%
        #
        export_to_yt(
            yt_client,
            fmt_path('acts_q_prof_direct', dt, act_fmt),
            lambda: [
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=11
                ),
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=21
                ),
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=31
                ),
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=41
                ),
                new_act(
                    cls.contract_1, cls.agency_1, cls.service_order_1, cls.act_1, cls.contract_turnover / 5, brand_id=51
                ),
            ],
            ACTS_YT_COLUMNS,
            False,
        )
