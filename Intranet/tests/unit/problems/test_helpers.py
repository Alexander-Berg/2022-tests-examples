import pytest

from django.conf import settings

from intranet.femida.src.problems.helpers import has_access_to_problems
from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('department_id,is_intern,expected', (
    (settings.YANDEX_DEPARTMENT_ID, False, True),
    (settings.YANDEX_DEPARTMENT_ID, True, False),
    (settings.OUTSTAFF_DEPARTMENT_ID, False, False),
))
def test_has_access_to_problems(department_id, is_intern, expected):
    user = f.UserFactory(
        department__ancestors=[department_id],
        is_intern=is_intern,
    )
    assert has_access_to_problems(user) is expected


def test_has_access_to_problems_recruiter():
    user = f.create_recruiter(
        department__ancestors=[settings.EXTERNAL_DEPARTMENT_ID],
        is_intern=True,
    )
    assert has_access_to_problems(user)
