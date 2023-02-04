import pytest
import json
from waffle.models import Switch

from django.core.urlresolvers import reverse

from staff.person_profile.views.cards.staff_cards import StaffCards


@pytest.mark.parametrize('rkn, position', [(True, False), (False, True)])
def test_hiding_position_of_dismissed_from_staff_card(company, rf, mocked_mongo, rkn, position):
    working = company.persons['dep11-person']
    dismissed = company.persons['yam2-person']
    dismissed.is_dismissed = True
    dismissed.save()

    Switch(name='rkn_mode', active=rkn).save()
    view = StaffCards.as_view()

    observer = company.persons['yandex-chief']
    observer.tz = 'Europe/Moscow'
    observer.save()

    request = rf.get(reverse('cards:staff_cards'))

    request.service_is_readonly = False
    request.user = observer.user
    request.META['HTTP_REFERER'] = 'https://any.yandex-team.ru/my'
    request.GET = {
        'format': 'json',
        working.login: 1,
        dismissed.login: 1,
    }

    response = view(request)
    content = json.loads(response.content)

    assert content[working.login]['position'] == working.position
    assert (dismissed.login in content) == (not rkn)
