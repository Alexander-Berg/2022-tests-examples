# coding: utf-8
__author__ = 'blubimov'

# noinspection PyUnresolvedReferences
from balance.tests.conftest import *

def load_value(storage, key_prefix, build_number=None, additional_info_required=False):
    key = utils.make_build_unique_key(key_prefix, build_number, additional_info_required=additional_info_required)

    def helper():
        if storage.is_present(key):
            with reporter.reporting(level=reporter.Level.NOTHING):
                return pickle.loads(storage.get_string_value(key))
        return None

    return utils.try_to_execute(helper, description="load {}".format(key))


CORP_TAXI_DATA = load_value(utils.s3storage(), 'CORP_TAXI_RESTORE_DUMP_DEBUG')