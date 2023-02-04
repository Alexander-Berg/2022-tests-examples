from collections import OrderedDict

import mock
import pytest
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import ELASTIC


@pytest.mark.django_db
def test_enrich_acl(client, wiki_users, organizations, groups, test_org):
    with override_switch(ELASTIC, active=True):
        with mock.patch('wiki.cloudsearch.cloudsearch_client.CloudSearchClient._call_api') as m:
            m.return_value = []
            groups.group_org_42.user_set.add(wiki_users.thasonic)
            client.login(wiki_users.thasonic)

            query = 'i luv psychonauts'
            test_org.is_indexed = True
            test_org.save()

            response = client.post('/_api/frontend/.search', data={'query': query, 'page_id': 1, 'page_size': 5})

            assert 200 == response.status_code
            assert m.called

            data = m.call_args[1]['json']
            assert data['acl']['group_ids'] == [groups.group_org_42.id]
            assert data['acl']['org_id'] == organizations.org_42.dir_id
            assert data['acl']['user']['uid'] == wiki_users.thasonic.get_uid()
            assert data['acl']['user']['cloud_uid'] == wiki_users.thasonic.cloud_uid

            assert data['query'] == query

            query = 'hello'
            page_id = 2
            response = client.get(f'/_api/frontend/.search?q={query}&page_id={page_id}')
            assert 200 == response.status_code

            data = m.call_args[1]['json']
            assert data['query'] == query
            assert data['page_id'] == str(page_id)

            search_data = {
                'query': query,
                'page_id': 1,
                'page_size': 5,
                'filters': {'type': {'equal': 'file'}, 'authors.uid': {'contains': ['1']}, 'created_at': {'from': 10}},
            }

            response = client.post('/_api/frontend/.search', data=search_data)

            assert 200 == response.status_code
            data = m.call_args[1]['json']

            for el in search_data:
                if el == 'filters':
                    for inner_el in search_data[el]:
                        assert OrderedDict(data[el][inner_el]) == OrderedDict(search_data[el][inner_el])
                else:
                    assert data[el] == search_data[el]

            search_data = {
                'query': query,
                'page_id': 1,
                'page_size': 5,
                'filters': {'authors.cloud_uid': {'contains': [None]}},
            }

            response = client.post('/_api/frontend/.search', data=search_data)

            assert 400 <= response.status_code < 500

            search_data = {
                'query': query,
                'page_id': 1,
                'page_size': 5,
                'filters': {'slug': {'prefix': 'lololo'}, 'modified_at': {'from': 123113}},
            }

            response = client.post('/_api/frontend/.search', data=search_data)
            assert 200 == response.status_code


@pytest.mark.django_db
def test_header(client, wiki_users, groups, test_org):
    with override_switch(ELASTIC, active=True):
        with mock.patch('wiki.cloudsearch.cloudsearch_client.CloudSearchClient._call_api') as m:
            m.return_value = []
            groups.group_org_42.user_set.add(wiki_users.thasonic)
            client.login(wiki_users.thasonic)

            query = 'hello'
            test_org.is_indexed = True
            test_org.save()

            page_id = 2

            header = {
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9',
                'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 YaBrowser/21.5.3.753 (beta) Yowser/2.5 Safari/537.36',
            }
            response = client.get(f'/_api/frontend/.search?q={query}&page_id={page_id}', **header)
            assert 200 == response.status_code
