import pytest

from mock import patch
from waffle.testutils import override_switch
from wiki.api_core.waffle_switches import RETRIEVER_SEND_TYPE_AND_PAYLOAD
from wiki.integrations.ms.wiki_to_retriever_sqs import RETRIEVER_SQS_CLIENT


@patch.object(RETRIEVER_SQS_CLIENT, '_send_message')
@pytest.mark.django_db
def test_send_message_to_retriever_when_delete_cloud_page(test_send_message, api_url, client, cloud_page_cluster):
    client.login('thasonic')
    page = cloud_page_cluster['root/b']

    with override_switch(RETRIEVER_SEND_TYPE_AND_PAYLOAD, active=True):
        response = client.delete(f'{api_url}/{page.supertag}')

    assert response.status_code == 200

    page.refresh_from_db()  # для обновления page.supertag
    data = {'type': 'delete', 'payload': page.cloudpage.get_cloud_src().embedding.dict() | {'supertag': page.supertag}}
    test_send_message.assert_called_with(data=data)


@patch.object(RETRIEVER_SQS_CLIENT, '_send_message')
@pytest.mark.django_db
def test_no_send_message_to_retriever_when_delete_no_cloud_page(test_send_message, api_url, client, page_cluster):
    client.login('thasonic')
    page = page_cluster['root/a']

    with override_switch(RETRIEVER_SEND_TYPE_AND_PAYLOAD, active=True):
        response = client.delete(f'{api_url}/{page.supertag}')

    assert response.status_code == 200
    test_send_message.assert_not_called()
