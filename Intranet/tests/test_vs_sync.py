from datetime import date
from typing import Iterator, List

import pytest

from staff.syncs.models import StartrekVs, StartrekUmbrella
from staff.syncs.umbrellas.sync import UmbrellasSync
from staff.syncs.umbrellas.umbrellas_fetcher import VS, UmbrellasFetcher, Umbrella


class UmbrellasFetcherMock(UmbrellasFetcher):
    def __init__(self, mocked_result: List[VS]) -> None:
        super().__init__()
        self.mocked_result = mocked_result

    def get_umbrellas_from_st(self) -> Iterator[VS]:
        yield from self.mocked_result


@pytest.mark.django_db
def test_vs_creation():
    # given
    vs1 = VS(issue_key='GOALZ-1', abc_service_id=2, name='VS-1', umbrellas=[], deadline=date.today())
    vs2 = VS(issue_key='GOALZ-2', abc_service_id=3, name='VS-2', umbrellas=[], deadline=date.today())
    fetcher = UmbrellasFetcherMock([vs1, vs2])
    sync = UmbrellasSync(fetcher)

    # when
    sync.sync()

    # then
    assert StartrekVs.objects.count() == 2
    vs1_model = StartrekVs.objects.get(issue_key=vs1.issue_key)
    assert vs1_model.name == vs1.name
    assert vs1_model.abc_service_id == vs1.abc_service_id

    vs2_model = StartrekVs.objects.get(issue_key=vs2.issue_key)
    assert vs2_model.name == vs2.name
    assert vs2_model.abc_service_id == vs2.abc_service_id


@pytest.mark.django_db
def test_vs_update():
    # given
    StartrekVs.objects.create(issue_key='GOALZ-1', abc_service_id=20, name='VSS-1', deadline=date.today())
    StartrekVs.objects.create(issue_key='GOALZ-2', abc_service_id=30, name='VSS-2', deadline=date.today())
    vs1 = VS(issue_key='GOALZ-1', abc_service_id=2, name='VS-1', umbrellas=[], deadline=date.today())
    vs2 = VS(issue_key='GOALZ-2', abc_service_id=3, name='VS-2', umbrellas=[], deadline=date.today())
    fetcher = UmbrellasFetcherMock([vs1, vs2])
    sync = UmbrellasSync(fetcher)

    # when
    sync.sync()

    # then
    assert StartrekVs.objects.count() == 2
    vs1_model = StartrekVs.objects.get(issue_key=vs1.issue_key)
    assert vs1_model.name == vs1.name
    assert vs1_model.abc_service_id == vs1.abc_service_id

    vs2_model = StartrekVs.objects.get(issue_key=vs2.issue_key)
    assert vs2_model.name == vs2.name
    assert vs2_model.abc_service_id == vs2.abc_service_id


@pytest.mark.django_db
def test_umbrella_creation():
    # given
    umbrella1 = Umbrella('GOALZ-3', 'umbrella1')
    umbrella2 = Umbrella('GOALZ-4', 'umbrella2')
    umbrella3 = Umbrella('GOALZ-5', 'umbrella3')
    vs1 = VS(
        issue_key='GOALZ-1',
        abc_service_id=2,
        name='VS-1',
        umbrellas=[umbrella1],
        deadline=date.today(),
    )
    vs2 = VS(
        issue_key='GOALZ-2',
        abc_service_id=3,
        name='VS-2',
        umbrellas=[umbrella2, umbrella3],
        deadline=date.today(),
    )
    fetcher = UmbrellasFetcherMock([vs1, vs2])
    sync = UmbrellasSync(fetcher)

    # when
    sync.sync()

    # then
    assert StartrekUmbrella.objects.count() == 3
    umbrella1_model = StartrekUmbrella.objects.get(issue_key=umbrella1.issue_key)
    assert umbrella1_model.name == umbrella1.name
    assert umbrella1_model.vs.issue_key == vs1.issue_key

    umbrella2_model = StartrekUmbrella.objects.get(issue_key=umbrella2.issue_key)
    assert umbrella2_model.name == umbrella2.name
    assert umbrella2_model.vs.issue_key == vs2.issue_key

    umbrella3_model = StartrekUmbrella.objects.get(issue_key=umbrella3.issue_key)
    assert umbrella3_model.name == umbrella3.name
    assert umbrella3_model.vs.issue_key == vs2.issue_key


@pytest.mark.django_db
def test_umbrella_update():
    # given
    vs1_model = StartrekVs.objects.create(issue_key='GOALZ-1', abc_service_id=1, name='VS-1', deadline=date.today())
    vs2_model = StartrekVs.objects.create(issue_key='GOALZ-2', abc_service_id=3, name='VS-2', deadline=date.today())
    StartrekUmbrella.objects.create(issue_key='GOALZ-3', name='oldname', vs_id=vs1_model.id)
    StartrekUmbrella.objects.create(issue_key='GOALZ-4', name='oldname', vs_id=vs2_model.id)

    umbrella1 = Umbrella('GOALZ-3', 'umbrella1')
    umbrella2 = Umbrella('GOALZ-4', 'umbrella2')
    vs1 = VS(issue_key='GOALZ-1', abc_service_id=1, name='VS-1', umbrellas=[], deadline=date.today())
    vs2 = VS(
        issue_key='GOALZ-2',
        abc_service_id=3,
        name='VS-2',
        umbrellas=[umbrella1, umbrella2],
        deadline=date.today(),
    )
    fetcher = UmbrellasFetcherMock([vs1, vs2])
    sync = UmbrellasSync(fetcher)

    # when
    sync.sync()

    # then
    assert StartrekUmbrella.objects.count() == 2
    umbrella1_model = StartrekUmbrella.objects.get(issue_key=umbrella1.issue_key)
    assert umbrella1_model.name == umbrella1.name
    assert umbrella1_model.vs.issue_key == vs2.issue_key

    umbrella2_model = StartrekUmbrella.objects.get(issue_key=umbrella2.issue_key)
    assert umbrella2_model.name == umbrella2.name
    assert umbrella2_model.vs.issue_key == vs2.issue_key
