import re

import pytest
from django.core.urlresolvers import reverse

from staff.audit.factory import create_log, RENDER_AS_IS
from staff.lib.testing import StaffFactory
from staff.audit.views import blob


@pytest.fixture
def get_blob(rf, tester):
    def getter(log_id):
        request = rf.get(reverse('audit:audit-blob', kwargs={'pk': log_id}))
        request.user = tester.user
        response = blob(request, log_id)
        return str(response.content, errors='ignore')
    return getter


@pytest.mark.django_db
def test_any_action(get_blob):
    user = StaffFactory().user
    obj = {'id': 1, 'state': 2}
    create_log(obj, user, 'some_action', obj['id'])
    obj['state'] = 5
    cur_log = create_log(obj, user, 'some_action', obj['id'])

    blob_html = get_blob(cur_log.id)
    to_check_regexes = (
        r'-.*state.*2',   # previous state, starts with 'minus' sign
        r'\+.*state.*5',  # current state, start with 'plus' sign
        r'id.*1',
    )
    failed = [it for it in to_check_regexes if re.search(it, blob_html) is None]
    assert not failed, failed


@pytest.mark.django_db
def test_with_prev_log(get_blob):
    user = StaffFactory().user
    obj = {'id': 1, 'state': 2}
    prev_log = create_log(obj, user, 'some_action', obj['id'])
    obj['state'] = 5
    cur_log = create_log(obj, user, 'some_action', obj['id'])

    blob_html = get_blob(cur_log.id)
    regex = r'href="/audit/{}".*prev'.format(prev_log.id)
    assert re.search(regex, blob_html), regex


@pytest.mark.django_db
def test_with_next_log(get_blob):
    user = StaffFactory().user
    obj = {'id': 1, 'state': 2}
    cur_log = create_log(obj, user, 'some_action', obj['id'])
    obj['state'] = 10
    next_log = create_log(obj, user, 'some_action', obj['id'])

    blob_html = get_blob(cur_log.id)
    regex = r'href="/audit/{}".*next'.format(next_log.id)
    assert re.search(regex, blob_html), regex


@pytest.mark.parametrize('action', RENDER_AS_IS)
@pytest.mark.django_db
def test_action_render_as_is(get_blob, action):
    user = StaffFactory().user
    obj = {'id': 1, 'state': 2}
    cur_log = create_log(obj, user, action, obj['id'])

    blob_html = get_blob(cur_log.id)
    to_check_regexes = (
        r'{}.*{}'.format(field, value)
        for field, value in obj.items()
    )
    failed = [it for it in to_check_regexes if re.search(it, blob_html) is None]
    assert not failed, failed
