# coding: utf-8
__author__ = 'chihiro'
from datetime import datetime, timedelta
from decimal import Decimal
from random import randint

import pytest
import hamcrest
from dateutil.relativedelta import relativedelta

import balance.balance_db as db
from balance import balance_steps as steps
from btestlib import secrets, constants, utils as b_utils
from check.shared import CheckSharedBefore
from check import shared_steps, steps as check_steps
from check.db import insert_into_partner_completion_buffer, get_new_search_id
from check.steps import COMPLETION_DT
from btestlib.data import defaults
from balance.distribution.distribution_types import DistributionType
import yt.wrapper as yt
import yt.yson as yson

NOT_TODAY = (datetime.now() + timedelta(days=6)).strftime("%d.%m.%y 00:00:00")
TWO_MONTH_AGO = datetime.now() - relativedelta(months=2)
REVERSE_DATE = (datetime.now() - relativedelta(months=1)).replace(day=3)


def _create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "pageid",
            "type": "string"
        },
        {
            "name": "placeid",
            "type": "string"
        },
        {
            "name": "eventtime",
            "type": "string"
        },
        {
            "name": "unixtime",
            "type": "string"
        },
        {
            "name": "typeid",
            "type": "string"
        },
        {
            "name": "options",
            "type": "string"
        },
        {
            "name": "partnerstatid",
            "type": "string"
        },
        {
            "name": "tagid",
            "type": "string"
        },
        {
            "name": "resourcetype",
            "type": "string"
        },
        {
            "name": "fraudbits",
            "type": "string"
        },
        {
            "name": "countertype",
            "type": "string"
        },
        {
            "name": "oldeventcost",
            "type": "string"
        },
        {
            "name": "neweventcost",
            "type": "string"
        }
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def create_data_in_yt(data):
    client = yt.YtClient(config={
        'token': secrets.get_secret(*secrets.Tokens.YT_OAUTH_TOKEN),
        'proxy': {
            'url': 'hahn.yt.yandex.net'
        },
    })
    client.mkdir('//home/balance_reports/dcs/test/test_data', recursive=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/bs-undochevent-log', recursive=True)
    client.remove('//home/balance_reports/dcs/test/test_data/bs-undochevent-log/1d', recursive=True, force=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/bs-undochevent-log/1d', recursive=True)
    client.remove('//home/balance_reports/dcs/test/test_data/bs-undochevent-log/5m', recursive=True, force=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/bs-undochevent-log/5m', recursive=True)
    _create_table(client, '//home/balance_reports/dcs/test/test_data/bs-undochevent-log/5m/{}'.format(
        (datetime.now() + timedelta(hours=1)).strftime("%Y-%m-%dT%H:30:00")))
    path = '//home/balance_reports/dcs/test/test_data/bs-undochevent-log/1d/{}'.format(
        REVERSE_DATE.strftime("%Y-%m-%d"))
    _create_table(client, path)
    client.write_table(
        path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def get_place_id():
    return db.balance().execute("SELECT s_test_place_id.nextval place FROM dual")[0]['place']


class TestDcInstalls(object):
    COMPLETION_TYPE = DistributionType.INSTALLS
    PAGE_ID = 10001
    SOURCE_ID = 4
    SHOWS = COMPLETION_TYPE.default_amount
    DIFFS_COUNT = 20  # Тут 20 и ещё 1 из  test_dc_auto_analyze.py

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {'page_id': self.PAGE_ID, 'shows': self.SHOWS, 'place_id': place_id}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        assert place_id not in [row['place_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_external(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 1) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            dc_data = {'page_id': self.PAGE_ID, 'shows': self.SHOWS, 'place_id': place_id}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 2) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_shows_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {'page_id': self.PAGE_ID, 'shows': self.SHOWS * 2, 'place_id': place_id}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 3) in result

    # TODO: удалить, в установках нет кликов
    # @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    # def test_dc_clicks_not_converge(self, shared_data):
    #     with CheckSharedBefore(shared_data=shared_data,
    #                            cache_vars=['place_id', 'source_id', 'dc_data']) as before:
    #         before.validate()
    #         place_id = get_place_id()
    #         source_id = self.SOURCE_ID
    #
    #         insert_into_partner_completion_buffer(
    #             place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, self.SHOWS, 5, 0, 0, 0, date=NOT_TODAY)
    #         dc_data = {'page_id': self.PAGE_ID, 'shows': self.SHOWS, 'place_id': place_id}
    #
    #     cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
    #
    #     cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    #
    #     result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
    #     assert len(result) == 1
    #     assert (place_id, 4) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_diff_without_diff(self, shared_data):
        # расхождения менее 1% не учитываются. Подробнее в CHECK-2133
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            shows_new = int(self.SHOWS * Decimal('1.01') - 1)
            dc_data = {'page_id': self.PAGE_ID, 'shows': shows_new, 'place_id': place_id}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        assert place_id not in [row['place_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT


class TestDcPartners(object):

    # В рамках тикета https://st.yandex-team.ru/CHECK-2841 было разделение BK
    # на РСЯ (contract_type = PARTNERS) и Downloads (contract_type = DISTRIBUTION)
    # Тут тесты на проверку РСЯ. Тесты на Downloads гораздо ниже

    COMPLETION_TYPE = 6
    PAGE_ID = 542
    SOURCE_ID = 1
    SHOWS = 113

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_without_diff_partners(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()

            place_id = get_place_id()
            source_id = self.SOURCE_ID

            # Дублируем данные для проверки того, что импорт происходит только в одном месте
            entity_id = db.balance().execute('select s_tarification_entity.nextval as id_ from dual')[0]['id_']
            query = """
                insert into BO.T_TARIFICATION_ENTITY (id, product_id, key_num_1, key_num_2, key_num_3, key_num_4, key_num_5, key_num_6) 
                values (:entity_id, :product_id, :key_num_1, -1, -1, -1, -1, -1)
            """
            query_params = dict(entity_id=entity_id, product_id=self.PAGE_ID, key_num_1=place_id)
            db.balance().execute(query, query_params)

            query = """
                insert into bo.t_entity_completion values
                (:dt, :product_id, :entity_id, :src_id, :val_num_1, 0, 0, 0)
            """
            query_params = dict(dt=COMPLETION_DT, product_id=self.PAGE_ID, entity_id=entity_id, src_id=source_id, val_num_1=self.SHOWS)
            db.balance().execute(query, query_params)

            insert_into_partner_completion_buffer(
                place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, self.SHOWS, 0, 0, 0, 0, date=COMPLETION_DT)
            dc_data = {'place_id': place_id, 'page_id': self.PAGE_ID, 'completion_type': self.COMPLETION_TYPE,
                       'shows': self.SHOWS, 'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        assert place_id not in [row['place_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_external_partners(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            insert_into_partner_completion_buffer(
                place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, self.SHOWS, 0, 0, 0, 0, date=COMPLETION_DT)

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 1) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_billing_partners(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            dc_data = {'place_id': place_id, 'page_id': self.PAGE_ID, 'completion_type': self.COMPLETION_TYPE,
                       'shows': self.SHOWS, 'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 2) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_shows_not_converge_partners(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            insert_into_partner_completion_buffer(
                place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, self.SHOWS, 0, 0, 0, 0, date=COMPLETION_DT)
            dc_data = {'place_id': place_id, 'page_id': self.PAGE_ID, 'completion_type': self.COMPLETION_TYPE,
                       'shows': self.SHOWS + 10, 'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 3) in result

    # @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    # def test_dc_shows_not_converge_revers_partners(self, shared_data):
    #     with CheckSharedBefore(shared_data=shared_data,
    #                            cache_vars=['place_id', 'source_id']) as before:
    #         before.validate()
    #         place_id = get_place_id()
    #         source_id = self.SOURCE_ID
    #
    #         insert_into_partner_completion_buffer(
    #             place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, 9, 0, 0, 0, 0, date=NOT_TODAY)
    #
    #         self.create_data_in_partners(
    #             [{'place_id': place_id, 'page_id': self.PAGE_ID, 'completion_type': self.COMPLETION_TYPE, 'shows': 8,
    #               'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0}],
    #             file_date=TWO_MONTH_AGO
    #         )
    #         event_time = int(time.mktime(TWO_MONTH_AGO.replace(minute=0, hour=0, second=0, microsecond=0).timetuple()))
    #         create_data_in_yt([{
    #             'pageid': str(place_id), 'placeid': str(self.PAGE_ID), 'eventtime': str(event_time),
    #             'unixtime': str(int(time.mktime(REVERSE_DATE.timetuple()))), 'typeid': '1',
    #             'options': "picture,commerce,flat-page,stationary-connection,autobudget,fast-phrase-price-cost,reach-frequency-got,bscount-responded",
    #             'partnerstatid': '100003074', 'tagid': '42', 'resourcetype': '0', 'fraudbits': '4194304',
    #             'countertype': '1',
    #             'oldeventcost': '0', 'neweventcost': '0'
    #         }])
    #
    #     cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
    #
    #     cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    #
    #     result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
    #     assert len(result) == 1
    #     assert (place_id, 3) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_clicks_not_converge_partners(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            insert_into_partner_completion_buffer(
                place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, self.SHOWS, 5, 0, 0, 0, date=COMPLETION_DT)
            dc_data = {'place_id': place_id, 'page_id': self.PAGE_ID, 'completion_type': self.COMPLETION_TYPE,
                       'shows': self.SHOWS, 'clicks': 10, 'bucks': 0, 'mbucks': 0, 'hits': 0}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 4) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_bucks_not_converge_partners(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            insert_into_partner_completion_buffer(
                place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, self.SHOWS, 0, 13, 0, 0, date=COMPLETION_DT)
            dc_data = {'place_id': place_id, 'page_id': self.PAGE_ID, 'completion_type': self.COMPLETION_TYPE,
                       'shows': self.SHOWS, 'clicks': 0, 'bucks': 8, 'mbucks': 0, 'hits': 0}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 5) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_hits_not_converge_partners(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            insert_into_partner_completion_buffer(
                place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, self.SHOWS, 0, 0, 0, 0, date=COMPLETION_DT)
            dc_data = {'place_id': place_id, 'page_id': self.PAGE_ID, 'completion_type': self.COMPLETION_TYPE,
                       'shows': self.SHOWS, 'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 11}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert place_id not in [row['place_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_diff_without_diff_dc(self, shared_data):
        # расхождения менее 1% не учитываются. Подробнее в CHECK-2133
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            shows_new = round(Decimal(self.SHOWS) + Decimal(self.SHOWS) / Decimal('101'), 5)
            query = """
                            select
                            (abs( :1 - :2) / (( :1 + :2) / 2)) * 100 as val
                            from dual
                        """
            query_params = {'1': self.SHOWS, '2': shows_new}
            while Decimal(db.balance().execute(query, query_params)[0]['val']) > Decimal(1):
                shows_new -= Decimal('0.01')

            insert_into_partner_completion_buffer(
                place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, shows_new, 0, 0, 0, 0, date=COMPLETION_DT)
            dc_data = {'place_id': place_id, 'page_id': self.PAGE_ID, 'completion_type': self.COMPLETION_TYPE,
                       'shows': self.SHOWS, 'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        assert place_id not in [row['place_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_check_3304_page_id_filter(self, shared_data):
        """
        В задаче CHECK-3304 потребовалось фильтровать page_id = 1181.
        Создаем такую запись только в системе, проверяем что ничего не нашлось.
        """
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            dc_data = {'place_id': place_id, 'page_id': 1181, 'completion_type': self.COMPLETION_TYPE,
                       'shows': self.SHOWS, 'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        result = [row['place_id'] for row in cmp_data if row['place_id'] == place_id]
        b_utils.check_that(result, hamcrest.empty())


class TestDcTaxiDistr(object):
    COMPLETION_TYPE = DistributionType.TAXI_LUCKY_RIDE
    SOURCE_ID = COMPLETION_TYPE.source_id

    PLACE_ID = COMPLETION_TYPE.result_page_id
    SHOWS = defaults.Distribution.DEFAULT_REVSHARE_SHOWS
    NDS_PCT = constants.Nds.get_pct(constants.Nds.YANDEX_RESIDENT)
    BUCKS = Decimal(COMPLETION_TYPE.default_amount) * 30 / NDS_PCT / Decimal(COMPLETION_TYPE.units_type_rate)

    DATE = date = COMPLETION_DT
    DATE_STR = DATE.strftime('%Y-%m-%d')

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {
                'date': self.DATE_STR,
                'tag_id': tag_id,
                'place_id': self.PLACE_ID,
                'bucks': self.BUCKS,
                'shows': self.SHOWS,
            }

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        assert tag_id not in [row['tag_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_external(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 1
        assert (tag_id, 1) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            tag_id = get_new_search_id()
            source_id = self.SOURCE_ID

            dc_data = {'date': self.DATE_STR, 'tag_id': tag_id, 'place_id': self.PLACE_ID, 'bucks': self.BUCKS,
                       'shows': self.SHOWS}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 1
        assert (tag_id, 2) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_shows_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {
                'date': self.DATE_STR,
                'tag_id': tag_id,
                'place_id': self.COMPLETION_TYPE.result_page_id,
                'bucks': self.BUCKS,
                'shows': self.SHOWS ** 2
            }
        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 1
        assert (tag_id, 3) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_bucks_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {
                'date': self.DATE_STR,
                'tag_id': tag_id,
                'place_id': self.PLACE_ID,
                'bucks': self.BUCKS ** 2,
                'shows': self.SHOWS
            }

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 1
        assert (tag_id, 5) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_shows_bucks_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {
                'date': self.DATE_STR,
                'tag_id': tag_id,
                'place_id': self.PLACE_ID,
                'bucks': self.BUCKS ** 2,
                'shows': self.SHOWS ** 2
            }

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests,)
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['tag_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
        assert len(result) == 2
        assert (tag_id, 5) in result
        assert (tag_id, 3) in result


class TestDirectRevshare(object):
    COMPLETION_TYPE = DistributionType.DIRECT
    SOURCE_ID = 31
    CLICKS = defaults.Distribution.DEFAULT_REVSHARE_CLICKS
    SHOWS = defaults.Distribution.DEFAULT_REVSHARE_SHOWS
    BUCKS = COMPLETION_TYPE.default_amount
    DATE = COMPLETION_DT

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['tag_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {'date': self.DATE.strftime("%Y%m%d"), 'tag_id': tag_id,
                       'clicks': self.CLICKS, 'shows': self.SHOWS, 'bucks': self.BUCKS}

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        assert tag_id not in [row['tag_id'] for row in cmp_data]


class TestDcActivations(object):
    COMPLETION_TYPE = DistributionType.ACTIVATIONS
    SOURCE_ID = 8
    PAGE_ID = 3010
    SHOWS = COMPLETION_TYPE.default_amount
    # DATE = date = (datetime.now() + timedelta(days=6)).strftime("%Y-%m-%d")
    DATE = COMPLETION_DT
    DATE_STR = DATE.strftime('%Y-%m-%d')

    def format_data(self, place_id, shows):
        return {
            'billing_period': self.DATE_STR,
            'product_id': self.PAGE_ID,
            'clid': int(place_id),
            'vid': -1,
            'count': int(shows),
        }

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            source_id = self.SOURCE_ID

            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = self.format_data(place_id, self.SHOWS)

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        assert place_id not in [row['place_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_external(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id']) as before:
            before.validate()
            source_id = self.SOURCE_ID
            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 1) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()

            place_id = get_place_id()
            source_id = self.SOURCE_ID

            dc_data = self.format_data(place_id, self.SHOWS)

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 2) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_shows_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = self.format_data(place_id, self.SHOWS ** 2)

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 3) in result


class TestDcDownloads(object):
    COMPLETION_TYPE = DistributionType.DOWNLOADS
    SOURCE_ID = 101
    PAGE_ID = 909  # Один из большого количества
    SHOWS = COMPLETION_TYPE.default_amount

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {
                'place_id': place_id,
                'page_id': self.PAGE_ID,
                'shows': self.SHOWS,
                'completion_type': 0,
                'clicks': 0,
                'bucks': 0,
                'mbucks': 0,
                'hits': 0,
            }

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        assert place_id not in [row['place_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_external(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 1) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()

            place_id = get_place_id()
            source_id = self.SOURCE_ID

            dc_data = {
                'place_id': place_id,
                'page_id': self.PAGE_ID,
                'shows': self.SHOWS,
                'completion_type': 0,
                'clicks': 0,
                'bucks': 0,
                'mbucks': 0,
                'hits': 0,
            }

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 2) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_shows_not_converge(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            before.validate()

            source_id = self.SOURCE_ID
            place_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)

            dc_data = {
                'place_id': place_id,
                'page_id': self.PAGE_ID,
                'shows': self.SHOWS ** 2,
                'completion_type': 0,
                'clicks': 0,
                'bucks': 0,
                'mbucks': 0,
                'hits': 0,
            }

        cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
        assert len(result) == 1
        assert (place_id, 3) in result


# Отключили адаптеры: CHECK-3396
# class TestDcAdapter(object):
#     COMPLETION_TYPE = DistributionType.DEVELOPER_INCOME
#     SOURCE_ID = 26
#     PAGE_ID = 4012
#     HITS = COMPLETION_TYPE.default_amount
#     MONEY = (Decimal(COMPLETION_TYPE.default_amount) /
#              Decimal(COMPLETION_TYPE.units_type_rate) *
#              COMPLETION_TYPE.default_price)
#     DATE = COMPLETION_DT
#     DATE_STR = DATE.strftime('%Y-%m-%d')
#
#     @staticmethod
#     def create_completions():
#         completion_dt = (datetime.now() + timedelta(days=6)).replace(
#             hour=0, minute=0, second=0, microsecond=0)
#         distribution_type_dev = steps.DistributionType.DEVELOPER_INCOME
#         distribution_type_ret = steps.DistributionType.RETAIL_EXPENSES
#         search_id_dev = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']
#         search_id_ret = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']
#         steps.DistributionSteps.create_addapter_completion_row(search_id_dev, search_id_ret,
#                                                                distribution_type_dev, completion_dt)
#         steps.DistributionSteps.create_addapter_completion_row(search_id_ret, search_id_dev,
#                                                                distribution_type_ret, completion_dt)
#
#         distribution_type = steps.DistributionType.DEVELOPER_EXPENSES
#         distribution_type_2 = steps.DistributionType.RETAIL_INCOME
#         steps.DistributionSteps.create_addapter_completion_row(search_id_dev, search_id_ret,
#                                                                distribution_type, completion_dt,
#                                                                amount=distribution_type_dev.default_amount,
#                                                                price=distribution_type_dev.default_price)
#         steps.DistributionSteps.create_addapter_completion_row(search_id_ret, search_id_dev,
#                                                                distribution_type_2, completion_dt,
#                                                                amount=distribution_type_ret.default_amount,
#                                                                price=distribution_type_ret.default_price)
#         adapter_data = {
#             'dt': completion_dt.strftime("%Y-%m-%d"),
#             'clid_d': search_id_dev,
#             'clid_r': search_id_ret,
#             'money_d': distribution_type_dev.default_amount * distribution_type_dev.default_price,
#             'money_r': distribution_type_ret.default_amount * distribution_type_ret.default_price,
#             'installs': distribution_type_dev.default_amount}
#         return search_id_dev, search_id_ret, adapter_data
#
#     @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
#     def test_dc_without_diff(self, shared_data):
#         with CheckSharedBefore(shared_data=shared_data,
#                                cache_vars=['tag_id', 'dc_data', 'source_id']) as before:
#             before.validate()
#             source_id = self.SOURCE_ID
#             tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)
#
#             dc_data = {
#                 'dt': self.DATE_STR,
#                 'clid_d': tag_id,
#                 'clid_r': '',
#                 'money_d': self.MONEY,
#                 'money_r': '',
#                 'installs': self.HITS,
#             }
#
#         cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
#         cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []
#
#         assert tag_id not in [row['tag_id'] for row in cmp_data]
#
#     @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
#     def test_dc_not_found_in_adapter(self, shared_data):
#         with CheckSharedBefore(shared_data=shared_data,
#                                cache_vars=['tag_id', 'source_id']) as before:
#             before.validate()
#             source_id = self.SOURCE_ID
#             tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)
#
#         cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
#         cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []
#
#         result = [(row['tag_id'], row['page_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
#         assert len(result) == 1
#         assert (tag_id, self.PAGE_ID, 1) in result
#
#     @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
#     def test_dc_bucks_not_converge(self, shared_data):
#         with CheckSharedBefore(shared_data=shared_data,
#                                cache_vars=['tag_id', 'dc_data', 'source_id']) as before:
#             before.validate()
#
#             source_id = self.SOURCE_ID
#             tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)
#
#             dc_data = {
#                 'dt': self.DATE_STR,
#                 'clid_d': tag_id,
#                 'clid_r': '',
#                 'money_d': self.MONEY ** 2,
#                 'money_r': '',
#                 'installs': self.HITS,
#             }
#
#         cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
#         cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []
#
#         result = [(row['tag_id'], row['page_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
#         assert len(result) == 1
#         assert (tag_id, self.PAGE_ID, 5) in result
#
#     @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
#     def test_dc_hits_not_converge(self, shared_data):
#         with CheckSharedBefore(shared_data=shared_data,
#                                cache_vars=['tag_id', 'dc_data', 'source_id']) as before:
#             before.validate()
#
#             source_id = self.SOURCE_ID
#             tag_id = check_steps.create_distribution_completions(self.COMPLETION_TYPE)
#
#             dc_data = {
#                 'dt': self.DATE_STR,
#                 'clid_d': tag_id,
#                 'clid_r': '',
#                 'money_d': self.MONEY,
#                 'money_r': '',
#                 'installs': self.HITS ** 2,
#             }
#
#         cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
#         cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []
#
#         result = [(row['tag_id'], row['page_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
#         assert len(result) == 1
#         assert (tag_id, self.PAGE_ID, 6) in result
#
#     @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
#     def test_dc_not_found_in_billing(self, shared_data):
#         with CheckSharedBefore(shared_data=shared_data,
#                                cache_vars=['tag_id', 'dc_data', 'source_id']) as before:
#             before.validate()
#             source_id = self.SOURCE_ID
#             tag_id = randint(1000000, 9999999)
#
#             dc_data = {
#                 'dt': self.DATE_STR,
#                 'clid_d': tag_id,
#                 'clid_r': '',
#                 'money_d': self.MONEY,
#                 'money_r': '',
#                 'installs': self.HITS,
#             }
#
#         cmp_data = shared_steps.SharedBlocks.run_dc(shared_data, before, pytest.active_tests)
#         cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []
#
#         result = [(row['tag_id'], row['page_id'], row['state']) for row in cmp_data if row['tag_id'] == tag_id]
#         assert len(result) == 1
#         assert (tag_id, self.PAGE_ID, 2) in result

