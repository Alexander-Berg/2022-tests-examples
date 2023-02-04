import json
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory, get_random_date
from staff.person.models import MemorialProfile

from staff.person_profile.views import meta_view


@pytest.mark.django_db
def test_meta_view(rf):
    user = StaffFactory()
    observer = StaffFactory()
    request = rf.get(reverse('profile:meta', kwargs={'login': user.login}))
    request.user = observer.user
    setattr(request, 'service_is_readonly', False)

    response = meta_view.meta(request, user.login)
    data = json.loads(response.content)
    print(data)

    assert data['target']['first_name'] == user.first_name
    assert data['observer']['is_owner'] is False

    links = data.get('links')

    assert 'feedback' in links
    assert 'yamb' in links
    assert 'slack' in links

    for private_field in ('add_external_login', 'upravlyator', 'finances', 'fincab'):
        assert private_field not in links

    assert settings.FEEDBACK_HOST in links['feedback']['url']
    assert str(user.id) in links['feedback']['url']

    assert '//q.yandex-team.ru/' in links['yamb']['url']
    assert user.login in links['yamb']['url']

    assert 'slack' in links['slack']['url']
    assert user.login in links['slack']['url']


@pytest.mark.django_db
def test_meta_view_memorial(rf):
    user = StaffFactory()
    MemorialProfile.objects.create(intranet_status=1, person=user, death_date=get_random_date())
    observer = StaffFactory()
    request = rf.get(reverse('profile:meta', kwargs={'login': user.login}))
    request.user = observer.user
    setattr(request, 'service_is_readonly', False)

    response = meta_view.meta(request, user.login)
    data = json.loads(response.content)
    print(data)

    assert data['target']['first_name'] == user.first_name
    assert data['observer']['is_owner'] is False
