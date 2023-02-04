# -*- coding: utf-8 -*-
import requests
from btestlib import reporter, secrets

HITMAN_URL = 'https://hitman.yandex-team.ru/'
START_PROC = 'api/v1/execution/start/'
SERVICE = 'testing_balance_auto'
START_PROC_URL = HITMAN_URL + START_PROC + SERVICE

BOOKING_ID = 0
BOOKING_REGULAR_ID = ''
VERSION_ID = ''

TOKEN = secrets.get_secret(*secrets.Tokens.TESTPALM_OAUTH_TOKEN)


def start_process(booking_id, booking_regular_id, version):
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'OAuth ' + TOKEN
        }

    data = {
        'requester': 'robot-test-balance',
        'properties': {'booking_id': booking_id,
                       'version': version,
                       'booking_regular_id': booking_regular_id}
    }

    with reporter.step(u'Запускаем процесс в Hitman'):
        resp = requests.post(START_PROC_URL, headers=headers, json=data, verify=False)
        if resp.status_code != 200 or 'id' not in resp.json():
            raise Exception("Something went wrong: " + resp.text)
        else:
            reporter.log(u'Ссылка на запуск: {}'.format(resp.json()['url']))


if __name__ == "__main__":
    start_process(BOOKING_ID, BOOKING_REGULAR_ID, VERSION_ID)
