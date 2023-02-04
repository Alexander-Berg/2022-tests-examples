import csv
import logging
import smtplib
from collections import defaultdict
from email.message import EmailMessage
from io import StringIO
import datetime

import yt.wrapper as yt

from source.config import YT_HAHN_CLIENT

YT_CLIENT = YT_HAHN_CLIENT

logger = logging.getLogger(__name__)

MODELS_ORDER_CONF = [{'MB Pro 13 Touch':
                          ['MR9Q2RU/A', 'MR9R2RU/A', 'Z0UN000F7',
                           'Z0V7000L7','Z0WQ000DJ']},
                     {'MB Pro 15 Touch':
                          ['Z0SG000DQ', 'Z0UB0002R', 'Z0V0000NW',
                           'Z0WV00069']},
                     {'XPS':
                          ['9570 4K Touch', '9570 FHD']},
                     {'LENOVO':
                          ['T580 (20LASV0Y00)',
                           'X1 Carbon 6th Gen (20KGS6PC00)', 'T580 (20LAS5V100)', 'X1 Carbon 6th Gen (20KGSBUL00)']},
                     {'AQUARIUS':
                          ['Pro P30 K15', 'Pro G40 S58']},
                     {'INTEL':
                          ['NUC7i3DNHE']},
                     {'LENOVO-MONITORS':
                          ['P24h-10 (F16238QP1)', 'P27u-10 (A16270UP0)']},
                     {'VOIP':
                          ['Cisco - CP-7821']},
                     ]

RECIEVERS = ['litovskikhd@yandex-team.ru']

KNOWN_LOCATIONS = {"ALA": "OTHER",
                   "AMS": "OTHER",
                   "ASTANA": "OTHER",
                   "BERLIN": "OTHER",
                   "BKU": "OTHER",
                   "CHELYABINSK": "OTHER",
                   "EKB": "EKB",
                   "HERZLIYA": "OTHER",
                   "INNOPOLIS": "OTHER",
                   "IST": "OTHER",
                   "IVA": "OTHER",
                   "IZHEVSK": "OTHER",
                   "KHUTOR_LENINA": "OTHER",
                   "KIEV": "OTHER",
                   "KRASNODAR": "OTHER",
                   "KYA": "OTHER",
                   "KZN": "OTHER",
                   "LCN": "OTHER",
                   "MANTSALA": "OTHER",
                   "MOW": "MOW",
                   "MSQ": "MSQ",
                   "MYT": "MYT",
                   "NEWBURYPORT": "OTHER",
                   "NNOV": "NNOV",
                   "NVB": "NVB",
                   "ODS": "OTHER",
                   "OMSK": "OTHER",
                   "PAO": "OTHER",
                   "PERM": "OTHER",
                   "RND": "OTHER",
                   "RZN": "OTHER",
                   "SAS": "OTHER",
                   "SHANGHAI": "OTHER",
                   "SIMF": "SIMF",
                   "SJC": "OTHER",
                   "SMR": "OTHER",
                   "SOCHI": "SOCHI",
                   "SPB": "SPB",
                   "TJM": "OTHER",
                   "TOMILINO": "OTHER",
                   "UFA": "OTHER",
                   "VLADIMIR": "OTHER",
                   "VOLGOGRAD": "OTHER",
                   "VORONEZH": "OTHER",
                   "VVK": "OTHER",
                   "ZRH": "OTHER"}

class EquipmentStockMonitoring():
    def __init__(self, yt_status, mail_header):
        raw_locations = set(KNOWN_LOCATIONS.values())
        location_order = {'MOW':'3',
                          'MYT':'2',
                          'SPB':'1',
                          'OTHER':'Z'}

        self.locations = sorted(raw_locations, key=lambda x:location_order.get(x,x))
        self.other_locations = [x[0] for x in KNOWN_LOCATIONS.items() if x[1]=='OTHER']
        self.yt_status = yt_status
        self.mail_header = mail_header
        self._generate_known_models()

    def _generate_known_models(self):
        self.known_models = {}
        for item in MODELS_ORDER_CONF:
            for key in item:
                real_models = item[key]
                for model in real_models:
                    self.known_models[model] = key

    def _fetch_yt_data(self):
        search_nb = list(YT_CLIENT.select_rows(
            'loc_segment2,segment1 from [{}] where status_name="{}"'.format(
                '//home/helpdesk/cmdb/notebooks', self.yt_status),
            format=yt.JsonFormat(attributes={"encode_utf8": False})
        ))
        search_eq = list(YT_CLIENT.select_rows(
            'loc_segment2,segment1 from [{}] where status_name="{}"'.format(
                '//home/helpdesk/cmdb/equipment', self.yt_status),
            format=yt.JsonFormat(attributes={"encode_utf8": False})
        ))
        return search_eq + search_nb

    def _calculate_models(self, yt_data):
        result = defaultdict(lambda : defaultdict(int))
        for item in yt_data:
            real_model = item['segment1']
            virtual_model = self.known_models.get(item['segment1'])
            location = KNOWN_LOCATIONS.get(item['loc_segment2'])
            if virtual_model:
                result[real_model][location] += 1
                result[virtual_model]['sum'] += 1

        return result

    def _generate_model_rows(self, data, virtual_model, real_models):
        result = []
        result.append([virtual_model,'','','','','','','','','',' ',data[virtual_model]['sum']])
        for model in real_models:
            model_sum = 0
            model_rows = [model]
            extracted_data = data[model]
            for location in self.locations:
                model_rows.append(extracted_data.get(location))
                model_sum += extracted_data.get(location, 0)
            model_rows.append(model_sum)
            result.append(model_rows)

        return result

    def _generate_csv(self, data):
        result = [['Отчет сформирован в {}'.format(datetime.datetime.now().strftime('%Y-%m-%d %H:%M'))],
                  ['Model'] + list(self.locations) + ['Sum']]
        for item in MODELS_ORDER_CONF:
            for key in item:
                virtual_model = key
                real_models = item[key]
            result.extend(self._generate_model_rows(data, virtual_model, real_models))

        csvfile = StringIO()
        csvwriter = csv.writer(csvfile)
        result.extend([['OTHER локация включает в себя {}'.format(','.join(self.other_locations))]])

        for item in result:
            csvwriter.writerow(item)

        return csvfile

    def _send_message(self, file):
        msg = EmailMessage()
        msg['Subject'] = self.mail_header
        msg['From'] = "robot-help@yandex-team.ru"
        msg['To'] = RECIEVERS
        msg.add_attachment(file.getvalue(), filename='result.csv')
        with smtplib.SMTP('outbound-relay.yandex.net') as s:
            s.send_message(msg)

    def process(self):
        data = self._fetch_yt_data()
        result = self._calculate_models(data)
        csvfile = self._generate_csv(result)
        self._send_message(csvfile)

def monitoring_equipment_stock():
    logger.info('Start equipment stock monitoring')
    for item in [('NOT_USED', 'Отчет об оборудовании в статусе NOT_USED'),
                 ('RESERVED', 'Отчет об оборудовании в статусе RESERVED')]:
        monitor = EquipmentStockMonitoring(item[0], item[1])
        monitor.process()