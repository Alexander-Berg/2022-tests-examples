import pytest
import sform

from staff.lib.testing import StaffFactory

from staff.person.models import Staff


@pytest.mark.django_db()
def test_suggest_field():
    person = StaffFactory(first_name='Clark', last_name='Kent')

    field = sform.SuggestField(
        queryset=Staff.objects.all(),
        label_fields=['first_name', 'last_name'],
    )
    data = field.data_as_dict(
        prefix='', name='test', value=person.id, state=sform.NORMAL,
        base_initial={},
        base_data={}
    )
    assert data['caption'] == 'Clark Kent'

    field = sform.SuggestField(
        queryset=Staff.objects.all(),
        label_fields='first_name',
    )
    data = field.data_as_dict(
        prefix='', name='test', value=person.id, state=sform.NORMAL,
        base_initial={},
        base_data={}
    )
    assert data['caption'] == 'Clark'


@pytest.mark.django_db()
def test_multiple_suggest_field():
    person = StaffFactory(first_name='Clark', last_name='Kent')

    field = sform.MultipleSuggestField(
        queryset=Staff.objects.all(),
        label_fields={'caption': ['first_name', 'last_name']},
    )
    data = field.data_as_dict(
        prefix='', name='test', value=[person.id], state=sform.NORMAL,
        base_initial=[],
        base_data=[],
    )
    assert data['label_fields'] == {person.id: {'caption': 'Clark Kent'}}

    field = sform.MultipleSuggestField(
        queryset=Staff.objects.all(),
        label_fields={'caption': ['first_name']},
    )
    data = field.data_as_dict(
        prefix='', name='test', value=[person.id], state=sform.NORMAL,
        base_initial=[],
        base_data=[],
    )
    assert data['label_fields'] == {person.id: {'caption': 'Clark'}}
