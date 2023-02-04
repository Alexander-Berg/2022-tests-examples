import pytest
from django.utils import timezone
from freezegun import freeze_time

from idm.core.constants.action import ACTION
from idm.core.models import Action
from idm.core.management.commands.idm_count_reviews import count_reviews_for_year

pytestmark = [pytest.mark.django_db]


@pytest.mark.django_db
def test_count_reviews():

    current_date = timezone.now().date()

    for days_delta in range(0, 500, 100):
        with freeze_time(current_date - timezone.timedelta(days=days_delta)):
            Action.objects.create(action=ACTION.REVIEW_REREQUEST)

    reviews_by_day = count_reviews_for_year()
    for days_delta in range(365):
        actions_per_day = 0
        if days_delta % 100 == 0:
            actions_per_day = 1
        date = current_date - timezone.timedelta(days=days_delta)
        assert reviews_by_day[str(date)] == actions_per_day
