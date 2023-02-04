import requests
import logging
from source.monitoring.monitoring import SimpleYTBasedCollector
from source.utils import YT_HAHN_CLIENT
import yt.wrapper as yt
import datetime

logger = logging.getLogger(__name__)

VENDOMAT_URL = "vendomat.yandex-team.ru"

class VendomatCollector(SimpleYTBasedCollector):
    def __init__(self):
        super().__init__(yt_table_name='monitoring/vendomat')
        self.schema = [
            {'name': 'equipment_name', 'type': 'string'},
            {'name': 'card_id', 'type': 'string'},
            {'name': 'issue_date', 'type': 'string'},
            {'name': 'vendomat_id', 'type': 'string'},
        ]
        self.data_for_yt = []
        logger.info('Start vendomat yt collection')

    def _fetch_data_by_day(self, year, month, day):
        data = requests.get('https://{url}/back/api/v1/core/issuing_history_by_date/{year}/{month}/{day}/?format=json'.format(
            year=year,
            month=month,
            day=day,
            url=VENDOMAT_URL
        ))
        return data.json()

    def __create_table(self):
        YT_HAHN_CLIENT.create("table",self.yt_table, attributes={"schema": self.schema}, force=True)

    def write_data_to_yt(self, data):
        data_len = int(len(data) / 100000) + 1
        table = yt.TablePath(name=self.yt_table, append=True)

        for num in range(data_len):
            start = num*100000
            end = (num+1)*100000
            YT_HAHN_CLIENT.write_table(table, data[start:end], format=yt.JsonFormat(attributes={"encode_utf8": False}))

    def process(self):
        today = datetime.datetime.now()
        data = self._fetch_data_by_day(today.year, today.month, today.day)
        self.write_data_to_yt(data)

    def process_all(self):
        result = []
        start = datetime.datetime(2019,5,15)
        end = datetime.datetime(2019,6,1)
        while start < end:
            start = start + datetime.timedelta(days=1)
            result.extend(self._fetch_data_by_day(start.year, start.month, start.day))
        self.write_data_to_yt(result)

def vendomat_collection_historical():
    VendomatCollector().process_all()

def vendomat_daily():
    VendomatCollector().process()