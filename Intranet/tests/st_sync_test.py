from datetime import date
import pytest

from mock import MagicMock

from staff.lib.sync_tools.st_translation_sync import FieldSyncParams, StartrekFieldsSync
from staff.oebs.models import Geography, Review


@pytest.mark.django_db
def test_sync_translations_creation_for_geography(settings):
    # given
    geography = Geography.objects.create(
        code='ru',
        start_date=date.today(),
        end_date=date.today(),
        description='test descr',
    )
    params = FieldSyncParams(
        qs=Geography.objects.all(),
        key_prefix='oebs',
        key_field='code',
        translation_ru_field='description',
        translation_en_field='description',
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
    geography.refresh_from_db()
    assert geography.st_translation_id == 42


@pytest.mark.django_db
def test_sync_translations_creation_for_review(settings):
    # given
    review = Review.objects.create(
        scheme_id=1,
        name='ru',
        start_date=date.today(),
        end_date=date.today(),
        description='test descr',
    )
    params = FieldSyncParams(
        qs=Review.objects.all(),
        key_prefix='oebs',
        key_field='scheme_id',
        translation_ru_field='name',
        translation_en_field='name',
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
    review.refresh_from_db()
    assert review.st_translation_id == 42
