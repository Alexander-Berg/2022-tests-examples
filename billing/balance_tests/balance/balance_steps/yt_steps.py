# coding: utf-8
import datetime as dt

import yt.wrapper as yt

from btestlib import reporter, secrets
from btestlib.constants import YtCluster


class YTSteps(object):
    @staticmethod
    def create_yt_client(yt_cluster=YtCluster.HAHN):
        return yt.YtClient(config={
            'token': secrets.get_secret(*secrets.Tokens.YT_OAUTH_TOKEN),
            'proxy': {
                'url': '{}.yt.yandex.net'.format(yt_cluster),
            },
        })

    @staticmethod
    def remove_table_in_yt(filepath, yt_client):
        if yt_client.exists(filepath):
            yt_client.remove(filepath, force=True)

    @staticmethod
    def create_data_in_yt(yt_client, filepath, data, attributes_dict=None):
        path, filename = yt.ypath_split(filepath)
        yt_client.mkdir(path, recursive=True)
        tp = yt.TablePath(filepath, sorted_by=["transaction_id"])
        yt_client.write_table(tp, data, force_create=True, format=yt.JsonFormat())
        if attributes_dict:
            for attribute, value in attributes_dict.iteritems():
                yt_client.set_attribute(tp, attribute, value)

    @staticmethod
    def read_table(yt_client, path):
        with reporter.step(u"Читаем табличку по пути: {}".format(path)):
            table = yt.TablePath(path, sorted_by=["transaction_id"])
            return list(yt_client.read_table(table, format=yt.JsonFormat()).rows)

    @staticmethod
    def exists_table(yt_client, path):
        with reporter.step(u"Проверяем наличие таблички по пути: {}".format(path)):
            return yt_client.exists(path)

    @staticmethod
    def list_table_attributes(yt_client, path):
        with reporter.step(u"Получаем атрибуты таблички по пути: {}".format(path)):
            return yt_client.list_attributes(path)

    @staticmethod
    def remove_tables(yt_client, dt_list, path):
        for date in dt_list:
            with reporter.step(u"Удаляем таблицу в yt за дату {}".format(date)):
                filepath = YTSteps.get_table_path(path, date)
                YTSteps.remove_table_in_yt(filepath, yt_client)

    @staticmethod
    def fill_table(yt_client, date, data_for_yt, path, attributes=None):
        with reporter.step(u"Заполняем в yt таблицу на дату {}".format(date)):
            filepath = YTSteps.get_table_path(path, date)
            YTSteps.create_data_in_yt(yt_client, filepath, data_for_yt, attributes_dict=attributes)

    @staticmethod
    def get_table_path(path, date):
        return path + ("" if path.endswith("/") else "/") + dt.datetime.strftime(date, "%Y-%m-%d")
