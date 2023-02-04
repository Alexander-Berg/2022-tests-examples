# coding: utf-8
from btestlib import utils
from btestlib import reporter
import pickle
from hamcrest import empty

ALL_STATS_KEY = 'audit_test'
STATS_DELETED_KEY = 'audit_deleted_test'
DIFF_KEY = 'audit_diff_test_Audit_Audit Collector_'
BUILD_NUM_KEY = 'audit_last_build_num'


def load_value(key_prefix, build_number=None, storage=utils.s3storage()):
    key = key_prefix + str(build_number) if build_number else key_prefix

    def helper():
        if storage.is_present(key):
            with reporter.reporting(level=reporter.Level.NOTHING):
                return pickle.loads(storage.get_string_value(key))
        return None

    return utils.try_to_execute(helper, description="load {}".format(key))


def get_all_stats():
    all_stats = dict(load_value(ALL_STATS_KEY))
    reporter.log(u"Аудируемые тесты:\n{}".format(all_stats))


def test_get_deleted_stats():
    deleted_stats = dict(load_value(STATS_DELETED_KEY))
    reporter.log(u"Удаленные тесты: название \t метка")
    for test in sorted(deleted_stats):
        reporter.log(u"{}\t{}".format(test[0], test[1]))
    utils.check_that(deleted_stats, empty(), 'Проверяем, что нет удаленных аудируемых тестов')


def get_diff(build_number):
    diff = dict(load_value(DIFF_KEY, build_number=build_number))
    reporter.log(u"Diff запуска {}:\n{}".format(build_number, diff))
    return diff