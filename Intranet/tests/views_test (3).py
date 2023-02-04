import json
import pytest

from django.core.urlresolvers import reverse
from staff.dismissal.tests.factories import DismissalFactory
from staff.dismissal.tests.factories import ClearanceChitTemplateFactory, CheckPointTemplateFactory

from staff.dismissal.models import (
    INITIATOR,
    IMPRESSION,
    DELETE_FROM_SEARCH,
    DISMISSAL_STATUS,
    REHIRE_RECOMMENDATION,
)


@pytest.mark.django_db()
def test_create_get(superuser_client):
    cp = CheckPointTemplateFactory()

    c = ClearanceChitTemplateFactory(office=None, department=None)
    c.checkpoints.add(cp)
    c.save()

    url = reverse('dismissal-api:create', kwargs={'login': 'tester'})
    response = superuser_client.get(url)

    result = json.loads(response.content)

    assert 'structure' in result


@pytest.mark.django_db
def test_get_dismissal(superuser_client, groups_and_user):
    dismissal = DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr,
        forward_correspondence_to=groups_and_user.hr,
        give_files_to=groups_and_user.hr,
        status=DISMISSAL_STATUS.IN_PROGRESS,
        quit_datetime_real=None,
        initiator=INITIATOR.COMPANY,
        impression=IMPRESSION.NEUTRAL,
        delete_from_search=DELETE_FROM_SEARCH.IMMEDIATELY,
        rehire_recommendation=REHIRE_RECOMMENDATION.RECOMMEND,
    )

    url = reverse('dismissal-api:get_dismissal', kwargs={'dismissal_id': dismissal.id})
    response = superuser_client.get(url)
    result = json.loads(response.content)
    assert 'data' in result
    assert 'structure' in result
