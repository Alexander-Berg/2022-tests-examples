# -*- coding: utf-8 -*-
import time
import datetime
import collections
from threading import Thread
from requests.exceptions import ConnectionError

from btestlib.constants import Users
from btestlib.utils import get_secret_key
import btestlib.passport_steps as passport_steps
from balance.snout_steps.api_steps import get_handle_url, HEADERS, call_http_raw

_idx = 0

LOCAL = True
LOCALHOST = 'http://127.0.0.1:8081'
# LOCALHOST = 'http://127.0.0.1:8080'

user = Users.YB_ADM

false = False
true = True
null = None


class Session(object):
    cookies = {
        'Session_id': '1234',
        'sessionid2': '1111',
    }


def print_log(*datas):
    global _idx
    _idx += 1
    dt = '{:%H:%M:%S.%f}'.format(datetime.datetime.now())
    print '{}. {} - {}'.format(_idx, dt[:-4], ', '.join(map(str, datas)))


def get_session():
    if LOCAL:
        return Session()
    else:
        return passport_steps.auth_session(user=user)


def make_request(url, session, method='GET', custom_headers=None, data=None, json_data=None, files=None):
    if LOCAL:
        method_url = LOCALHOST + url
    else:
        method_url = get_handle_url(url, None, None)
    headers = HEADERS[method].copy()
    if custom_headers:
        headers.update(custom_headers)
    print_log(method_url)
    response, result = call_http_raw(method_url, method=method, headers=headers, params=data, json_data=json_data, files=None,
                                     cookies=dict(Session_id=session.cookies['Session_id'],
                                                  sessionid2=session.cookies['sessionid2']),
                                     )
    return response, result


def get_timeout(seconds, session):
    res, result = make_request(
        'debug/timeout?timeout=%s&unique_key=5' % seconds,
        session=session,
        method='GET',
        custom_headers=None,
    )
    print_log(res.status_code, res.json())
    return res.status_code


def get_version(session):
    res, result = make_request(
        '/version',
        session=session,
        method='GET',
        custom_headers=None,
    )
    print_log(res.status_code, res.json())
    return res.status_code


def get_option_for_create_request(session):
    res, result = make_request(
        '/v1/invoice/create-request',
        session=session,
        method='OPTIONS',
        custom_headers=None,
    )
    print_log(res.status_code, res.json())
    return res.status_code


def create_request(session):
    res, result = make_request(
        'invoice/create-request',
        session=session,
        method='OPTIONS',
        custom_headers={'Content-Type': 'application/x-www-form-urlencoded'},
        data={
            "firm_id": 1,
            "raw_order_data": [
                {
                    "order_client_id": 1,
                    "discount": "10",
                    "memo": "string",
                    "product_id": 1,
                    "quantity": 1
                }
            ],
            "manager_code": 10,
            "client_id": 10,
            "documents_dt": "2020-07-28T15:45:59.419Z"
        },
    )
    print_log(res.status_code, res.json())
    return res.status_code


def create_person(session):
    res, result = make_request(
        'person/set-person',
        session=session,
        method='POST',
        custom_headers={
            'Content-Type': 'application/json',
        },
        json_data={
          "client_id": 187768322,
          "is_partner": true,
          "person_type": "ur",
          "data": {
            "phone": "00000",
            "fax": "456",
            "email": "glazkova-test@yandex.ru",
            "representative": "Лицо",
            "postaddress": "Литейная, 15",
            "postcode": "76454",
            "inn": "9705121040",
            "iban": "",
            "other":"",
            "account": "12345678",
            "swift": "ABSRNOK1XXX",
            "signer_person_name": "Иванов И.И.",
            "signer_position_name": "Управляющий",
            "authority_doc_type": "Доверенность",
            "invalid_bankprops": true,
            "pay_type": "1",
            "corr_swift": "09876544",
            "ben_bank": ""
          },
          "person_id": 13508287,
          "_csrf": get_secret_key(user.uid)
        },
    )
    print_log(res.status_code, res.json())
    return res.status_code


def validate_docs(session):
    res, result = make_request(
        '/v1/person/validate-docs',
        session=session,
        method='POST',
        custom_headers={
            'Accept': 'application/x-www-form-urlencoded; charset=utf-8',
            # 'X-CSRF': get_secret_key(user.uid),
            # 'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Type': 'multipart/form-data',
        },
        data={'person_id': 13301210},
        files=[],
    )
    print_log(res.status_code, res.json())
    return res.status_code


