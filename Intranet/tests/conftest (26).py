import pytest

from staff.dismissal.tests.factories import ClearanceChitTemplateFactory
from staff.lib.testing import (
    UserFactory,
    StaffFactory,
    DepartmentFactory,
    OfficeFactory,
    GroupFactory,
    RouteFactory,
)


class TestData(object):
    factory = None
    yandex = None
    morozov = None
    staff = None
    hr_user = None
    hr = None
    # всегда должен быть шаблон department:*, office: *
    tpl = None


@pytest.fixture
def groups_and_user():
    RouteFactory(
        target='@',
        department=None,
        office=None,
        staff=None,
        transport_id='email',
        params='{}'
    )
    RouteFactory(
        target='DISMISSAL_ANY',
        department=None,
        office=None,
        staff=None,
        transport_id='email',
        params='{}'
    )
    RouteFactory(
        target='DISMISSAL_BROADCAST',
        department=None,
        office=None,
        staff=None,
        transport_id='email',
        params='{}'
    )
    RouteFactory(
        target='DISMISSAL_MAIL_FILES',
        department=None,
        office=None,
        staff=None,
        transport_id='email',
        params='{}'
    )
    GroupFactory(
        name='Яндекс',
        parent=None,
        department=None,
        service_id=None
    )

    result = TestData()

    result.yandex = DepartmentFactory(name='yandex', parent=None)
    result.morozov = OfficeFactory(name='morozov')

    result.staff = StaffFactory(login='mouse', department=result.yandex,
                                office=result.morozov)
    result.hr_user = UserFactory()
    result.hr = StaffFactory(login='hr', user=result.hr_user)

    # всегда должен быть шаблон department:*, office: *
    result.tpl = ClearanceChitTemplateFactory(office=None, department=None)

    RouteFactory(target='DISMISSAL_HR',
                 department=None, office=None, staff=None,
                 transport_id='email', params='{}')
    RouteFactory(target='@',
                 department=None, office=None, staff=None,
                 transport_id='email', params='{}')
    RouteFactory(target='DISMISSAL_BROADCAST',
                 department=None, office=None, staff=None,
                 transport_id='email', params='{}')

    return result
