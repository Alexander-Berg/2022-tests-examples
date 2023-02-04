# -*- coding: utf-8 -*-

import argparse
import calendar
import datetime
import os
import socket
import time
from shutil import copyfile

from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from btestlib.utils import GraphitSender

MONTH_FORMATS = ['YYYY-MM', 'MM-YYYY', 'YYYY.MM', 'MM.YYYY']
NOW = datetime.datetime.now()


# DEFAULT_HOST = 'greed-dev3f_balance_os_yandex_ru'
# METRIC_TEMPLATE = 'one_day.%(sender_host)s.%(source)s.%(name)s %(metric)s %(time)s'

class Utils(object):
    @staticmethod
    def add_months_to_date(base_date, months):
        a_months = months
        if abs(a_months) > 11:
            s = a_months // abs(a_months)
            div, mod = divmod(a_months * s, 12)
            a_months = mod * s
            a_years = div * s
        else:
            a_years = 0

        year = base_date.year + a_years

        month = base_date.month + a_months
        if month > 12:
            year += 1
            month -= 12
        elif month < 1:
            year -= 1
            month += 12
        day = min(calendar.monthrange(year, month)[1],
                  base_date.day)
        return datetime.datetime.combine(datetime.date(year, month, day), base_date.time())

    @staticmethod
    def str_to_date(string, format='%Y-%m'):
        # year, month = string.split('-')
        # year = int(year)
        # month = int(month)
        # start, end = calendar.monthrange(year, month)
        # return datetime.datetime(year, month, end)

        dt = datetime.datetime.strptime(string, format).date()
        return datetime.datetime(dt.year, dt.month, dt.day)

    @staticmethod
    def to_timestamp(dt):
        return time.mktime(dt.timetuple())

    @staticmethod
    def date_to_str(date, format='%Y-%m'):
        return date.strftime(format)

    @staticmethod
    def first_day_of_month(dt=None):
        if not dt:
            dt = datetime.datetime.now()
        return dt.replace(day=1)

    @classmethod
    def last_day_of_month(cls, dt=None):
        if not dt:
            dt = datetime.datetime.now()
        return cls.first_day_of_month(dt) + relativedelta(months=1, days=-1)

    @classmethod
    def get_list_to_send(cls, base, previous_n):
        return [Utils.last_day_of_month(cls.add_months_to_date(Utils.last_day_of_month(cls.str_to_date(base)), -shift))
                for shift in xrange(previous_n)]


class Steps(object):
    @staticmethod
    def parse_arguments():
        parser = argparse.ArgumentParser(description='Get BALANCE quality metrics')
        parser.add_argument('--base', help='base month to get stats')
        parser.add_argument('--n', type=int, help='number of month before base')
        parser.add_argument('--force', type=int, help='refresh data from startrek OR use')
        args = parser.parse_args()
        base = args.base or Utils.date_to_str(Utils.add_months_to_date(NOW, -1))
        previous_n = args.n or 1
        force = args.force
        return base, previous_n, force

    @staticmethod
    def get_monthly_data_from_file(filename, keys, date_format='%Y-%m'):
        stats = {}
        with open(filename) as f:
            for line in f.readlines():
                splitted = line.strip().split(';')
                period = Utils.last_day_of_month(Utils.str_to_date(splitted.pop(0), date_format))
                stats[period] = dict(zip(keys, splitted))
        return stats

    @staticmethod
    def get_daily_data_from_file(filename, keys, date_format='%Y-%m'):
        stats = {}
        with open(filename) as f:
            for line in f.readlines():
                splitted = line.strip().split(';')
                period = Utils.str_to_date(splitted.pop(0), date_format)
                stats[period] = dict(zip(keys, splitted))
        return stats

    @staticmethod
    def process_raw_stats(raw_stats, keys, generate_from):
        stats = {}
        for row in raw_stats:
            period, stats_tuple = Utils.last_day_of_month(Utils.str_to_date(row[0])), row[1:]
            # splitted = row.split(' ')
            # period = Utils.str_to_date(splitted.pop(0))
            if period >= generate_from:
                stats[period] = dict(zip(keys, [str(item) for item in stats_tuple]))
        return stats

    @staticmethod
    def save_actualized_stats(stats, keys, filename, backup_folder, date_format='%Y-%m'):
        periods = stats.keys()
        periods.sort()

        # filename - абсолютный путь, разбиваем его на имя файла и путь к папке csv_files
        directory, name = os.path.split(filename)
        # Разделяем имя файла с кешированными данными на имя и расширение
        original, extension = tuple(name.split('.'))
        # По имени файла с кеш. данными формируем имя файла с бекапом
        backup_filename = '{0}_{1}.{2}'.format(original,
                                               NOW.strftime('%Y-%m-%dT%H_%M_%S'),
                                               extension)
        # Находим директорию backup (лежит на том же уровне, что и csv_files)
        backup_directory = os.path.join(os.path.split(directory)[0], backup_folder)
        # Делаем бекап файла
        reporter.log(filename)
        reporter.log('{0}{1}'.format(backup_directory, backup_filename))
        copyfile(filename, '{0}{1}'.format(backup_directory, backup_filename))

        reporter.log(stats)

        with open(filename, 'w') as f:
            for period in periods:
                reporter.log([Utils.date_to_str(period, date_format)] + [stats[period][item] for item in keys])
                row = ';'.join([Utils.date_to_str(period, date_format)] + [stats[period][item] for item in keys])
                f.write(row + '\n')

    @staticmethod
    def actualize_stats(stats, list_to_send, callable, force=0):
        missed_periods = set(list_to_send) - set(stats.keys())
        if force:
            # Определяем дату, с которой будем пересчитывать статистику из StarTrek
            generate_from = min(list_to_send).replace(day=1)
            reporter.log(('Force param sent. Start calculation from {0}'.format(generate_from)))
            calculated_stats = callable(generate_from)
            stats.update(calculated_stats)
            return stats
        elif missed_periods:
            # Определяем дату, с которой будем пересчитывать статистику из StarTrek
            generate_from = min(missed_periods).replace(day=1)
            reporter.log(('No cached data for some month. Start calculation from {0}'.format(generate_from)))
            calculated_stats = callable(generate_from)
            stats.update(calculated_stats)
            return stats
        else:
            reporter.log(('Cached data will be used'))
            return stats

    @classmethod
    def send_data(cls, source, stats, list_to_send, tpl=GraphitSender.DEFAULT_TPL,
                  sender_host=GraphitSender.DEFAULT_SENDER_HOST, receiver_host=GraphitSender.DEFAULT_RECEIVER_HOST,
                  port=GraphitSender.DEFAULT_RECEIVER_PORT):

        # stats should be a dict
        for period in list_to_send:
            # Skip missing (due to data absence) periods
            if period in stats:
                metrics = stats[period]
                for metric in metrics:
                    name = metric
                    value = metrics[metric]
                    time = int(Utils.to_timestamp(period))
                    cls.sender(source, name, value, time, tpl, sender_host, receiver_host, port)

    @staticmethod
    def sender(source, name, value, time, tpl=GraphitSender.DEFAULT_TPL, sender_host=GraphitSender.DEFAULT_SENDER_HOST,
               receiver_host=GraphitSender.DEFAULT_RECEIVER_HOST, port=GraphitSender.DEFAULT_RECEIVER_PORT):
        GraphitSender.send(source=source, name=name, value=value, time_point=time, tpl=tpl, sender_host=sender_host,
                           receiver_host=receiver_host, port=port)
