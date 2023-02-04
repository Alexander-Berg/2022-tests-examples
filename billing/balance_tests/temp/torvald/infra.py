# -*- coding: utf-8 -*-

import requests
import datetime
import time
import json

TOKEN = ''

INFRA_URL = 'https://infra-api.yandex-team.ru/v1/'

BALANCE_SERVICE_ID = 1592

PROD = 2454
TEST = 2455
LOAD = 2456
PT = 2457
MNCLOSE = 2653

headers = {
        'Authorization': 'OAuth {}'.format(TOKEN),
        'Content-Type': 'application/json',
    }

def to_timestamp(dt):
    return time.mktime(dt.timetuple())

def create_event():
    handle = 'events'
    data = {
              "title": "Test event3",
              "description": "Some description",
              "environmentId": MNCLOSE,
              "serviceId": BALANCE_SERVICE_ID,
              "startTime": to_timestamp(datetime.datetime(2020,6,26,17,53,0)),
              "finishTime": to_timestamp(datetime.datetime(2020,6,26,18,00,0)),
              "type": "issue",
              "severity": "minor",
              # "man": False,
              # "myt": True,
              # "sas": True,
              # "vla": True,
              # "iva": True,
              "tickets": "",
              "meta": {
                "someKey": "someValue13",
                "anotherKey": 1313
              },
              "sendEmailNotifications": False,
              # "setAllAvailableDc": True
            }

    response = requests.post(INFRA_URL + handle, data=json.dumps(data), headers=headers, verify=False)
    if response.status_code == 200:
        result = response.json()
    pass

def resolve_event(event_id):
    handle = 'events'
    response = requests.delete(INFRA_URL + handle + '/{}'.format(event_id), headers=headers, verify=False)
    pass

def namespaces():
    handle = 'namespaces'
    response = requests.get(INFRA_URL + handle, headers=headers, verify=False)
    print response

if __name__ == '__main__':
    create_event()
    # resolve_event(997874)