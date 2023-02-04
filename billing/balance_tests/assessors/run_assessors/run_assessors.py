# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import argparse
import datetime

import hitman
import testpalm


SUITES = testpalm.REGRESSION_SUITES
ENVIRONMENTS = testpalm.ENVIRONMENTS

VERSION_NAME = 'RELEASE-2_242'


BOOKING_ID = 0
BOOKING_REGULAR_ID = '48d5cba4-9530-4e95-94ad-cce6438d8462'


def run_assessors(version_name, version_postfix, booking_id, booking_regular_id):
    testpalm.create_testpalm_version(SUITES, version_postfix, version_name)
    testpalm.create_run(str(version_name + version_postfix), SUITES, ENVIRONMENTS)
    hitman.start_process(booking_id, booking_regular_id, str(version_name + version_postfix))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument('--booking_id', default=0, help='single booking id')
    parser.add_argument('--booking_regular_id', help='regular booking id')
    parser.add_argument('--version_name', help='release name')

    args = parser.parse_args()
    # забираем id броней и название версии (номер релиза приводим к виду release_N_NNN)
    booking_id, booking_regular_id, version_name = args.booking_id, args.booking_regular_id, \
                                                   args.version_name.lower().replace('-', '_')
    version_postfix = '_' + datetime.datetime.today().strftime('%d%m')

    # для локального запуска:
    # version_name = VERSION_NAME
    # booking_id = BOOKING_ID
    # booking_regular_id = BOOKING_REGULAR_ID

    run_assessors(version_name, version_postfix, booking_id, booking_regular_id)

