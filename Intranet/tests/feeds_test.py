import factory
import pytest

from staff.lenta.feeds import fetch_action_fields
from staff.lib.testing import OrganizationFactory, StaffFactory


class LentaLogFactory(factory.DjangoModelFactory):
    staff = factory.SubFactory(StaffFactory)

    class Meta:
        model = 'lenta.LentaLog'


@pytest.mark.django_db
def test_fetch_action_fields_organization_change_without_old_organization():
    value = LentaLogFactory(
        action='o',
        organization_old=None,
        organization_new=OrganizationFactory(),
    )

    result = fetch_action_fields(value)

    assert result == {
        'organization_new': {
            'id': value.organization_new.id,
            'name': {
                'ru': value.organization_new.name,
                'en': value.organization_new.name_en,
            },
            'filter_id': value.organization_new.filter_id,
        },
        'organization_old': {'id': None, 'name': {'en': None, 'ru': None}},
    }
