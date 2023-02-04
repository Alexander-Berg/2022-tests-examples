import pytest
from django.contrib.auth.models import Permission

from staff.lib.testing import StaffFactory


@pytest.fixture
def tester():
    tester = StaffFactory(login='tester')
    tester.user.user_permissions.add(
        Permission.objects.get(codename='can_see_audit')
    )
    return tester
