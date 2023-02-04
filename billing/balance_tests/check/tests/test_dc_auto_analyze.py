# coding: utf-8
__author__ = 'chihiro'
from datetime import datetime, timedelta
import time

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string, is_in, has_length

import balance.balance_db as db
from btestlib import utils as b_utils
from check.shared import CheckSharedBefore
from check import shared_steps
from check.db import insert_into_partner_completion_buffer
from check import utils


NOT_TODAY = (datetime.now() + timedelta(days=6)).strftime("%d.%m.%y 00:00:00")
TWO_MONTH_AGO = datetime.now() - relativedelta(months=2)
REVERSE_DATE = (datetime.now() - relativedelta(months=1)).replace(day=3)
ACT_DT = b_utils.Date.get_last_day_of_previous_month()
COMPLETION_DT = b_utils.Date.first_day_of_month(ACT_DT)


def get_place_id():
    return db.balance().execute("SELECT s_test_place_id.nextval place FROM dual")[0]['place']


class TestDcBK(object):
    COMPLETION_TYPE = 6
    PAGE_ID = 542
    SOURCE_ID = 1
    SHOWS = 113

    @pytest.mark.skip(reason='Will be fixed later. Unknown reason yet')
    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_shows_not_converge_revers_bk(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data', 'yt_reverse']) as before:
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            insert_into_partner_completion_buffer(
                place_id, self.PAGE_ID, self.COMPLETION_TYPE, self.SOURCE_ID, 9, 0, 0, 0, 0,
                date=TWO_MONTH_AGO.replace(second=0, minute=0, hour=0, microsecond=0))


            dc_data = {
                'place_id': place_id,
                'page_id': self.PAGE_ID,
                'completion_type': self.COMPLETION_TYPE,
                'shows': 8,
                'clicks': 0,
                'bucks': 0,
                'mbucks': 0,
                'hits': 0
            }

            event_time = int(time.mktime(TWO_MONTH_AGO.replace(minute=0, hour=0, second=0, microsecond=0).timetuple()))
            yt_reverse = {
                'pageid': str(place_id), 'placeid': str(self.PAGE_ID), 'eventtime': str(event_time),
                'unixtime': str(int(time.mktime(REVERSE_DATE.timetuple()))), 'typeid': '1',
                'options': "picture,commerce,flat-page,stationary-connection,autobudget,fast-phrase-price-cost,reach-frequency-got,bscount-responded",
                'partnerstatid': '100003074', 'tagid': '42', 'resourcetype': '0', 'fraudbits': '4194304',
                'countertype': '1',
                'oldeventcost': '0', 'neweventcost': '0'
            }

        cmp_data = shared_steps.SharedBlocks.run_dc_with_auto_analyze(
            shared_data, before, pytest.active_tests, begin_dt=TWO_MONTH_AGO.strftime('%Y-%m-%d')
        )
        cmp_data = cmp_data or shared_data.cache.get('cmp_data')
        cmp_id = cmp_data[0]['cmp_id']

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]

        b_utils.check_that(result, has_length(1))
        b_utils.check_that((place_id, 3), is_in(result))

        ticket = utils.get_check_ticket('dc', cmp_id)
        comments = list(ticket.comments.get_all())

        comment_text = utils.get_db_config_value('bk_reverses_rationale')

        if len(comments) is not 0:
            for comment in comments:
                if str(place_id) in comment.text:
                    b_utils.check_that(comment.text, contains_string(comment_text),
                                       u'Проверяем, что в комментарии содержится требуемый текст')
                    b_utils.check_that(comment.text, contains_string(u'Расходится shows'))
                    break
                else:
                    assert False, u'Требуемый комментарий авторазбора не найден'
        else:
            assert False, u'Ожидаем комментарии, но их нет'

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_DC)
    def test_dc_check_not_exist_product(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['place_id', 'source_id', 'dc_data']) as before:
            page_id = 123456
            before.validate()
            place_id = get_place_id()
            source_id = self.SOURCE_ID

            insert_into_partner_completion_buffer(
                place_id, page_id, self.COMPLETION_TYPE, source_id, self.SHOWS, 0, 0, 0, 0,
                date=COMPLETION_DT
            )

            dc_data = {
                'place_id': place_id,
                'page_id': page_id,
                'completion_type': self.COMPLETION_TYPE,
                'shows': self.SHOWS,
                'clicks': 0,
                'bucks': 0,
                'mbucks': 0,
                'hits': 0
            }


        cmp_data = shared_steps.SharedBlocks.run_dc_with_auto_analyze(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []
        cmp_id = cmp_data[0]['cmp_id']

        result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]

        b_utils.check_that((place_id, 2), is_in(result))

        comment_text = utils.get_db_config_value('dc_product_is_not_connected')

        ticket = utils.get_check_ticket('dc', cmp_id)
        for comment in ticket.comments.get_all():
            if str(page_id) in comment.text:
                b_utils.check_that(comment.text, contains_string(comment_text),
                                   u'Проверяем, что в комментарии содержится требуемый текст')
                break
            else:
                assert False, u'Требуемый комментарий авторазбора не найден'

