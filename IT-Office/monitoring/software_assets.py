import yt.wrapper as yt
from source.utils import (YT_HAHN_CLIENT, AUTH_HEADERS_BOT, exc_thread_wrapper)
import requests
import json
import logging
import time

MS_OFFICE_NUM = "L10025182"
YT_CLIENT = YT_HAHN_CLIENT

logger = logging.getLogger(__name__)

class OfficeAssets():
    def _fetch_data_mac_from_yt(self):
        held_licenses = YT_CLIENT.select_rows(
            'inv_number from [{}] where inv_num_lic="L10025182"'.format(
                "//home/helpdesk/cmdb/soft_oebs"),
            format=yt.JsonFormat(attributes={"encode_utf8": False}))
        installed_software = YT_CLIENT.select_rows(
            'NB.instance_number from [{}] as SOFT left join [{}] as NB ON (SOFT.instance_number)=(NB.instance_number) where SOFT.ms_office=true AND NB.status_name="OPERATION"'.format(
                "//home/helpdesk/cmdb/jss_installed_software",
                "//home/helpdesk/cmdb/notebooks"
            ),
            format=yt.JsonFormat(attributes={"encode_utf8": False}))

        set_held_license = {x["inv_number"] for x in held_licenses}
        set_installed_software = {x["NB.instance_number"] for x in installed_software}

        result = set_installed_software.difference(set_held_license)
        logger.info('Parsed {} record in macOS installed apps'.format(len(list(result))))
        return result

    def _fetch_data_win_from_yt(self):
        held_licenses = YT_CLIENT.select_rows(
            'inv_number from [{}] where inv_num_lic="L10025182"'.format(
                "//home/helpdesk/cmdb/soft_oebs"),
            format=yt.JsonFormat(attributes={"encode_utf8": False}))
        installed_software = YT_CLIENT.select_rows(
            'NB.instance_number from [{}] as SOFT left join [{}] as NB ON (SOFT.instance_number)=(NB.instance_number) where SOFT.ms_office=true AND NB.status_name="OPERATION"'.format(
                "//home/helpdesk/cmdb/sccm_installed_software",
                "//home/helpdesk/cmdb/notebooks"
            ),
            format=yt.JsonFormat(attributes={"encode_utf8": False}))

        set_held_license = {x["inv_number"] for x in held_licenses}
        set_installed_software = {x["NB.instance_number"] for x in installed_software}

        result = set_installed_software.difference(set_held_license)
        logger.info('Parsed {} record in macOS installed apps'.format(len(list(result))))
        return result

    def process(self):
        mac_data = self._fetch_data_mac_from_yt()
        win_data = self._fetch_data_win_from_yt()
        all_data = win_data.union(mac_data)

        for item in all_data:
            error = True
            try_count = 2

            while error and try_count:
                data = json.dumps({"softwares":[MS_OFFICE_NUM],
                        "quantity":1,
                        "hardwares": [item]})

                req = requests.post("https://bot.yandex-team.ru/api/v2/oebs/software/relation",
                              data=data,
                              headers=AUTH_HEADERS_BOT)
                try_count -= 1
                error = req.json()["error"]
                if error:
                    logger.warning(
                        '500 in bot api data: lic {} , inv: {}, response: {}'.format(
                            MS_OFFICE_NUM, item, req.content))
                    time.sleep(2)
            if not error:
                logger.info('Successfuly added lic {} to inv {}'.format(MS_OFFICE_NUM, item))

@exc_thread_wrapper
def main_office_assets():
    logger.info('Start Office Assets')
    OfficeAssets().process()