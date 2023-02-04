import pytest

from django.core.urlresolvers import reverse

from staff.gap.edit_views.gap_state_views import cancel_gap, confirm_gap


URL_NAME_TO_FUNC = {
    'cancel-gap': cancel_gap,
    'confirm-gap': confirm_gap,
}


def post_change_state(rf, url_name, observer, gap_id):
    request = rf.post(
        reverse('gap:{}'.format(url_name), kwargs={'gap_id': gap_id}),
        content_type='application/json',
    )
    request.user = observer.user
    response = URL_NAME_TO_FUNC[url_name](request, gap_id)
    return response.status_code


@pytest.mark.django_db
@pytest.mark.parametrize('url_to_check', URL_NAME_TO_FUNC)
def test_access_external_self_without_perm(external_gap_case, rf, url_to_check):
    observer = external_gap_case['external_person']
    gap = external_gap_case['external_gap']

    code = post_change_state(rf, url_to_check, observer, gap['id'])

    assert code == 403


@pytest.mark.django_db
@pytest.mark.parametrize('url_to_check', URL_NAME_TO_FUNC)
def test_access_external_self_if_perm(external_gap_case, rf, url_to_check):
    observer = external_gap_case['external_person']
    gap = external_gap_case['external_gap']
    external_gap_case['open_for_self']()

    code = post_change_state(rf, url_to_check, observer, gap['id'])

    assert code == 200


@pytest.mark.django_db
@pytest.mark.parametrize('url_to_check', URL_NAME_TO_FUNC)
def test_403_external_without_perm(external_gap_case, rf, url_to_check):
    observer = external_gap_case['external_person']
    gap = external_gap_case['inner_gap']

    code = post_change_state(rf, url_to_check, observer, gap['id'])

    assert code == 403


@pytest.mark.django_db
@pytest.mark.parametrize('url_to_check', URL_NAME_TO_FUNC)
def test_external_can_edit_other_with_perm(external_gap_case, rf, url_to_check):
    observer = external_gap_case['external_person']
    gap = external_gap_case['inner_gap']
    external_gap_case['create_permission']()

    code = post_change_state(rf, url_to_check, observer, gap['id'])

    assert code == 200
