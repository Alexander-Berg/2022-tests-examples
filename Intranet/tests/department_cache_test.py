import pytest

from staff.lenta.objects import DepartmentCache


@pytest.mark.django_db
def test_department_cache(departments_and_offices):
    cache = DepartmentCache()
    cache.populate()

    assert cache._cache == {
        departments_and_offices.yandex.id: (departments_and_offices.yandex.id, None, None),
        departments_and_offices.subyandex.id: (departments_and_offices.yandex.id, None, None),
        departments_and_offices.infra.id: (departments_and_offices.yandex.id, departments_and_offices.infra.id, None),
        departments_and_offices.intranet.id: (
            departments_and_offices.yandex.id,
            departments_and_offices.infra.id,
            departments_and_offices.intranet.id,
        ),
        departments_and_offices.matrixnet.id: (
            departments_and_offices.yandex.id,
            departments_and_offices.infra.id,
            departments_and_offices.intranet.id,
        ),
        departments_and_offices.staffnet.id: (
            departments_and_offices.yandex.id,
            departments_and_offices.infra.id,
            departments_and_offices.intranet.id,
        ),
        departments_and_offices.extra.id: (departments_and_offices.yandex.id, departments_and_offices.extra.id, None),
        departments_and_offices.shmyandex.id: (departments_and_offices.shmyandex.id, None, None),
        departments_and_offices.humans.id: (
            departments_and_offices.shmyandex.id,
            departments_and_offices.humans.id,
            None,
        ),
        departments_and_offices.zombies.id: (
            departments_and_offices.shmyandex.id,
            departments_and_offices.humans.id,
            departments_and_offices.zombies.id,
        ),
        departments_and_offices.robots.id: (
            departments_and_offices.shmyandex.id,
            departments_and_offices.humans.id,
            departments_and_offices.robots.id,
        ),
        departments_and_offices.xenomorphs.id: (
            departments_and_offices.shmyandex.id,
            departments_and_offices.xenomorphs.id,
            None,
        ),
        departments_and_offices.plants.id: (
            departments_and_offices.shmyandex.id,
            departments_and_offices.xenomorphs.id,
            departments_and_offices.plants.id,
        ),
    }
