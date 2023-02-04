import pytest
from waffle.testutils import override_switch
from wiki.api_core.waffle_switches import ELASTIC, RETRIEVER_SEND_TYPE_AND_PAYLOAD
from wiki.cloudsearch.cloudsearch_client import CLOUD_SEARCH_CLIENT
from wiki.integrations.ms.wiki_to_retriever_sqs import RETRIEVER_SQS_CLIENT

from mock import patch
from wiki.files.models import File
from wiki.pages.models import Page
from model_mommy import mommy


@patch.object(CLOUD_SEARCH_CLIENT, '_send_message')
@pytest.mark.django_db
def test_on_model_upsert(test_send_message, page_cluster):
    with override_switch(ELASTIC, active=True):
        page = page_cluster['root']
        CLOUD_SEARCH_CLIENT.on_model_upsert(page)
        assert test_send_message.called


@patch.object(CLOUD_SEARCH_CLIENT, '_send_message')
@pytest.mark.django_db
def test_on_model_delete(test_send_message, page_cluster):
    with override_switch(ELASTIC, active=True):
        page = page_cluster['root']
        CLOUD_SEARCH_CLIENT.on_model_delete(page)
        assert test_send_message.call_count == 1


@patch.object(CLOUD_SEARCH_CLIENT, '_send_message')
@pytest.mark.django_db
def test_on_model_update_acl(test_send_message, page_cluster, wiki_users):
    with override_switch(ELASTIC, active=True):
        page = page_cluster['root']
        mommy.make(File, page=page, user=wiki_users.thasonic)

        CLOUD_SEARCH_CLIENT.on_model_update_acl(page)
        assert test_send_message.call_count == 1 + len(
            Page.objects.filter(supertag__startswith=page.supertag, org=page.get_model_org())
        )


@patch.object(RETRIEVER_SQS_CLIENT, '_send_message')
@pytest.mark.django_db
def test_retriever_sqs_client_delete_send_message(test_send_message, cloud_page_cluster):
    page: Page = cloud_page_cluster['root/b']
    with override_switch(RETRIEVER_SEND_TYPE_AND_PAYLOAD, active=True):
        RETRIEVER_SQS_CLIENT.on_model_delete(page)

    data = {'type': 'delete', 'payload': page.cloudpage.get_cloud_src().embedding.dict() | {'supertag': page.supertag}}
    test_send_message.assert_called_with(data=data)
