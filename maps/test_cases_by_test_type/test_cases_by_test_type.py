import json
import urllib.parse
from datetime import date
from os import getenv
import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

TESTPALM_TOKEN = getenv('TP_TOKEN')
STAT_TOKEN = getenv('STAT_TOKEN')

STAT_URL = 'Maps_Plus_Beta/QA/Confeta/test_cases_by_test_type'

PRODUCTS_LIST = [
    ['agency_room', 'attributes.5c751b7ebb580fe7cbc91b06'],
    ['autolauncher', 'attributes.5ed4f365c7b640b556d6cd60'],
    ['automotive', 'attributes.599d7d458895504b58d9e0e2'],
    ['courier', 'attributes.5a956ad9b3678123b197b9a8'],
    ['fbapi', 'attributes.5f3cdeba8807116ecabb7028'],
    ['mapkit', 'attributes.5a9e4594429f6167340e710c'],
    ['mirrors', 'attributes.5a97c199b9ace0b5249cd1c2'],
    ['mpro', 'attributes.5e1d897b27a990afebf4b600'],
    ['navi-mobile-testing', 'attributes.5fca14d9a5208c00116720dd'],
    ['navigator', 'attributes.56003712e4b0ea0b7660b0ce'],
    ['nmaps', 'attributes.58d2550bc6123349a8991292'],
    ['smb_cabinet', 'attributes.5ed4f561c7b640b556d6cdaa'],
    ['ugc_profile', 'attributes.6040f74043fbcc0085b4972b'],
    ['zapravki', 'attributes.5dce583ff97f248809cfd701'],
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

    smoke = {'type': 'EQ', 'key': tp_attr, 'value': 'smoke'}
    acceptance = {'type': 'EQ', 'key': tp_attr, 'value': 'acceptance'}
    regress = {'type': 'EQ', 'key': tp_attr, 'value': 'regress'}
    not_smoke = {'type': 'NEQ', 'key': tp_attr, 'value': 'smoke'}
    not_acceptance = {'type': 'NEQ', 'key': tp_attr, 'value': 'acceptance'}
    not_regress = {'type': 'NEQ', 'key': tp_attr, 'value': 'regress'}
    other = {'type': 'AND', 'left': {'type': 'AND', 'left': not_smoke, 'right': not_acceptance}, 'right': not_regress}
    actual_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'actual'}
    automated_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'automated'}
    smoke_manual_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': smoke}
    smoke_automated_cases_query = {'type': 'AND', 'left': automated_cases_query, 'right': smoke}
    acceptance_manual_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': acceptance}
    acceptance_automated_cases_query = {'type': 'AND', 'left': automated_cases_query, 'right': acceptance}
    regress_manual_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': regress}
    regress_automated_cases_query = {'type': 'AND', 'left': automated_cases_query, 'right': regress}
    other_manual_cases_query = {'type': 'AND', 'left': actual_cases_query, 'right': other}
    other_automated_cases_query = {'type': 'AND', 'left': automated_cases_query, 'right': other}

    actual_cases_query = get_test_cases_by_filter(tp_project, filter=actual_cases_query)
    automated_cases_query = get_test_cases_by_filter(tp_project, filter=automated_cases_query)
    smoke_manual_cases_query = get_test_cases_by_filter(tp_project, filter=smoke_manual_cases_query)
    smoke_automated_cases_query = get_test_cases_by_filter(tp_project, filter=smoke_automated_cases_query)
    acceptance_manual_cases_query = get_test_cases_by_filter(tp_project, filter=acceptance_manual_cases_query)
    acceptance_automated_cases_query = get_test_cases_by_filter(tp_project, filter=acceptance_automated_cases_query)
    regress_manual_cases_query = get_test_cases_by_filter(tp_project, filter=regress_manual_cases_query)
    regress_automated_cases_query = get_test_cases_by_filter(tp_project, filter=regress_automated_cases_query)
    other_manual_cases_query = get_test_cases_by_filter(tp_project, filter=other_manual_cases_query)
    other_automated_cases_query = get_test_cases_by_filter(tp_project, filter=other_automated_cases_query)

    queries_list.extend([actual_cases_query, automated_cases_query, smoke_manual_cases_query,
                         smoke_automated_cases_query, acceptance_manual_cases_query, acceptance_automated_cases_query,
                         regress_manual_cases_query, regress_automated_cases_query, other_manual_cases_query,
                         other_automated_cases_query])

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
                'automated_cases_count': queries_list[1],
                'smoke_manual_cases_count': queries_list[2],
                'smoke_automated_cases_count': queries_list[3],
                'acceptance_manual_cases_count': queries_list[4],
                'acceptance_automated_cases_count': queries_list[5],
                'regress_manual_cases_count': queries_list[6],
                'regress_automated_cases_count': queries_list[7],
                'other_manual_cases_count': queries_list[8],
                'other_automated_cases_count': queries_list[9],
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
