# coding: utf-8
__author__ = 'chihiro'
from datetime import datetime, timedelta
import xmlrpclib

from hamcrest import equal_to, not_

from btestlib import utils as butils
from check import utils


def test_CHECK_2275_run_check():
    cmp_id = utils.run_check_new('arh', str('1'), {'run-on': datetime.now().strftime('%Y-%m-%d')})
    butils.check_that(cmp_id, not_(equal_to(None)))


def test_CHECK_2275():
    run_on = (datetime.now() + timedelta(days=1)).strftime('%Y-%m-%d')
    try:
        cmp_id = utils.run_check_new('arh', str('1'), {'run-on': run_on})
    except xmlrpclib.Fault:
        cmp_id = 0
    butils.check_that(cmp_id, equal_to(0))
