import pytest

from unittest.mock import patch, MagicMock, PropertyMock

from django.db.models import QuerySet
from startrek_client.exceptions import NotFound, Forbidden, StartrekError

from ok.api.scenarios.forms import (
    ScenarioCreateValidateForm,
    ScenarioCreateForm,
    ScenarioUpdateForm,
)
from ok.api.scenarios.serializers import ScenarioUpdateFormSerializer

from tests import factories as f
from tests.utils.mock import AnyOrderList


pytestmark = pytest.mark.django_db


scenario_create_required_fields: dict = {
    'slug': 'vacation',
    'name': 'Vacation approvement',
    'tracker_queue': 'OK',
}


def _check_form_field(form, field_name, expected):
    if form.is_valid():
        result = form.cleaned_data[field_name]
        if isinstance(result, QuerySet):
            result = AnyOrderList(result)
    else:
        first_error = form.errors['errors'][field_name][0]
        result = first_error['code']
    assert result == expected


def test_scenario_create_validate_form_no_required_fields():
    form = ScenarioCreateValidateForm({})
    assert form.is_valid(), form.errors


@patch('ok.api.scenarios.validators.get_user_tracker_client', MagicMock())
@pytest.mark.parametrize('fields, is_valid', (
    (['slug', 'name', 'tracker_queue'], True),
    (['slug'], False),
    (['name'], False),
    (['tracker_queue'], False),
))
def test_scenario_create_form_required_fields(fields, is_valid):
    data = {k: v for k, v in scenario_create_required_fields.items() if k in fields}
    form = ScenarioCreateForm(data)
    assert form.is_valid() is is_valid


@patch('ok.api.scenarios.validators.get_user_tracker_client', MagicMock())
@pytest.mark.parametrize('slug, is_valid, expected', (
    ('kebab-slug', True, 'kebab-slug'),
    ('snake_slug', True, 'snake_slug'),
    ('camelSlug', True, 'camelslug'),
    (' strip  ', True, 'strip'),
    ('slug-with-DIGITS-123', True, 'slug-with-digits-123'),
    ('existing-slug', False, 'already_exists'),
    ('inval!d-slug', False, 'invalid'),
    ('too-long-slug' * 20, False, 'max_length'),
))
def test_scenario_create_form_slug(slug, is_valid, expected):
    f.ScenarioFactory(slug='existing-slug')
    data = scenario_create_required_fields | {'slug': slug}
    form = ScenarioCreateForm(data)
    assert form.is_valid() is is_valid
    _check_form_field(form, 'slug', expected)


@patch('ok.api.scenarios.validators.get_user_tracker_client', MagicMock())
@pytest.mark.parametrize('responsible_groups, is_valid, error', (
    (['existing-group'], True, None),
    (['existing-group', 'unknown-group'], False, 'invalid_choice'),
    (['unknown-group'], False, 'invalid_choice'),
))
def test_scenario_create_form_responsible_groups(responsible_groups, is_valid, error):
    group = f.GroupFactory(url='existing-group')
    data = scenario_create_required_fields | {'responsible_groups': responsible_groups}
    form = ScenarioCreateForm(data)
    assert form.is_valid() is is_valid
    expected = [group] if is_valid else error
    _check_form_field(form, 'responsible_groups', expected)


@pytest.mark.parametrize('tracker_queue, exception, is_valid, expected', (
    ('OK', None, True, 'OK'),
    ('low', None, True, 'LOW'),
    ('TOOLONGQUEUENAME', None, False, 'max_length'),
    ('IN-VALID', None, False, 'invalid'),
    ('UNKNOWN', NotFound, False, 'queue_does_not_exist'),
    ('REMOVED', Forbidden, False, 'queue_does_not_exist'),
    ('OK', StartrekError, False, 'startrek_error'),
))
@patch('ok.api.scenarios.validators.get_user_tracker_client')
def test_scenario_create_form_tracker_queue(get_tracker_client, tracker_queue, exception,
                                            is_valid, expected):
    if exception:
        get_tracker_client().queues.__getitem__.side_effect = exception(MagicMock())
    data = scenario_create_required_fields | {'tracker_queue': tracker_queue}
    form = ScenarioCreateForm(data)
    assert form.is_valid() is is_valid
    _check_form_field(form, 'tracker_queue', expected)


@patch('ok.api.scenarios.validators.get_user_tracker_client')
def test_scenario_create_form_tracker_queue_permission_denied(get_tracker_client):
    """
    Проверяет, когда Forbidden возникает именно на получение permissions
    """
    mocked_queue = get_tracker_client().queues.__getitem__()
    mocked_queue.lead.id = 'queue-owner'
    type(mocked_queue).permissions = PropertyMock(side_effect=Forbidden(MagicMock()))

    data = scenario_create_required_fields | {'tracker_queue': 'OK'}
    form = ScenarioCreateForm(data)

    assert not form.is_valid()
    assert form.errors['errors']['tracker_queue'][0] == {
        'code': 'permission_denied',
        'params': {'owner': 'queue-owner'},
    }


@patch('ok.api.scenarios.validators.get_user_tracker_client', MagicMock())
def test_scenario_create_form_approvement_data():
    approvement_data = {
        'text': 'Approvement',
        'users': ['denis-an', 'ktussh', 'qazaq'],
    }
    data = scenario_create_required_fields | {'approvement_data': approvement_data}
    form = ScenarioCreateForm(data)
    assert form.is_valid()
    _check_form_field(form, 'approvement_data', approvement_data)


@pytest.mark.parametrize('is_editable', (True, False))
def test_scenario_update_form_slug(is_editable):
    scenario = f.ScenarioFactory()
    scenario_data = ScenarioUpdateFormSerializer(scenario).data
    data = dict(scenario_data, slug='new_slug')
    form = ScenarioUpdateForm(
        data=data,
        initial=scenario_data,
        base_initial={'is_editable': is_editable},
    )
    assert form.is_valid()
    _check_form_field(form, 'slug', scenario.slug)


@pytest.mark.parametrize('is_editable, expected_name', (
    (True, 'new_name'),
    (False, 'name'),
))
def test_scenario_update_form_name(is_editable, expected_name):
    scenario = f.ScenarioFactory(name='name')
    scenario_data = ScenarioUpdateFormSerializer(scenario).data
    data = dict(scenario_data, name='new_name')
    form = ScenarioUpdateForm(
        data=data,
        initial=scenario_data,
        base_initial={'is_editable': is_editable},
    )
    assert form.is_valid()
    _check_form_field(form, 'name', expected_name)
