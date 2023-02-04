import json
import urllib.parse
from datetime import date
from os import getenv
import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

TESTPALM_TOKEN = getenv('TP_TOKEN')
STAT_TOKEN = getenv('STAT_TOKEN')

STAT_URL = 'Maps_Plus_Beta/QA/Confeta/test_cases_yauto_emulator'

PRODUCTS_LIST = [
    ['agency_room', 'attributes.5f0c27d5396e4d4e0a85a3a1'],
    ['autolauncher', 'attributes.5f0c2820c7b640a6c4060de6'],
    ['automotive', 'attributes.5e5e97729fcbfa71d31ab414'],
    ['courier', 'attributes.5f0c2af5396e4d4e0a85a512'],
    ['fbapi', 'attributes.5f3cdef0e1ff18c4633ef4f2'],
    ['mapkit', 'attributes.5f0c2b987440c319edd71189'],
    ['mirrors', 'attributes.5f0c2bc87440c319edd7118d'],
    ['mpro', 'attributes.5f0c2c007440c319edd71191'],
    ['navi-mobile-testing', 'attributes.5fce3b84d434cb00113056fe'],
    ['navigator', 'attributes.5f0c2c357440c319edd71194'],
    ['nmaps', 'attributes.5f0c2704b9a544fbb0709f9d'],
    ['smb_cabinet', 'attributes.5f0c2c9db9a544fbb070a202'],
    ['ugc_profile', 'attributes.6040f75f6b47e800fdd8b19b'],
    ['zapravki', 'attributes.5f0c2cd9880711ea6ae99b7a'],
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
    tp_attr = product[1]

    suitable = {'type': 'EQ', 'key': tp_attr, 'value': 'suitable'}
    ntm = {'type': 'EQ', 'key': tp_attr, 'value': 'need to modify'}
    nsfe = {'type': 'EQ', 'key': tp_attr, 'value': 'not suitable for emulator'}
    other = {'type': 'EQ', 'key': tp_attr, 'value': None}
    actual_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'actual'}
    suitable_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': suitable}
    ntm_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': ntm}
    nsfe_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': nsfe}
    other_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': other}

    actual_cases_query = get_test_cases_by_filter(tp_project, filter=actual_cases_query)
    suitable_cases_query = get_test_cases_by_filter(tp_project, filter=suitable_cases_query)
    ntm_cases_query = get_test_cases_by_filter(tp_project, filter=ntm_cases_query)
    nsfe_cases_query = get_test_cases_by_filter(tp_project, filter=nsfe_cases_query)
    other_cases_query = get_test_cases_by_filter(tp_project, filter=other_cases_query)

    queries_list.extend([actual_cases_query, suitable_cases_query, ntm_cases_query, nsfe_cases_query,
                         other_cases_query])

    return queries_list


def main():
    today = date.today().isoformat()

    for product in PRODUCTS_LIST:
        queries_list = get_queries_list_by_product(product)

        data = [
            {
                'fielddate': today,
                'product': product[0],
                'actual_cases_count': queries_list[0],
                'suitable_cases_count': queries_list[1],
                'ntm_cases_count': queries_list[2],
                'nsfe_cases_count': queries_list[3],
                'other_cases_count': queries_list[4],
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
