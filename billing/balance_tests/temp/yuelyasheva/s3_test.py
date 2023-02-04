# coding: utf-8

import btestlib.utils as utils
import pickle
from btestlib import reporter, secrets

def load_value(storage, key_prefix, build_number=None):
    #if not utils.is_local_launch():
        key = utils.make_build_unique_key(key_prefix, build_number)

        def helper():
            if storage.is_present(key):
                with reporter.reporting(level=reporter.Level.NOTHING):
                    return pickle.loads(storage.get_string_value(key))
            return None

        return utils.try_to_execute(helper, description="load {}".format(key))

    #return None

def save_value(storage, key_prefix, value):
    #if not utils.is_local_launch():
        key = utils.make_build_unique_key(key_prefix)

        reporter.log("Saving data to key: {}\nData: {}".format(key, utils.Presenter.pretty(value)))

        with reporter.reporting(level=reporter.Level.NOTHING):
            utils.try_to_execute(
                lambda: storage.set_string_value(key, pickle.dumps(value)),
                description="save {}".format(key)
            )

#print utils.s3storage().get_string_value().__dict__
save_value(utils.s3storage(), 'rerun_count', '0')
rerun_count = load_value(utils.s3storage(), 'rerun_count')
print rerun_count