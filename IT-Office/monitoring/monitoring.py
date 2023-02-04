import csv
import datetime
import hashlib
import logging
import re
from collections import defaultdict, Iterable

import requests
import yt.wrapper as yt
from source.config import *
from startrek_client import exceptions
from source.utils import (StatConnector,
                   StatConnectorMoreDimensions,
                   exc_thread_wrapper,
                   EmailLogger,
                   StartrekUtils,
                   OtherUtils,
                   BotApiUtils)

logger = logging.getLogger(__name__)

def computeMD5hash(my_string):
    m = hashlib.md5()
    m.update(my_string.encode('utf-8'))
    return m.hexdigest()

def is_ticket_long_ago(ticket_num):
    try:
        date = st_client.issues[ticket_num].resolvedAt[0:10]
        ticket_datetime = datetime.datetime.strptime(date, '%Y-%m-%d')
        delta = datetime.datetime.now() - ticket_datetime
        if delta > datetime.timedelta(days=90):
            return (False, 'more that 90 days ago {}'.format(delta))
    except exceptions.BadRequest:
        return (True, 'incorrect ticket')
    except TypeError:
        return (False, 'type error')
    except AttributeError:
        return (True, ' attr error')
    except exceptions.Forbidden:
        return (True, 'forbidden')
    return (True, 'no more attempts')

class SimpleYTBasedCollector():
    """Default YT class. Should to describe fields in schema attribute"""
    def __init__(self, yt_table_name, recreate=False, *args, **kwargs):
        self.yt_table_name = yt_table_name
        self.yt_table = '//home/helpdesk/{}'.format(self.yt_table_name)
        self.schema = []
        self.recreate = recreate
        self.yt_client = kwargs.get('client', yt)
        self.is_table_dynamic = kwargs.get('is_table_dynamic', True)
        self.static_append = kwargs.get('static_append', True)

    def __create_table(self):
        logger.info('Creating YT Table')
        self.yt_client.create("table",self.yt_table, attributes={"schema": self.schema, "dynamic": self.is_table_dynamic}, force=True)

        if self.is_table_dynamic:
            self.yt_client.mount_table(self.yt_table)

    def write_data_to_yt(self, data):
        self.check_or_create_table()

        if self.is_table_dynamic:
            self.yt_client.mount_table(self.yt_table)
            data_len = int(len(data) / 100000) + 1

            for num in range(data_len):
                start = num * 100000
                end = (num + 1) * 100000
                self.yt_client.insert_rows(self.yt_table, data[start:end],
                                           update=True, format=yt.JsonFormat(
                        attributes={"encode_utf8": False}))
        else:
            table = yt.TablePath(name=self.yt_table, append=self.static_append)
            self.yt_client.write_table(table, data,
                                        format=yt.JsonFormat(attributes={
                                            "encode_utf8": False}))

    def check_or_create_table(self):
        if not self.yt_client.exists(self.yt_table) or self.recreate:
            logger.info('Cannot find table with path {} creating it...'.format(self.yt_table))
            self.__create_table()


class SimpleStatUploader():
    def process_upload_data_to_stat(self, stat_name, stat_title, data):
        stat = StatConnector(stat_name, title=stat_title)
        stat.upload_data(dict(data))