def validate_docs_2(session):
    import io
    import requests

    with open('/home/natabers/projects/mine/sandbox/!notes/team-city.txt', 'rb') as f:
        res = requests.post(
            'http://127.0.0.1:8080/v1/person/validate-docs',
            headers={
                # 'accept': 'application/x-www-form-urlencoded; charset=utf-8',
                # 'Accept-Encoding': 'gzip, deflate, br',
                # 'Accept-Language': 'ru,en;q=0.9',
                # 'Connection': 'keep-alive',
                # 'Content-Length': '49105',
                # 'Content-Type': 'multipart/form-data',
                # # 'Content-Type': 'application/x-www-form-urlencoded',
                # 'Host': '127.0.0.1:8081',
                # 'Origin': 'http://127.0.0.1:8081',
                # 'Referer': 'http://127.0.0.1:8081/v1/',
                # 'Sec-Fetch-Mode': 'cors',
                # 'Sec-Fetch-Site': 'same-origin',
                # 'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.136 YaBrowser/20.2.3.320 (beta) Yowser/2.5 Safari/537.36',
                # 'X-CSRF': 'ud188fc2c525dcab8e3fb0986220b886fc871552eaf3d6b98401a385e9a5af86cabb9f1dcaf1bac3fc9ff0daf888be18f0c063474d28adc4a8c09b0acb115d577',
                # 'X-Is-Admin': 'true',
            },
            data={
                'person_id': 13301210,
                # 'file': f,
            },
            files={'file': f.read()},
        )
    print res.status_code
    print res.json()
    return res.status_code, res.json()


def debug_dump(session):
    res, result = make_request(
        'debug/dump',
        session=session,
        method='GET',
    )
    print_log(res.status_code, res.json())
    return res.status_code


def get_pin_code(session):
    res, result = make_request(
        'user/pin-code',
        session=session,
        method='GET',
    )
    print_log(res.status_code, res.json())
    return res.status_code


def get_phones(session):
    res, result = make_request(
        '/v1/passport_sms/phone/list',
        session=session,
        method='GET',
    )
    print_log(res.status_code, res.json())
    return res.status_code


class MyTread(Thread):

    def __init__(self, method_name, kwargs):
        super(MyTread, self).__init__()
        self.method_name = method_name
        self.kwargs = kwargs

    def run(self):
        self.method_name(**self.kwargs)


def run(session):
    while True:
        for _i in range(1):
            thread = MyTread(get_timeout, {'seconds': 20, 'session': session})
            thread.start()
        time.sleep(1)


def check_timings(session):
    codes = collections.defaultdict(lambda: 0)
    start_dt = stop_dt = None
    start_dt_502 = []
    stop_dt_502 = []
    try:
        start_dt = datetime.datetime.now()
        ok = True
        while True:
            try:
                codes[get_timeout(1, session)] += 1
            except ConnectionError:
                if ok:
                    ok = False
                    start_dt_502.append(datetime.datetime.now())
                print_log(502)
                codes[502] += 1
                time.sleep(1)
            else:
                if not ok:
                    ok = True
                    stop_dt_502.append(datetime.datetime.now())
    except KeyboardInterrupt:
        stop_dt = datetime.datetime.now()
        for code, count in codes.items():
            print '%s: %s' % (code, count)

        if start_dt and stop_dt:
            print 'General: {:%H:%M:%S.%f} - {:%H:%M:%S.%f} = {}'.format(start_dt, stop_dt, stop_dt - start_dt)

        if start_dt_502 and stop_dt_502:
            durations = []
            for t1, t2 in zip(start_dt_502, stop_dt_502):
                duration = (t2 - t1)
                durations.append(duration)
                print '502: {:%H:%M:%S.%f} - {:%H:%M:%S.%f} = {}'.format(t1, t2, duration)

            print 'Reloads: %s' % len(durations)
            if durations:
                sum_duration = durations[0]
                for d in durations[1:]:
                    sum_duration += d
                print 'Sum duration: ', sum_duration
                print 'Avg duration: ', (sum_duration / len(durations))


if __name__ == '__main__':
    session = get_session()

    # get_version(session)
    # try:
    #     get_timeout(50, session)
    # except Exception as e:
    #     print 'fop server error: %s' % format(e)

    # try:
    #     run(session)
    # except KeyboardInterrupt:
    #     print 'STOP'

    # get_option_for_create_request(session)
    # create_request(session)
    # create_person(session)
    # validate_docs(session)
    # validate_docs_2(session)
    # debug_dump(session)
    # get_pin_code(session)
    get_phones(session)

