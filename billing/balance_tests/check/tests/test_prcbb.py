# coding: utf-8
__author__ = 'chihiro'
from datetime import datetime, timedelta

import pytest
import time
from dateutil.relativedelta import relativedelta

from hamcrest import contains_string

from balance.balance_db import balance
from check.shared import CheckSharedBefore
from check import shared_steps
from check import utils
from check.db import insert_into_partner_dsp_stat
import btestlib.reporter as reporter
from btestlib import utils as b_utils



YESTERDAY = (datetime.now() - timedelta(days=1)).replace(second=0, minute=0, hour=0, microsecond=0)


DATE = date = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
BLOCK_ID = 430426729
DSP_ID = 481516
MAX_PLACE_ID = None
DIFFS_COUNT = 5 # Сюда попадает расхождение из test_prcbb_revers.py


def get_place_id():
    place_id = int(balance().execute('select s_test_place_id.nextval place FROM dual')[0]['place'])
    reporter.log("Получаем 'place_id': {}".format(place_id))

    return place_id

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_prcbb_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['place_id', 'prcbb_data']) as before:
        before.validate()
        place_id = get_place_id()
        shows, dsp_charge, partner_reward = insert_into_partner_dsp_stat(
            place_id, BLOCK_ID, DSP_ID, date=YESTERDAY
        )
        prcbb_data = {
            'date': DATE, 'place_id': place_id, 'block_id': BLOCK_ID, 'dsp_id': DSP_ID,
            'dsp_charge': int(dsp_charge) * 1000000, 'partner_reward': int(partner_reward) * 1000000,
            'shows': shows
        }

    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    reporter.log("CMP_DATA: {}".format(cmp_data))  # удалить

    assert place_id not in [row['place_id'] for row in cmp_data]

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_prcbb_not_found_in_dsp(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['place_id']) as before:
        before.validate()
        place_id = get_place_id()
        shows, dsp_charge, partner_reward = insert_into_partner_dsp_stat(
            place_id, BLOCK_ID, DSP_ID, date=YESTERDAY
        )

    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
    assert len(result) == 1
    assert (place_id, 1) in result

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_prcbb_partner_reward_mismatch(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['place_id', 'prcbb_data']) as before:
        before.validate()
        place_id = get_place_id()
        shows, dsp_charge, partner_reward = insert_into_partner_dsp_stat(
            place_id, BLOCK_ID, DSP_ID, date=YESTERDAY
        )
        prcbb_data = {
            'date': DATE, 'place_id': place_id, 'block_id': BLOCK_ID, 'dsp_id': DSP_ID,
            'dsp_charge': int(dsp_charge) * 1000000, 'partner_reward': int(partner_reward * 1.5),
            'shows': shows
        }
    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
    assert len(result) == 1
    assert (place_id, 3) in result

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_prcbb_dsp_charge_mismatch(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['place_id', 'prcbb_data']) as before:
        before.validate()
        place_id = get_place_id()
        shows, dsp_charge, partner_reward = insert_into_partner_dsp_stat(
            place_id, BLOCK_ID, DSP_ID, date=YESTERDAY
        )
        prcbb_data = {
            'date': DATE, 'place_id': place_id, 'block_id': BLOCK_ID, 'dsp_id': DSP_ID,
            'dsp_charge': int(dsp_charge * 1.5), 'partner_reward': int(partner_reward) * 1000000,
            'shows': shows
        }
    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
    assert len(result) == 1
    assert (place_id, 4) in result

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_prcbb_little_mismatch(shared_data):
    # расхождения менее 1% не учитываются. Подробнее CHECK-2133
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['place_id', 'prcbb_data']) as before:
        before.validate()
        place_id = get_place_id()
        shows, dsp_charge, partner_reward = insert_into_partner_dsp_stat(
            place_id, BLOCK_ID, DSP_ID, date=YESTERDAY, partner_reward=15.9
        )
        prcbb_data = {
            'date': DATE, 'place_id': place_id, 'block_id': BLOCK_ID, 'dsp_id': DSP_ID,
            'dsp_charge': int(dsp_charge) * 1000000, 'partner_reward': 15000000,
            'shows': shows
        }

    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert place_id not in [row['place_id'] for row in cmp_data]

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_prcbb_not_found_in_billing(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['place_id', 'prcbb_data']) as before:
        before.validate()
        place_id = get_place_id()
        prcbb_data = {
            'date': DATE, 'place_id': place_id, 'block_id': BLOCK_ID, 'dsp_id': DSP_ID,
            'dsp_charge': int(9) * 1000000, 'partner_reward': int(10) * 1000000,
            'shows': 111
        }

    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
    assert len(result) == 1
    assert (place_id, 2) in result

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_prcbb_not_found_in_billing_wo_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['place_id']) as before:
        before.validate()
        place_id = 1234567899999

    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert place_id not in [row['place_id'] for row in cmp_data]




TWO_MONTH_AGO = datetime.now() - relativedelta(months=2)
REVERSE_DATE = (datetime.now() - relativedelta(months=1)).replace(day=3)

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_prcbb_check_revers(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['place_id', 'prcbb_data', 'yt_reverse']) as before:
        before.validate()
        event_time = TWO_MONTH_AGO.replace(second=0, minute=0, hour=0, microsecond=0)

        place_id = get_place_id()
        shows, dsp_charge, partner_reward = insert_into_partner_dsp_stat(
            place_id, BLOCK_ID, DSP_ID, date=event_time
        )
        yt_reverse = {"pageid": str(place_id), "eventtime": str(int(time.mktime(event_time.timetuple()))),
                      "unixtime": str(int(time.mktime(REVERSE_DATE.timetuple()))), "win": '1', "dspfraudbits": '11',
                      "dspeventflags": '0', "countertype": '1', "partnerprice": '2400000', 'price': '0'}

        prcbb_data = {
            'date': event_time.strftime("%Y-%m-%d"), 'place_id': place_id, 'block_id': BLOCK_ID,
            'dsp_id': DSP_ID, 'dsp_charge': int(dsp_charge) * 1000000,
            'partner_reward': int(partner_reward) * 1000000 - 2400000, 'shows': shows
        }


    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    cmp_id = None
    for row in cmp_data:
        if prcbb_data['date'] in str(row['stat_dt']) and 'cmp_id' in row:
            cmp_id = row['cmp_id']
            break

    result = [(row['place_id'], row['state']) for row in cmp_data if row['place_id'] == place_id]
    assert len(result) == 1
    assert (place_id, 3) in result

    # TODO раскомментировать после задачи CHECK-3037
    # ticket = utils.get_check_ticket("prcbb", cmp_id)
    # comments = list(ticket.comments.get_all())
    # b_utils.check_that(comments[0].text, contains_string(u'Расхождения вызваны откатами статистики'))
    # b_utils.check_that(comments[0].text, contains_string(u'Расходится partner_reward'))
    # b_utils.check_that(comments[0].text, contains_string(str(place_id)))




@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PRCBB)
def test_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_prcbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert len(cmp_data) == DIFFS_COUNT
