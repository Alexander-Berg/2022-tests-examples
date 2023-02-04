import json
import urllib.parse
import urllib3
from datetime import date
from os import getenv
import requests

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

TESTPALM_TOKEN = getenv('TP_TOKEN')
STAT_TOKEN = getenv('STAT_TOKEN')

STAT_URL = 'Maps_Plus_Beta/QA/Confeta/test_cases_by_status'

PRODUCTS_LIST = [
    'agency_room',
    'autolauncher',
    'automotive',
    'courier',
    'fbapi',
    'mapkit',
    'mirrors',
    'mpro',
    'navi-mobile-testing',
    'navigator',
    'nmaps',
    'smb_cabinet',
    'ugc_profile',
    'zapravki'
]


def get_test_cases_by_filter(project, filter):
    tp_url = f'https://testpalm-api.yandex-team.ru/testcases/{project}?expression='
    req_headers = {'Authorization': 'OAuth %s' % TESTPALM_TOKEN}
    if filter != '':
        url = tp_url + urllib.parse.quote(json.dumps(filter, ensure_ascii=False, separators=(',', ': ')))
    else:
        url = tp_url
    params = {'include': 'id'}

    res = requests.get(url, params=params, headers=req_headers, verify=False)
    cases = res.json()

    return len(cases)


def get_queries_list_by_product(product):
    queries_list = []

    draft_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'draft'}
    review_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'on review'}
    actual_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'actual'}
    nc_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'needs changes'}
    aip_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'automation in progress'}
    automated_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'automated'}
    nr_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'needs repair'}
    duplicate_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'duplicate'}
    archived_cases_query = {'type': 'EQ', 'key': 'status', 'value': 'archived'}

    all_cases_query = get_test_cases_by_filter(product, filter='')
    draft_cases_query = get_test_cases_by_filter(product, filter=draft_cases_query)
    review_cases_query = get_test_cases_by_filter(product, filter=review_cases_query)
    actual_cases_query = get_test_cases_by_filter(product, filter=actual_cases_query)
    nc_cases_query = get_test_cases_by_filter(product, filter=nc_cases_query)
    aip_cases_query = get_test_cases_by_filter(product, filter=aip_cases_query)
    automated_cases_query = get_test_cases_by_filter(product, filter=automated_cases_query)
    nr_cases_query = get_test_cases_by_filter(product, filter=nr_cases_query)
    duplicate_cases_query = get_test_cases_by_filter(product, filter=duplicate_cases_query)
    archived_cases_query = get_test_cases_by_filter(product, filter=archived_cases_query)

    queries_list.extend([all_cases_query, draft_cases_query, review_cases_query, actual_cases_query, nc_cases_query,
                         aip_cases_query, automated_cases_query, nr_cases_query, duplicate_cases_query,
                         archived_cases_query])

    return queries_list


def main():
    today = date.today().isoformat()

    for product in PRODUCTS_LIST:
        queries_list = get_queries_list_by_product(product)

        data = [
            {
                'fielddate': today,
                'product': product,
                'all_cases_count': queries_list[0],
                'draft_cases_count': queries_list[1],
                'review_cases_count': queries_list[2],
                'actual_cases_count': queries_list[3],
                'nc_cases_count': queries_list[4],
                'aip_cases_count': queries_list[5],
                'automated_cases_count': queries_list[6],
                'nr_cases_count': queries_list[7],
                'duplicate_cases_count': queries_list[8],
                'archived_cases_count': queries_list[9]
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
