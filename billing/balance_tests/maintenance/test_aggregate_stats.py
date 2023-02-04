# coding: utf-8
__author__ = 'a-vasin'

import pickle
from collections import defaultdict
from xml.etree import ElementTree

import pytest

from balance.tests.conftest import TestStatsCollector
from btestlib import reporter
from btestlib import shared
from btestlib import utils
from btestlib.constants import Users
from simpleapi.common.utils import call_http

BUILD_IDS_MAP = {
    'Full': 'Billing_Autotesting_PythonTests_RunTests',
    'Smoke': 'Billing_Autotesting_PythonTests_Smoke'
}

STORAGE = utils.s3storage_stats()
THRESHOLD = 0.5


@pytest.mark.parametrize('build', ['Full', 'Smoke'])
def test_aggregate_stats(build):
    last_aggr_key = 'last_aggregated_{}'.format(build)
    aggr_key = 'aggregation_{}'.format(build)

    start = int(STORAGE.get_string_value(last_aggr_key)) + 1 if STORAGE.is_present(last_aggr_key) else 0
    end = get_max_build_number(build) - 1

    if start > end:
        return

    acc = load_value(aggr_key)

    acc = aggregate_stats(build, start, end, acc)

    STORAGE.set_string_value(last_aggr_key, str(end))
    with reporter.reporting(level=reporter.Level.NOTHING):
        STORAGE.set_string_value(aggr_key, pickle.dumps(acc))


# ---------------------------------------
# Utils

def load_value(key):
    if STORAGE.is_present(key):
        with reporter.reporting(level=reporter.Level.NOTHING):
            return pickle.loads(STORAGE.get_string_value(key))
    return None


def aggregate_stats(build, start, end, acc=None, prefix=None):
    prefix = prefix if prefix else TestStatsCollector.S3_PREFIX
    acc = defaultdict(lambda: defaultdict(int), acc if acc else {})

    for build_number in xrange(start, end + 1):
        keys = {
            shared.NO_STAGE: '{}_{}_{}_{}'.format(prefix, 'BALANCE', build, build_number),
            shared.BEFORE: '{}_{}_{}_{}_{}'.format(prefix, shared.BEFORE, 'BALANCE', build, build_number),
            shared.AFTER: '{}_{}_{}_{}_{}'.format(prefix, shared.AFTER, 'BALANCE', build, build_number),
        }

        all_stages_stats = {k: load_value(v) for k, v in keys.iteritems()}

        if all_stages_stats[shared.NO_STAGE] is None and all_stages_stats[shared.AFTER] is None \
                or all_stages_stats[shared.NO_STAGE] \
                        and all_stages_stats[shared.NO_STAGE].get('stage', None) != shared.AFTER:
            continue

        for all_stats in all_stages_stats.values():
            if all_stats is None:
                continue

            relevant = {name: stats for name, stats in all_stats['tests'].iteritems() if not stats['skipped']}

            if all_stats.get('stage', None) == shared.AFTER:
                for name, stats in relevant.iteritems():
                    acc[name]['failed'] += int(stats['failed'])
                    acc[name]['passed'] += int(stats['passed'])

            if all_stats['skipped'] >= THRESHOLD * all_stats['total']:
                continue

            for name, stats in all_stats['tests'].iteritems():
                acc[name]['duration'] += stats['duration']

                if all_stats.get('stage', None) == shared.AFTER:
                    acc[name]['duration_count'] += 1

                if 'runtime' not in stats:
                    continue

                if 'runtime' not in acc[name]:
                    acc[name]['runtime'] = defaultdict(int)

                for entry, times in stats['runtime'].iteritems():
                    acc[name]['runtime'][entry] += sum(times)

                if all_stats.get('stage', None) == shared.AFTER:
                    acc[name]['runtime_count'] += 1

    return dict(acc)


def get_max_build_number(build):
    url = 'https://teamcity.yandex-team.ru/app/rest/buildTypes/id:{}'.format(BUILD_IDS_MAP[build])
    xml_response = call_http(url, method='GET', auth_user=Users.TESTUSER_BALANCE1)
    return int(ElementTree.fromstring(xml_response).find(r".//property[@name='buildNumberCounter']").attrib['value'])
