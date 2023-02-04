import datetime

import requests
from collections import defaultdict

from source.config import *
from source.monitoring.monitoring import SimpleYTBasedCollector
from source.utils import exc_thread_wrapper

logger = logging.getLogger(__name__)

YT_CLIENT = make_hahn_client()

class SIPBotYTCollector(SimpleYTBasedCollector):
    def __init__(self, **kwargs):
        super().__init__(yt_table_name='monitoring/sip_logs',
                         recreate=kwargs.get('recreate',False),
                         client=YT_HAHN_CLIENT,
                         is_table_dynamic=False,
                         static_append=False
                         )
        self.schema = [
            {'name': 'log_id', 'type': 'int64'},
            {'name': 'instance', 'type': 'string'},
            {'name': 'location', 'type': 'string'},
            {'name': 'staff_login', 'type': 'string'},
            {'name': 'role', 'type': 'string'},
            {'name': 'log_start', 'type': 'int64'},
            {'name': 'log_end', 'type': 'int64'},
        ]

    @staticmethod
    def _get_timestamp(date):
        try:
            return int(datetime.datetime.strptime(
                date,
                "%Y-%m-%d %H:%M:%S").timestamp())
        except TypeError:
            return None

    def _fetch_data(self):
        result = []
        results = requests.get(
            'https://bot.yandex-team.ru/api/view.php?name=view_hd_sip_logs&format=json',
            headers=AUTH_HEADERS_BOT).json()
        approved_keys = [x['name'] for x in self.schema]
        seen_logins = set()

        for count, record in enumerate(results):
            midd_dict = {key: record[key] for key in record if
                         key in approved_keys}

            check_string = "{}:{}".format(midd_dict["staff_login"],
                                          midd_dict["log_start"][:10])
            if check_string in seen_logins:
                continue

            seen_logins.add(check_string)

            midd_dict["log_id"] = int(midd_dict["log_id"])
            midd_dict["log_start"] = self._get_timestamp(midd_dict["log_start"])
            midd_dict["log_end"] = self._get_timestamp(midd_dict["log_end"])
            result.append(midd_dict)
        return result

    def main(self):
        data = self._fetch_data()
        self.write_data_to_yt(data)

@exc_thread_wrapper
def main_sip_logs():
    logger.info('CMDB SIP logs started')
    SIPBotYTCollector().main()
