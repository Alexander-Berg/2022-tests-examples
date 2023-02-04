import json
import urllib.parse
from datetime import date
from os import getenv
import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

TESTPALM_TOKEN = getenv('TP_TOKEN')
STAT_TOKEN = getenv('STAT_TOKEN')

STAT_URL = 'Maps_Plus_Beta/QA/Confeta/test_cases_by_nsfa'

PRODUCTS_LIST = [
    ['agency_room', 'attributes.5eb3c9d29fcbfa398eb63ac9', 'attributes.5efb553ec7b640a6c4028030'],
    ['autolauncher', 'attributes.5ea181b29fcbfa398eb1dcff', 'attributes.5efb390d396e4d4e0a7f634c'],
    ['automotive', 'attributes.5dd3d23f27a9909ea5a30eb9', 'attributes.5ef0abbde1ff184aeb53189f'],
    ['courier', 'attributes.5e32a75927a9906d48031fc3', 'attributes.5efb56e0c7b640a6c402804c'],
    ['fbapi', 'attributes.5f364853b9a54406ddec8436', 'attributes.5f3cde85e1ff18c4633ef4ef'],
    ['mapkit', 'attributes.5da6ca123d42cbe0f89efee8', 'attributes.5eea0d65396e4d303e036986'],
    ['mirrors', 'attributes.5ea8787d42a89a496ee62735', 'attributes.5ef10abc8807116bfdf41c3b'],
    ['mpro', 'attributes.5dfc03c827a990afebedab05', 'attributes.5ef109d6e1ff184aeb531c41'],
    ['navi-mobile-testing', 'attributes.5fca14d8b2333200112b0730', 'attributes.5fca14d85536700011bf3487'],
    ['navigator', 'attributes.5dd50ef38d20300e237b7501', 'attributes.5eea02abe1ff184aeb52721d'],
    ['nmaps', 'attributes.5dd56a463d42cb93aafc7c0c', 'attributes.5ef10a69e1ff184aeb531c45'],
    ['smb_cabinet', 'attributes.5eb3ca94396e4d94bb52236d', 'attributes.5efb5aecb9a544fbb06d883d'],
    ['ugc_profile', 'attributes.6040f41cc2b1880104d58f45', 'attributes.6040f7198adb6d00fd80b174'],
    ['zapravki', 'attributes.5ea181c3396e4d94bb4d24de', 'attributes.5efb3955e1ff18bc88862811'],
]


def get_test_cases_by_filter(project, filter):
    tp_url = f'https://testpalm-api.yandex-team.ru/testcases/{project}?expression='
    req_headers = {'Authorization': 'OAuth %s' % TESTPALM_TOKEN}
    url = tp_url + urllib.parse.quote(json.dumps(filter, ensure_ascii=False, separators=(',', ': ')))
    params = {'include': 'id'}

    res = requests.get(url, params=params, headers=req_headers, verify=False)
    cases = res.json()

    return len(cases)


