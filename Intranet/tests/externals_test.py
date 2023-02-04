from functools import partial
import pytest

from staff.person.models import Staff
from staff.groups.models import Group, GroupMembership

from staff.lib.testing import StaffFactory, DepartmentFactory, GroupFactory
from staff.groups.tasks import recalculate_externals_count_for_group, RecalculateGroupsCounts
from staff.person.effects.base import actualize_affiliation

UNREAL_EXTERNALS_COUNT = 100500


class TestData(object):
    yandex = None
    ext = None
    yandexoid = None
    evil_spy = None
    devs = None
    tools = None

    def refetch(self):
        def get_from_db(group):
            return Group.objects.get(id=group.id)

        self.devs = get_from_db(self.devs)
        self.tools = get_from_db(self.tools)
        self.yandexoid = Staff.objects.get(id=self.yandexoid.id)
        self.evil_spy = Staff.objects.get(id=self.evil_spy.id)


@pytest.fixture
def test_data(db, settings):
    result = TestData()

    result.yandex = DepartmentFactory(url='yandex', name='Yandex', parent=None, from_staff_id=0)
    result.ext = DepartmentFactory(url='ext', name='External', parent=None, from_staff_id=0)

    settings.YANDEX_DEPARTMENT_ID = result.yandex.id

    result.yandexoid = StaffFactory(
        department=result.yandex,
        table=None,
        organization=None,
        from_staff_id=0,
        login='ya'
    )

    actualize_affiliation(result.yandexoid)
    result.yandexoid.save()

    result.evil_spy = StaffFactory(
        department=result.ext,
        table=None,
        organization=None,
        from_staff_id=0,
        login='spy'
    )

    actualize_affiliation(result.evil_spy)
    result.evil_spy.save()

    result.devs = GroupFactory(
        service_id=None,
        department=None,
        parent=None,
        url='devs',
        name='devs',
        externals_count=UNREAL_EXTERNALS_COUNT,
        intranet_status=1
    )

    result.tools = GroupFactory(
        service_id=None,
        department=None,
        parent=result.devs,
        url='tools',
        name='tools',
        externals_count=UNREAL_EXTERNALS_COUNT,
        intranet_status=1
    )
    Group.tree.rebuild()
    result.refetch()
    return result


# в тестах будем использовать синхронную версия таски
recalculate = partial(recalculate_externals_count_for_group,
                      delay_parent_task=False)


# region Тесты функции пересчета числа внешних сотрудников.

def test_initial(test_data):
    """Один раз убедимся, что в начале заведомо кривое число."""
    assert test_data.devs.externals_count == UNREAL_EXTERNALS_COUNT
    assert test_data.tools.externals_count == UNREAL_EXTERNALS_COUNT


def test_empty_root(test_data):
    recalculate(group_id=test_data.devs.id)
    test_data.refetch()

    assert test_data.devs.externals_count == 0


def test_empty_second_level(test_data):
    recalculate(group_id=test_data.tools.id)
    test_data.refetch()

    assert test_data.tools.externals_count == 0
    assert test_data.devs.externals_count == 0


def test_only_internal_in_root(test_data):
    GroupMembership.objects.create(group=test_data.devs, staff=test_data.yandexoid)

    recalculate(group_id=test_data.devs.id)
    test_data.refetch()

    assert test_data.devs.externals_count == 0


def test_only_internal_in_second_lvl(test_data):
    GroupMembership.objects.create(group=test_data.devs, staff=test_data.yandexoid)
    GroupMembership.objects.create(group=test_data.tools, staff=test_data.yandexoid)

    recalculate(group_id=test_data.tools.id)
    test_data.refetch()

    assert test_data.tools.externals_count == 0
    assert test_data.devs.externals_count == 0


def test_only_external_in_root(test_data):
    GroupMembership.objects.create(group=test_data.devs, staff=test_data.evil_spy)

    recalculate(group_id=test_data.devs.id)
    test_data.refetch()

    assert test_data.devs.externals_count == 1


def test_only_external_in_second_lvl(test_data):
    GroupMembership.objects.create(group=test_data.tools, staff=test_data.evil_spy)

    recalculate(group_id=test_data.tools.id)
    test_data.refetch()

    assert test_data.tools.externals_count == 1
    assert test_data.devs.externals_count == 1


def test_external_in_both_without_duplication(test_data):
    GroupMembership.objects.create(group=test_data.devs, staff=test_data.evil_spy)
    GroupMembership.objects.create(group=test_data.tools, staff=test_data.evil_spy)

    recalculate(group_id=test_data.tools.id)
    test_data.refetch()

    assert test_data.tools.externals_count == 1
    assert test_data.devs.externals_count == 1


# endregion

# region Тесты функции пересчета внешних сотрудников для всех групп

def test_recalculate_with_external_in_child(test_data):
    GroupMembership.objects.create(group=test_data.tools, staff=test_data.evil_spy)

    RecalculateGroupsCounts()
    test_data.refetch()

    assert test_data.tools.externals_count == 1
    assert test_data.devs.externals_count == 1

# endregion
