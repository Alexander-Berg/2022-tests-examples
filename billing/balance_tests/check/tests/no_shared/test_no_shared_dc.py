# coding=utf-8

import datetime
import decimal

from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_api as api
from balance.balance_db import balance
from btestlib import utils as butils
from check.db import insert_into_partner_completion_buffer
from check import utils
from check.utils import create_data_file_in_s3

MAX_PLACE_ID_SEARCH_ATTEMPTS = 60

QTY_CAMPAIGN = decimal.Decimal("23")
QTY_MONEY = decimal.Decimal("22")
QTY = decimal.Decimal("55.7")

check_list = ['dc']


def get_next_unused_place_id():
    return balance().execute("SELECT s_test_place_id.nextval place FROM dual")[0]['place']


def create_data_in_bk(data_list, file_date):
    orders = ''
    for data in data_list:
        data_list[data].update({'date': file_date.strftime("%Y%m%d000000")})
        one_order = '{date}\t{place_id}\t{page_id}\t{completion_type}\t0\t{shows}\t{clicks}\t{bucks}\t{mbucks}\t{hits}\n'.format(
            **data_list[data])
        orders += one_order
    orders += '#End'

    create_data_file_in_s3(
        content=orders,
        file_name='dc_bk_{}.csv'.format(file_date.strftime("%Y%m%d")),
        db_key='dc_stat_page_url_bk'
    )
    reporter.log(orders)


def new_section(description):
    print(description)
    return description


def get_diff_orders(cmp_id):
    query = """
        select *
        from cmp.{0}_cmp_data
        where cmp_id = {1}
    """.format('dc', cmp_id)
    return api.test_balance().ExecuteSQL('cmp', query)


def test_different_date():
    page_id = int(100001)
    completion_type = int(6)
    shows = 113
    source_id = int(1)  # for dc_bk
    bk_data = {}
    cache = {}
    two_day_ago = (datetime.datetime.now() - datetime.timedelta(days=2))
    one_day_ago = (datetime.datetime.now() - datetime.timedelta(days=1))

    print('----------------------------TWO_DAY_AGO------------------------------------------')
    description = new_section('No_diffs_two_day_ago')
    place_id = get_next_unused_place_id()

    insert_into_partner_completion_buffer(place_id, page_id, completion_type, source_id, shows, 0, 0, 0, 0,
                                             date=two_day_ago.strftime("%d.%m.%y 00:00:00"))


    bk_data[description + '_1'] = {'place_id': place_id, 'page_id': page_id,
                                   'completion_type': completion_type, 'shows': int(shows) - 25,
                                   'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0,
                                   'date': two_day_ago.strftime("%Y%m%d000000")}
    bk_data[description + '_2'] = {'place_id': place_id, 'page_id': page_id,
                                   'completion_type': completion_type, 'shows': 25,
                                   'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0,
                                   'date': two_day_ago.strftime("%Y%m%d000000")}
    create_data_in_bk(bk_data, file_date=two_day_ago)

    reporter.log(">>>>>>>>>> BK_DATA = {}".format(bk_data))

    params = {
        'begin-dt': two_day_ago.strftime('%Y-%m-%d'),
        'end-dt': two_day_ago.strftime('%Y-%m-%d'),
        'source-ids': str(source_id),
    }
    objects = [str(place_id)]
    cmp_id = utils.run_check_new('dc', str(','.join(objects)), params)

    reporter.log(">>>>>>>>>> OBJECTS = {}".format(objects))
    reporter.log(">>>>>>>>>> CMP_ID = {}".format(cmp_id))

    butils.check_that(get_diff_orders(cmp_id=cmp_id), empty())


    print('----------------------------ONE_DAY_AGO------------------------------------------')
    bk_data = {}

    place_id = get_next_unused_place_id()
    description = new_section('No_diffs_one_dy_ago_1')
    insert_into_partner_completion_buffer(place_id, page_id, completion_type, source_id, 11, 0, 0, 0, 0,
                                             date=one_day_ago.strftime("%d.%m.%y 00:00:00"))
    cache[description] = {'place_id': place_id, 'page_id': page_id,
                          'source_id': source_id,
                          'completion_type': completion_type, 'expected': 0}

    bk_data[description] = {'place_id': place_id, 'page_id': page_id,
                            'completion_type': completion_type, 'shows': 11,
                            'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0,
                            'date': one_day_ago.strftime("%Y%m%d000000")}
    print('----------------------------------------------------------------------')
    description = new_section('No_diffs_one_dy_ago_2')
    place_id = get_next_unused_place_id()
    insert_into_partner_completion_buffer(place_id, page_id, completion_type, source_id, shows, 0, 0, 0, 0,
                                             date=one_day_ago.strftime("%d.%m.%y 00:00:00"))
    cache[description] = {'place_id': place_id, 'page_id': page_id,
                          'source_id': source_id,

                          'completion_type': completion_type, 'expected': 0}
    bk_data[description] = {'place_id': place_id, 'page_id': page_id,
                            'completion_type': completion_type, 'shows': shows,
                            'clicks': 0, 'bucks': 0, 'mbucks': 0, 'hits': 0,
                            'date': one_day_ago.strftime("%Y%m%d000000")}
    create_data_in_bk(bk_data, file_date=one_day_ago)

    reporter.log(">>>>>>>>>> BK_DATA = {}".format(bk_data))

    params = {
        'begin-dt': one_day_ago.strftime('%Y-%m-%d'),
        'end-dt': one_day_ago.strftime('%Y-%m-%d'),
        'source-ids': str(source_id),
    }
    objects = []
    for key in cache:
        objects.append(str(cache[key]['place_id']))
    cmp_id = utils.run_check_new('dc', str(','.join(objects)), params)

    reporter.log(">>>>>>>>>> OBJECTS = {}".format(objects))
    reporter.log(">>>>>>>>>> CACHE = {}".format(cache))
    reporter.log(">>>>>>>>>> CMP_ID = {}".format(cmp_id))

    butils.check_that(get_diff_orders(cmp_id=cmp_id), empty())
