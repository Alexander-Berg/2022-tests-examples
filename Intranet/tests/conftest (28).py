import pytest

from staff.groups.models import Group
from staff.lib.testing import GroupFactory, DepartmentFactory, StaffFactory


def create_groups(test_data):
    test_data.root = GroupFactory(
        parent=None,
        code='__wiki__', url='__wiki__',
        native_lang='ru', intranet_status=1
    )
    test_data.first_lvl = GroupFactory(
        parent=test_data.root,
        code='first', url='first',
        intranet_status=1
    )
    test_data.second_lvl = GroupFactory(
        parent=test_data.first_lvl,
        code='second', url='second',
        intranet_status=1
    )
    test_data.third_lvl = GroupFactory(
        parent=test_data.second_lvl,
        code='third', url='third',
        intranet_status=1
    )
    test_data.options = GroupFactory(
        parent=None,
        id=Group.OPTION,
        code='evil', url='evil',
        intranet_status=1,
    )

    Group.tree.rebuild()
    test_data.refetch()


def create_departments(test_data):
    test_data.yandex = DepartmentFactory(url='yandex')
    test_data.ext = DepartmentFactory(url='ext')


def create_users(test_data):
    test_data.mouse = StaffFactory(login='mouse')
    test_data.squirrel = StaffFactory(login='squirrel')
    test_data.eye = StaffFactory(login='eye')


@pytest.fixture
def test_data(db, settings):
    result = TestData()
    create_groups(result)
    create_departments(result)
    create_users(result)

    settings.YANDEX_DEPARTMENT_ID = result.yandex.id

    return result


class TestData(object):
    root = None
    first_lvl = None
    second_lvl = None
    third_lvl = None
    options = None
    yandex = None
    ext = None
    mouse = None
    squirrel = None
    eye = None

    def refetch(self):
        def get_from_db(group):
            return Group.objects.get(id=group.id)

        self.root = get_from_db(self.root)
        self.first_lvl = get_from_db(self.first_lvl)
        self.second_lvl = get_from_db(self.second_lvl)
        self.third_lvl = get_from_db(self.third_lvl)
