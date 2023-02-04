from pprint import pprint
from time import sleep

from intranet.wiki.tools.wikiclient import EnvType, Flavor, get_contour
from intranet.wiki.tests.wiki_tests.common.e2e.decorators import e2e, must_be_true_within
from intranet.wiki.tests.wiki_tests.common.e2e.config import e2e_settings


@e2e
def test_async_ops():
    wiki_api = get_contour(EnvType.TESTING, Flavor.INTRANET).wiki_api.oauth(e2e_settings.oauth)
    code, response = wiki_api.api_call('post', 'frontend/.async_operations/test', json={'number': 10})
    assert code == 200
    task_id = response['data']['task_id']
    resps = []
    for i in range(60):
        code, response = wiki_api.api_call('get', f'frontend/.async_operations?id={task_id}')

        resps.append(response['data'])
        sleep(0.1)
        if response['data']['status'] == 'success':
            return

    assert False, f'Never succeeded! {resps}'


@e2e
def test_async_ops_api_v2():
    wiki_api = get_contour(EnvType.TESTING, Flavor.INTRANET).wiki_api.oauth(e2e_settings.oauth)
    wiki_api.use_api_v2_public()

    code, response = wiki_api.api_call('post', 'operations/counter', json={'number': 30})
    assert code == 200
    status_url = response['status_url']

    resps = []
    for i in range(60):
        code, response = wiki_api.api_call('get', status_url, api='')
        assert code == 200
        resps.append(response)
        sleep(0.1)
        if response['status'] == 'success':
            pprint(resps)
            return

    assert False, f'Never succeeded! {resps}'


CLUSTER_PAGES = ['root', 'root/a', 'root/a/aa', 'root/b']


@e2e
def test_move_smoke():
    service_api = get_contour(EnvType.TESTING, Flavor.INTRANET).wiki_api
    user_api = get_contour(EnvType.TESTING, Flavor.INTRANET).wiki_api.oauth(e2e_settings.oauth)

    for f in CLUSTER_PAGES:
        assert service_api.page_exists(f'e2e/fixture/{f}')

    service_api.use_api_v2_public()
    user_api.use_api_v2_public()

    with service_api.fixture_ctx() as slug_prefix:
        for f in CLUSTER_PAGES:
            assert service_api.page_exists(f'{slug_prefix}{f}')

        code, data = user_api.api_call('get', f'pages?slug={slug_prefix}root')
        assert code == 200
        page_id = data['id']

        code, data = user_api.api_call('post', f'pages/{page_id}/move', {'target': f'{slug_prefix}xxx'})
        assert code == 200

        status_url = data['status_url']
        assert operation_finished(status_url, user_api)

        for f in CLUSTER_PAGES:
            new_path = f.replace('root', 'xxx', 1)
            assert service_api.page_exists(f'{slug_prefix}{new_path}')


@must_be_true_within(60)
def operation_finished(url, user_api):
    code, response = user_api.api_call('get', url, api='', parse_json=True)
    return response['status'] == 'success'
