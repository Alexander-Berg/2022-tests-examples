import pytest
from django.contrib.auth import get_user_model

from intranet.femida.src.utils.queryset import queryset_iterator
from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db

User = get_user_model()


def test_queryset_iterator():
    users = f.UserFactory.create_batch(10)
    user_ids = [u.id for u in users]
    qs = (
        User.objects
        .filter(id__in=user_ids)
        .order_by('id')
    )

    qs_iterator = queryset_iterator(qs, from_pk=min(user_ids), chunk_size=3)
    iterated_user_ids = [u.id for u in qs_iterator]
    assert user_ids == iterated_user_ids
