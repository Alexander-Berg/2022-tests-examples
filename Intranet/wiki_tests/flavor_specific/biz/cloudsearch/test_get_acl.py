import pytest
from waffle.testutils import override_switch
from wiki.api_core.waffle_switches import ELASTIC
from wiki.cloudsearch.utils import calculate_acl
from django.conf import settings


@pytest.mark.django_db
def test_get_document(client, wiki_users, groups, page_cluster):
    with override_switch(ELASTIC, active=True):
        groups.group_org_42.user_set.add(wiki_users.thasonic)
        client.login(wiki_users.thasonic).use_tvm2(settings.CLOUDSEARCH_TVM_CLIENT_ID)
        page = page_cluster['root']

        response = client.post('/_api/frontend/.get_acl', data={'uuid': page.get_search_uuid()})

        assert 200 == response.status_code
        assert calculate_acl(page.get_acl_subject()) == response.json()['data']
