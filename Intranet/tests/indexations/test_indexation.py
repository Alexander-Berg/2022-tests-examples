from intranet.search.core.swarm.tasks import reindex
from intranet.search.core.models import Indexation
from intranet.search.core.storages.indexation import IndexationStorage
from intranet.search.tests.helpers import models_helpers as mh

import pytest
from unittest.mock import patch

pytestmark = pytest.mark.django_db


def check_indexation_stats(indexation_id):
    indexation_storage = IndexationStorage()
    stage_stats = indexation_storage.get_combined_stage_stats([indexation_id]).data
    STAGES = ('setup', 'walk', 'fetch', 'load', 'create', 'store')
    for stage in STAGES:
        assert stage_stats[stage]['fail']['total'] == 0
    assert stage_stats['setup']['done']['total'] == 1
    assert stage_stats['walk']['done']['total'] == 1


@patch('intranet.search.core.sources.invite.invite.Source.do_walk')
@patch('intranet.search.core.swarm.storage.SaasDocumentStorage.send_request')
@patch('intranet.search.core.storages.cache.CacheStorage.clear')
def test_reindex_invite(*args):
    mh.Service(name='invite')
    reindex('invite', noqueue=True, nolock=True)
    index = Indexation.objects.get(search='invite')
    assert index.status == Indexation.STATUS_DONE

    check_indexation_stats(index.id)


@patch('intranet.search.core.sources.plan.base.BaseSource.do_walk')
@patch('intranet.search.core.sources.utils.get_metrix_data', lambda x: [])
@patch('intranet.search.core.swarm.storage.SaasDocumentStorage.send_request')
@patch('intranet.search.core.storages.cache.CacheStorage.clear')
def test_reindex_service(*args):
    mh.Service(name='plan')
    reindex('plan', noqueue=True, nolock=True, index='services')
    index = Indexation.objects.get(search='plan')
    assert index.status == Indexation.STATUS_DONE

    check_indexation_stats(index.id)