def get_queries_list_by_product(product):
    queries_list = []
    tp_project = product[0]
    tp_attr_nsfa = product[1]
    tp_attr_reason = product[2]

    time = {'type': 'EQ', 'key': tp_attr_reason, 'value': 'time'}
    access = {'type': 'EQ', 'key': tp_attr_reason, 'value': 'access'}
    special_app = {'type': 'EQ', 'key': tp_attr_reason, 'value': 'special app'}
    special_accessories = {'type': 'EQ', 'key': tp_attr_reason, 'value': 'special accessories'}
    field_test = {'type': 'EQ', 'key': tp_attr_reason, 'value': 'field test'}
    logs = {'type': 'EQ', 'key': tp_attr_reason, 'value': 'logs'}
    payments = {'type': 'EQ', 'key': tp_attr_reason, 'value': 'payments'}
    head_unit = {'type': 'EQ', 'key': tp_attr_reason, 'value': 'special devices'}
    not_time = {'type': 'NEQ', 'key': tp_attr_reason, 'value': 'time'}
    not_access = {'type': 'NEQ', 'key': tp_attr_reason, 'value': 'access'}
    not_special_app = {'type': 'NEQ', 'key': tp_attr_reason, 'value': 'special app'}
    not_special_accessories = {'type': 'NEQ', 'key': tp_attr_reason, 'value': 'special accessories'}
    not_field_test = {'type': 'NEQ', 'key': tp_attr_reason, 'value': 'field test'}
    not_logs = {'type': 'NEQ', 'key': tp_attr_reason, 'value': 'logs'}
    not_payments = {'type': 'NEQ', 'key': tp_attr_reason, 'value': 'payments'}
    not_head_unit = {'type': 'NEQ', 'key': tp_attr_reason, 'value': 'special devices'}
    wrong =\
        {'type': 'AND', 'left':
            {'type': 'AND', 'left':
                {'type': 'AND', 'left':
                    {'type': 'AND', 'left':
                        {'type': 'AND', 'left':
                            {'type': 'AND', 'left':
                                {'type': 'AND', 'left': not_logs, 'right': not_payments},
                             'right': not_access},
                         'right': not_field_test},
                     'right': not_time},
                 'right': not_special_app},
             'right': not_special_accessories},
         'right': not_head_unit}

    actual_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'actual'}
    nsfa = {'type': 'EQ', 'key': tp_attr_nsfa, 'value': 'nsfa'}
    actual_nsfa = {'type': 'AND', 'left': actual_cases_query, 'right': nsfa}
    time_cases = {'type': 'AND', 'left': actual_nsfa, 'right': time}
    access_cases = {'type': 'AND', 'left': actual_nsfa, 'right': access}
    special_app_cases = {'type': 'AND', 'left': actual_nsfa, 'right': special_app}
    special_accessories_cases = {'type': 'AND', 'left': actual_nsfa, 'right': special_accessories}
    field_test_cases = {'type': 'AND', 'left': actual_nsfa, 'right': field_test}
    logs_cases = {'type': 'AND', 'left': actual_nsfa, 'right': logs}
    payments_cases = {'type': 'AND', 'left': actual_nsfa, 'right': payments}
    head_unit_cases = {'type': 'AND', 'left': actual_nsfa, 'right': head_unit}
    wrong_cases = {'type': 'AND', 'left': actual_nsfa, 'right': wrong}

    actual_nsfa_cases_query = get_test_cases_by_filter(tp_project, filter=actual_nsfa)
    time_cases_query = get_test_cases_by_filter(tp_project, filter=time_cases)
    access_cases_query = get_test_cases_by_filter(tp_project, filter=access_cases)
    special_app_cases_query = get_test_cases_by_filter(tp_project, filter=special_app_cases)
    special_accessories_cases_query = get_test_cases_by_filter(tp_project, filter=special_accessories_cases)
    field_test_cases_query = get_test_cases_by_filter(tp_project, filter=field_test_cases)
    logs_cases_query = get_test_cases_by_filter(tp_project, filter=logs_cases)
    payments_cases_query = get_test_cases_by_filter(tp_project, filter=payments_cases)
    head_unit_cases_query = get_test_cases_by_filter(tp_project, filter=head_unit_cases)
    wrong_cases_query = get_test_cases_by_filter(tp_project, filter=wrong_cases)

    queries_list.extend([actual_nsfa_cases_query, time_cases_query, access_cases_query, special_app_cases_query,
                         special_accessories_cases_query, field_test_cases_query, logs_cases_query,
                         payments_cases_query, head_unit_cases_query, wrong_cases_query])

    return queries_list


def main():
    today = date.today().isoformat()

    for product in PRODUCTS_LIST:
        queries_list = get_queries_list_by_product(product)

        data = [
            {
                'fielddate': today,
                'product': product[0],
                'total': queries_list[0],
                'time': queries_list[1],
                'access': queries_list[2],
                'special_app': queries_list[3],
                'special_accessories': queries_list[4],
                'special_condition': queries_list[5],
                'logs': queries_list[6],
                'payments': queries_list[7],
                'head_unit': queries_list[8],
                'wrong': queries_list[9]
            }
        ]

        requests.post(
            'https://upload.stat.yandex-team.ru/_api/report/data',
            headers={'Authorization': 'OAuth %s' % STAT_TOKEN},
            data={
                'name': STAT_URL,
                'scale': 'd',
                'data': json.dumps({'values': data}),
            },
        )


if __name__ == '__main__':
    main()
