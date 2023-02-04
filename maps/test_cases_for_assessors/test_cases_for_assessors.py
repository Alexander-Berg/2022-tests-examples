import json
import urllib.parse
from datetime import date
from os import getenv
import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

TESTPALM_TOKEN = getenv('TP_TOKEN')
STAT_TOKEN = getenv('STAT_TOKEN')

STAT_URL = 'Maps_Plus_Beta/QA/Confeta/test_count'

PRODUCTS_LIST = [
    ['agency_room', 'attributes.5eb3c9d29fcbfa398eb63ac9'],
    ['autolauncher', 'attributes.5ea181b29fcbfa398eb1dcff'],
    ['automotive', 'attributes.5dd3d23f27a9909ea5a30eb9'],
    ['courier', 'attributes.5e32a75927a9906d48031fc3'],
    ['fbapi', 'attributes.5f364853b9a54406ddec8436'],
    ['mapkit', 'attributes.5da6ca123d42cbe0f89efee8'],
    ['mirrors', 'attributes.5ea8787d42a89a496ee62735'],
    ['mpro', 'attributes.5dfc03c827a990afebedab05'],
    ['navi-mobile-testing', 'attributes.5fca14d8b2333200112b0730'],
    ['navigator', 'attributes.5dd50ef38d20300e237b7501'],
    ['nmaps', 'attributes.5dd56a463d42cb93aafc7c0c'],
    ['smb_cabinet', 'attributes.5eb3ca94396e4d94bb52236d'],
    ['ugc_profile', 'attributes.6040f41cc2b1880104d58f45'],
    ['zapravki', 'attributes.5ea181c3396e4d94bb4d24de'],
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

    ready = {'type': 'EQ', 'key': tp_attr, 'value': 'ready'}
    nsfa = {'type': 'EQ', 'key': tp_attr, 'value': 'nsfa'}
    not_nsfa = {'type': 'NEQ', 'key': tp_attr, 'value': 'nsfa'}
    not_ready = {'type': 'NEQ', 'key': tp_attr, 'value': 'ready'}
    wrong = {'type': 'AND', 'left': not_nsfa, 'right': not_ready}
    actual_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'actual'}
    ready_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': ready}
    nsfa_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': nsfa}
    wrong_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': wrong}

    actual_cases_query = get_test_cases_by_filter(tp_project, filter=actual_cases_query)
    ready_cases_query = get_test_cases_by_filter(tp_project, filter=ready_cases_query)
    nsfa_cases_query = get_test_cases_by_filter(tp_project, filter=nsfa_cases_query)
    wrong_cases_query = get_test_cases_by_filter(tp_project, filter=wrong_cases_query)

    queries_list.extend([actual_cases_query, ready_cases_query, nsfa_cases_query, wrong_cases_query])

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
                'ready_cases_count': queries_list[1],
                'nsfa_cases_count': queries_list[2],
                'wrong_cases_count': queries_list[3]
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