class CommonMonitoringCollector():
    def __init__(self):
        self.CONFIG = {

            ('Helpdesk/RFS/com_monitoringclear','CommonMonitoring'): {
            'cr_today':'Queue: "Helpdesk Requests" and Created: today()',
            'cr_today_y': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour',
            'cr_today_inc': 'Queue: "Helpdesk Requests" and Created: today() AND Type: Инцидент',
            'cr_today_y_inc': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour AND Type: Инцидент',
            'cr_today_ofr': 'Queue: "Helpdesk Requests" and Created: today() AND (Type: OFR OR Type: "Запрос на обслуживание") ',
            'cr_today_y_ofr': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour AND (Type: OFR OR Type: "Запрос на обслуживание") ',

            'cr_and_res_today':'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today()',
            'cr_and_res_today_y': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour',
            'cr_and_res_today_inc': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() AND Type: Инцидент',
            'cr_and_res_today_y_inc': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour AND Type: Инцидент',
            'cr_and_res_today_ofr': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() AND (Type: OFR OR Type: "Запрос на обслуживание") ',
            'cr_and_res_today_y_ofr': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour (Type: OFR OR Type: "Запрос на обслуживание") ',

            'res_today':'Queue: "Helpdesk Requests" and Resolved: today()',
            'res_today_y': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour',
            'res_today_inc': 'Queue: "Helpdesk Requests" and Resolved: today() AND Type: Инцидент',
            'res_today_y_inc': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour AND Type: Инцидент',
            'res_today_ofr': 'Queue: "Helpdesk Requests" and Resolved: today() AND (Type: OFR OR Type: "Запрос на обслуживание") ',
            'res_today_y_ofr': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour (Type: OFR OR Type: "Запрос на обслуживание") ',

            'backlog':'Queue: "Helpdesk Requests" and Resolved: empty()',
            'backlog_inc':'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент',
            'backlog_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") '
            },

            ('Helpdesk/RFS/mow_monitoring', 'MoscowMonitoring'): {
                'cr_today': 'Queue: "Helpdesk Requests" and Created: today() and "Fix Version": "MSK Morozov" ',
                'cr_today_y': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour and "Fix Version": "MSK Morozov" ',
                'cr_today_inc': 'Queue: "Helpdesk Requests" and Created: today() AND Type: Инцидент and "Fix Version": "MSK Morozov" ',
                'cr_today_y_inc': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour AND Type: Инцидент and "Fix Version": "MSK Morozov" ',
                'cr_today_ofr': 'Queue: "Helpdesk Requests" and Created: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Morozov" ',
                'cr_today_y_ofr': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Morozov" ',

                'cr_and_res_today': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() and "Fix Version": "MSK Morozov" ',
                'cr_and_res_today_y': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour and "Fix Version": "MSK Morozov" ',
                'cr_and_res_today_inc': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() AND Type: Инцидент and "Fix Version": "MSK Morozov" ',
                'cr_and_res_today_y_inc': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour AND Type: Инцидент and "Fix Version": "MSK Morozov" ',
                'cr_and_res_today_ofr': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Morozov" ',
                'cr_and_res_today_y_ofr': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Morozov" ',

                'res_today': 'Queue: "Helpdesk Requests" and Resolved: today() and "Fix Version": "MSK Morozov" ',
                'res_today_y': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour and "Fix Version": "MSK Morozov" ',
                'res_today_inc': 'Queue: "Helpdesk Requests" and Resolved: today() AND Type: Инцидент and "Fix Version": "MSK Morozov" ',
                'res_today_y_inc': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour AND Type: Инцидент and "Fix Version": "MSK Morozov" ',
                'res_today_ofr': 'Queue: "Helpdesk Requests" and Resolved: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Morozov" ',
                'res_today_y_ofr': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Morozov" ',

                'backlog': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "MSK Morozov" ',
                'backlog_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "MSK Morozov" ',
                'backlog_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Morozov" '},

            ('Helpdesk/RFS/avr_monitoring', 'AvroraMonitoring'): {
                'cr_today': 'Queue: "Helpdesk Requests" and Created: today() and "Fix Version": "MSK Avrora" ',
                'cr_today_y': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour and "Fix Version": "MSK Avrora" ',
                'cr_today_inc': 'Queue: "Helpdesk Requests" and Created: today() AND Type: Инцидент and "Fix Version": "MSK Avrora" ',
                'cr_today_y_inc': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour AND Type: Инцидент and "Fix Version": "MSK Avrora" ',
                'cr_today_ofr': 'Queue: "Helpdesk Requests" and Created: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Avrora" ',
                'cr_today_y_ofr': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Avrora" ',

                'cr_and_res_today': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() and "Fix Version": "MSK Avrora" ',
                'cr_and_res_today_y': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour and "Fix Version": "MSK Avrora" ',
                'cr_and_res_today_inc': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() AND Type: Инцидент and "Fix Version": "MSK Avrora" ',
                'cr_and_res_today_y_inc': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour AND Type: Инцидент and "Fix Version": "MSK Avrora" ',
                'cr_and_res_today_ofr': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Avrora" ',
                'cr_and_res_today_y_ofr': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Avrora" ',

                'res_today': 'Queue: "Helpdesk Requests" and Resolved: today() and "Fix Version": "MSK Avrora" ',
                'res_today_y': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour and "Fix Version": "MSK Avrora" ',
                'res_today_inc': 'Queue: "Helpdesk Requests" and Resolved: today() AND Type: Инцидент and "Fix Version": "MSK Avrora" ',
                'res_today_y_inc': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour AND Type: Инцидент and "Fix Version": "MSK Avrora" ',
                'res_today_ofr': 'Queue: "Helpdesk Requests" and Resolved: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Avrora" ',
                'res_today_y_ofr': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Avrora" ',

                'backlog': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "MSK Avrora" ',
                'backlog_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "MSK Avrora" ',
                'backlog_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "MSK Avrora" '},

            ('Helpdesk/RFS/msk_all_monitoring', 'MskAllMonitoring'): {
                'cr_today': 'Queue: "Helpdesk Requests" and Created: today() and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_today_y': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_today_inc': 'Queue: "Helpdesk Requests" and Created: today() AND Type: Инцидент and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_today_y_inc': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour AND Type: Инцидент and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_today_ofr': 'Queue: "Helpdesk Requests" and Created: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_today_y_ofr': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour AND (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',

                'cr_and_res_today': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_and_res_today_y': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_and_res_today_inc': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() AND Type: Инцидент and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_and_res_today_y_inc': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour AND Type: Инцидент and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_and_res_today_ofr': 'Queue: "Helpdesk Requests" and Created: today()  and Resolved: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'cr_and_res_today_y_ofr': 'Queue: "Helpdesk Requests" and Created: today() - 1d .. now() - 24hour  and Resolved: today() - 1d .. now() - 24hour (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',

                'res_today': 'Queue: "Helpdesk Requests" and Resolved: today() and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'res_today_y': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'res_today_inc': 'Queue: "Helpdesk Requests" and Resolved: today() AND Type: Инцидент and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'res_today_y_inc': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour AND Type: Инцидент and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'res_today_ofr': 'Queue: "Helpdesk Requests" and Resolved: today() AND (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'res_today_y_ofr': 'Queue: "Helpdesk Requests" and Resolved: today() - 1d .. now() - 24hour (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',

                'backlog': 'Queue: "Helpdesk Requests" and Resolved: empty() and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'backlog_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")',
                'backlog_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "MSK Morozov" or "Fix Version": "MSK Avrora")'
            },
            ('Helpdesk/RFS/region_monitoring', 'RegionMonitoring'): {
                'backlog_kazan': 'Queue: "Helpdesk Requests" and Resolved: empty() and ("Fix Version": "Kazan" OR "Fix Version": "Kazan Innopolis")',
                'backlog_kazan_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and ("Fix Version": "Kazan" OR "Fix Version": "Kazan Innopolis")',
                'backlog_kazan_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "Kazan" OR "Fix Version": "Kazan Innopolis")',

                'backlog_sochi': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Sochi"',
                'backlog_sochi_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Sochi"',
                'backlog_sochi_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Sochi"',

                'backlog_nsk': 'Queue: "Helpdesk Requests" and Resolved: empty() and ("Fix Version": "NSK Academpark" OR "Fix Version": "NSK Greenwich")',
                'backlog_nsk_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and ("Fix Version": "NSK Academpark" OR "Fix Version": "NSK Greenwich")',
                'backlog_nsk_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and ("Fix Version": "NSK Academpark" OR "Fix Version": "NSK Greenwich")',

                'backlog_ekb': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Ekaterinburg"',
                'backlog_ekb_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Ekaterinburg"',
                'backlog_ekb_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Ekaterinburg"',

                'backlog_nn': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Nizhny Novgorod"',
                'backlog_nn_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Nizhny Novgorod"',
                'backlog_nn_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Nizhny Novgorod"',

                'backlog_simf': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Simferopol"',
                'backlog_simf_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Simferopol"',
                'backlog_simf_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Simferopol"',

                'backlog_minsk': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Minsk"',
                'backlog_minsk_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Minsk"',
                'backlog_minsk_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Minsk"',

                'backlog_vor': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Voronezh"',
                'backlog_vor_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Voronezh"',
                'backlog_vor_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Voronezh"',

                'backlog_home': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Homework"',
                'backlog_home_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Homework"',
                'backlog_home_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Homework"',

                'backlog_spb': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "St.Peterburg"',
                'backlog_spb_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "St.Peterburg"',
                'backlog_spb_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "St.Peterburg"',

                'backlog_tur': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Turkey"',
                'backlog_tur_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Turkey"',
                'backlog_tur_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Turkey"',

                'backlog_rost': 'Queue: "Helpdesk Requests" and Resolved: empty() and "Fix Version": "Rostov"',
                'backlog_rost_inc': 'Queue: "Helpdesk Requests" and Resolved: empty() AND Type: Инцидент and "Fix Version": "Rostov"',
                'backlog_rost_ofr': 'Queue: "Helpdesk Requests" and Resolved: empty() AND (Type: OFR OR Type: "Запрос на обслуживание") and "Fix Version": "Rostov"',

            }
        }
        self.result_for_upload = defaultdict(int)

    def process_upload_data_to_stat(self, stat_name, stat_title, data):
        stat = StatConnector(stat_name, title=stat_title)
        stat.upload_data(dict(data))

    def main(self):
        for action_item in self.CONFIG:
            stat_name = action_item[0]
            stat_title = action_item[1]
            data = self.CONFIG[action_item]

            result = {x:len(st_client.issues.find(data[x] + " AND Shelezyaka: !Да")) for x in data}
            self.process_upload_data_to_stat(stat_name, stat_title, result)


class TelephonySIPMonitoringCollector(CommonMonitoringCollector):
    def __init__(self, date=None):
        super(TelephonySIPMonitoringCollector, self).__init__()
        self.loc_list = [
            "hd_regions_kazan,"
            "hd_regions_nsk,"
            "hd_regions_rostov,"
            "hd_regions_ekb,"
            "hd_regions_nn,"
            "hd_regions_minsk,"
            "hd_regions_spb,"
            "hd_regions_sochi,"
            "hd_regions_all,"
            "hd_regions_mrp,"
            "hd_regions_brp,"
            "hd_regions_nnul,"
            "hd_regions_nnlob,"
            "hd_regions_nnns,"
            "hd_regions_techpar,"
            "hd_regions_innopolis,"
            "hd_regions_vrmsk,"
            "hd_regions_vrnzh,"
            "hd_homeworker_all",
            "hd_msk_all",
            "hd_helpdesk",
            "hd_helpdesk_last_bound"
        ]
        self.results_dict = {
            "ABANDON":"rejected",
            "COMPLETEAGENT":"answered",
            "COMPLETECALLER":"answered",
            "EXITWITHTIMEOUT":"rejected"
        }
        self.talking_time = []
        self.waiting_time = []
        self.date = datetime.datetime.now().strftime('%Y-%m-%d') if not date else date
        self.data = requests.get('https://telegraph.yandex-team.ru/api/v3/charts/calls/?date={date}&queue={locs}'.format(
            locs = ','.join(self.loc_list), date=self.date
        ), headers = AUTH_HEADERS_BOT).json()['result']
        logger.info(self.data)

    def process(self):
        for timestamp in self.data:
            item_data = self.data[timestamp]
            if not item_data.get('enterTimestamp'):
                logger.warning('Cannot find timestamp in data :{}'.format(item_data))
                continue
            if not item_data.get('completeTimestamp') or item_data['completeTimestamp'] - item_data['enterTimestamp'] < 5:
                logger.info('Too fast call : {}'.format(timestamp))
                continue
            result = self.results_dict.get(item_data['complete'])

            if self.data[timestamp].get('connectTimestamp'):
                self.talking_time.append(item_data['completeTimestamp'] - item_data['connectTimestamp'])
                self.waiting_time.append(item_data['connectTimestamp'] - item_data['enterTimestamp'])
            else:
                self.waiting_time.append(item_data['completeTimestamp'] - item_data['enterTimestamp'])
            self.result_for_upload[result] += 1
        self.result_for_upload['avg_wait'] = round((sum(self.waiting_time) / len(self.waiting_time)),2) if self.waiting_time else 0
        self.result_for_upload['avg_talk'] = round((sum(self.talking_time) / len(self.talking_time)),2) if self.talking_time else 0
        self.process_upload_data_to_stat('Helpdesk/RFS/telephony_days', 'SIP Telephony monitoring', self.result_for_upload)

    def process_hourly(self):
        result = []
        for timestamp in self.data:
            item_data = self.data[timestamp]
            if not item_data.get('completeTimestamp') or item_data['completeTimestamp'] - item_data['enterTimestamp'] < 5:
                logger.info('Too fast call : {}'.format(timestamp))
                continue

            date = datetime.datetime.fromtimestamp(int(timestamp[:10])).strftime('%Y-%m-%d %H:%M:%S')
            while date in [x['fielddate'] for x in result]:
                date = (datetime.datetime.strptime(date,'%Y-%m-%d %H:%M:%S') + datetime.timedelta(seconds=1)).strftime('%Y-%m-%d %H:%M:%S')
            result.append({'fielddate':date,
                           'count':1})
        logger.info(result)
        stat = StatConnector('Helpdesk/RFS/telephony_hours', title='Telephony Hours')
        stat.upload_data(result, scale='s')


class SLACollector(CommonMonitoringCollector):
    def _temp_sla_api(self, key):
        url = "https://st-api.yandex-team.ru/v2/issues/{}?expand=sla".format(
            key)
        data = requests.get(url, headers=AUTH_HEADERS_TOOLS)
        return data.json().get('sla')

    def _get_sla_time_by_issue(self, issue):
        """Get issue (object of startrek library)
        return information about known SLA counters in ticket in format:
        {'reaction': {'spent': TIME_IN_MINUTES, 'status': 'ok'},
        'solve': {'spent': TIME_IN_MINUTES, 'status': 'ok'}}
        """
        result = {}
        sla_types_dict = {605: 'solve',
                          604: 'reaction'}
        violation_dict = {'NOT_VIOLATED': 'ok',
                          'FAIL_CONDITIONS_VIOLATED': 'failed',
                          'WARN_CONDITIONS_VIOLATED': 'ok'}
        sla_list = self._temp_sla_api(issue.key)
        if not sla_list: return result

        for sla_counter in sla_list:
            sla_status = sla_counter['violationStatus']
            sla_type = sla_types_dict.get(sla_counter['settingsId'])

            if not sla_counter['failedThreshold'] or not sla_counter['toFailTimeWorkDuration']:
                spent = 0
                logger.warning('SLA Failed {} | {}'.format(issue.key, issue.type.key))
            else:
                spent = sla_counter['failedThreshold'] - sla_counter['toFailTimeWorkDuration']

            result[sla_type] = {'spent': spent / 60000,
                                'status': violation_dict.get(sla_status,
                                                             sla_status)}
        return result

    def _calculate_sla_by_issues(self, issues, prefix):
        results = defaultdict(int)
        for issue in issues:
            sla_list = self._get_sla_time_by_issue(issue)
            if not sla_list: continue
            for sla_type in sla_list:
                sla_value = sla_list[sla_type]
                format_dict = {
                    'type': str(issue.type.key).lower(),
                    'sla_type': sla_type,
                    'prefix': prefix
                }

                for measure, value in [
                    ('spent', round(sla_value['spent'])),
                    (sla_value['status'], 1),
                    ('count', 1)]:
                    results['{type}_{sla_type}_{measure}_{prefix}'.format(
                        measure=measure,
                        **format_dict
                    )] += value

        return results

    def process(self):
        issues = st_client.issues.find('Queue: "HDRFS" AND Resolved: today()-7d..today() AND Shelezyaka: !Да"')
        data = self._calculate_sla_by_issues(issues, 'closed')
        self.process_upload_data_to_stat('Helpdesk/RFS/sla_monitoring_closed', 'RTT', data)

        issues = st_client.issues.find('Queue: "HDRFS" AND Resolution: empty() AND Shelezyaka: !Да"')
        data = self._calculate_sla_by_issues(issues, 'open')
        self.process_upload_data_to_stat('Helpdesk/RFS/sla_monitoring_opened', 'RTT', data)

    def process_regions(self):
        issues = st_client.issues.find('Queue: "HDRFS" AND Resolved: today()-7d..today() AND "Fix Version": !"MSK Morozov" AND "Fix Version": ! "MSK Avrora" AND "Fix Version": notEmpty() AND "Fix Version": ! "MSK Workki" AND "Fix Version": ! "MSK Zeleniy" AND Shelezyaka: !Да"')
        data = self._calculate_sla_by_issues(issues, 'closed')
        self.process_upload_data_to_stat('Helpdesk/RFS/Monitoring/sla_monitoring_region_closed', 'SLA_Regions_closed', data)

        issues = st_client.issues.find('Queue: "HDRFS" AND Resolution: empty() AND "Fix Version": !"MSK Morozov" AND "Fix Version": ! "MSK Avrora" AND "Fix Version": notEmpty() AND "Fix Version": ! "MSK Workki" AND "Fix Version": ! "MSK Zeleniy" AND Shelezyaka: !Да" ')
        data = self._calculate_sla_by_issues(issues, 'open')
        self.process_upload_data_to_stat('Helpdesk/RFS/Monitoring/sla_monitoring_region_opened', 'SLA_Regions_opened', data)

    def generate_report(self):
        issues = st_client.issues.find('Queue: "Helpdesk Requests" AND Resolved: today()-2d..today() AND Created: today()-2d..today() AND Shelezyaka: !Да"')
        result = []
        for issue in issues:
            sla_data = self._get_sla_time_by_issue(issue)
            time = sla_data['reaction']['spent']
            status = sla_data['reaction']['status']
            result.append({'time':time,
                           'stat':status,
                           'key':issue.key})
        with open('csv_report.csv', 'w') as f:  # Just use 'w' mode in 3.x
            w = csv.DictWriter(f, result[0].keys())
            w.writeheader()
            for item in result:
                w.writerow(item)


class EquipmentFinder():
    def _check_inv_by_sn(self, sn):
        dict_for_return = {
            'owner': None,
            'temp_user': None,
            'finded': False,
            'type': None
        }
        data = requests.get('https://bot.yandex-team.ru/api/osinfo.php?sn={sn}&output=EMPLOYEE_OWNED|EMPLOYEE_TEMPORARY_USE|item_segment3'.format(sn=sn)).json()
        if data["res"] == 1:
            extracted_data = data["os"][0]
            dict_for_return["owner"] = extracted_data.get('EMPLOYEE_OWNED')
            dict_for_return["temp_user"] = extracted_data.get('EMPLOYEE_TEMPORARY_USE')
            dict_for_return["finded"] = True
            dict_for_return["type"] = extracted_data.get('item_segment3')

        return dict_for_return


class DiskFinder(EquipmentFinder):
    @staticmethod
    def _date_formatter(digit):
        if digit < 10:
            return '0{}'.format(digit)

    def _fetch_data_from_cmdb(self):
        now_date = datetime.datetime.now()
        year, month, day = (now_date.year,
                            self._date_formatter(now_date.month),
                            self._date_formatter(now_date.day))
        search = list(YT_HAHN_CLIENT.select_rows(
            'instance_number,attached_disk_devices_collection,username,last_check_in from [{}] where regex_full_match("[A-Z0-9]+",attached_disk_devices_collection) and regex_full_match("{}\-{}\-{}.*",last_check_in)'.format(
                '//home/helpdesk/cmdb/jss', year, month, day),
            format=yt.JsonFormat(attributes={"encode_utf8": False})
        ))
        return search

    def _get_office_id_by_login(self, login):
        office_id = OtherUtils().extract_data_from_staff_by_login(login, {"office_id":"location.office.id"})["office_id"]
        return OtherUtils().get_fix_version_by_office_id(office_id)

    def _create_ticket(self, data_dict):
        macros_changes = StartrekUtils().get_st_macros_actions('772')["Result"]
        description = """
**Замечено подозрительное использование жесткого диска:**
#|
|| Инвентарный номер ноутбука | Владелец ноутбука | На кого проведён диск | Серийный номер диска ||
|| {instance_number} | {notebook_owner}@ |{owner}@ | {sn}||
|#""".format(**data_dict)

        create_dict = {
            "summary":"Мониторинг внешних накопителей (жесткие диски)",
            "description": description,
            "queue":"HDRFS",
            "channel":"ST API",
            **macros_changes,
            **self._get_office_id_by_login(data_dict["notebook_owner"])
        }
        issue = st_client.issues.create(
            **create_dict
        )
        issue.transitions['treated'].execute()

    def process(self):
        all_records = self._fetch_data_from_cmdb()
        for record in all_records:
            for sn in record['attached_disk_devices_collection'].split(','):
                result = self._check_inv_by_sn(sn)
                if result["finded"] and not result["owner"] and not result[
                    "temp_user"] and result["type"] == 'USR-ACCESSORIES':

                    result["instance_number"] = record["instance_number"]
                    result["notebook_owner"] = record["username"]
                    result["sn"] = sn
                    self._create_ticket(result)

class TransceiversFinder(EquipmentFinder):
    def __init__(self):
        self.SEARCH_FIELDS = ["loc_segment1",
                         "loc_segment2",
                         "loc_segment3",
                         "loc_segment4",
                         "loc_segment5",
                         "loc_segment6",
                         "loc_room_type"]

        self.BAD_LOCS = ["VLADIMIR",
                         "SAS",
                         "MANTSALA",
                         "HELSINKI",
                         "MYT"]

        self.find_result = []
        self.bot_connector = BotApiUtils()
        super().__init__()

    def _create_ticket(self, data):
        macros_changes = StartrekUtils().get_st_macros_actions('831')["Result"]
        table = " ||".join(["{action} | {data} | {sn} | {switch}".format(
            action = x["action"],
            data = x["data"],
            sn = x["sn"],
            switch = x["switch_name"]
        ) for x in data])

        description = """
**Замечены неправильно проведённые трансиверы:**
#|
|| Описание проблемы | Информация | Серийный номер | Switch ||\n
|| {} ||
|#""".format(table)

        create_dict = {
            "summary": "Мониторинг трансиверов",
            "description": description,
            "queue": "HDRFS",
            **macros_changes
        }

        st_client.issues.create(
            **create_dict
        )

    def _tranceivers_checks(self, transceivers, switch_info, switch_name):
        for transceiver in transceivers:
            logger.info('Start parsing transiever {}'.format(transceiver))
            sn = transceiver.get('sn')
            if not sn : continue
            trans_info = self.bot_connector.fetch_osinfo('sn', sn, self.SEARCH_FIELDS)
            if not trans_info: continue
            if switch_info != trans_info:
                self.find_result.append(
                    {"action":"tranciever and swith in diff locs",
                     "data":trans_info["os"][0],
                     "sn":sn,
                     "switch_name":switch_name}
                )

    def process(self):
        all_switches = OtherUtils().fetch_trancievers_info()
        for switch in all_switches:
            switch_info = self.bot_connector.fetch_osinfo('fqdn',switch, self.SEARCH_FIELDS)
            if switch_info \
                    and switch_info["os"][0]["loc_room_type"] == "COMCENTER"\
                    and switch_info["os"][0]["loc_segment2"] not in self.BAD_LOCS:
                transceivers = all_switches[switch]["inventory"]
                self._tranceivers_checks(transceivers, switch_info, switch)
            else:
                logger.info(switch_info)
                logger.info("switch : {} seems in DC".format(switch))
        if self.find_result:
            self.find_result.sort(key=lambda x: x["sn"])
            self._create_ticket(self.find_result)


class ExtDismissalFinder():
    def __init__(self):
        self.comment_template= """
Привет!

Мы заметили, что твой сотрудник покинул команду и на нём числится оборудование.

**ФИО сотрудника**: кто:{login}
**Логин**: {login}@
**Подразделение**: {division}
**Должность**: {duty}

{table}

Напоминаем тебе, что данное оборудование необходимо забрать у сотрудника и организовать передачу в HelpDesk. Для этого тебе необходимо заполнить ((https://forms.yandex-team.ru/surveys/1308/ форму на отправку оборудования)).

Как заполнять поля:

"ЦФО": указать бизнес-юнит запрашивающего отправку.
"ФИО получателя, название организации": HelpDesk.
"Адрес получателя": г. Москва, ул. Льва Толстого, 16.
"Телефон получателя": +79154476993.
"""

    def _generate_table_infs(self, list):
        table = "||".join(["{instance_number} | {segment2} ||\n".format(
            instance_number = x["NB.instance_number"],
            segment2 = x["NB.segment2"]
        ) for x in list])
        return "#|\n||{}\n|#".format(table)

    def _merge_yt_data(self, data):
        logger.info('Start merging data records')
        result = []
        result_invs = defaultdict(list)
        seen_logins = set()
        logger.info(list(data))
        for item in data:
            result_invs[item["NB.ext_login"]].append(item)
        for item in data:
            login = item["NB.ext_login"]
            if login not in seen_logins:
                seen_logins.add(login)
                result.append({
                    "login":login,
                    "division":item["ST.dep_path"],
                    "duty":item["ST.position"],
                    "table": self._generate_table_infs(result_invs[login]),
                    "summonees": item.get('NB.oebs_login') if item.get('NB.oebs_login') else "litovskikhd"
                })
        return result

    def _get_data_from_YT(self):
        logger.info('Get Data from YT')
        today = datetime.datetime.now().strftime('%Y-%m-%d')
        select_query = "NB.instance_number, NB.ext_login, NB.oebs_login, ST.quit_at, ST.dep_path, ST.position, NB.segment2  from [//home/helpdesk/cmdb/notebooks] as NB left join [//home/helpdesk/cmdb/staff] as ST ON (NB.ext_login)=(ST.login) where NB.ext_login !='' and NB.ext_login != null and ST.quit_at='{}'".format(today)
        data = YT_HAHN_CLIENT.select_rows(
            select_query,
            format=yt.JsonFormat(attributes={"encode_utf8": False}),
            allow_join_without_index=True
            )
        return list(data)

    def _create_tickets(self, data):
        for incident in data:
            summon = incident["summonees"]
            del incident["summonees"]

            macros_info = StartrekUtils().get_st_macros_actions(562)["Result"]
            issue = st_client.issues.create(
                queue = 'HDRFS',
                summary = 'Проверка оборудования на уволенных ext сотрудниках',
                description = self.comment_template.format(**incident),
                **macros_info
            )
            issue.comments.create(summonees=summon)
            logger.info('Succesfully create task: {}'.format(issue.key))

    def main(self):
        yt_data = self._get_data_from_YT()
        logger.info(list(yt_data))
        analyzed_data = self._merge_yt_data(yt_data)
        self._create_tickets(analyzed_data)

class CrmStat():
    def _fetch(slef, role):
        st = requests.get(
            'https://api.hd.yandex-team.ru/api/external/crm/search-incident?_format=json&role={role}&status=CLOSE'.format(
                role=role),
            headers=AUTH_HEADERS_BOT,
            timeout=300)
        st.raise_for_status()
        return st.json()


    def _process(self, role, scale='d'):
        count = 0
        while count != 3:
            try:
                all = self._fetch(role)
            except (requests.exceptions.RequestException) as err:
                count += 1
                logger.warning('Connection {count} - CRM API error: {error}'.format(count=count, error=err))
            else:
                result = defaultdict(lambda: (defaultdict(int)))
                for item in all:
                    location = str(item['location']).lower()
                    requestType = str(item['type']).lower()
                    if scale == 'h':
                        date = datetime.datetime.fromtimestamp(
                            float(item['dateOpen'])).strftime('%Y-%m-%d %H:00:00')
                    else:
                        date = datetime.datetime.fromtimestamp(
                            float(item['dateOpen'])).strftime('%Y-%m-%d')
                    result[date]['all'] += 1
                    result[date][location] += 1
                    if role == 'welcome':
                        result[date][requestType] += 1
                        result[date]['{location}_{requestType}'.format(location=location, requestType=requestType)] += 1
                list_for_result = []
                for item in result:
                    formatted_dict = result[item]
                    formatted_dict['fielddate'] = item
                    list_for_result.append(formatted_dict)
                return list_for_result
        raise ValueError('Connection CRM API is failed. Role {}'.format(role))


    def main(self):
        for role in ['welcome', 'income', 'st', 'tlg']:
            try:
                scale = 'h' if role == 'welcome' else 'd'
                data = self._process(role, scale=scale)
                stat = StatConnector('Helpdesk/RFS/CRM/{role}'.format(role=role.upper()), title='CRM_{}'.format(role.upper()))
                stat.upload_data(data, scale=scale)
            except ValueError as err:
                logger.error(err)


@exc_thread_wrapper
def main_common_monitoring_collect():
    logger.info('Start common monitoring')
    now_hour = datetime.datetime.now().hour
    if now_hour in range(0,7):
        return
    CommonMonitoringCollector().main()

@exc_thread_wrapper
def main_sip_monitoring():
    if datetime.datetime.now().hour in range(7,24):
        logger.info('start SIP information collection')
        collector = TelephonySIPMonitoringCollector()
        collector.process_hourly()
        collector.process()

@exc_thread_wrapper
def main_sla_monitoring():
    logger.info('start SLA information collection')
    SLACollector().process()
    SLACollector().process_regions()

def __pretty_string(string):
    remove_whitespaces = re.sub('\ ','_',string)
    remove_other = re.sub('[^0-9a-zA-Z_]+','',remove_whitespaces).lower()
    return remove_other

@exc_thread_wrapper
def main_history_actual_model():
    data = requests.get('https://help.yandex-team.ru/back/api/v1/reserve/equip_models/?format=json', verify=False).json()
    filtred_data = [x for x in data["equips"]]
    models_set = set()
    models_set.add('actual')
    aggregated_results = defaultdict(lambda: defaultdict(int))

    for model in filtred_data:
        pretty_model = __pretty_string(model['model_name'])
        office_name = __pretty_string(model['office_name'])
        count = model['len_current_stock']

        aggregated_results[pretty_model][office_name] = count
        aggregated_results['actual'][office_name] += count

    for item in aggregated_results:
        stat = StatConnector('Helpdesk/Reserve/{}'.format(item),title=item)
        stat.upload_data(dict(aggregated_results[item]))

@exc_thread_wrapper
def main_help_eq_monitoring():
    EXCL_LOGINS = ["sbalandin"]

    logger.info('Start main help eq monitoring')
    email_sender = EmailLogger('Неучтённое оборудование')

    letter_template = 'Привет, на тебе числится оборудование в "красной" зоне —\
     пожалуйста, актуализируй информацию на \
     https://help.yandex-team.ru/cab/monitoring/help_eq/?type=add_comment\
      → "Внести комментарий". Подробнее можно прочитать тут:\
    https://wiki.yandex-team.ru/users/orange13/Monitoring-oborudovanija/'

    r = requests.get(
        'https://help.yandex-team.ru/back/api/v1/monitoring/eq_helps/?format=json',
        headers={'Authorization': TEMP_OAUTH})

    data = r.json()['result']
    logger.info('get result :{}'.format(data))
    if data:
        bad_records = {x['owner'] for x in data if x['color'] == 'red'
                       if x['owner'] not in EXCL_LOGINS}
        for login in bad_records:
            email_sender.send_email(
                '{}@yandex-team.ru'.format(login),
                letter_template)
            logger.info('Send email to {}'.format(login))

@exc_thread_wrapper
def main_disk_finder():
    logger.info('Start disk finder')
    DiskFinder().process()

@exc_thread_wrapper
def main_tranceivers_finder():
    logger.info('Start tranceiever finder')
    TransceiversFinder().process()

@exc_thread_wrapper
def ext_dismissal_finder():
    ExtDismissalFinder().main()

@exc_thread_wrapper
def main_crm_stat():
    logger.info('Start CRM stat collection')
    CrmStat().main()
    logger.info('End CRM stat collection')