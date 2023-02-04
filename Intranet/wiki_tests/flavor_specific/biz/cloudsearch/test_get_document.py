import pytest

from mock import patch
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import ELASTIC
from wiki.cloudsearch.utils import find_by_search_uuid, render_document_for_indexation
from wiki.utils.wfaas.client import WfaasClient

pytestmark = [pytest.mark.django_db]


@patch.object(WfaasClient, 'raw_to_html')
def test_get_document__page(test_raw_to_html, client, wiki_users, groups, page_cluster):
    with override_switch(ELASTIC, active=True):
        formatted_body = 'kek'
        test_raw_to_html.return_value = formatted_body

        client.login(wiki_users.thasonic)
        page = page_cluster['root']
        page.body = 'lol'
        page.save()

        response = client.post('/_api/frontend/.get_document', data={'uuid': page.get_search_uuid()})

        assert 200 == response.status_code

        real_data = render_document_for_indexation(find_by_search_uuid(page.get_search_uuid()))
        real_data['document']['body'] = formatted_body
        assert real_data == response.json()['data']


@patch.object(WfaasClient, 'yfm_to_html')
def test_get_document__wysiwyg(test_yfm_to_html, client, wiki_users, groups, test_wysiwyg):
    with override_switch(ELASTIC, active=True):
        formatted_body = 'kek'
        test_yfm_to_html.return_value = formatted_body

        client.login(wiki_users.thasonic)

        response = client.post('/_api/frontend/.get_document', data={'uuid': test_wysiwyg.get_search_uuid()})

        assert 200 == response.status_code

        real_data = render_document_for_indexation(find_by_search_uuid(test_wysiwyg.get_search_uuid()))
        real_data['document']['body'] = formatted_body
        assert real_data == response.json()['data']
