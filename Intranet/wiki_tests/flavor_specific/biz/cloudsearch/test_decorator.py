import pytest
from wiki.cloudsearch.cloudsearch_client import CLOUD_SEARCH_CLIENT
from waffle.testutils import override_switch
from wiki.api_core.waffle_switches import ELASTIC
import mock
from wiki.cloudsearch.indexable_model_mixin import IndexableModelMixin


@pytest.mark.django_db
def test_flag_and_no_exception():
    with mock.patch('wiki.cloudsearch.cloudsearch_client.CloudSearchClient.get_sqs_client') as m:

        m.return_value = None
        CLOUD_SEARCH_CLIENT._send_message('{lol}')
        assert m.call_count == 1

        with override_switch(ELASTIC, active=False):
            CLOUD_SEARCH_CLIENT.on_model_upsert('empty')

        with override_switch(ELASTIC, active=True):
            CLOUD_SEARCH_CLIENT.on_model_upsert(IndexableModelMixin())

        assert m.call_count == 2
