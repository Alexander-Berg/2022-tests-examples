import re

import pytest
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory
from staff.audit.factory import create_log
from staff.audit.views import index


@pytest.mark.django_db
def test_with_next_log(rf, tester):
    user = StaffFactory().user
    logs = [
        create_log({'id': i, 'state': i}, user, 'some_action', i)
        for i in range(3)
    ]

    request = rf.get(reverse('audit:audit-index'))
    request.user = tester.user
    response = index(request)
    html = str(response.content, errors='ignore')

    to_check_regexes = (r'href=.*{}'.format(log.id) for log in logs)
    failed = [it for it in to_check_regexes if re.search(it, html) is None]
    assert not failed, failed
