import freezegun
import pytest
from django.utils import timezone

from plan.metrics.idm import get_not_interactive_intervals
from common import factories

pytestmark = [pytest.mark.django_db]


@freezegun.freeze_time()
def test_service_is_interactive_after():
    now = timezone.now()
    f = factories.ServiceCreateRequestFactory
    f(interactive_at=now - timezone.timedelta(days=8))
    f(completed_at=now - timezone.timedelta(days=2), interactive_at=now - timezone.timedelta(days=1))
    f(completed_at=now - timezone.timedelta(days=3))

    assert sorted(get_not_interactive_intervals()) == [
        timezone.timedelta(days=1),
        timezone.timedelta(days=3),
    ]
