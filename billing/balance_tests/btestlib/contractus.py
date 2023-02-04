# coding: utf-8

import json

from btestlib import config, reporter, utils, dictdiff
from btestlib.utils import cached


def collect(id_, collector, storage=None):
    with reporter.step(u'Получаем данные контракта: ' + id_):
        data = collector()
        if need_rebuild_etalon(id_):
            store_etalon(id_, data, storage or default_storage())
        else:
            store_current(id_, data, storage or default_storage())


def check(id_, differ, filters=None, storage=None, diff_reporter=None):
    etalon = get_etalon(id_, storage or default_storage())
    current = get_current(id_, storage or default_storage())
    diff = differ(etalon, current)
    if filters:
        diff.filter(filters)
    diff_reporter = diff_reporter or default_diff_reporter()
    diff_reporter(diff)


def need_rebuild_etalon(id_):
    ids_to_rebuild = config.CONTRACTUS_REBUILD
    return ids_to_rebuild and (ids_to_rebuild == '*' or id_ in ids_to_rebuild)


class ContractorS3(object):
    def __init__(self):
        self.s3storage = utils.S3Storage(bucket_name="balance-autotesting-contractor")

    def store(self, key, data):
        data_json = json.dumps(data)
        self.s3storage.set_string_value(key, data_json)

    def get(self, key):
        data_json = self.s3storage.get_string_value(key)
        return json.loads(data_json)


@cached
def default_storage():
    return ContractorS3()


def store_etalon(id_, data, storage):
    with reporter.step(u"Сохраняем эталон в хранилище: " + id_):
        with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
            storage.store(id_ + '_etalon', data)


def get_etalon(id_, storage):
    with reporter.step(u'Получаем эталон из хранилища: ' + id_):
        with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
            return storage.get(id_ + '_etalon')


def store_current(id_, data, storage):
    with reporter.step(u"Сохраняем текущие данные в хранилище: " + id_):
        with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
            storage.store(id_ + '_current', data)


def get_current(id_, storage):
    with reporter.step(u'Получаем текущие данные из хранилища: ' + id_):
        with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
            return storage.get(id_ + '_current')


def default_diff_reporter():
    return dictdiff.report
