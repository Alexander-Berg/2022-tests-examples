import pytest

from mock import MagicMock

from staff.lib.sync_tools.st_translation_sync import FieldSyncParams, StartrekFieldsSync
from staff.lib.testing import OrganizationFactory

from staff.person.models import Organization


@pytest.mark.django_db()
def test_sync_translations_for_organization(settings):
    # given
    org = OrganizationFactory(st_translation_id=None)

    params = FieldSyncParams(
        qs=Organization.objects.filter(intranet_status=1),
        key_prefix='oebs',
        key_field='id',
        translation_ru_field='name',
        translation_en_field='name_en',
        startrek_field=100500,
    )

    translation_mock = MagicMock(id=42)
    translations_mock = MagicMock()
    translations_mock.create = MagicMock(return_value=translation_mock)
    settings.ROBOT_STAFF_OAUTH_TOKEN = 'some'
    st_sync = StartrekFieldsSync(params)
    st_sync._translations = translations_mock

    # when
    st_sync._update_st_translations()

    # then
    org.refresh_from_db()
    assert org.st_translation_id == 42
