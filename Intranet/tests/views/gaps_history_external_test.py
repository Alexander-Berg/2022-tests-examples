import json

import pytest

from django.core.urlresolvers import reverse

from staff.gap.views.gaps_history_view import gaps_history


def get_gaps_history(rf, observer, login, gap):
    request = rf.get(
        reverse('gap:gaps-history'),
        {
            'login': login,
            'workflow': 'absence',
            'date_from': gap['date_from'],
            'date_to':  gap['date_to'],
        }
    )
    request.user = observer.user
    response = gaps_history(request)
    try:
        js = json.loads(response.content)
    except Exception:
        js = None
    return response.status_code, js


@pytest.mark.django_db
def test_not_empty_external_self_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    gap = external_gap_case['inner_gap']

    code, resp = get_gaps_history(rf, observer, observer.login, gap)

    assert code == 403


@pytest.mark.django_db
def test_not_empty_external_self_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    gap = external_gap_case['inner_gap']
    external_gap_case['open_for_self']()

    code, resp = get_gaps_history(rf, observer, observer.login, gap)

    assert code == 200
    assert len(resp['gaps']) == 1


@pytest.mark.django_db
def test_403_external_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    gap = external_gap_case['inner_gap']
    login = external_gap_case['inner_person'].login

    code, resp = get_gaps_history(rf, observer, login, gap)

    assert code == 403


@pytest.mark.django_db
def test_not_empty_external_other_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    gap = external_gap_case['inner_gap']
    login = external_gap_case['inner_person'].login
    external_gap_case['create_permission']()

    code, resp = get_gaps_history(rf, observer, login, gap)

    assert code == 200
    assert len(resp['gaps']) == 1
