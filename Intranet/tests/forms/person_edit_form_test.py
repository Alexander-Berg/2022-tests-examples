import pytest

from staff.lib.testing import StaffFactory, verify_forms_error_code
from staff.oebs.tests.factories import JobFactory

from staff.proposal.forms.person import PersonEditForm


@pytest.mark.django_db()
def test_person_edit_form_doesnt_accept_form_with_missing_sections():
    job = JobFactory(name='test')
    person = StaffFactory()

    form_data = {
        'action_id': '0',
        'login': person.login,
        'sections': ['position', 'grade'],
        'position': {
            'new_position': job.name,
            'position_legal': job.code,
        },
        'comment': 'comment',
    }

    target = PersonEditForm(data=form_data)

    assert not target.is_valid()
    verify_forms_error_code(target.errors, 'grade', 'required')


@pytest.mark.django_db()
def test_person_edit_form_for_dismissed_user():
    job = JobFactory(name='test')
    person = StaffFactory(is_dismissed=True)

    form_data = {
        'action_id': '0',
        'login': person.login,
        'sections': ['position'],
        'position': {
            'new_position': job.name,
            'position_legal': job.code,
        },
        'comment': 'comment',
    }

    target = PersonEditForm(data=form_data)

    assert not target.is_valid()
    verify_forms_error_code(target.errors, 'login', 'person_is_dismissed')
