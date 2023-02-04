# -*- coding: utf-8 -*-

__author__ = 'pelmeshka'

import datetime

import pytest
from hamcrest import contains_string, equal_to, not_none, is_not, is_in

import btestlib.reporter as reporter
import btestlib.utils as utils
import balance.balance_db as db
from balance import balance_api as api
from balance.features import Features
from btestlib.constants import CompletionSource


DT = utils.Date.nullify_time_of_date(datetime.datetime.today())
OLD_START_DT = '2000-01-01'
OLD_END_DT = '2000-01-02'


# @reporter.feature(Features.ADDAPPTER2, Features.AUTOBUS)
# @pytest.mark.parametrize('source', [
#     # CompletionSource.BUSES2,
#     CompletionSource.ADDAPPTER2])
# # тест просто проверяет, что происходит забор откруток и ручка не падает, сами данные не проверяются
# def test_get_partner_completions(source):
#     api.test_balance().GetPartnerCompletions({'start_dt': DT, 'end_dt': DT, 'completion_source': source})


@reporter.feature(Features.TO_UNIT)
# тест проверяет, что несуществующую таблицу можно игнорировать выставляя флаг ignore_source_error в конфиге сборщика
def test_missing_source_ignore():
    ignored_sources = get_sources_with_error_ignoring()
    utils.check_that(ignored_sources, not_none())
    result = api.test_balance().GetPartnerCompletions(
        {
            'start_dt': OLD_START_DT,
            'end_dt': OLD_END_DT,
            'completion_source': ignored_sources[0]
        }
    )

    utils.check_that(result, equal_to([0, 'Success']))


@reporter.feature(Features.TO_UNIT)
# тест проверяет, что без флага ignore_source_error в конфиге сборщика забор откруток рейзит ошибку
def test_unignored_missing_source_raises_exception():
    source = CompletionSource.CLOUD

    ignored = set(get_sources_with_error_ignoring())
    utils.check_that(source, is_not(is_in(ignored)))

    with pytest.raises(Exception) as error:
        api.test_balance().GetPartnerCompletions(
            {  # missing source
                'start_dt': OLD_START_DT,
                'end_dt': OLD_END_DT,
                'completion_source': source
            }
        )

    expected_message = u'ResourceUnreachableError'
    utils.check_that(error.value.faultString,
                     contains_string(expected_message),
                     u'Checkout xmlrpc error message')


def get_sources_with_error_ignoring():
    result = db.balance().execute(
        "select code from bo.t_completion_source where "
        " queue = 'PARTNER_COMPL' and lower(config) like '%\"ignore_source_error\": 1%' and enabled = 1")
    return [row['code'] for row in result]
