from datetime import datetime

from intranet.search.tests.helpers.indexations_helpers import create_stats
from intranet.search.core.models import Revision, Indexation, IndexationStats, Organization
from intranet.search.core.storages import StageStatusStorage
from intranet.search.core.storages.indexation import IndexationStatsWrapper, IndexationStorage, sum_lists

import pytest


pytestmark = pytest.mark.django_db


@pytest.fixture
def stats():
    return StageStatusStorage.get_default_stats()


@pytest.fixture
def storage():
    return IndexationStorage()


@pytest.fixture
def organization():
    return Organization.objects.create(directory_id=123, directory_revision=123, name='Test Org', label='testorg')


@pytest.fixture
def revision(organization):
    return Revision.objects.create(search='wiki', organization=organization)


@pytest.fixture
def indexation(revision):
    return Indexation.objects.create(revision=revision, start_time=datetime.now())


@pytest.mark.parametrize(
    "s1,t1,s2,t2,window,result",
    [
        ([5, 6], 6, [4, 5], 6, 2, [9, 11]),
        ([5, 6], 6, [4, 5], 5, 2, [10, 6]),
        (range(1, 6), 5, range(1, 7), 6, 7, [2, 4, 6, 8, 10, 6]),
        (range(1, 6), 5, [2, 3, 4, 5, 6], 6, 5, [4, 6, 8, 10, 6]),
        ([2, 3], 3, [5, 6], 6, 2, [5, 6]),
        ([5, 6], 6, [2, 3], 3, 2, [5, 6]),
        ([1, 2, 3], 3, [1], 1, 5, [2, 2, 3]),
        ([2, 3], 3, [1], 1, 2, [2, 3]),
        ([2, 3], 2, [1], 1, 2, [3, 3]),
    ]
)
def test_func(s1, t1, s2, t2, window, result):
    assert sum_lists(s1, t1, s2, t2, window) == result


def test_stats_wrapper_update(stats):
    stats.update({('fetch', 'new'): 10,
                  ('store', 'done'): 5})

    wrapper = IndexationStatsWrapper()

    wrapper.update(stats)

    assert wrapper.data['fetch']['new'] == {'total': 10, 'counts': [10]}
    assert wrapper.data['store']['done'] == {'total': 5, 'counts': [5]}

    stats.update({('fetch', 'new'): 7,
                  ('store', 'done'): 3})

    wrapper.update(stats)

    assert wrapper.data['fetch']['new'] == {'total': 7, 'counts': [10, 7]}
    assert wrapper.data['store']['done'] == {'total': 8, 'counts': [5, 3]}


def test_stats_wrapper_merge(stats):
    wrapper1 = IndexationStatsWrapper(tick=1)

    stats.update({('fetch', 'new'): 10,
                  ('store', 'done'): 5})

    wrapper1.update(stats)

    wrapper2 = IndexationStatsWrapper(tick=2)

    stats.update({('fetch', 'new'): 7,
                  ('store', 'done'): 3})
    wrapper2.update(stats)

    stats.update({('fetch', 'new'): 5,
                  ('store', 'done'): 8})
    wrapper2.update(stats)

    wrapper1.merge_with(wrapper2)

    assert wrapper1.tick == wrapper2.tick

    assert wrapper1.data['walk']['new'] == {'total': 0, 'counts': [0, 0]}
    assert wrapper1.data['fetch']['new'] == {'total': 15, 'counts': [17, 5]}
    assert wrapper1.data['store']['done'] == {'total': 16, 'counts': [8, 8]}


def test_update_host_stage_stats(storage, indexation, stats):
    def update_stats(hostname):
        storage.update_host_stage_stats(indexation.id, indexation.revision.id, hostname, stats)

    update_stats('foobar-a.com')
    update_stats('foobar-b.com')
    update_stats('foobar-c.com')

    assert IndexationStats.objects.count() == 3

    update_stats('foobar-a.com')

    assert IndexationStats.objects.count() == 3


def test_get_stage_stats(storage, indexation):
    create_stats(indexation, 'foobar-a.com', {('walk', 'done'): 1})
    create_stats(indexation, 'foobar-b.com', {('fetch', 'done'): 1})
    create_stats(indexation, 'foobar-c.com', {('store', 'done'): 1})

    wrapper = storage.get_stage_stats(indexation.id, indexation.revision.id)

    assert wrapper.data['walk']['done']['total'] == 1
    assert wrapper.data['fetch']['done']['total'] == 1
    assert wrapper.data['store']['done']['total'] == 1
    assert wrapper.data['create']['done']['total'] == 0


def test_archive_stage_stats(storage, indexation):
    host_a = create_stats(indexation, 'foobar-a.com', {('walk', 'done'): 1, ('store', 'done'): 2})
    host_b = create_stats(indexation, 'foobar-b.com', {('fetch', 'fail'): 10, ('store', 'done'): 3})
    assert indexation.indexationstats_set.count() == 2
    storage.archive_stage_stats(indexation.id, indexation.revision_id)

    # все записи смержились в одну
    assert indexation.indexationstats_set.count() == 1
    stat = indexation.indexationstats_set.get()
    assert stat.hostname == ''
    assert stat.tick == max(host_a.tick, host_b.tick)
    for stage, data in stat.stats.items():
        for status, status_data in data.items():
            # данные по промежуточным количествам пустые
            assert status_data['counts'] == []
            # в total - сумма по всем хостам
            total = host_a.stats[stage][status]['total'] + host_b.stats[stage][status]['total']
            assert status_data['total'] == total
